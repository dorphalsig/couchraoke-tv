package com.couchraoke.tv.gate

import com.couchraoke.tv.data.control.KtorControlTransport
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlMessageCodec
import com.couchraoke.tv.domain.control.SessionControlConnectionHandler
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.domain.session.SessionCoordinator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.net.Inet4Address

/**
 * T036/T037: the loopback harness's JUnit rule, plus the real join-dispatch happy path.
 *
 * [LoopbackJoinGateRule] starts the real [KtorControlTransport] on an ephemeral port
 * (`0`) against a real [SessionCoordinator] built through the production
 * [SessionComponent] composition root, dispatching every connection to the real
 * production [SessionControlConnectionHandler] — no in-process transport fake anywhere
 * in this file (FR-039). This file proves acceptance scenario 4 (a correct-token join is
 * admitted end to end, and a second, distinct peer is admitted with a distinct
 * `connectionId`) and, since T047, every US2 refusal: wrong token, unsupported protocol
 * version, a hello missing a required field, a first frame that is not JSON, and a peer
 * that never introduces itself at all. T055 later adds the reconnect cases.
 *
 * Both tests use `gate.coordinator.snapshot.value.joinCode.display` as the `--token`
 * they hand the peer, never a hardcoded literal, so they keep working regardless of
 * which join code [JoinCodeGenerator] happens to mint for a given run.
 */
class LoopbackJoinGateTest {

    private val codec = ControlMessageCodec(
        Json {
            explicitNulls = false
            ignoreUnknownKeys = false
        },
    )

    @get:Rule
    val gate = LoopbackJoinGateRule { coordinator -> SessionControlConnectionHandler(coordinator, codec) }

    @Test(timeout = 60_000)
    fun joinOnlyWithTheCorrectTokenIsAcceptedAndReceivesSessionState() {
        assertTrue("boundPort must be a real ephemeral port, not the requested 0", gate.boundPort > 0)

        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = gate.coordinator.snapshot.value.joinCode.display,
            extraArgs = listOf("--join-only"),
        )

        assertEquals(0, result.exitStatus)
        assertEquals("accepted", result.outcome)
        val connectionId = result.connectionId
        assertTrue("connectionId must be >= 1, was $connectionId", connectionId != null && connectionId >= 1)
    }

    /**
     * The counterpart the happy path needs to mean anything: without it, a dispatch that
     * ignored the token entirely would still pass every other case in this file. It is here
     * rather than with US2's refusal cases (T051) because it is what proves the *acceptance*
     * above was a decision and not a formality — see spec.md Observation 19.
     */
    @Test(timeout = 60_000)
    fun joinOnlyWithAWrongTokenIsRefusedWithInvalidToken() {
        val correct = gate.coordinator.snapshot.value.joinCode.display
        val wrong = "WRONG-WORD"
        assertNotEquals("the test's wrong token must actually differ from the session's", correct, wrong)

        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = wrong,
            extraArgs = listOf("--join-only"),
        )

        assertEquals(3, result.exitStatus)
        assertEquals("rejected", result.outcome)
        assertEquals("invalid_token", result.errorCode)
        assertEquals(1008, result.closeCode)
        assertEquals("invalid_token", result.closeReason)
    }

    /**
     * T047: a peer speaking protocolVersion 2 is refused with `protocol_mismatch`.
     *
     * This is the one refusal whose wording the F20 fixture pins, so it is also the one
     * that proves the fixture-driven validator is the thing running over a real socket
     * and not a second, hand-written copy of the rules.
     */
    @Test(timeout = 60_000)
    fun aPeerSpeakingAnUnsupportedProtocolVersionIsRefusedWithProtocolMismatch() {
        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = gate.coordinator.snapshot.value.joinCode.display,
            extraArgs = listOf("--join-only", "--protocol-version", "2"),
        )

        assertRefused(result, expectedCode = "protocol_mismatch")
    }

    /** T047: a hello missing a required field is refused with `invalid_message`. */
    @Test(timeout = 60_000)
    fun aHelloMissingItsClientIdIsRefusedWithInvalidMessage() {
        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = gate.coordinator.snapshot.value.joinCode.display,
            extraArgs = listOf("--join-only", "--malformed-hello", "clientId"),
        )

        assertRefused(result, expectedCode = "invalid_message")
    }

    /**
     * T047: a first frame that is not JSON at all is refused with `invalid_message`.
     *
     * Distinct from the case above: that one decodes cleanly and fails a field check,
     * this one cannot be decoded at all. Before T050 the handler silently dropped
     * undecodable frames and left the socket open, which this case would have caught
     * as exit 6 rather than 3.
     */
    @Test(timeout = 60_000)
    fun aFirstFrameThatIsNotJsonIsRefusedWithInvalidMessage() {
        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = gate.coordinator.snapshot.value.joinCode.display,
            extraArgs = listOf("--join-only", "--malformed-hello", "invalid-json"),
        )

        assertRefused(result, expectedCode = "invalid_message")
    }

    /**
     * T047/FR-017: a peer that connects and then says nothing is refused by the TV's own
     * five-second deadline.
     *
     * The peer is given `--join-timeout 10`, deliberately longer than the deadline, which
     * is what makes this case non-vacuous. If the TV failed to enforce any deadline the
     * peer would give up first and exit 6 (`no_response`); it can only reach exit 3 with
     * an error frame if the TV closed the connection on its own initiative, before the
     * peer's own patience ran out. Asserting exit 3 therefore asserts the deadline.
     */
    @Test(timeout = 60_000)
    fun aPeerThatNeverIntroducesItselfIsRefusedByTheTvBeforeItsOwnTimeout() {
        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = gate.coordinator.snapshot.value.joinCode.display,
            extraArgs = listOf("--silent-handshake", "--join-timeout", "10"),
        )

        assertEquals(
            "exit 6 would mean the peer timed out first and the TV enforced no deadline",
            3,
            result.exitStatus,
        )
        assertRefused(result, expectedCode = "invalid_message")
    }

    /**
     * T055/SC-005/SC-006: the roster fills at ten devices, refuses an eleventh, and still
     * welcomes back one of its own.
     *
     * Ten sequential peers suffice because a disconnected device keeps its roster slot in
     * this slice (FR-023) — each peer has already exited by the time the next dials in, so
     * the live connection count is zero throughout and only the roster's retained entries
     * hold the capacity. That is exactly what makes the last leg meaningful: `phone-1` comes
     * back to a *full* roster with no live connection of its own, which is the reconnect
     * shape spec.md Observation 22 describes, and it must still be admitted. A capacity
     * check placed before the reclaim branch would refuse it, so this case is what pins
     * that ordering over a real socket rather than in a unit test's imagination.
     */
    @Test(timeout = 120_000)
    fun theRosterFillsAtTenDevicesRefusesAnEleventhAndStillWelcomesBackOneOfItsOwn() {
        val token = gate.coordinator.snapshot.value.joinCode.display

        val firstTen = (1..10).map { n ->
            val result = MockPhonePeer.run(
                tvPort = gate.boundPort,
                token = token,
                extraArgs = listOf("--join-only", "--client-id", "loopback-phone-$n"),
            )
            assertEquals("device $n of the first ten must be admitted", 0, result.exitStatus)
            result.connectionId
        }
        assertEquals("the ten admitted devices must hold ten distinct connectionIds", 10, firstTen.toSet().size)

        val eleventh = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = token,
            extraArgs = listOf("--join-only", "--client-id", "loopback-phone-eleventh"),
        )
        assertRefused(eleventh, expectedCode = "session_full")

        val returning = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = token,
            extraArgs = listOf("--join-only", "--client-id", "loopback-phone-1"),
        )
        assertEquals(
            "a device already on the roster must be readmitted even though the roster is full (SC-006)",
            0,
            returning.exitStatus,
        )
        assertEquals("accepted", returning.outcome)
        assertNotEquals(
            "a reclaim must mint a fresh connectionId, not hand back the old one (FR-020)",
            firstTen.first(),
            returning.connectionId,
        )
    }

    /**
     * Every refusal must look the same on the wire: an `error` frame carrying the code,
     * then a 1008 close whose reason repeats that code (FR-019, contracts/control-protocol.md).
     * Asserting the close alongside the frame is what catches a refusal that sends the
     * error and then leaks the socket — the peer reports that as exit 4, not 3.
     */
    private fun assertRefused(result: JoinProbeResult, expectedCode: String) {
        assertEquals("expected a clean refusal, got outcome=${result.outcome}", 3, result.exitStatus)
        assertEquals("rejected", result.outcome)
        assertEquals(expectedCode, result.errorCode)
        assertEquals(1008, result.closeCode)
        assertEquals("the close reason must repeat the error code", expectedCode, result.closeReason)
    }

    @Test(timeout = 60_000)
    fun aSecondPeerWithADifferentClientIdAlsoJoinsAndReceivesADistinctConnectionId() {
        val token = gate.coordinator.snapshot.value.joinCode.display

        val first = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = token,
            extraArgs = listOf("--join-only"),
        )
        val second = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = token,
            extraArgs = listOf("--join-only", "--client-id", "phone-second-peer"),
        )

        assertEquals(0, first.exitStatus)
        assertEquals("accepted", first.outcome)
        assertEquals(0, second.exitStatus)
        assertEquals("accepted", second.outcome)

        val firstConnectionId = first.connectionId
        val secondConnectionId = second.connectionId
        assertTrue(
            "first connectionId must be >= 1, was $firstConnectionId",
            firstConnectionId != null && firstConnectionId >= 1,
        )
        assertTrue(
            "second connectionId must be >= 1, was $secondConnectionId",
            secondConnectionId != null && secondConnectionId >= 1,
        )
        assertNotEquals(
            "two distinct devices must receive distinct connectionIds (acceptance scenario 4)",
            firstConnectionId,
            secondConnectionId,
        )
    }
}

/**
 * Starts the real [KtorControlTransport] on port 0 against a real [SessionCoordinator]
 * built through [SessionComponent], and tears both down after each test.
 *
 * [SessionComponent]'s `announcer`, `addressProvider` and `multicastLease` parameters
 * are unused by `SessionComponent.createCoordinator` — see that class's own
 * `@Suppress("UnusedPrivateProperty")`, and `SessionCoordinatorTest`'s identical
 * no-op stand-ins. They exist only to satisfy that constructor, so trivial no-op
 * implementations of them are not "in-process fakes" of anything this file claims to
 * prove. The one real thing under test is [transport]: the very same
 * [KtorControlTransport] instance is passed to [SessionComponent] (where it is inert)
 * and started directly below, over a real socket.
 *
 * [handlerFactory] hands the real, constructed [SessionCoordinator] to the caller so
 * later tasks can wire whatever connection handling their scenario needs; this rule
 * itself makes no assumption about how a connection is dispatched.
 */
class LoopbackJoinGateRule(
    private val handlerFactory: (SessionCoordinator) -> ControlConnectionHandler,
) : ExternalResource() {

    private val transport = KtorControlTransport(
        ControlMessageCodec(
            Json {
                explicitNulls = false
                ignoreUnknownKeys = false
            },
        ),
    )
    private var started: StartedTransport? = null

    lateinit var coordinator: SessionCoordinator
        private set

    val boundPort: Int
        get() = started?.boundPort ?: 0

    override fun before() {
        coordinator = SessionComponent(
            transport = transport,
            announcer = NoOpSessionAnnouncer,
            addressProvider = LocalAddressProvider { null },
            multicastLease = NoOpMulticastLease,
            joinCodeGenerator = JoinCodeGenerator(),
            clock = { 0L },
        ).createCoordinator()

        started = runBlocking { transport.start(port = 0, handler = handlerFactory(coordinator)) }
    }

    override fun after() {
        runBlocking { transport.stop() }
    }

    private object NoOpSessionAnnouncer : SessionAnnouncer {
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

    private object NoOpMulticastLease : MulticastLease {
        override fun acquire() = Unit
        override fun release() = Unit
    }
}
