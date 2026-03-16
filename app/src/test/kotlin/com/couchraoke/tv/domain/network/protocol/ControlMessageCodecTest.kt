package com.couchraoke.tv.domain.network.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ControlMessageCodecTest {

    private val testJson = Json { encodeDefaults = true }

    @Test
    fun `decodeType returns type field when present`() {
        val json = """{"type": "hello", "other": "value"}"""
        assertEquals("hello", ControlMessageCodec.decodeType(json))
    }

    @Test
    fun `decodeType returns null when type field missing`() {
        val json = """{"not_type": "hello"}"""
        assertNull(ControlMessageCodec.decodeType(json))
    }

    @Test
    fun `decodeType returns null on malformed json`() {
        val json = """{"type": "hello", """
        assertNull(ControlMessageCodec.decodeType(json))
    }

    @Test
    fun `decode hello message`() {
        val msg = HelloMessage(
            protocolVersion = 1,
            clientId = "client1",
            deviceName = "phone1",
            appVersion = "1.0",
            httpPort = 8080,
            capabilities = Capabilities(pitchFps = 60)
        )
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is HelloMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode sessionState message`() {
        val msg = SessionStateMessage(
            sessionId = "sess1",
            slots = SlotMap(
                P1 = SlotInfo(connected = true, deviceName = "D1"),
                P2 = SlotInfo(connected = false, deviceName = "")
            ),
            inSong = true
        )
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is SessionStateMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode assignSinger message`() {
        val msg = AssignSingerMessage(
            sessionId = "sess1",
            songInstanceSeq = 123L,
            playerId = "P1",
            difficulty = "Easy",
            thresholdIndex = 1,
            effectiveMicDelayMs = 50,
            expectedPitchFps = 60,
            startMode = "Normal",
            endTimeTvMs = 10000L,
            udpPort = 5000
        )
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is AssignSingerMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode error message`() {
        val msg = ErrorMessage(code = "ERR_001", message = "Something went wrong")
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is ErrorMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode ping message`() {
        val msg = PingMessage(pingId = "ping1", tTvSendMs = 1000L)
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is PingMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode pong message`() {
        val msg = PongMessage(pingId = "ping1", tTvSendMs = 1000L, tPhoneRecvMs = 1010L, tPhoneSendMs = 1020L)
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is PongMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode clockAck message`() {
        val msg = ClockAckMessage(pingId = "ping1", tTvRecvMs = 1050L)
        val json = testJson.encodeToString(msg)
        val decoded = ControlMessageCodec.decode(json)
        assertTrue(decoded is ClockAckMessage)
        assertEquals(msg, decoded)
    }

    @Test
    fun `decode unknown message type returns null`() {
        val json = """{"type": "unknownType", "data": "whatever"}"""
        assertNull(ControlMessageCodec.decode(json))
    }

    @Test
    fun `decode malformed json returns null`() {
        val json = """{"type": "hello", malformed}"""
        assertNull(ControlMessageCodec.decode(json))
    }

    @Test
    fun `decode message with wrong fields for type returns null`() {
        // "hello" requires protocolVersion, clientId, etc.
        val json = """{"type": "hello", "wrongField": 123}"""
        assertNull(ControlMessageCodec.decode(json))
    }
}
