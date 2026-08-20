package com.couchraoke.tv.domain.session

import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.domain.control.AdmissionDecision
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.HandshakeValidator
import com.couchraoke.tv.domain.control.JoinCodeMatcher
import com.couchraoke.tv.domain.control.RefusalReason
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.JoinCode
import com.couchraoke.tv.domain.session.model.SessionId
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address

/**
 * T024: asserts session identity (FR-001) — the [com.couchraoke.tv.domain.session.model.SessionId]
 * handed to [SessionCoordinator] is non-empty, and many sessions minted through
 * [SessionComponent] never repeat one. T035 extends this file to cover `admit`'s accept
 * path, T042 to cover `onDisconnected`'s FR-022/FR-023 ordering guarantee, and T050 to
 * cover `admit`'s `session_full` refusal; T058 extends it further for US3's reclaim. See
 * plan.md's feature-level completion gate.
 *
 * This phase (T022) also implements construction, both `StateFlow`s, `events`,
 * `requestPhase` and `end()` for real, so the tests below cover that genuinely-implemented
 * behaviour. `authorize`'s `invalid_token` refusal and `admit`'s `protocol_mismatch` /
 * `invalid_message` refusals are covered in `SessionControlConnectionHandlerTest` instead,
 * since both require a raw wire frame -- `authorize` takes the token directly, and
 * `HandshakeValidator` (not `SessionCoordinator`) rejects a malformed frame before `admit`
 * is ever reached.
 */
class SessionCoordinatorTest {

    @Test(timeout = 30_000)
    fun sessionIdHandedToCoordinatorIsNonEmpty() {
        val coordinator = newComponent().createCoordinator()

        assertTrue(
            "SessionId must be non-empty (FR-001)",
            coordinator.snapshot.value.sessionId.value.isNotEmpty(),
        )
    }

    @Test(timeout = 30_000)
    fun manySessionsThroughSessionComponentYieldNoRepeatSessionId() {
        val component = newComponent()

        val sessionIds = (1..SESSION_SAMPLE_SIZE).map { component.createCoordinator().snapshot.value.sessionId }

        // Guards the uniqueness assertion below against a vacuous pass: if construction
        // silently produced fewer sessions than requested, this fails loudly first.
        assertEquals(
            "expected $SESSION_SAMPLE_SIZE sessions to be constructed",
            SESSION_SAMPLE_SIZE,
            sessionIds.size,
        )
        assertEquals(
            "SessionComponent must never mint a repeat SessionId across sessions (FR-001)",
            sessionIds.size,
            sessionIds.toSet().size,
        )
    }

    @Test(timeout = 30_000)
    fun requestPhaseAcceptsAValidTransitionAndUpdatesTheSnapshot() {
        val coordinator = newComponent().createCoordinator()

        val result = coordinator.requestPhase(GamePhase.Preparing)

        assertEquals(PhaseTransitionResult.Accepted(GamePhase.Open, GamePhase.Preparing), result)
        assertEquals(GamePhase.Preparing, coordinator.snapshot.value.phase)
    }

    @Test(timeout = 30_000)
    fun requestPhaseRejectsAnInvalidTransitionAndLeavesTheSnapshotUnchanged() {
        val coordinator = newComponent().createCoordinator()

        val result = coordinator.requestPhase(GamePhase.Live)

        assertEquals(PhaseTransitionResult.Rejected(GamePhase.Open, GamePhase.Live), result)
        assertEquals(
            "a rejected transition must leave the snapshot's phase untouched (FR-026)",
            GamePhase.Open,
            coordinator.snapshot.value.phase,
        )
    }

    @Test(timeout = 30_000)
    fun connectedDevicesAndEventsStartEmptyOnConstruction() {
        val coordinator = newComponent().createCoordinator()

        assertEquals(0, coordinator.connectedDevices.value.size)
        assertEquals(0, coordinator.events.replayCache.size)
    }

    @Test(timeout = 30_000)
    fun endMarksTheSessionLifecycleEnded() {
        val coordinator = newComponent().createCoordinator()

        coordinator.end()

        assertEquals(SessionLifecycle.Ended, coordinator.snapshot.value.lifecycle)
    }

    @Test(timeout = 30_000)
    fun admitAcceptsANewDeviceAndReturnsAdmittedWithAFreshConnectionId() {
        val coordinator = newComponent().createCoordinator()

        val decision = coordinator.admit(helloFrom(DEVICE_A))

        val admitted = decision as? AdmissionDecision.Admitted
            ?: error("expected AdmissionDecision.Admitted, was $decision")
        assertEquals(1, admitted.connectionId.value)
    }

    @Test(timeout = 30_000)
    fun admitProjectsTheAdmittedDeviceIntoConnectedDevicesAndTheSnapshot() {
        val coordinator = newComponent().createCoordinator()

        val admitted = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        assertEquals(
            "the newly admitted device must be the sole entry in connectedDevices",
            listOf(DeviceId(DEVICE_A)),
            coordinator.connectedDevices.value.map { it.deviceId },
        )
        assertEquals(admitted.connectionId, coordinator.connectedDevices.value.single().connectionId)
        assertEquals(
            "snapshot.connected must mirror connectedDevices",
            coordinator.connectedDevices.value,
            coordinator.snapshot.value.connected,
        )
        assertEquals(
            "snapshot.roster must carry the durable entry alongside the live projection",
            listOf(DeviceId(DEVICE_A)),
            coordinator.snapshot.value.roster.map { it.deviceId },
        )
    }

    @Test(timeout = 30_000)
    fun admitEmitsAConnectedEventCarryingTheDeviceIdAndConnectionId() = runBlocking {
        val coordinator = newComponent().createCoordinator()

        // The collector is started here, before `admit` runs, matching how a real observer
        // (e.g. a future `JoinViewModel`) is expected to subscribe once and stay subscribed
        // for the session's lifetime -- `events` is a one-shot notification stream, not a
        // replay of history, so a subscriber that only starts collecting *after* `admit` has
        // already returned is not guaranteed (nor meant) to see it.
        // `CoroutineStart.UNDISPATCHED` runs this block immediately on this thread up to its
        // first real suspension point (registering as a subscriber and waiting for a value),
        // so the subscription is guaranteed to be live by the time `admit` is called below.
        val received = async(start = CoroutineStart.UNDISPATCHED) { coordinator.events.first() }

        val admitted = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        val event = withTimeout(5_000) { received.await() }
        assertEquals(SessionEvent.Connected(DeviceId(DEVICE_A), admitted.connectionId), event)
    }

    @Test(timeout = 30_000)
    fun onDisconnectedRemovesTheDeviceFromConnectedBeforeAnySubsequentAdmissionDecisionWhileRetainingItsRosterSlot() {
        // A plain synchronous test, not a coroutine-`runTest`/virtual-time one: `admit` and
        // `onDisconnected` are ordinary (non-`suspend`) functions with no suspension point to
        // schedule around -- every state change below happens on this thread, in call order,
        // before each method returns. The ordering guarantee (FR-022/FR-023) is proven by
        // interleaving a real disconnect with a real subsequent admission and checking the
        // projection after each step, not merely after both have finished.
        val coordinator = newComponent().createCoordinator()
        val admittedA = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        // Guards against vacuity: A must be live before we disconnect it.
        assertEquals(listOf(DeviceId(DEVICE_A)), coordinator.connectedDevices.value.map { it.deviceId })

        coordinator.onDisconnected(DeviceId(DEVICE_A), admittedA.connectionId)

        assertTrue(
            "device A must no longer be projected as connected immediately after onDisconnected returns",
            coordinator.connectedDevices.value.isEmpty(),
        )
        assertTrue(
            "snapshot.connected must mirror connectedDevices immediately after the disconnect",
            coordinator.snapshot.value.connected.isEmpty(),
        )
        assertEquals(
            "the roster entry -- and its capacity slot -- must be retained across a disconnect (FR-024)",
            listOf(DeviceId(DEVICE_A)),
            coordinator.snapshot.value.roster.map { it.deviceId },
        )

        // The ordering guarantee itself: admitting a *different* device evaluates its
        // decision strictly after A's removal was applied. If that removal were somehow not
        // yet visible at this point, B's admission would see A still connected and this
        // would report both devices instead of B alone.
        val admittedB = coordinator.admit(helloFrom(DEVICE_B)) as AdmissionDecision.Admitted

        assertEquals(
            "only device B may be connected -- A's disconnect must already be visible to B's admission decision",
            listOf(DeviceId(DEVICE_B)),
            coordinator.connectedDevices.value.map { it.deviceId },
        )
        assertEquals(
            "device B must be allocated its own fresh ConnectionId, independent of A's former one",
            2,
            admittedB.connectionId.value,
        )
        assertEquals(
            "both roster entries must coexist -- capacity is a slot count, not a live-connection count",
            setOf(DeviceId(DEVICE_A), DeviceId(DEVICE_B)),
            coordinator.snapshot.value.roster.map { it.deviceId }.toSet(),
        )
    }

    @Test(timeout = 30_000)
    fun onDisconnectedForAnUnknownDeviceLeavesConnectedDevicesAndTheSnapshotUnchanged() {
        val coordinator = newComponent().createCoordinator()
        coordinator.admit(helloFrom(DEVICE_A))
        val before = coordinator.connectedDevices.value

        coordinator.onDisconnected(DeviceId(DEVICE_B), ConnectionId(99))

        assertEquals(
            "an onDisconnected call for a device never admitted must be a no-op",
            before,
            coordinator.connectedDevices.value,
        )
    }

    @Test(timeout = 30_000)
    fun admitRefusesAPreviouslyUnseenDeviceWithSessionFullWhenTheRosterIsAtCapacity() {
        // A one-slot roster is enough to prove FR-015 without admitting ten devices first:
        // the check is on `size == capacity`, not on the number ten specifically.
        val coordinator = newCoordinatorWithRoster(SessionRoster(capacity = 1))
        val firstAdmission = coordinator.admit(helloFrom(DEVICE_A))

        // Guards against vacuity: the sole slot must actually be taken by A before B's
        // admission is asserted to be refused for capacity.
        assertTrue(
            "device A must be admitted so the one-slot roster is genuinely at capacity",
            firstAdmission is AdmissionDecision.Admitted,
        )

        val decision = coordinator.admit(helloFrom(DEVICE_B))

        val refused = decision as? AdmissionDecision.Refused
            ?: error("expected AdmissionDecision.Refused for a previously-unseen device at capacity, was $decision")
        assertEquals(RefusalReason.SESSION_FULL, refused.reason)
        assertTrue("the session_full message must be non-empty and human-readable", refused.message.isNotBlank())
        assertEquals(
            "a refused previously-unseen device must never be projected as connected",
            listOf(DeviceId(DEVICE_A)),
            coordinator.connectedDevices.value.map { it.deviceId },
        )
    }

    @Test(timeout = 30_000)
    fun admitReclaimsASupersededConnectionAndTheStaleCloseDoesNotEvictItsReplacement() = runBlocking {
        // Device A's first connection is still live when its second admission arrives --
        // this is the supersession shape of reconnect (FR-020/FR-021/FR-022, acceptance
        // scenario 4), as opposed to the clean-drop-then-reconnect shape covered below.
        val coordinator = newComponent().createCoordinator()
        val admittedFirst = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        // Started before the second admit, UNDISPATCHED, so the subscription is guaranteed
        // live before `admit` tryEmits -- see admitEmitsAConnectedEventCarryingTheDeviceIdAndConnectionId.
        val reconnectEvent = async(start = CoroutineStart.UNDISPATCHED) { coordinator.events.first() }
        val admittedSecond = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted
        val event = withTimeout(5_000) { reconnectEvent.await() }

        assertEquals(
            "a reclaim while the prior connection is still live must supersede it, not grow the roster (FR-020)",
            1,
            coordinator.snapshot.value.roster.size,
        )
        assertEquals(
            "reclaiming a still-connected device must emit Reconnected carrying its displaced connectionId",
            SessionEvent.Reconnected(DeviceId(DEVICE_A), admittedSecond.connectionId, admittedFirst.connectionId),
            event,
        )
        assertEquals(
            "the reclaimed device must be projected into connectedDevices under its fresh connectionId",
            listOf(admittedSecond.connectionId),
            coordinator.connectedDevices.value.map { it.connectionId },
        )

        // The stale close: connection 1's late disconnect arrives after connection 2 has
        // already reclaimed the device. Subscribing before the call, UNDISPATCHED, proves a
        // real absence of emission rather than merely a race won by the assertion.
        val staleCloseEvent = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(STALE_CLOSE_SILENCE_MS) { coordinator.events.first() }
        }
        coordinator.onDisconnected(DeviceId(DEVICE_A), admittedFirst.connectionId)

        assertEquals(
            "the superseded connection's late close must not remove the roster entry (FR-022)",
            1,
            coordinator.snapshot.value.roster.size,
        )
        assertEquals(
            "the superseded connection's late close must not invalidate the replacement connectionId (FR-022)",
            listOf(admittedSecond.connectionId),
            coordinator.connectedDevices.value.map { it.connectionId },
        )
        assertNull(
            "the superseded connection's late close must not emit Disconnected (FR-022)",
            staleCloseEvent.await(),
        )
    }

    @Test(timeout = 30_000)
    fun admitAfterACleanDropEmitsReconnectedWithNoPreviousConnectionAndRestoresTheDevice() = runBlocking {
        // The ordinary reconnect shape: the phone drops, its connection is fully detached
        // first, and only then does it come back (FR-021, FR-023, spec.md Observation 22).
        val coordinator = newComponent().createCoordinator()
        val admittedFirst = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted
        coordinator.onDisconnected(DeviceId(DEVICE_A), admittedFirst.connectionId)

        // Guards against vacuity: the device must genuinely be gone from the connected
        // projection before we assert anything about it coming back.
        assertTrue(
            "device A must be disconnected before its reconnect is exercised",
            coordinator.connectedDevices.value.isEmpty(),
        )

        val reconnectEvent = async(start = CoroutineStart.UNDISPATCHED) { coordinator.events.first() }
        val admittedSecond = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted
        val event = withTimeout(5_000) { reconnectEvent.await() }

        assertEquals(
            "a reconnect after a clean drop has no live connection to supersede, so previous must be null",
            SessionEvent.Reconnected(DeviceId(DEVICE_A), admittedSecond.connectionId, null),
            event,
        )
        assertEquals(
            "the reconnected device must be back in connectedDevices",
            listOf(DeviceId(DEVICE_A)),
            coordinator.connectedDevices.value.map { it.deviceId },
        )
        assertEquals(
            "reclaiming an existing roster entry must not grow the roster",
            1,
            coordinator.snapshot.value.roster.size,
        )
    }

    private fun helloFrom(clientId: String): Hello = Hello(
        type = "hello",
        protocolVersion = 1,
        clientId = clientId,
        deviceName = "$clientId's Phone",
        appVersion = "1.0.0",
        httpPort = 8080,
    )

    private fun newComponent(): SessionComponent = SessionComponent(
        transport = FakeControlTransport,
        announcer = FakeSessionAnnouncer,
        addressProvider = LocalAddressProvider { null },
        multicastLease = FakeMulticastLease,
        joinCodeGenerator = JoinCodeGenerator(),
        clock = { 0L },
    )

    /**
     * Builds a [SessionCoordinator] directly, bypassing [SessionComponent], so [roster]'s
     * capacity can be set below the default 10 -- [SessionComponent.createCoordinator] always
     * constructs its own default-capacity [SessionRoster] with no way to override it.
     */
    private fun newCoordinatorWithRoster(roster: SessionRoster): SessionCoordinator = SessionCoordinator(
        roster = roster,
        phaseMachine = GamePhaseMachine(),
        connectionIds = ConnectionIdAllocator(),
        validator = HandshakeValidator(),
        codeMatcher = JoinCodeMatcher,
        sessionId = SessionId("sess-capacity-test"),
        joinCode = JoinCode(adjective = "brave", noun = "otter"),
    )

    /** A do-nothing [ControlTransport]; T028's `LoopbackJoinGateTest` proves the real one. */
    private object FakeControlTransport : ControlTransport {
        override suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport =
            object : StartedTransport {
                override val boundPort: Int = port
            }

        override suspend fun stop() = Unit
    }

    private object FakeSessionAnnouncer : SessionAnnouncer {
        override suspend fun publish(
            address: Inet4Address,
            instanceName: String,
            port: Int,
            joinCode: String,
            protocolVersion: Int,
        ): AnnouncementHandle = object : AnnouncementHandle {
            override val registeredInstanceName: String = instanceName
        }

        override suspend fun withdraw(handle: AnnouncementHandle) = Unit
    }

    private object FakeMulticastLease : MulticastLease {
        override fun acquire() = Unit
        override fun release() = Unit
    }

    private companion object {
        const val SESSION_SAMPLE_SIZE = 1_000
        const val STALE_CLOSE_SILENCE_MS = 200L
        const val DEVICE_A = "device-aaaa"
        const val DEVICE_B = "device-bbbb"
    }
}
