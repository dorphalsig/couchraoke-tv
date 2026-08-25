package com.couchraoke.tv.gate

import com.couchraoke.tv.data.control.KtorControlTransport
import com.couchraoke.tv.data.discovery.JmdnsSessionAnnouncer
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.di.SessionStartOutcome
import com.couchraoke.tv.domain.control.ControlMessageCodec
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * **Opt-in probe. Skipped unless explicitly enabled — it is not part of any declared gate.**
 *
 * ```powershell
 * .\gradlew.bat :app:testDebugUnitTest --tests "*LanDiscoveryProbe*" -PlanDiscoveryProbe=true
 * ```
 *
 * It is opt-in because it needs a real LAN interface with working multicast. On a corporate
 * or guest network that filters mDNS it would fail rather than skip, and a failure there
 * looks exactly like a product bug — so leaving it in the default suite would make the suite
 * report the network's policy as our defect. It also costs ~10s and puts real multicast on
 * the wire. `Assume` is used rather than an early `return` so an opt-out is reported as a
 * skip instead of a false pass. Nothing in plan.md's gates names this class, so the skip
 * cannot satisfy anything (FR-039).
 *
 * **What it proves.** How much of T065 is reachable without an Android device. It runs the
 * real production [SessionComponent.startSession] path — real [KtorControlTransport], real
 * [JmdnsSessionAnnouncer] on a real multicast socket, real join-code generation and
 * instance-name readback — over this machine's real LAN interface, then asks the real
 * `mock-phone` peer to find it by mDNS alone and join.
 *
 * The peer is given **no** `--tv-host` and no `--token`. That is the positive control, and it
 * is also mandatory: the peer's `--help` claims `--discover` "Ignores --tv-host/--tv-port/
 * --token", but its argparse declares them in a mutually exclusive group, so supplying both
 * is a usage error. With no address on the command line, a successful join can only have come
 * from the mDNS TXT record — the address, the port and the join code were all read off the
 * announcement. Verified non-vacuous by tripwire: swapping [JmdnsSessionAnnouncer] for a
 * no-op announcer and changing nothing else fails the probe with exit 5, "no TV found".
 *
 * **What it cannot prove, and why T065 still needs a device.** Only the two Android-framework
 * adapters are substituted, and each substitution is exactly the thing absent from a desktop
 * JVM:
 * - [LocalAddressProvider] normally wraps `ConnectivityManager`; here it returns the same LAN
 *   address resolved from [NetworkInterface].
 * - [MulticastLease] normally wraps `WifiManager.MulticastLock`. Android's WiFi driver drops
 *   multicast before it reaches userspace to save power, and that lock is what stops it; a
 *   desktop has no such filtering, so there is nothing here to unlock. **`WifiMulticastLease`
 *   is therefore still never executed by anything, and its failure mode is silent** — jmDNS
 *   would register without error and simply never be found.
 */
class LanDiscoveryProbe {

    @Test(timeout = 120_000)
    fun theRealPeerFindsTheRealAnnouncementOverTheRealLanAndJoins() {
        Assume.assumeTrue(
            "opt-in: re-run with -PlanDiscoveryProbe=true on a LAN that permits multicast",
            System.getProperty(PROBE_PROPERTY) == "true",
        )

        val lanAddress = firstSiteLocalIpv4()
            ?: throw AssertionError("no non-loopback site-local IPv4 interface: this probe needs a real LAN")

        val component = SessionComponent(
            transport = KtorControlTransport(codec()),
            announcer = JmdnsSessionAnnouncer(),
            addressProvider = LocalAddressProvider { lanAddress },
            multicastLease = NoOpLease,
            joinCodeGenerator = JoinCodeGenerator(),
            clock = { 0L },
        )

        val outcome = runBlocking { component.startSession(controlPort = 0) }
        val started = outcome as? SessionStartOutcome.Started
            ?: throw AssertionError("the real start path failed before discovery could be tested: $outcome")

        try {
            val result = MockPhonePeer.run(
                tvHost = null,
                tvPort = 0,
                token = null,
                extraArgs = listOf("--join-only", "--discover", "--discover-timeout", "20"),
                timeoutSeconds = 90,
            )

            assertEquals("exit 5 would mean mDNS discovery found no TV at all", 0, result.exitStatus)
            assertEquals("accepted", result.outcome)
            assertTrue(
                "a discovered join must carry a real connectionId, was ${result.connectionId}",
                (result.connectionId ?: 0) >= 1,
            )
        } finally {
            runBlocking { component.stopSession(started.result) }
        }
    }

    private fun codec() = ControlMessageCodec(
        Json {
            explicitNulls = false
            ignoreUnknownKeys = false
        },
    )

    private fun firstSiteLocalIpv4(): Inet4Address? =
        NetworkInterface.getNetworkInterfaces()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }

    private object NoOpLease : MulticastLease {
        override fun acquire() = Unit
        override fun release() = Unit
    }

    private companion object {
        const val PROBE_PROPERTY = "lan.discovery.probe"
    }
}
