package com.couchraoke.tv.domain.network.pitch

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PitchFrameCodecTest {

    @Test
    fun `Decode a hand-crafted 16-byte array verifies all 6 fields`() {
        val bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(123)            // seq
            .putInt(456)            // tvTimeMs
            .putInt(789)            // songInstanceSeq
            .put(1.toByte())        // playerId
            .put(60.toByte())       // midiNote
            .putShort(10.toShort()) // connectionId
            .array()

        val frame = PitchFrameCodec.decode(bytes)
        assertNotNull(frame)
        assertEquals(123L, frame?.seq)
        assertEquals(456, frame?.tvTimeMs)
        assertEquals(789L, frame?.songInstanceSeq)
        assertEquals(1, frame?.playerId)
        assertEquals(60, frame?.midiNote)
        assertEquals(10, frame?.connectionId)
    }

    @Test
    fun `midiNote=255 means toneValid=false`() {
        val frame = PitchFrame(1, 1, 1, 0, 255, 1)
        assertFalse(frame.toneValid)
    }

    @Test
    fun `midiNote=0 means toneValid=true`() {
        val frame = PitchFrame(1, 1, 1, 0, 0, 1)
        assertTrue(frame.toneValid)
    }

    @Test
    fun `encode decode round-trip results in identical bytes`() {
        val originalFrame = PitchFrame(999L, 5000, 3L, 1, 64, 7)
        val bytes = PitchFrameCodec.encode(originalFrame)
        val decodedFrame = PitchFrameCodec.decode(bytes)
        assertEquals(originalFrame, decodedFrame)
    }

    @Test
    fun `ByteArray length not 16 means decode returns null`() {
        assertNull(PitchFrameCodec.decode(ByteArray(15)))
        assertNull(PitchFrameCodec.decode(ByteArray(17)))
    }
}
