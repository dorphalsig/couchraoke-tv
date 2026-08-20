package com.couchraoke.tv.domain.session

import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address

/**
 * T024: asserts session identity (FR-001) — the [com.couchraoke.tv.domain.session.model.SessionId]
 * handed to [SessionCoordinator] is non-empty, and many sessions minted through
 * [SessionComponent] never repeat one. Later tasks (T035, T042, T050, T058...) extend this
 * same file; see plan.md's feature-level completion gate.
 *
 * This phase (T022) also implements construction, both `StateFlow`s, `events`,
 * `requestPhase` and `end()` for real, so the tests below cover that genuinely-implemented
 * behaviour rather than the stubbed `authorize`/`admit` (owned by US1/US2).
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

    private fun newComponent(): SessionComponent = SessionComponent(
        transport = FakeControlTransport,
        announcer = FakeSessionAnnouncer,
        addressProvider = LocalAddressProvider { null },
        multicastLease = FakeMulticastLease,
        joinCodeGenerator = JoinCodeGenerator(),
        clock = { 0L },
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
    }
}
