package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.network.protocol.AssignSingerMessage
import com.couchraoke.tv.domain.network.protocol.PlaybackStateMessage
import com.couchraoke.tv.domain.network.protocol.SlotInfo
import com.couchraoke.tv.domain.network.protocol.SlotMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ISessionGateTest {

    @Test
    fun `default replay snapshot implementation returns null`() {
        val gate = object : ISessionGate {
            override val isLocked = false
            override val sessionId = "session"
            override val maxConnections = 10
            override val slots = SlotMap(
                P1 = SlotInfo(connected = false, deviceName = ""),
                P2 = SlotInfo(connected = false, deviceName = ""),
            )
            override val inSong = false
        }

        assertNull(gate.getPlaybackReplaySnapshot("client-1"))
    }

    @Test
    fun `fake session gate returns configured replay snapshot`() {
        val gate = FakeSessionGate()
        val snapshot = PlaybackReplaySnapshot(
            assignSinger = AssignSingerMessage(
                sessionId = gate.sessionId,
                songInstanceSeq = 42L,
                playerId = "P1",
                difficulty = "Medium",
                effectiveMicDelayMs = 120,
                expectedPitchFps = 50,
                stopAtLyricsTimeMs = 187000L,
                udpPort = 49152,
            ),
            playbackState = PlaybackStateMessage(
                sessionId = gate.sessionId,
                songInstanceSeq = 42L,
                revision = 7L,
                state = "playing",
                lyricsTimeMs = 15320L,
                stopAtLyricsTimeMs = 187000L,
                reason = "custom_reason",
                tsTvMs = 1234567890L,
            ),
        )

        gate.playbackReplaySnapshots["client-1"] = snapshot

        assertEquals(snapshot, gate.getPlaybackReplaySnapshot("client-1"))
    }
}
