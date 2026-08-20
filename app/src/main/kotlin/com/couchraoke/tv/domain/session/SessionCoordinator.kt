package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.control.AdmissionDecision
import com.couchraoke.tv.domain.control.HandshakeValidator
import com.couchraoke.tv.domain.control.JoinCodeMatcher
import com.couchraoke.tv.domain.control.RefusalReason
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.session.model.AssetPort
import com.couchraoke.tv.domain.session.model.ConnectedDevice
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.JoinCode
import com.couchraoke.tv.domain.session.model.SessionId
import com.couchraoke.tv.domain.session.model.SessionSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The single host-owned session surface (contracts/domain-api.md). Owns the roster, the
 * phase, the connection allocator, and both observable flows.
 *
 * This phase (T022) implements construction, [snapshot], [connectedDevices], [events],
 * [requestPhase] (delegating to [phaseMachine]) and [end]. T035 completes `admit`'s accept
 * path and T042 completes [onDisconnected]; [authorize] and `admit`'s refusal branches
 * (`invalid_token`, `protocol_mismatch`, `invalid_message`, `session_full`) remain stubbed
 * for US2's T050, which is also expected to emit `SessionEvent.Reconnected` (T058) for a
 * roster reclaim.
 *
 * [connectedDevices] and `snapshot.connected` are derived from [SessionRoster.connected] by
 * [refreshConnectedProjection], called after every roster mutation that can change it: a
 * successful [admit] and a successful [onDisconnected].
 */
class SessionCoordinator(
    private val roster: SessionRoster,
    private val phaseMachine: GamePhaseMachine,
    private val connectionIds: ConnectionIdAllocator,
    @Suppress("UnusedPrivateProperty") private val validator: HandshakeValidator,
    private val codeMatcher: JoinCodeMatcher,
    private val sessionId: SessionId,
    private val joinCode: JoinCode,
) {

    private val mutableSnapshot = MutableStateFlow(
        SessionSnapshot(
            sessionId = sessionId,
            joinCode = joinCode,
            lifecycle = SessionLifecycle.Open,
            phase = phaseMachine.current,
            roster = roster.entries,
            connected = emptyList(),
        ),
    )
    val snapshot: StateFlow<SessionSnapshot> = mutableSnapshot.asStateFlow()

    private val mutableConnectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = mutableConnectedDevices.asStateFlow()

    /**
     * `extraBufferCapacity` is required, not cosmetic: [admit] and [onDisconnected] are both
     * non-`suspend` (contracts/domain-api.md), so they can only publish via
     * [MutableSharedFlow.tryEmit]. Verified empirically, `tryEmit` on a zero-capacity shared
     * flow (`replay = 0`, `extraBufferCapacity = 0`) returns `false` even when a collector is
     * actively suspended on [events] -- unlike suspending `emit`, `tryEmit` never hands a
     * value directly to a waiting collector, only into a buffer slot, so with no slots every
     * `tryEmit` silently drops. [EVENT_BUFFER_CAPACITY] gives enough headroom for every
     * device a default-capacity [SessionRoster] can hold to have a pending event in flight at
     * once.
     */
    private val mutableEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    val events: SharedFlow<SessionEvent> = mutableEvents.asSharedFlow()

    /**
     * Checks the token presented on the connection query string against this session's
     * [joinCode] (FR-009).
     *
     * Acceptance is [AdmissionDecision.Authorized], which carries nothing: no
     * [com.couchraoke.tv.domain.session.model.ConnectionId] exists yet, because the
     * identifier is minted by [admit] and only for a connection that actually introduces
     * itself, so that a connection which authorizes and then goes quiet holds nothing
     * (FR-017). See spec.md Observation 19.
     */
    fun authorize(token: String?): AdmissionDecision =
        if (codeMatcher.matches(joinCode, token)) {
            AdmissionDecision.Authorized
        } else {
            AdmissionDecision.Refused(
                reason = RefusalReason.INVALID_TOKEN,
                message = "The join code did not match the one shown on the TV.",
            )
        }

    /**
     * Completes only the accept path (T035): a new device is allocated a fresh
     * [ConnectionId], admitted into [roster], projected into [connectedDevices] /
     * `snapshot.connected`, and announced via [SessionEvent.Connected]. `admit` receives an
     * already-parsed [Hello] -- [HandshakeValidator] validates the *raw* wire frame before
     * this is ever called (contracts/domain-api.md's `HelloValidation.Valid(hello)`), so the
     * `protocol_mismatch`/`invalid_message` refusals it guards cannot occur here; they and
     * `session_full` are left as US2's T050 to complete below. `Reclaimed` (T057/T058) is
     * likewise unreachable until [SessionRoster.admit] implements reclaim.
     */
    fun admit(hello: Hello): AdmissionDecision {
        val connectionId = connectionIds.next()
        return when (
            val admission = roster.admit(
                deviceId = DeviceId(hello.clientId),
                displayName = hello.deviceName,
                appVersion = hello.appVersion,
                assetPort = AssetPort(hello.httpPort),
                connectionId = connectionId,
            )
        ) {
            is RosterAdmission.Admitted -> {
                refreshConnectedProjection()
                mutableEvents.tryEmit(SessionEvent.Connected(admission.entry.deviceId, connectionId))
                AdmissionDecision.Admitted(connectionId)
            }

            is RosterAdmission.Reclaimed -> TODO(
                "SessionCoordinator.admit's Reconnected emission for a reclaimed device is " +
                    "completed by T058 (US3): admission=$admission",
            )

            RosterAdmission.AtCapacity -> TODO(
                "SessionCoordinator.admit's session_full refusal is completed by T050 (US2)",
            )
        }
    }

    /**
     * Completes FR-022/FR-023 (T042): [SessionRoster.detach] removes [deviceId] from the
     * roster's live connections and this method refreshes [connectedDevices] /
     * `snapshot.connected` before returning -- both are synchronous state updates with no
     * suspension point, so the removal is visible to any [admit] call evaluated afterwards.
     * The roster entry itself survives with its connection cleared, retaining its capacity
     * slot (FR-024). A `detach` that reports `false` (unknown device) leaves the projection
     * untouched and emits nothing.
     */
    fun onDisconnected(deviceId: DeviceId, connectionId: ConnectionId) {
        if (roster.detach(deviceId, connectionId)) {
            refreshConnectedProjection()
            mutableEvents.tryEmit(SessionEvent.Disconnected(deviceId, connectionId))
        }
    }

    /** Re-derives [connectedDevices] and `snapshot.connected` from [roster]'s current state. */
    private fun refreshConnectedProjection() {
        val connected = roster.connected.map { entry ->
            ConnectedDevice(
                deviceId = entry.deviceId,
                displayName = entry.displayName,
                appVersion = entry.appVersion,
                assetPort = entry.assetPort,
                connectionId = requireNotNull(entry.connection) {
                    "SessionRoster.connected must only report entries with a live connection"
                },
            )
        }
        mutableConnectedDevices.value = connected
        mutableSnapshot.update { it.copy(roster = roster.entries, connected = connected) }
    }

    /**
     * Delegates to [phaseMachine]. A rejection leaves [snapshot]'s phase untouched, matching
     * FR-026: the machine's own [GamePhaseMachine.current] is already left unchanged, and
     * [snapshot] is only ever updated on [PhaseTransitionResult.Accepted].
     */
    fun requestPhase(target: GamePhase): PhaseTransitionResult {
        val result = phaseMachine.transition(target)
        if (result is PhaseTransitionResult.Accepted) {
            mutableSnapshot.update { it.copy(phase = phaseMachine.current) }
        }
        return result
    }

    fun end() {
        mutableSnapshot.update { it.copy(lifecycle = SessionLifecycle.Ended) }
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 16
    }
}
