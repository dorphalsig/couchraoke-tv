package com.couchraoke.tv.data.library

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManifestLibraryManagerTest {
    @Test(timeout = 30_000)
    fun aggregatesConnectedPhoneManifestIntoIndexedSongs() = runBlocking {
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(song(title = "Beta"))),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshFromConnectedPhones()

        val songs = manager.songs.first()
        assertEquals(1, songs.size)
        assertEquals("Beta", songs.single().title)
        assertEquals(SoloSingFixtures.PhoneClientId, songs.single().phoneClientId)
        assertEquals(songs.single(), manager.getSong(songs.single().songId))
    }

    @Test(timeout = 30_000)
    fun rejectsManifestEntriesMissingRequiredPlayableAssetUrls() = runBlocking {
        val valid = song(title = "Playable")
        val invalidWithoutAudio = song(title = "No Audio", audioUrl = "")
        val invalidWithoutTxt = song(title = "No TXT", txtUrl = "")
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(valid, invalidWithoutAudio, invalidWithoutTxt)),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshFromConnectedPhones()

        assertEquals(listOf("Playable"), manager.songs.first().map { it.title })
        assertNull(manager.getSong("missing"))
    }

    @Test(timeout = 30_000)
    fun sortsByArtistThenAlbumThenTitleCaseInsensitive() = runBlocking {
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(
                SoloSingFixtures.PhoneClientId to listOf(
                    song(artist = "Zulu", album = "A", title = "Last"),
                    song(artist = "alpha", album = "B", title = "Second"),
                    song(artist = "Alpha", album = "A", title = "third"),
                    song(artist = "Alpha", album = "A", title = "First"),
                ),
            ),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshFromConnectedPhones()

        assertEquals(listOf("First", "third", "Second", "Last"), manager.songs.first().map { it.title })
    }

    private fun phone(): ConnectedPhone = ConnectedPhone(
        clientId = SoloSingFixtures.PhoneClientId,
        connectionId = SoloSingFixtures.PhoneConnectionId,
        deviceName = SoloSingFixtures.PhoneDeviceName,
        httpPort = SoloSingFixtures.PhoneHttpPort,
        ipAddress = SoloSingFixtures.PhoneIpAddress,
    )

    private fun song(
        artist: String = SoloSingFixtures.SongArtist,
        album: String? = SoloSingFixtures.SongAlbum,
        title: String = SoloSingFixtures.SongTitle,
        txtUrl: String = SoloSingFixtures.assetUrl("/songs/${SoloSingFixtures.RelativeTxtPath}"),
        audioUrl: String = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
    ): SongEntry = SongEntry(
        relativeTxtPath = "songs/$title.txt",
        modifiedTimeMs = SoloSingFixtures.ModifiedTimeMs + title.hashCode(),
        title = title,
        artist = artist,
        album = album,
        year = SoloSingFixtures.SongYear,
        genre = SoloSingFixtures.SongGenre,
        isDuet = false,
        hasRap = false,
        hasVideo = false,
        hasInstrumental = false,
        canMedley = false,
        medleySource = null,
        medleyStartBeat = null,
        medleyEndBeat = null,
        startSec = SoloSingFixtures.StartSec,
        previewStartSec = SoloSingFixtures.PreviewStartSec,
        txtUrl = txtUrl,
        audioUrl = audioUrl,
        videoUrl = null,
        coverUrl = null,
        backgroundUrl = null,
    )

    private class FakeNetworkController(
        phones: List<ConnectedPhone>,
        private val manifests: Map<String, List<SongEntry>>,
    ) : NetworkController {
        override val connectedPhones = MutableStateFlow(phones)
        override val phoneEvents = MutableSharedFlow<com.couchraoke.tv.data.network.PhoneEvent>()
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> =
            Result.success(manifests[phone.clientId].orEmpty())

        override suspend fun fetchTxt(url: String): Result<ByteArray> =
            Result.failure(UnsupportedOperationException())

        override suspend fun sendAssignSinger(
            phoneId: String,
            message: com.couchraoke.tv.data.network.AssignSingerMessage,
        ) = Unit

        override suspend fun broadcastPlaybackState(
            message: com.couchraoke.tv.data.network.PlaybackStateMessage,
        ) = Unit

        override suspend fun sendSessionState(phoneId: String) = Unit

        override suspend fun sendPing(phoneId: String): com.couchraoke.tv.data.network.PongResponse = error("unused")

        override suspend fun sendClockAck(
            phoneId: String,
            ack: com.couchraoke.tv.data.network.ClockAckMessage,
        ) = Unit
    }
}
