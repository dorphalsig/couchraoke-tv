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
import com.couchraoke.tv.domain.session.model.SessionId
import kotlinx.serialization.json.Json
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

    fun createCoordinator(): SessionCoordinator = SessionCoordinator(
        roster = SessionRoster(),
        phaseMachine = GamePhaseMachine(),
        connectionIds = ConnectionIdAllocator(),
        validator = HandshakeValidator(),
        codeMatcher = JoinCodeMatcher,
        sessionId = SessionId(UUID.randomUUID().toString()),
        joinCode = joinCodeGenerator.next(),
    )

    /**
     * Starts one session end-to-end (T036, FR-004/FR-005/FR-008): resolves [addressProvider]'s
     * active IPv4, acquires [multicastLease], binds [transport] on [controlPort] behind a
     * [SessionControlConnectionHandler] wrapping a freshly built coordinator, then publishes
     * the `KaraokeTV-<noun>` announcement via [announcer] and verifies the readback name
     * matches what was requested — jmDNS silently renames on a LAN collision
     * ([com.couchraoke.tv.domain.platform.AnnouncementHandle.registeredInstanceName]'s own
     * doc), so a mismatch is treated as a session-start failure rather than an unnoticed
     * rename.
     *
     * Returns `null` for either failure — no usable address, or a renamed announcement —
     * releasing whatever was already acquired first so a failed start never leaks the
     * multicast lock or leaves the transport bound. Turning `null` into the FR-028 blocking
     * notice is T060's job (`SessionStartFailure`, tasks.md T060): that domain type does not
     * exist yet, and inventing one here would leave T060 nothing of its own to add.
     */
    suspend fun startSession(controlPort: Int): SessionStartResult? {
        val address = addressProvider.activeLocalIpv4() ?: return null

        val coordinator = createCoordinator()
        multicastLease.acquire()

        val started = transport.start(controlPort, SessionControlConnectionHandler(coordinator, codec))
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
            null
        } else {
            SessionStartResult(coordinator, address, started.boundPort, announcement)
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
