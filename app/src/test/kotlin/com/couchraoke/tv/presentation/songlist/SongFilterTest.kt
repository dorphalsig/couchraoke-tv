package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongFilterTest {

    private fun testSong(songId: String, title: String = "T", artist: String = "A", album: String? = null) = SongEntry(
        songId = songId, phoneClientId = "p1", relativeTxtPath = "t.txt", modifiedTimeMs = 0L,
        isValid = true, txtUrl = "http://x", title = title, artist = artist, album = album,
    )

    private fun createSong(
        songId: String = "id",
        artist: String? = null,
        album: String? = null,
        title: String? = null
    ): SongEntry {
        return SongEntry(
            songId = songId,
            phoneClientId = "client",
            relativeTxtPath = "path/to/$songId.txt",
            modifiedTimeMs = 0L,
            isValid = true,
            artist = artist,
            album = album,
            title = title,
            txtUrl = "http://example.com/$songId.txt"
        )
    }

    @Test
    fun `given empty query, when filterSongs called, then all songs returned`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Artist 1"),
            createSong(songId = "2", artist = "Artist 2")
        )
        val result = filterSongs(songs, "")
        assertEquals(songs, result)
    }

    @Test
    fun `given blank query, when filterSongs called, then all songs returned`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Artist 1"),
            createSong(songId = "2", artist = "Artist 2")
        )
        val result = filterSongs(songs, "   ")
        assertEquals(songs, result)
    }

    @Test
    fun `given query matching artist case-insensitively, then song included`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Radiohead"),
            createSong(songId = "2", artist = "Muse")
        )
        val result = filterSongs(songs, "RADIO")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given query matching album case-insensitively, then song included`() {
        val songs = listOf(
            createSong(songId = "1", album = "OK Computer"),
            createSong(songId = "2", album = "Absolution")
        )
        val result = filterSongs(songs, "computer")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given query matching title case-insensitively, then song included`() {
        val songs = listOf(
            createSong(songId = "1", title = "Paranoid Android"),
            createSong(songId = "2", title = "Hysteria")
        )
        val result = filterSongs(songs, "ANDROID")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given non-matching query, then empty list returned`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Artist", album = "Album", title = "Title")
        )
        val result = filterSongs(songs, "nomatch")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `given query matching only artist (not album or title), then song included`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Target", album = "Other", title = "Other")
        )
        val result = filterSongs(songs, "Target")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given query matching only album (not artist or title), then song included`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Other", album = "Target", title = "Other")
        )
        val result = filterSongs(songs, "Target")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given query matching only title (not artist or album), then song included`() {
        val songs = listOf(
            createSong(songId = "1", artist = "Other", album = "Other", title = "Target")
        )
        val result = filterSongs(songs, "Target")
        assertEquals(1, result.size)
        assertEquals("1", result[0].songId)
    }

    @Test
    fun `given query matches title only, when filtered, then song included`() {
        val songs = listOf(testSong(songId = "1", title = "MatchMe", artist = "Other", album = "Other"))
        val result = filterSongs(songs, "MatchMe")
        assertEquals(1, result.size)
    }

    @Test
    fun `given query matches album only, when filtered, then song included`() {
        val songs = listOf(testSong(songId = "1", title = "Other", artist = "Other", album = "MatchMe"))
        val result = filterSongs(songs, "MatchMe")
        assertEquals(1, result.size)
    }

    @Test
    fun `given songs with null fields, when filtering, then no exception and correct matching`() {
        val songs = listOf(
            createSong(songId = "1", artist = null, album = null, title = null),
            createSong(songId = "2", artist = "Target", album = null, title = null),
            createSong(songId = "3", artist = null, album = "Target", title = null),
            createSong(songId = "4", artist = null, album = null, title = "Target")
        )
        val result = filterSongs(songs, "Target")
        assertEquals(3, result.size)
        val ids = result.map { it.songId }.toSet()
        assertTrue(ids.contains("2"))
        assertTrue(ids.contains("3"))
        assertTrue(ids.contains("4"))
    }
}
