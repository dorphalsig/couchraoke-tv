package com.couchraoke.tv.data.library

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.data.network.parseManifestJson
import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ManifestLibraryManagerTest {
    @Test(timeout = 30_000)
    fun aggregatesConnectedPhoneManifestIntoIndexedSongs() = runBlocking {
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(song(title = "Beta"))),
        )
        val manager = ManifestLibraryManager(network)

        manager.onPhoneConnected(phone())

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

        manager.refreshAll()

        assertEquals(listOf("Playable"), manager.songs.first().map { it.title })
        assertNull(manager.getSong("missing"))
    }

    @Test(timeout = 30_000)
    fun parseManifestDropsEntriesWithMissingRequiredBooleanOrNumericFields() {
        val json = """
            [
              {
                "relativeTxtPath":"songs/missing-boolean.txt",
                "modifiedTimeMs":1,
                "title":"Missing Boolean",
                "artist":"Artist",
                "hasRap":false,
                "hasVideo":false,
                "hasInstrumental":false,
                "canMedley":false,
                "startSec":0.0,
                "previewStartSec":0.0,
                "txtUrl":"http://127.0.0.1/missing-boolean.txt",
                "audioUrl":"http://127.0.0.1/missing-boolean.mp3"
              },
              {
                "relativeTxtPath":"songs/missing-number.txt",
                "modifiedTimeMs":2,
                "title":"Missing Number",
                "artist":"Artist",
                "isDuet":false,
                "hasRap":false,
                "hasVideo":false,
                "hasInstrumental":false,
                "canMedley":false,
                "previewStartSec":0.0,
                "txtUrl":"http://127.0.0.1/missing-number.txt",
                "audioUrl":"http://127.0.0.1/missing-number.mp3"
              }
            ]
        """.trimIndent()

        assertEquals(emptyList<SongEntry>(), parseManifestJson(json))
    }

    @Test(timeout = 30_000)
    fun rejectsManifestEntriesWithInvalidRelativePaths() = runBlocking {
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(
                SoloSingFixtures.PhoneClientId to listOf(
                    song(title = "Absolute", relativeTxtPath = "/absolute/song.txt"),
                    song(title = "Current", relativeTxtPath = "songs/./song.txt"),
                    song(title = "Parent", relativeTxtPath = "songs/../song.txt"),
                    song(title = "Playable", relativeTxtPath = "songs/playable.txt"),
                ),
            ),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshAll()

        assertEquals(listOf("Playable"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun rejectsVideoFlagAndUrlMismatches() = runBlocking {
        val valid = song(title = "Playable", hasVideo = true, videoUrl = SoloSingFixtures.assetUrl("/video.mp4"))
        val missingVideoUrl = song(title = "Missing Video URL", hasVideo = true, videoUrl = null)
        val unexpectedVideoUrl = song(
            title = "Unexpected Video URL",
            hasVideo = false,
            videoUrl = SoloSingFixtures.assetUrl("/video.mp4"),
        )
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(valid, missingVideoUrl, unexpectedVideoUrl)),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshAll()

        assertEquals(listOf("Playable"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun preservesByteExactSongIdAndInstrumentalFlag() = runBlocking {
        val relativePath = "Artist/Case Sensitive Ä Song.txt"
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(
                SoloSingFixtures.PhoneClientId to listOf(
                    song(relativeTxtPath = relativePath, title = "Instrumental", genre = "Pop", hasInstrumental = true),
                ),
            ),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshAll()

        val indexedSong = manager.songs.first().single()
        assertEquals("${SoloSingFixtures.PhoneClientId}::$relativePath", indexedSong.songId)
        assertEquals(relativePath, indexedSong.relativeTxtPath)
        assertEquals(true, indexedSong.hasInstrumental)
    }

    @Test(timeout = 30_000)
    fun parsesManifestJsonArrayAndObjectForms() {
        val entryJson = """
            {
              "relativeTxtPath":"songs/Song.txt",
              "modifiedTimeMs":1712000000000,
              "title":"Song",
              "artist":"Artist",
              "album":"Album",
              "year":2024,
              "genre":"Pop",
              "isDuet":true,
              "hasRap":true,
              "hasVideo":true,
              "hasInstrumental":true,
              "canMedley":true,
              "medleySource":"tag",
              "medleyStartBeat":4,
              "medleyEndBeat":12,
              "startSec":1.5,
              "previewStartSec":2.5,
              "txtUrl":"http://127.0.0.1/song.txt",
              "audioUrl":"http://127.0.0.1/song.mp3",
              "videoUrl":"http://127.0.0.1/song.mp4",
              "coverUrl":"http://127.0.0.1/cover.jpg",
              "backgroundUrl":"http://127.0.0.1/background.jpg"
            }
        """.trimIndent()

        val arrayEntry = parseManifestJson("[$entryJson]").single()
        val objectEntry = parseManifestJson("{\"songs\":[$entryJson]}").single()

        assertEquals(arrayEntry, objectEntry)
        assertEquals("songs/Song.txt", arrayEntry.relativeTxtPath)
        assertEquals(true, arrayEntry.isDuet)
        assertEquals(true, arrayEntry.hasRap)
        assertEquals(true, arrayEntry.hasVideo)
        assertEquals(true, arrayEntry.hasInstrumental)
        assertEquals(true, arrayEntry.canMedley)
        assertEquals("tag", arrayEntry.medleySource)
        assertEquals(4, arrayEntry.medleyStartBeat)
        assertEquals(12, arrayEntry.medleyEndBeat)
    }

    @Test(timeout = 30_000)
    fun rejectsManifestJsonWithInstrumentalOrVocalsUrls() {
        val json = """
            [
              {
                "relativeTxtPath":"song.txt",
                "modifiedTimeMs":1,
                "title":"Song",
                "artist":"Artist",
                "isDuet":false,
                "hasRap":false,
                "hasVideo":false,
                "hasInstrumental":true,
                "canMedley":false,
                "startSec":0.0,
                "previewStartSec":0.0,
                "txtUrl":"http://127.0.0.1/song.txt",
                "audioUrl":"http://127.0.0.1/song.mp3",
                "instrumentalUrl":"http://127.0.0.1/instrumental.mp3",
                "vocalsUrl":"http://127.0.0.1/vocals.mp3"
              }
            ]
        """.trimIndent()

        try {
            parseManifestJson(json)
            fail("Expected unsupported dual-track manifest fields to be rejected")
        } catch (_: SerializationException) {
        }
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

        manager.refreshAll()

        assertEquals(listOf("First", "third", "Second", "Last"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun refreshesCatalogWhenConnectedPhonesChange() = runBlocking {
        val network = FakeNetworkController(
            phones = emptyList(),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(song(title = "Connected Song"))),
        )
        val manager = ManifestLibraryManager(network)
        val refreshJob = manager.launchConnectedPhoneRefresh(this)

        network.connectedPhones.value = listOf(phone())

        val songs = withTimeout(5_000L) { manager.songs.first { it.isNotEmpty() } }
        assertEquals(listOf("Connected Song"), songs.map { it.title })
        assertEquals(listOf(SoloSingFixtures.PhoneClientId), network.fetchedManifestPhoneIds)

        refreshJob.cancel()
    }

    @Test(timeout = 30_000)
    fun keepsPreviousPhoneCatalogAndLogsWhenManifestRefreshFails() = runBlocking {
        val logs = mutableListOf<String>()
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(SoloSingFixtures.PhoneClientId to listOf(song(title = "Cached Song"))),
        )
        val manager = ManifestLibraryManager(network, debugLog = logs::add)
        manager.refreshAll()
        network.manifestFailures += SoloSingFixtures.PhoneClientId

        manager.refreshAll()

        assertEquals(listOf("Cached Song"), manager.songs.first().map { it.title })
        assertEquals(
            listOf(
                "Refreshing catalog for 1 connected phone(s).",
                "Fetching manifest for phone-client-001 at 192.168.1.23:43210.",
                "Catalog refresh accepted 1 song(s), rejected 0 song(s), total visible 1.",
                "Refreshing catalog for 1 connected phone(s).",
                "Fetching manifest for phone-client-001 at 192.168.1.23:43210.",
                "Manifest fetch failed for phone-client-001; retaining 1 cached song(s).",
                "Catalog refresh accepted 0 song(s), rejected 0 song(s), total visible 1.",
            ),
            logs,
        )
    }

    @Test(timeout = 30_000)
    fun removesDisconnectedPhoneCatalogAndLogsRejectedManifestEntries() = runBlocking {
        val logs = mutableListOf<String>()
        val network = FakeNetworkController(
            phones = listOf(phone()),
            manifests = mapOf(
                SoloSingFixtures.PhoneClientId to listOf(
                    song(title = "Playable"),
                    song(title = "No Audio", audioUrl = ""),
                ),
            ),
        )
        val manager = ManifestLibraryManager(network, debugLog = logs::add)
        manager.refreshAll()
        network.connectedPhones.value = emptyList()

        manager.refreshAll()

        assertEquals(emptyList<String>(), manager.songs.first().map { it.title })
        assertEquals(
            listOf(
                "Refreshing catalog for 1 connected phone(s).",
                "Fetching manifest for phone-client-001 at 192.168.1.23:43210.",
                "Rejected manifest entry for phone-client-001: songs/No Audio.txt.",
                "Catalog refresh accepted 1 song(s), rejected 1 song(s), total visible 1.",
                "Refreshing catalog for 0 connected phone(s).",
                "Catalog refresh accepted 0 song(s), rejected 0 song(s), total visible 0.",
            ),
            logs,
        )
    }

    @Test(timeout = 30_000)
    fun onPhoneConnectedFetchesOnlyThatPhoneBeforeSongsBecomeVisible() = runBlocking {
        val firstPhone = phone(clientId = "phone-a")
        val secondPhone = phone(clientId = "phone-b")
        val network = FakeNetworkController(
            phones = listOf(firstPhone, secondPhone),
            manifests = mapOf(
                "phone-a" to listOf(song(title = "First Phone Song")),
                "phone-b" to listOf(song(title = "Second Phone Song")),
            ),
        )
        val manager = ManifestLibraryManager(network)

        manager.onPhoneConnected(firstPhone)

        assertEquals(listOf("phone-a"), network.fetchedManifestPhoneIds)
        assertEquals(listOf("First Phone Song"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun onPhoneDisconnectedImmediatelyRemovesOnlyThatPhonesSongs() = runBlocking {
        val firstPhone = phone(clientId = "phone-a")
        val secondPhone = phone(clientId = "phone-b")
        val network = FakeNetworkController(
            phones = listOf(firstPhone, secondPhone),
            manifests = mapOf(
                "phone-a" to listOf(song(title = "First Phone Song")),
                "phone-b" to listOf(song(title = "Second Phone Song")),
            ),
        )
        val manager = ManifestLibraryManager(network)
        manager.refreshAll()

        manager.onPhoneDisconnected("phone-a")

        assertEquals(listOf("Second Phone Song"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun refreshPhoneReplacesOnlyThatPhonesCatalog() = runBlocking {
        val firstPhone = phone(clientId = "phone-a")
        val secondPhone = phone(clientId = "phone-b")
        val network = FakeNetworkController(
            phones = listOf(firstPhone, secondPhone),
            manifests = mapOf(
                "phone-a" to listOf(song(title = "Old First Song")),
                "phone-b" to listOf(song(title = "Second Phone Song")),
            ),
        )
        val manager = ManifestLibraryManager(network)
        manager.refreshAll()
        network.manifests["phone-a"] = listOf(song(title = "New First Song"))
        network.manifests["phone-b"] = listOf(song(title = "Unexpected Replacement"))
        network.fetchedManifestPhoneIds.clear()

        manager.refreshPhone("phone-a")

        assertEquals(listOf("phone-a"), network.fetchedManifestPhoneIds)
        assertEquals(listOf("New First Song", "Second Phone Song"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun refreshAllRefreshesEveryConnectedPhone() = runBlocking {
        val firstPhone = phone(clientId = "phone-a")
        val secondPhone = phone(clientId = "phone-b")
        val network = FakeNetworkController(
            phones = listOf(firstPhone, secondPhone),
            manifests = mapOf(
                "phone-a" to listOf(song(title = "First Phone Song")),
                "phone-b" to listOf(song(title = "Second Phone Song")),
            ),
        )
        val manager = ManifestLibraryManager(network)

        manager.refreshAll()

        assertEquals(listOf("phone-a", "phone-b"), network.fetchedManifestPhoneIds)
        assertEquals(listOf("First Phone Song", "Second Phone Song"), manager.songs.first().map { it.title })
    }

    @Test(timeout = 30_000)
    fun connectedPhoneCollectionDoesNotRefetchUnrelatedPhones() = runBlocking {
        val firstPhone = phone(clientId = "phone-a")
        val secondPhone = phone(clientId = "phone-b")
        val network = FakeNetworkController(
            phones = listOf(firstPhone),
            manifests = mapOf(
                "phone-a" to listOf(song(title = "First Phone Song")),
                "phone-b" to listOf(song(title = "Second Phone Song")),
            ),
        )
        val manager = ManifestLibraryManager(network)
        val refreshJob = manager.launchConnectedPhoneRefresh(this)
        withTimeout(5_000L) { manager.songs.first { it.isNotEmpty() } }
        network.fetchedManifestPhoneIds.clear()

        network.connectedPhones.value = listOf(firstPhone, secondPhone)

        withTimeout(5_000L) { manager.songs.first { it.size == 2 } }
        assertEquals(listOf("phone-b"), network.fetchedManifestPhoneIds)
        refreshJob.cancel()
    }

    private fun phone(
        clientId: String = SoloSingFixtures.PhoneClientId,
        connectionId: UShort = SoloSingFixtures.PhoneConnectionId,
        deviceName: String = SoloSingFixtures.PhoneDeviceName,
        httpPort: Int = SoloSingFixtures.PhoneHttpPort,
        ipAddress: String = SoloSingFixtures.PhoneIpAddress,
    ): ConnectedPhone = ConnectedPhone(
        clientId = clientId,
        connectionId = connectionId,
        deviceName = deviceName,
        httpPort = httpPort,
        ipAddress = ipAddress,
    )

    private fun song(
        artist: String = SoloSingFixtures.SongArtist,
        album: String? = SoloSingFixtures.SongAlbum,
        title: String = SoloSingFixtures.SongTitle,
        relativeTxtPath: String = "songs/$title.txt",
        genre: String? = SoloSingFixtures.SongGenre,
        txtUrl: String = SoloSingFixtures.assetUrl("/songs/${SoloSingFixtures.RelativeTxtPath}"),
        audioUrl: String = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
        hasVideo: Boolean = false,
        hasInstrumental: Boolean = false,
        videoUrl: String? = null,
    ): SongEntry = SongEntry(
        relativeTxtPath = relativeTxtPath,
        modifiedTimeMs = SoloSingFixtures.ModifiedTimeMs + title.hashCode(),
        title = title,
        artist = artist,
        album = album,
        year = SoloSingFixtures.SongYear,
        genre = genre,
        isDuet = false,
        hasRap = false,
        hasVideo = hasVideo,
        hasInstrumental = hasInstrumental,
        canMedley = false,
        medleySource = null,
        medleyStartBeat = null,
        medleyEndBeat = null,
        startSec = SoloSingFixtures.StartSec,
        previewStartSec = SoloSingFixtures.PreviewStartSec,
        txtUrl = txtUrl,
        audioUrl = audioUrl,
        videoUrl = videoUrl,
        coverUrl = null,
        backgroundUrl = null,
    )

    private class FakeNetworkController(
        phones: List<ConnectedPhone>,
        manifests: Map<String, List<SongEntry>>,
    ) : NetworkController {
        val manifests = manifests.toMutableMap()
        override val connectedPhones = MutableStateFlow(phones)
        override val phoneEvents = MutableSharedFlow<com.couchraoke.tv.data.network.PhoneEvent>()
        val fetchedManifestPhoneIds = mutableListOf<String>()
        val manifestFailures = mutableSetOf<String>()
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> {
            fetchedManifestPhoneIds += phone.clientId
            return if (phone.clientId in manifestFailures) {
                Result.failure(IllegalStateException("manifest unavailable"))
            } else {
                Result.success(manifests[phone.clientId].orEmpty())
            }
        }

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
