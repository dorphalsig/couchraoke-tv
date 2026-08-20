package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.control.AdmissionDecision
import com.couchraoke.tv.domain.control.HandshakeValidator
import com.couchraoke.tv.domain.control.JoinCodeMatcher
import com.couchraoke.tv.domain.control.model.Hello
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
 * [requestPhase] (delegating to [phaseMachine]) and [end]. [authorize] and [admit] are
 * stubbed — US2's T050 completes `authorize`, and US1's T035 (accept path) plus T050
 * (refusal branches) complete `admit`. [onDisconnected] is declared only; T042 owns its
 * FR-022/FR-023 ordering guarantee.
 *
 * `roster.connected` is always empty while [SessionRoster] itself is forward-declared
 * (this unit's known-defect workaround), so [connectedDevices] and `snapshot.connected`
 * start empty rather than deriving from it — there is nothing yet to derive. T035/T042
 * are expected to wire that derivation once the roster is real.
 */
class SessionCoordinator(
    private val roster: SessionRoster,
    private val phaseMachine: GamePhaseMachine,
    private val connectionIds: ConnectionIdAllocator,
    private val validator: HandshakeValidator,
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

    private val mutableEvents = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = mutableEvents.asSharedFlow()

    /** Stubbed for this phase; US2's T050 implements the `invalid_token` refusal (FR-009). */
    fun authorize(token: String?): AdmissionDecision =
        TODO("SessionCoordinator.authorize is completed by US2 (T050): token=$token codeMatcher=$codeMatcher")

    /** Stubbed for this phase; US1's T035 (accept) and US2's T050 (refusals) complete it. */
    fun admit(hello: Hello): AdmissionDecision = TODO(
        "SessionCoordinator.admit is completed by US1/US2 (T035, T050): hello=$hello " +
            "validator=$validator connectionIds=$connectionIds",
    )

    /** Declared for the binding signature; T042 owns the FR-022/FR-023 ordering guarantee. */
    fun onDisconnected(deviceId: DeviceId, connectionId: ConnectionId): Unit = TODO(
        "SessionCoordinator.onDisconnected is completed by T042: deviceId=$deviceId connectionId=$connectionId",
    )

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
}
