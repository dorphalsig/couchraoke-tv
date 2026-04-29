package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkControllerContractTest {
    @Test(timeout = 30_000)
    fun connectedPhonePreservesHostHandshakeFields() {
        val phone = ConnectedPhone(
            clientId = SoloSingFixtures.PhoneClientId,
            connectionId = SoloSingFixtures.PhoneConnectionId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )

        assertEquals(SoloSingFixtures.PhoneClientId, phone.clientId)
        assertEquals(SoloSingFixtures.PhoneConnectionId, phone.connectionId)
        assertEquals(SoloSingFixtures.PhoneDeviceName, phone.deviceName)
        assertEquals(SoloSingFixtures.PhoneHttpPort, phone.httpPort)
        assertEquals(SoloSingFixtures.PhoneIpAddress, phone.ipAddress)
    }

    @Test(timeout = 30_000)
    fun songEntryMapsToIndexedSongWithoutPersistingRemoteAssets() {
        val entry = SongEntry(
            relativeTxtPath = SoloSingFixtures.RelativeTxtPath,
            modifiedTimeMs = SoloSingFixtures.ModifiedTimeMs,
            title = SoloSingFixtures.SongTitle,
            artist = SoloSingFixtures.SongArtist,
            album = SoloSingFixtures.SongAlbum,
            year = SoloSingFixtures.SongYear,
            genre = SoloSingFixtures.SongGenre,
            isDuet = false,
            hasRap = false,
            hasVideo = true,
            hasInstrumental = false,
            canMedley = false,
            medleySource = null,
            medleyStartBeat = null,
            medleyEndBeat = null,
            startSec = SoloSingFixtures.StartSec,
            previewStartSec = SoloSingFixtures.PreviewStartSec,
            txtUrl = SoloSingFixtures.assetUrl("/songs/${SoloSingFixtures.RelativeTxtPath}"),
            audioUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
            videoUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp4"),
            coverUrl = SoloSingFixtures.assetUrl("/covers/demo-song.png"),
            backgroundUrl = SoloSingFixtures.assetUrl("/backgrounds/demo-song.jpg"),
        )

        val indexed = entry.toIndexedSong(SoloSingFixtures.PhoneClientId)

        assertEquals(SoloSingFixtures.SongId, indexed.songId)
        assertEquals(entry.txtUrl, indexed.txtUrl)
        assertEquals(entry.audioUrl, indexed.audioUrl)
        assertEquals(entry.videoUrl, indexed.videoUrl)
        assertTrue(indexed.txtUrl.startsWith("http://"))
    }

    @Test(timeout = 30_000)
    fun assignSingerMessageUsesProtocolVersionOneAndSelectedSingerPayload() {
        val message = AssignSingerMessage(
            sessionId = SoloSingFixtures.SessionId,
            songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            startMode = StartMode.Countdown,
            countdownMs = 3_000,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            udpPort = SoloSingFixtures.UdpPort,
            songTitle = SoloSingFixtures.SongTitle,
            songArtist = SoloSingFixtures.SongArtist,
        )

        assertEquals("assignSinger", message.type)
        assertEquals(1, message.protocolVersion)
        assertEquals("countdown", message.startMode.wireValue)
        assertEquals(3_000, message.countdownMs)
        assertEquals(SoloSingFixtures.UdpPort, message.udpPort)
    }

    @Test(timeout = 30_000)
    fun liveAssignSingerOmitsCountdown() {
        val message = AssignSingerMessage(
            sessionId = SoloSingFixtures.SessionId,
            songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            startMode = StartMode.Live,
            countdownMs = null,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            udpPort = SoloSingFixtures.UdpPort,
            songTitle = SoloSingFixtures.SongTitle,
            songArtist = SoloSingFixtures.SongArtist,
        )

        assertEquals("live", message.startMode.wireValue)
        assertNull(message.countdownMs)
    }

    @Test(timeout = 30_000)
    fun phoneEventsCarryRequiredDisconnectAndReconnectState() {
        val phone = ConnectedPhone(
            clientId = SoloSingFixtures.PhoneClientId,
            connectionId = SoloSingFixtures.PhoneConnectionId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )

        val spectatorDisconnect = PhoneEvent.Disconnected(phone.clientId, wasAssignedSinger = false)
        val singerReconnect = PhoneEvent.Reconnected(phone, wasAssignedSinger = true)

        assertFalse(spectatorDisconnect.wasAssignedSinger)
        assertTrue(singerReconnect.wasAssignedSinger)
        assertEquals(phone.clientId, singerReconnect.clientId)
    }
}
