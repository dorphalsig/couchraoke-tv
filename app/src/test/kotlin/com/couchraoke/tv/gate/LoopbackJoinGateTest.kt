package com.couchraoke.tv.gate

import com.couchraoke.tv.data.control.KtorControlTransport
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.domain.control.ControlConnection
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlMessageCodec
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.net.Inet4Address

/**
 * T028: the loopback harness's JUnit rule, plus the one scaffold smoke test that proves
 * the rule itself works.
 *
 * [LoopbackJoinGateRule] starts the real [KtorControlTransport] on an ephemeral port
 * (`0`) against a real [SessionCoordinator] built through the production
 * [SessionComponent] composition root — no in-process transport fake anywhere in this
 * file (FR-039). Later tasks T037, T047 and T055 add the per-story cases; this file
 * only proves the scaffold binds and is reachable.
 *
 * `SessionCoordinator.authorize` and `SessionCoordinator.admit` are still `TODO()`
 * stubs at this point in the task order (T035/T050/T051 complete them), so the
 * connection handler used here deliberately never calls them — doing so would throw
 * `NotImplementedError`, which is exactly the join/admission logic this task is scoped
 * to leave alone. Instead it proves reachability the same way every later case will:
 * over the real socket, with a real (if deliberately trivial) response.
 */
class LoopbackJoinGateTest {

    @get:Rule
    val gate = LoopbackJoinGateRule { ScaffoldConnectionHandler() }

    @Test(timeout = 60_000)
    fun ruleBindsARealPortAndTheRealPeerCanReachIt() {
        assertTrue("boundPort must be a real ephemeral port, not the requested 0", gate.boundPort > 0)

        val result = MockPhonePeer.run(
            tvPort = gate.boundPort,
            token = "SCAFFOLD-TOKEN",
            extraArgs = listOf("--join-only", "--join-timeout", "5"),
        )

        // The scaffold handler always refuses with an explicit, non-protocol code, so a
        // successful run here proves the real mock-phone subprocess reached the real
        // KtorControlTransport over 127.0.0.1 and received an explicit response — not a
        // connect failure (exit 5), and not an un-enforced deadline (exit 6, which
        // MockPhonePeer.run already turns into a hard failure regardless of scenario).
        assertEquals(3, result.exitStatus)
        assertEquals("rejected", result.outcome)
        assertEquals(ScaffoldConnectionHandler.REFUSAL_CODE, result.errorCode)
        assertEquals(1008, result.closeCode)
        assertEquals(ScaffoldConnectionHandler.REFUSAL_CODE, result.closeReason)
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

/**
 * The scaffold's own [ControlConnectionHandler]: reads the peer's `hello` frame (proving
 * bytes flow in both directions over the real socket) and then refuses unconditionally
 * with a code that names itself as a placeholder, rather than any real
 * `com.couchraoke.tv.domain.control.RefusalReason`. It deliberately never consults the
 * [SessionCoordinator] handed to it — `authorize`/`admit` are not implemented until
 * T035/T050/T051, and calling either here would just throw `NotImplementedError`.
 */
private class ScaffoldConnectionHandler : ControlConnectionHandler {
    override suspend fun onConnection(connection: ControlConnection) {
        connection.receiveText()
        connection.refuse(REFUSAL_CODE, REFUSAL_MESSAGE)
    }

    companion object {
        const val REFUSAL_CODE = "scaffold_not_implemented"
        private const val REFUSAL_MESSAGE =
            "T028 scaffold: SessionCoordinator.authorize/admit are not implemented until T035/T050/T051"
    }
}
