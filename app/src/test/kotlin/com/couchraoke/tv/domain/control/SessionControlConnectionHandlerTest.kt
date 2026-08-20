package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.session.ConnectionIdAllocator
import com.couchraoke.tv.domain.session.GamePhaseMachine
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.SessionRoster
import com.couchraoke.tv.domain.session.model.JoinCode
import com.couchraoke.tv.domain.session.model.SessionId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-level coverage for [SessionControlConnectionHandler] (T036) using a [FakeControlConnection]
 * — a test double of the *port* [ControlConnection], which ports.md's own note permits ("a
 * JVM test can build a coordinator with test doubles for the ports — which is legitimate,
 * since the ports are not the transport"). This is a different claim from the loopback
 * gate's: [LoopbackJoinGateTest] proves the real [com.couchraoke.tv.data.control.KtorControlTransport]
 * carries these bytes over a real socket to a real peer (FR-039); this file proves the
 * dispatch logic itself — authorize, decode, admit, reply, and close routing — in
 * isolation, including branches (an unparseable first frame, a peer that disconnects before
 * sending anything) the mandated happy-path gate does not need to exercise.
 */
class SessionControlConnectionHandlerTest {

    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = false
    }
    private val codec = ControlMessageCodec(json)

    @Test(timeout = 30_000)
    fun onConnectionAdmitsAValidHelloAndSendsASessionStateCarryingTheAllocatedConnectionId() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf(VALID_HELLO_JSON, null),
        )

        handler.onConnection(connection)

        assertEquals("exactly one sessionState reply must be sent", 1, connection.sentTexts.size)
        assertNull("the happy path must never refuse", connection.refusal)

        val payload = Json.parseToJsonElement(connection.sentTexts.single()).jsonObject
        assertEquals("sessionState", payload.getValue("type").jsonPrimitive.content)
        assertEquals(1, payload.getValue("connectionId").jsonPrimitive.content.toInt())

        val connectedDevices = payload.getValue("connectedDevices").jsonArray
        assertEquals("the responding device must be the sole entry in connectedDevices", 1, connectedDevices.size)
        val device = connectedDevices.single().jsonObject
        assertEquals(CLIENT_ID, device.getValue("clientId").jsonPrimitive.content)
        assertEquals("connected_unassigned", device.getValue("state").jsonPrimitive.content)
    }

    @Test(timeout = 30_000)
    fun onConnectionRoutesTheSocketCloseToOnDisconnectedAfterAdmission() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(token = JOIN_CODE.display, inbound = listOf(VALID_HELLO_JSON, null))

        handler.onConnection(connection)

        assertTrue(
            "the device must be admitted before the connection loop reads the closing null",
            connection.sentTexts.isNotEmpty(),
        )
        assertTrue(
            "onDisconnected must run once receiveText() returns null, dropping the device from connectedDevices",
            coordinator.connectedDevices.value.isEmpty(),
        )
    }

    @Test(timeout = 30_000)
    fun onConnectionDoesNothingWhenThePeerDisconnectsBeforeSendingAnything() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(token = JOIN_CODE.display, inbound = listOf(null))

        handler.onConnection(connection)

        assertTrue("no reply may be sent when the peer never sends a first frame", connection.sentTexts.isEmpty())
        assertNull(connection.refusal)
        assertTrue(coordinator.connectedDevices.value.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAFirstFrameThatFailsToDecodeWithInvalidMessage() = runBlocking {
        // T050 wires HandshakeValidator into the handler: a frame that fails to decode is no
        // longer silently dropped (that was T036's placeholder behaviour, before this
        // task's validator existed) -- it is now refused with invalid_message, same as any
        // other handshake validation failure. `{"type":"hello"}` is missing every field
        // after `type`; HandshakeValidator reports the first missing field in
        // `required`-array order, which is `protocolVersion`.
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf("""{"type":"hello"}""", null),
        )

        handler.onConnection(connection)

        assertEquals("invalid_message" to "Missing required field: protocolVersion", connection.refusal)
        assertTrue("no reply may be sent for a refused frame", connection.sentTexts.isEmpty())
        assertTrue(coordinator.connectedDevices.value.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesUnparseableJsonAsInvalidMessage() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf("{ this is not json", null),
        )

        handler.onConnection(connection)

        assertEquals("invalid_message" to "Malformed message", connection.refusal)
        assertTrue("no reply may be sent for a refused frame", connection.sentTexts.isEmpty())
        assertTrue(coordinator.connectedDevices.value.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAHelloMissingClientIdAsInvalidMessage() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf(
                """{"type":"hello","protocolVersion":1,""" +
                    """"deviceName":"Pixel 7","appVersion":"1.0.0","httpPort":34781}""",
                null,
            ),
        )

        handler.onConnection(connection)

        assertEquals("invalid_message" to "Missing required field: clientId", connection.refusal)
        assertTrue("no reply may be sent for a refused frame", connection.sentTexts.isEmpty())
        assertTrue(coordinator.connectedDevices.value.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAnUnsupportedProtocolVersionAsProtocolMismatch() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf(
                """{"type":"hello","protocolVersion":2,"clientId":"$CLIENT_ID",""" +
                    """"deviceName":"Pixel 7","appVersion":"1.0.0","httpPort":34781}""",
                null,
            ),
        )

        handler.onConnection(connection)

        assertEquals("protocol_mismatch" to "Unsupported protocolVersion: 2", connection.refusal)
        assertTrue("no reply may be sent for a refused frame", connection.sentTexts.isEmpty())
        assertTrue("a refused peer must never reach the roster", coordinator.connectedDevices.value.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAPreviouslyUnseenDeviceWithSessionFullWhenTheRosterIsAtCapacity() = runBlocking {
        val coordinator = newCoordinator(roster = SessionRoster(capacity = 1))
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val first = FakeControlConnection(token = JOIN_CODE.display, inbound = listOf(VALID_HELLO_JSON, null))

        handler.onConnection(first)

        // Guards against vacuity: the sole slot must actually be taken before the second
        // peer arrives, or a passing assertion below would prove nothing about capacity.
        assertTrue(
            "the first device must be admitted before the one-slot roster can be at capacity",
            first.sentTexts.isNotEmpty(),
        )

        val second = FakeControlConnection(
            token = JOIN_CODE.display,
            inbound = listOf(
                """{"type":"hello","protocolVersion":1,"clientId":"device-bbbb",""" +
                    """"deviceName":"Pixel 8","appVersion":"1.0.0","httpPort":34782}""",
                null,
            ),
        )

        handler.onConnection(second)

        assertEquals(
            "session_full" to "This session already has the maximum number of connected devices.",
            second.refusal,
        )
        assertTrue("a refused peer must never receive a sessionState", second.sentTexts.isEmpty())
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAWrongTokenBeforeReadingAnyFrameAndNeverAdmits() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(token = "timid-otter", inbound = listOf(VALID_HELLO_JSON, null))

        handler.onConnection(connection)

        assertEquals("invalid_token" to "The join code did not match the one shown on the TV.", connection.refusal)
        assertTrue("a refused peer must never receive a sessionState", connection.sentTexts.isEmpty())
        assertTrue("a refused peer must never reach the roster", coordinator.connectedDevices.value.isEmpty())
        assertTrue(
            "the token is checked before the first frame is read, so the hello must be left unread",
            connection.unreadFrames == 2,
        )
    }

    @Test(timeout = 30_000)
    fun onConnectionRefusesAMissingTokenOutright() = runBlocking {
        val coordinator = newCoordinator()
        val handler = SessionControlConnectionHandler(coordinator, codec)
        val connection = FakeControlConnection(token = null, inbound = listOf(VALID_HELLO_JSON, null))

        handler.onConnection(connection)

        assertEquals("invalid_token", connection.refusal?.first)
        assertTrue(coordinator.connectedDevices.value.isEmpty())
    }

    private fun newCoordinator(roster: SessionRoster = SessionRoster()): SessionCoordinator = SessionCoordinator(
        roster = roster,
        phaseMachine = GamePhaseMachine(),
        connectionIds = ConnectionIdAllocator(),
        validator = HandshakeValidator(),
        codeMatcher = JoinCodeMatcher,
        sessionId = SessionId("sess-handler-test"),
        joinCode = JOIN_CODE,
    )

    private class FakeControlConnection(
        override val token: String?,
        inbound: List<String?>,
    ) : ControlConnection {
        private val queue = ArrayDeque(inbound)
        val unreadFrames: Int get() = queue.size
        val sentTexts = mutableListOf<String>()
        var refusal: Pair<String, String>? = null
            private set
        var closeCalled = false
            private set

        override suspend fun receiveText(): String? = if (queue.isEmpty()) null else queue.removeFirst()

        override suspend fun sendText(text: String) {
            sentTexts += text
        }

        override suspend fun refuse(code: String, message: String) {
            refusal = code to message
        }

        override suspend fun close() {
            closeCalled = true
        }
    }

    private companion object {
        val JOIN_CODE = JoinCode(adjective = "brave", noun = "otter")
        const val CLIENT_ID = "device-aaaa"
        const val VALID_HELLO_JSON =
            """{"type":"hello","protocolVersion":1,"clientId":"$CLIENT_ID",""" +
                """"deviceName":"Pixel 7","appVersion":"1.0.0","httpPort":34781}"""
    }
}
