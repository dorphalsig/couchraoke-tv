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
class WebSocketServerAcceptanceTest {

    private val token = "ACCEPTANCETOKEN"
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

    private fun createHello(clientId: String, protocolVersion: Int = 1): String {
        val hello = JsonObject(mapOf(
            "type" to JsonPrimitive("hello"),
            "protocolVersion" to JsonPrimitive(protocolVersion),
            "clientId" to JsonPrimitive(clientId),
            "deviceName" to JsonPrimitive("Device $clientId"),
            "appVersion" to JsonPrimitive("1.0.0"),
            "httpPort" to JsonPrimitive(8080),
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
    fun `T8_3 handshake cases`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        // T8.3.1: Valid token
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c1")))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(1, state.connectionId)
        }

        // T8.3.2: Invalid token
        client.webSocket("/?token=INVALID") {
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("invalid_token", error.code)
        }

        // T8.3.3: Missing token
        client.webSocket("/") {
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("invalid_token", error.code)
        }

        // T8.3.5: Protocol mismatch
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c2", protocolVersion = 2)))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("protocol_mismatch", error.code)
        }

        // T8.3.7: Session full (>10)
        repeat(10) { registry.register("c_full_$it", "d$it", 8080, "1.1.1.1") }
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c_extra")))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("session_full", error.code)
        }
        
        // Reset registry for next tests
        registry = ConnectionRegistry()
    }

    @Test
    fun `T8_3_8 session_locked`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        gate.isLocked = true
        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c_locked")))
            val response = incoming.receive() as Frame.Text
            val error = ControlMessageCodec.json.decodeFromString<ErrorMessage>(response.readText())
            assertEquals("session_locked", error.code)
        }
    }

    @Test
    fun `T8_5 sender-ID cases`() = testApplication {
        val server = WebSocketServer(token, gate, registry, library, manifestFetcher, clockEngine)
        application(server.testModule())

        val client = createClient {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        // T8.5.1: First connection gets ID 1
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c1")))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(1, state.connectionId)
        }

        // T8.5.2: Second connection gets ID 2
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c2")))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(2, state.connectionId)
        }

        // T8.5.5: Reconnect gets NEW ID
        client.webSocket("/?token=$token") {
            send(Frame.Text(createHello("c1")))
            val response = incoming.receive() as Frame.Text
            val state = ControlMessageCodec.json.decodeFromString<SessionStateMessage>(response.readText())
            assertEquals(3, state.connectionId)
        }
    }
}
