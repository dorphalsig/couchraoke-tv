package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.library.DefaultSongLibrary
import com.couchraoke.tv.domain.network.clock.ClockSyncEngine
import com.couchraoke.tv.domain.network.protocol.*
import com.couchraoke.tv.domain.session.ConnectionRegistry
import com.couchraoke.tv.domain.session.FakeSessionGate
import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import io.ktor.server.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketServerTest {

    private val token = "TESTTOKEN"
    private lateinit var gate: FakeSessionGate
    private lateinit var registry: ConnectionRegistry
    private lateinit var library: DefaultSongLibrary
    private lateinit var manifestFetcher: ManifestFetcher
    private lateinit var clockEngine: ClockSyncEngine
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        gate = FakeSessionGate()
        registry = ConnectionRegistry()
        library = DefaultSongLibrary()
        manifestFetcher = ManifestFetcher(library, OkHttpClient())
        testScope = TestScope(testDispatcher)
        clockEngine = ClockSyncEngine({ 0L }, { _, _ -> }, testScope, testDispatcher)
    }

    private fun createHello(clientId: String = "client1", protocolVersion: Int = 1, httpPort: Int = 8081): String {
        val hello = JsonObject(mapOf(
            "type" to JsonPrimitive("hello"),
            "protocolVersion" to JsonPrimitive(protocolVersion),
            "clientId" to JsonPrimitive(clientId),
            "deviceName" to JsonPrimitive("Phone 1"),
            "appVersion" to JsonPrimitive("1.0.0"),
            "httpPort" to JsonPrimitive(httpPort),
            "capabilities" to JsonObject(mapOf("pitchFps" to JsonPrimitive(30))),
            "tsTvMs" to JsonNull
        ))
        return ControlMessageCodec.json.encodeToString(hello)
    }

    private fun WebSocketServer.testModule(): io.ktor.server.application.Application.() -> Unit = {
        installWebSockets()
        routing {
            configureWebSocket()
        }
    }

    @Test
    fun `valid hello - sessionState connectionId == 1`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello()))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(1, state.connectionId)
            assertEquals("test-session", state.sessionId)
        }
    }

    @Test
    fun `second connection - connectionId == 2`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c1")))
            incoming.receive() // Skip session state
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c2")))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(2, state.connectionId)
        }
    }

    @Test
    fun `wrong token - connection closed with invalid_token`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=WRONG") {
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("invalid_token", error.code)
            
            val reason = closeReason.await()
            assertEquals("invalid_token", reason?.message)
        }
    }

    @Test
    fun `wrong protocolVersion - protocol_mismatch`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello(protocolVersion = 2)))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("protocol_mismatch", error.code)
        }
    }

    @Test
    fun `session_full when registry has 10 connections`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        repeat(10) { registry.register("c$it", "d$it", 8080, "1.1.1.1") }

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c11")))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("session_full", error.code)
        }
    }

    @Test
    fun `session_locked when gate isLocked`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        gate.isLocked = true

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello()))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("session_locked", error.code)
        }
    }

    @Test
    fun `disconnect - registry deregistered + library removePhone called`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        val clientId = "client-to-disconnect"
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello(clientId)))
            incoming.receive() // Session state
            // registry should have it
            assertEquals(1, registry.size)
            close()
        }

        // Wait a bit for the server to process disconnect
        kotlinx.coroutines.delay(100)
        assertEquals(0, registry.size)
    }

    @Test
    fun `unknown message type after handshake - ignored`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello()))
            incoming.receive() // Session state
            
            send(Frame.Text("""{"type":"unknown","protocolVersion":1,"tsTvMs":null}"""))
            
            // Send a pong to verify we are still alive
            val pingId = "test-ping"
            val pong = JsonObject(mapOf(
                "type" to JsonPrimitive("pong"),
                "protocolVersion" to JsonPrimitive(1),
                "pingId" to JsonPrimitive(pingId),
                "tTvSendMs" to JsonPrimitive(0L),
                "tPhoneRecvMs" to JsonPrimitive(0L),
                "tPhoneSendMs" to JsonPrimitive(0L),
                "tsTvMs" to JsonNull
            ))
            send(Frame.Text(ControlMessageCodec.json.encodeToString(pong)))
            val response = incoming.receive() as Frame.Text
            val ack = ControlMessageCodec.json.decodeFromString<ClockAckMessage>(response.readText())
            assertEquals(pingId, ack.pingId)
        }
    }
}
