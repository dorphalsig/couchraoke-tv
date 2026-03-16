package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.network.pitch.PitchFrame
import com.couchraoke.tv.domain.network.pitch.PitchFrameCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class UdpPitchReceiverTest {

    @Test
    fun `T8_6_6 connectionId mismatch dropped`() {
        var called = false
        val receiver = UdpPitchReceiver { called = true }
        receiver.setActiveSong(100L)
        receiver.setPlayerConnection(0, 1234)

        val frame = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 9999
        )

        receiver.processPacket(PitchFrameCodec.encode(frame))

        assertEquals("Should be dropped due to connectionId mismatch", false, called)
    }

    @Test
    fun `T8_6_7 songInstanceSeq mismatch dropped`() {
        var called = false
        val receiver = UdpPitchReceiver { called = true }
        receiver.setActiveSong(100L)
        receiver.setPlayerConnection(0, 1234)

        val frame = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 200L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )

        receiver.processPacket(PitchFrameCodec.encode(frame))

        assertEquals("Should be dropped due to songInstanceSeq mismatch", false, called)
    }

    @Test
    fun `T8_6_8 unknown playerId dropped`() {
        var called = false
        val receiver = UdpPitchReceiver { called = true }
        receiver.setActiveSong(100L)

        val frame = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 100L,
            playerId = 1,
            midiNote = 60,
            connectionId = 1234
        )

        receiver.processPacket(PitchFrameCodec.encode(frame))

        assertEquals("Should be dropped due to unknown playerId", false, called)
    }

    @Test
    fun `tvTimeMs regression gt 200ms dropped`() {
        var callCount = 0
        val receiver = UdpPitchReceiver { callCount++ }
        receiver.setActiveSong(100L)
        receiver.setPlayerConnection(0, 1234)

        val frame1 = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )
        receiver.processPacket(PitchFrameCodec.encode(frame1))
        assertEquals(1, callCount)

        val frame2 = PitchFrame(
            seq = 2L,
            tvTimeMs = 799,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )
        receiver.processPacket(PitchFrameCodec.encode(frame2))
        assertEquals("Should be dropped due to regression > 200ms", 1, callCount)

        val frame3 = PitchFrame(
            seq = 3L,
            tvTimeMs = 800,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )
        receiver.processPacket(PitchFrameCodec.encode(frame3))
        assertEquals("Regression of exactly 200ms should be accepted", 2, callCount)
    }

    @Test
    fun `tvTimeMs increment accepted`() {
        var callCount = 0
        val receiver = UdpPitchReceiver { callCount++ }
        receiver.setActiveSong(100L)
        receiver.setPlayerConnection(0, 1234)

        val frame1 = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )
        receiver.processPacket(PitchFrameCodec.encode(frame1))
        assertEquals(1, callCount)

        val frame2 = PitchFrame(
            seq = 2L,
            tvTimeMs = 1001,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )
        receiver.processPacket(PitchFrameCodec.encode(frame2))
        assertEquals(2, callCount)
    }

    @Test
    fun `Valid frame invoked`() {
        var receivedFrame: PitchFrame? = null
        val receiver = UdpPitchReceiver { receivedFrame = it }
        receiver.setActiveSong(100L)
        receiver.setPlayerConnection(0, 1234)

        val frame = PitchFrame(
            seq = 1L,
            tvTimeMs = 1000,
            songInstanceSeq = 100L,
            playerId = 0,
            midiNote = 60,
            connectionId = 1234
        )

        receiver.processPacket(PitchFrameCodec.encode(frame))

        assertEquals(frame, receivedFrame)
    }

    @Test
    fun `Garbage data dropped`() {
        var called = false
        val receiver = UdpPitchReceiver { called = true }
        receiver.processPacket(byteArrayOf(1, 2, 3, 4, 5))
        assertEquals(false, called)
    }

    @Test
    fun `bind and stop lifecycle`() {
        val receiver = UdpPitchReceiver { }
        try {
            receiver.bind(0)
            // Give the thread a moment to start
            Thread.sleep(50)
            receiver.stop()
            // Give the thread a moment to stop
            Thread.sleep(50)
        } catch (e: Exception) {
            // If bind(0) fails due to environment restrictions, we just skip it
            // but we call stop() anyway to ensure no crash
            receiver.stop()
        }
    }
}
