package com.couchraoke.tv.domain.library

import org.junit.Assert.*
import org.junit.Test

class SongLibraryTest {

    private fun entry(phoneClientId: String, relativeTxtPath: String) = SongEntry(
        songId = "$phoneClientId::$relativeTxtPath",
        phoneClientId = phoneClientId,
        relativeTxtPath = relativeTxtPath,
        modifiedTimeMs = 0L,
        isValid = true,
        txtUrl = "http://stub",
        artist = relativeTxtPath,
        title = relativeTxtPath,
    )

    @Test
    fun `given two phones each with 3 songs, when both added, then library contains 6 songs`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-a", listOf(entry("phone-a", "a/s1.txt"), entry("phone-a", "a/s2.txt"), entry("phone-a", "a/s3.txt")))
        library.addPhone("phone-b", listOf(entry("phone-b", "b/s1.txt"), entry("phone-b", "b/s2.txt"), entry("phone-b", "b/s3.txt")))
        assertEquals(6, library.getSortedSongs().size)
    }

    @Test
    fun `given phone-a has 4 songs, when removePhone called, then all 4 removed`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-a", listOf(entry("phone-a","s1.txt"), entry("phone-a","s2.txt"), entry("phone-a","s3.txt"), entry("phone-a","s4.txt")))
        library.removePhone("phone-a")
        assertEquals(0, library.getSortedSongs().size)
        assertTrue(library.getSongsByPhone("phone-a").isEmpty())
    }

    @Test
    fun `given phone-b previously had 3 songs, when addPhone called with 1 song, then only 1 song remains`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-b", listOf(entry("phone-b","s1.txt"), entry("phone-b","s2.txt"), entry("phone-b","s3.txt")))
        library.addPhone("phone-b", listOf(entry("phone-b","s1.txt")))
        assertEquals(1, library.getSortedSongs().size)
    }

    @Test
    fun `given a song entry, when songId read, then equals phoneClientId double-colon relativeTxtPath`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-abc", listOf(entry("phone-abc", "artist/song.txt")))
        assertEquals("phone-abc::artist/song.txt", library.getSortedSongs().first().songId)
    }

    @Test
    fun `given songs from multiple phones, when getSortedSongs called, then sorted by artist then title case-insensitively`() {
        val library = DefaultSongLibrary()
        library.addPhone("p1", listOf(
            SongEntry(songId="p1::c.txt", phoneClientId="p1", relativeTxtPath="c.txt", modifiedTimeMs=0, isValid=true, txtUrl="", artist="Zebra", title="A"),
            SongEntry(songId="p1::a.txt", phoneClientId="p1", relativeTxtPath="a.txt", modifiedTimeMs=0, isValid=true, txtUrl="", artist="Apple", title="A"),
        ))
        library.addPhone("p2", listOf(
            SongEntry(songId="p2::b.txt", phoneClientId="p2", relativeTxtPath="b.txt", modifiedTimeMs=0, isValid=true, txtUrl="", artist="apple", title="A"),
        ))
        val sorted = library.getSortedSongs()
        assertEquals("Apple", sorted[0].artist)
        assertEquals("apple", sorted[1].artist)
        assertEquals("Zebra", sorted[2].artist)
    }

    @Test
    fun `given entry in library, when getSongById called with its songId, then returns that entry`() {
        val library = DefaultSongLibrary()
        val e = entry("phone-x", "song.txt")
        library.addPhone("phone-x", listOf(e))
        assertEquals(e, library.getSongById("phone-x::song.txt"))
    }

    @Test
    fun `given library with entries, when getSongById called with unknown id, then returns null`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-x", listOf(entry("phone-x", "song.txt")))
        assertNull(library.getSongById("nonexistent"))
    }

    @Test
    fun `given phone-a had songs, when addPhone called with empty list, then phone-a has no entries`() {
        val library = DefaultSongLibrary()
        library.addPhone("phone-a", listOf(entry("phone-a", "s.txt")))
        library.addPhone("phone-a", emptyList())
        assertTrue(library.getSongsByPhone("phone-a").isEmpty())
        assertEquals(0, library.getSortedSongs().size)
    }
}