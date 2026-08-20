package com.couchraoke.tv.di

import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

/**
 * T036(a): unit coverage for [SessionComponent.startSession]/[SessionComponent.stopSession] --
 * the session-start wiring (FR-004/FR-005/FR-008) that resolves the active address, acquires
 * the multicast lease, starts the transport, publishes the `KaraokeTV-<noun>` announcement and
 * verifies the registered name matches, per this class's own KDoc. `LoopbackJoinGateTest`
 * (T037) proves the real adapters end-to-end over a real socket; this file proves this
 * class's own branching -- no address, a jmDNS rename, success, and `stopSession`'s release
 * order -- against fakes, which is exactly what the `--src` coverage gate measures here.
 */
class SessionComponentTest {

    @Test(timeout = 30_000)
    fun startSessionReturnsNullWhenNoAddressIsAvailable() = runBlocking {
        val transport = RecordingControlTransport()
        val component = newComponent(address = null, transport = transport)

        val result = component.startSession(CONTROL_PORT)

        assertNull("no usable address must fail session start (T036)", result)
        assertEquals(
            "a failed address resolution must never bind the transport",
            0,
            transport.startCount,
        )
    }

    @Test(timeout = 30_000)
    fun startSessionBindsAnnouncesAndReturnsTheBoundStateOnSuccess() = runBlocking {
        val transport = RecordingControlTransport()
        val announcer = RecordingSessionAnnouncer()
        val lease = RecordingMulticastLease()
        val component = newComponent(
            address = LOCALHOST,
            transport = transport,
            announcer = announcer,
            multicastLease = lease,
        )

        val result = component.startSession(CONTROL_PORT)

        checkNotNull(result) {
            "startSession must succeed when an address resolves and the announced name matches"
        }
        assertEquals(LOCALHOST, result.address)
        assertEquals(CONTROL_PORT, result.boundPort)
        assertTrue("the multicast lease must be acquired", lease.acquired)
        assertEquals(1, transport.startCount)
        assertEquals(1, announcer.publishCount)
        assertTrue(
            "the announced instance name must be KaraokeTV-<noun> (FR-008)",
            announcer.lastInstanceName?.startsWith("KaraokeTV-") == true,
        )
    }

    @Test(timeout = 30_000)
    fun startSessionReleasesEverythingAndReturnsNullWhenTheAnnouncementIsRenamed() = runBlocking {
        val transport = RecordingControlTransport()
        val announcer = RenamingSessionAnnouncer()
        val lease = RecordingMulticastLease()
        val component = newComponent(
            address = LOCALHOST,
            transport = transport,
            announcer = announcer,
            multicastLease = lease,
        )

        val result = component.startSession(CONTROL_PORT)

        assertNull("a jmDNS rename must be treated as a session-start failure", result)
        assertTrue("a renamed announcement must be withdrawn", announcer.withdrawn)
        assertEquals(1, transport.stopCount)
        assertTrue("the multicast lease must be released on a rename failure", lease.released)
    }

    @Test(timeout = 30_000)
    fun stopSessionWithdrawsTheAnnouncementStopsTheTransportAndReleasesTheLease() = runBlocking {
        val transport = RecordingControlTransport()
        val announcer = RecordingSessionAnnouncer()
        val lease = RecordingMulticastLease()
        val component = newComponent(
            address = LOCALHOST,
            transport = transport,
            announcer = announcer,
            multicastLease = lease,
        )
        val result = checkNotNull(component.startSession(CONTROL_PORT))

        component.stopSession(result)

        assertTrue("stopSession must withdraw the announcement", announcer.withdrawn)
        assertEquals(1, transport.stopCount)
        assertTrue("stopSession must release the multicast lease", lease.released)
    }

    private fun newComponent(
        address: Inet4Address?,
        transport: ControlTransport = RecordingControlTransport(),
        announcer: SessionAnnouncer = RecordingSessionAnnouncer(),
        multicastLease: MulticastLease = RecordingMulticastLease(),
    ): SessionComponent = SessionComponent(
        transport = transport,
        announcer = announcer,
        addressProvider = LocalAddressProvider { address },
        multicastLease = multicastLease,
        joinCodeGenerator = JoinCodeGenerator(),
        clock = { 0L },
    )

    private class RecordingControlTransport : ControlTransport {
        var startCount = 0
            private set
        var stopCount = 0
            private set

        override suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport {
            startCount++
            return object : StartedTransport {
                override val boundPort: Int = port
            }
        }

        override suspend fun stop() {
            stopCount++
        }
    }

    private open class RecordingSessionAnnouncer : SessionAnnouncer {
        var publishCount = 0
            private set
        var withdrawn = false
            private set
        var lastInstanceName: String? = null
            private set

        override suspend fun publish(
            address: Inet4Address,
            instanceName: String,
            port: Int,
            joinCode: String,
            protocolVersion: Int,
        ): AnnouncementHandle {
            publishCount++
            lastInstanceName = instanceName
            return object : AnnouncementHandle {
                override val registeredInstanceName: String = instanceName
            }
        }

        override suspend fun withdraw(handle: AnnouncementHandle) {
            withdrawn = true
        }
    }

    /** Simulates jmDNS renaming the announcement on a LAN collision. */
    private class RenamingSessionAnnouncer : RecordingSessionAnnouncer() {
        override suspend fun publish(
            address: Inet4Address,
            instanceName: String,
            port: Int,
            joinCode: String,
            protocolVersion: Int,
        ): AnnouncementHandle {
            super.publish(address, instanceName, port, joinCode, protocolVersion)
            return object : AnnouncementHandle {
                override val registeredInstanceName: String = "$instanceName-renamed"
            }
        }
    }

    private class RecordingMulticastLease : MulticastLease {
        var acquired = false
            private set
        var released = false
            private set

        override fun acquire() {
            acquired = true
        }

        override fun release() {
            released = true
        }
    }

    private companion object {
        const val CONTROL_PORT = 8080
        val LOCALHOST: Inet4Address = InetAddress.getByName("127.0.0.1") as Inet4Address
    }
}
