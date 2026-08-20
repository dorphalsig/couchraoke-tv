package com.couchraoke.tv.di

import com.couchraoke.tv.domain.control.ControlMessageCodec
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.HandshakeValidator
import com.couchraoke.tv.domain.control.JoinCodeMatcher
import com.couchraoke.tv.domain.control.SessionControlConnectionHandler
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.ConnectionIdAllocator
import com.couchraoke.tv.domain.session.GamePhaseMachine
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.SessionRoster
import com.couchraoke.tv.domain.session.SessionStartFailure
import com.couchraoke.tv.domain.session.model.SessionId
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.Inet4Address
import java.util.UUID

/**
 * Manual composition root for one session (contracts/ports.md). No DI framework (FR-037).
 *
 * Mints the [SessionId] here — a random [UUID] string, which is opaque, non-empty and
 * practically unique per call (FR-001). No other task owns `SessionId` creation;
 * [SessionCoordinator] only ever receives one.
 *
 * [transport], [announcer], [addressProvider], [multicastLease] and [clock] are the four
 * injected ports plus the wall clock from contracts/ports.md's binding constructor, all
 * consumed by [startSession] (T036): resolving the active address, acquiring the
 * multicast lease, starting the transport and publishing the announcement.
 */
class SessionComponent(
    private val transport: ControlTransport,
    private val announcer: SessionAnnouncer,
    private val addressProvider: LocalAddressProvider,
    private val multicastLease: MulticastLease,
    private val joinCodeGenerator: JoinCodeGenerator,
    @Suppress("UnusedPrivateProperty") private val clock: () -> Long,
) {

    private val codec = ControlMessageCodec(
        Json {
            explicitNulls = false
            ignoreUnknownKeys = false
        },
    )

    private val validator = HandshakeValidator()

    fun createCoordinator(): SessionCoordinator = SessionCoordinator(
        roster = SessionRoster(),
        phaseMachine = GamePhaseMachine(),
        connectionIds = ConnectionIdAllocator(),
        validator = validator,
        codeMatcher = JoinCodeMatcher,
        sessionId = SessionId(UUID.randomUUID().toString()),
        joinCode = joinCodeGenerator.next(),
    )

    /**
     * Starts one session end-to-end (T036/T060, FR-004/FR-005/FR-008/FR-028): resolves
     * [addressProvider]'s active IPv4, acquires [multicastLease], binds [transport] on
     * [controlPort] behind a [SessionControlConnectionHandler] wrapping a freshly built
     * coordinator, then publishes the `KaraokeTV-<noun>` announcement via [announcer] and
     * verifies the readback name matches what was requested — jmDNS silently renames on a
     * LAN collision ([com.couchraoke.tv.domain.platform.AnnouncementHandle.registeredInstanceName]'s
     * own doc), so a mismatch is treated as a session-start failure rather than an unnoticed
     * rename.
     *
     * Returns [SessionStartOutcome.Failed] for any of FR-028's three distinguishable failure
     * modes — [SessionStartFailure.NoUsableAddress], [SessionStartFailure.BindFailed] (a bind
     * exception from [transport], previously unhandled and left to propagate) or
     * [SessionStartFailure.AnnouncementFailed] — releasing whatever was already acquired on
     * every failure path so a failed start never leaks the multicast lock or leaves the
     * transport bound. See [bindAndAnnounce] for the bind/announce half.
     */
    suspend fun startSession(controlPort: Int): SessionStartOutcome {
        val address = addressProvider.activeLocalIpv4()
            ?: return SessionStartOutcome.Failed(SessionStartFailure.NoUsableAddress)

        val coordinator = createCoordinator()
        multicastLease.acquire()

        return bindAndAnnounce(controlPort, coordinator, address)
    }

    /**
     * Binds [transport] and publishes the announcement, given [multicastLease] is already
     * held. Split out of [startSession] so each function keeps at most two `return`s: this one
     * for the bind failure, and a final `if`/`else` for the announcement outcome.
     *
     * A bind failure (port already in use, permission denied, …) surfaces from
     * [ControlTransport.start] as an [IOException] rather than a typed result
     * (contracts/ports.md fixes that signature; it is not this task's to change). Catching it
     * here — narrowly, only around the bind call — releases [multicastLease] before reporting
     * [SessionStartFailure.BindFailed], so a bind failure never leaks the lease the way a bare
     * propagated exception would have.
     */
    private suspend fun bindAndAnnounce(
        controlPort: Int,
        coordinator: SessionCoordinator,
        address: Inet4Address,
    ): SessionStartOutcome {
        val started = try {
            transport.start(controlPort, SessionControlConnectionHandler(coordinator, codec, validator))
        } catch (_: IOException) {
            multicastLease.release()
            return SessionStartOutcome.Failed(SessionStartFailure.BindFailed)
        }

        val joinCode = coordinator.snapshot.value.joinCode
        val instanceName = "$INSTANCE_NAME_PREFIX${joinCode.noun}"
        val announcement = announcer.publish(
            address = address,
            instanceName = instanceName,
            port = started.boundPort,
            joinCode = joinCode.display,
            protocolVersion = PROTOCOL_VERSION,
        )

        return if (announcement.registeredInstanceName != instanceName) {
            announcer.withdraw(announcement)
            transport.stop()
            multicastLease.release()
            SessionStartOutcome.Failed(SessionStartFailure.AnnouncementFailed)
        } else {
            SessionStartOutcome.Started(SessionStartResult(coordinator, address, started.boundPort, announcement))
        }
    }

    /** Stops what [startSession] started: withdraws the announcement, then the transport, then the lease. */
    suspend fun stopSession(result: SessionStartResult) {
        announcer.withdraw(result.announcement)
        transport.stop()
        multicastLease.release()
    }

    private companion object {
        const val INSTANCE_NAME_PREFIX = "KaraokeTV-"
        const val PROTOCOL_VERSION = 1
    }
}

/**
 * The outcome of a successful [SessionComponent.startSession] — the live [coordinator]
 * plus enough of the bound transport ([address], [boundPort]) for a later caller (T040) to
 * build a QR/join endpoint, and the [announcement] handle [SessionComponent.stopSession]
 * needs to withdraw it.
 */
data class SessionStartResult(
    val coordinator: SessionCoordinator,
    val address: Inet4Address,
    val boundPort: Int,
    val announcement: AnnouncementHandle,
)

/**
 * [SessionComponent.startSession]'s result (T060, FR-028). Replaces a bare nullable
 * [SessionStartResult] so the three failure modes FR-028 requires the caller to distinguish —
 * [SessionStartFailure.NoUsableAddress], [SessionStartFailure.BindFailed] and
 * [SessionStartFailure.AnnouncementFailed] — are carried as data rather than collapsed into a
 * single `null` that cannot say which one occurred.
 */
sealed interface SessionStartOutcome {
    data class Started(val result: SessionStartResult) : SessionStartOutcome
    data class Failed(val failure: SessionStartFailure) : SessionStartOutcome
}
