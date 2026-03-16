package com.couchraoke.tv.domain.library

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Paths

@Serializable
data class ExpectedDiscovery(val rootRel: String, val songs: List<ExpectedSong>)

@Serializable
data class ExpectedSong(
    val songDirRel: String,
    val songTxtRel: String,
    val isValid: Boolean,
    val invalidReasonCode: String? = null,
    val invalidLineNumber: Int? = null,
    val artist: String? = null,
    val title: String? = null
)

class SongDiscoveryAcceptanceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `given a song directory when discovering songs then all songs should match expected discovery metadata`() {
        // Given
        val songsRootUri = javaClass.classLoader!!.getResource("fixtures/library/song_discovery/songs_root")!!.toURI()
        val songsRootPath = Paths.get(songsRootUri)

        val expectedJsonText = javaClass.classLoader!!.getResource("fixtures/library/song_discovery/expected.discovery.json")!!.readText()
        val expectedDiscovery = json.decodeFromString<ExpectedDiscovery>(expectedJsonText)

        // When
        val discoveredSongs = SongDiscovery().discoverFromDirectory(songsRootPath)
        val discoveredMap = discoveredSongs.associateBy { it.relativeTxtPath }

        // Then
        assertEquals("Should discover the expected number of songs", expectedDiscovery.songs.size, discoveredSongs.size)

        expectedDiscovery.songs.forEach { expected ->
            val actual = discoveredMap[expected.songTxtRel]
                ?: throw AssertionError("Expected song not discovered: ${expected.songTxtRel}")

            assertEquals("isValid mismatch for ${expected.songTxtRel}", expected.isValid, actual.isValid)
            assertEquals("invalidReasonCode mismatch for ${expected.songTxtRel}", expected.invalidReasonCode, actual.invalidReasonCode)
            assertEquals("invalidLineNumber mismatch for ${expected.songTxtRel}", expected.invalidLineNumber, actual.invalidLineNumber)

            if (expected.artist != null) {
                assertEquals("artist mismatch for ${expected.songTxtRel}", expected.artist, actual.artist)
            }
            if (expected.title != null) {
                assertEquals("title mismatch for ${expected.songTxtRel}", expected.title, actual.title)
            }
        }
    }
}
