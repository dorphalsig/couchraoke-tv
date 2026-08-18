package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkWireMessagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test(timeout = 30_000)
    fun clockAckUsesAppendixSchemaFieldsOnly() {
        val payload = encodeNetworkMessage(
            ClockAckMessage(
                pingId = "ping-1",
                tTvRecvMs = SoloSingFixtures.ClockAckTvTimeMs,
            ),
        ).asJsonObject()

        assertEquals("clockAck", payload.string("type"))
        assertEquals("1", payload.string("protocolVersion"))
        assertEquals("ping-1", payload.string("pingId"))
        assertEquals(SoloSingFixtures.ClockAckTvTimeMs.toString(), payload.string("tTvRecvMs"))
        assertFalse(payload.containsKey("tvTimeMs"))
        assertFalse(payload.containsKey("phoneTimeMs"))
        assertFalse(payload.containsKey("roundTripMs"))
        assertFalse(payload.containsKey("sessionId"))
    }

    @Test(timeout = 30_000)
    fun assignSingerOmitsConnectionIdAndConstrainsSongInstanceSeqToUInt32() {
        val payload = encodeNetworkMessage(
            AssignSingerMessage(
                sessionId = SoloSingFixtures.SessionId,
                songInstanceSeq = UInt.MAX_VALUE.toLong(),
                playerId = PlayerId.P1,
                difficulty = Difficulty.Medium,
                startMode = StartMode.Live,
                countdownMs = null,
                stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
                udpPort = SoloSingFixtures.UdpPort,
                songTitle = SoloSingFixtures.SongTitle,
                songArtist = SoloSingFixtures.SongArtist,
            ),
        ).asJsonObject()

        assertEquals("assignSinger", payload.string("type"))
        assertEquals(UInt.MAX_VALUE.toString(), payload.string("songInstanceSeq"))
        assertFalse(payload.containsKey("connectionId"))
        assertFalse(payload.containsKey("countdownMs"))
    }

    @Test(timeout = 30_000, expected = IllegalArgumentException::class)
    fun assignSingerRejectsSongInstanceSeqAboveUInt32() {
        AssignSingerMessage(
            sessionId = SoloSingFixtures.SessionId,
            songInstanceSeq = UInt.MAX_VALUE.toLong() + 1L,
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            startMode = StartMode.Live,
            countdownMs = null,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            udpPort = SoloSingFixtures.UdpPort,
            songTitle = SoloSingFixtures.SongTitle,
            songArtist = SoloSingFixtures.SongArtist,
        )
    }

    @Test(timeout = 30_000)
    fun playbackStateSerializesOnlyDocumentedStatesAndReasons() {
        PlaybackNetworkState.entries.map { it.wireValue }.forEach { state ->
            assertTrue(state in setOf("countdown", "playing", "paused", "stopped"))
        }
        PlaybackStateReason.entries.map { it.wireValue }.forEach { reason ->
            assertTrue(
                reason in setOf(
                    "",
                    "user_pause",
                    "singer_disconnected",
                    "song_end",
                    "user_quit",
                    "restart",
                    "segment_transition",
                    "medley_source",
                    "medley_end",
                ),
            )
        }

        val payload = encodeNetworkMessage(
            PlaybackStateMessage(
                sessionId = SoloSingFixtures.SessionId,
                songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
                revision = 1L,
                state = PlaybackNetworkState.Playing,
                lyricsTimeMs = 0L,
                stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
                reason = PlaybackStateReason.Unspecified,
                tsTvMs = null,
            ),
        ).asJsonObject()

        assertEquals("playing", payload.string("state"))
        assertEquals("", payload.string("reason"))
        assertFalse(payload.containsKey("countdownRemainingMs"))
        assertFalse(payload.containsKey("tsTvMs"))
    }

    @Test(timeout = 30_000, expected = IllegalArgumentException::class)
    fun playbackStateRejectsSongInstanceSeqAboveUInt32() {
        PlaybackStateMessage(
            sessionId = SoloSingFixtures.SessionId,
            songInstanceSeq = UInt.MAX_VALUE.toLong() + 1L,
            revision = 1L,
            state = PlaybackNetworkState.Playing,
            lyricsTimeMs = 0L,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            reason = PlaybackStateReason.Unspecified,
        )
    }

    private fun String.asJsonObject(): JsonObject = json.parseToJsonElement(this).jsonObject

    private fun JsonObject.string(name: String): String = requireNotNull(this[name]).jsonPrimitive.content
}
