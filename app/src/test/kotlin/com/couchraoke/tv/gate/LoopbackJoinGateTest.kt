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
 * in this file (FR-039). Later tasks T047 and T055 add the refusal and reconnect cases;
 * this file proves acceptance scenario 4: a correct-token join is admitted end to end,
 * and a second, distinct peer is admitted with a distinct `connectionId`.
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
