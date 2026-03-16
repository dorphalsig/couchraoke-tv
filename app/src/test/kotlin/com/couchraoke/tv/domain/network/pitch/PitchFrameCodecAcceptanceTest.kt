package com.couchraoke.tv.domain.network.pitch

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PitchFrameCodecAcceptanceTest {

    @Serializable
    data class ExpectedFrame(
        val offset: Int,
        val seq: Long,
        val tvTimeMs: Int,
        val songInstanceSeq: Long,
        val connectionId: Int,
        val playerId: Int,
        val midiNote: Int,
        val toneValid: Boolean
    )

    private val fixtureDir = "/home/paavum/Couchraoke/tv/original_spec/fixtures/F12v2_pitch_stream_validation_semantics/"
    private val framesBin by lazy { File(fixtureDir + "frames.bin").readBytes() }
    private val expectedJson by lazy { File(fixtureDir + "expected.json").readText() }
    private val expectedFrames by lazy { Json.decodeFromString<List<ExpectedFrame>>(expectedJson) }

    @Test
    fun `T8_6_1 Decode row 0 — all fields match expected`() {
        val expected = expectedFrames[0]
        val bytes = framesBin.copyOfRange(expected.offset, expected.offset + 16)
        val frame = PitchFrameCodec.decode(bytes)!!

        assertEquals("seq", expected.seq, frame.seq)
        assertEquals("tvTimeMs", expected.tvTimeMs, frame.tvTimeMs)
        assertEquals("songInstanceSeq", expected.songInstanceSeq, frame.songInstanceSeq)
        assertEquals("connectionId", expected.connectionId, frame.connectionId)
        assertEquals("playerId", expected.playerId, frame.playerId)
        assertEquals("midiNote", expected.midiNote, frame.midiNote)
        assertEquals("toneValid", expected.toneValid, frame.toneValid)
    }

    @Test
    fun `T8_6_2 Row 1 midiNote=255 means toneValid=false`() {
        val expected = expectedFrames[1]
        val bytes = framesBin.copyOfRange(expected.offset, expected.offset + 16)
        val frame = PitchFrameCodec.decode(bytes)!!

        assertEquals(255, frame.midiNote)
        assertFalse(frame.toneValid)
        assertEquals(expected.toneValid, frame.toneValid)
    }

    @Test
    fun `T8_6_3 Row 2 midiNote=36 means toneValid=true`() {
        val expected = expectedFrames[2]
        val bytes = framesBin.copyOfRange(expected.offset, expected.offset + 16)
        val frame = PitchFrameCodec.decode(bytes)!!

        assertTrue(frame.toneValid)
        assertEquals(expected.toneValid, frame.toneValid)
    }

    @Test
    fun `T8_6_4 encode decode results in identical bytes for row 0`() {
        val expected = expectedFrames[0]
        val originalBytes = framesBin.copyOfRange(expected.offset, expected.offset + 16)
        val frame = PitchFrameCodec.decode(originalBytes)!!
        val encodedBytes = PitchFrameCodec.encode(frame)

        assertArrayEquals(originalBytes, encodedBytes)
    }

    @Test
    fun `T8_6_5 ByteArray of length 15 returns null`() {
        assertNull(PitchFrameCodec.decode(ByteArray(15)))
    }
}
