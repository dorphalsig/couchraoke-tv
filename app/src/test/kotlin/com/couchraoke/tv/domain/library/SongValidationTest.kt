package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.FileResolver
import com.couchraoke.tv.domain.parser.UsdxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class SongValidationTest {

    private val parser = UsdxParser()
    private val alwaysPresent = FileResolver { true }
    private val neverPresent  = FileResolver { false }

    private fun index(
        rawText: String,
        fileResolver: FileResolver = alwaysPresent,
        relativeTxtPath: String = "song/song.txt",
    ): SongEntry {
        val result = parser.parse("test", rawText, fileResolver)
        return SongIndexer.fromParseResult(
            parseResult       = result,
            phoneClientId     = "phone",
            relativeTxtPath   = relativeTxtPath,
            modifiedTimeMs    = 0L,
            txtUrl            = "http://host/song.txt",
            audioUrl          = null,
            videoUrl          = null,
            coverUrl          = null,
            backgroundUrl     = null,
            instrumentalUrl   = null,
            vocalsUrl         = null,
        )
    }

    @Test
    fun `given missing required header, when indexing, then invalid with MISSING_REQUIRED_HEADER`() {
        val rawText = "#TITLE:T\n#BPM:120\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText)
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER", entry.invalidReasonCode)
    }

    @Test
    fun `given audio file not found, when indexing, then invalid with FILE_NOT_FOUND`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:120\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText, fileResolver = neverPresent)
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_FILE_NOT_FOUND", entry.invalidReasonCode)
    }

    @Test
    fun `given BPM zero, when indexing, then invalid with MISSING_REQUIRED_HEADER`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:0\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText)
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER", entry.invalidReasonCode)
    }

    @Test
    fun `given non-numeric BPM, when indexing, then invalid with MALFORMED_HEADER and line number`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:notanumber\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText)
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_MALFORMED_HEADER", entry.invalidReasonCode)
        assertEquals(3, entry.invalidLineNumber)
    }

    @Test
    fun `given valid song, when indexing, then valid`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:120\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText)
        
        assertTrue(entry.isValid)
        assertNull(entry.invalidReasonCode)
    }

    @Test
    fun `given relativeTxtPath with dot-dot, when indexing, then invalid with INVALID_PATH`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:120\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText, relativeTxtPath = "../evil/song.txt")
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_INVALID_PATH", entry.invalidReasonCode)
    }

    @Test
    fun `given relativeTxtPath starting with slash, when indexing, then invalid with INVALID_PATH`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:120\n#MP3:song.mp3\n: 0 4 0 la\nE"
        
        val entry = index(rawText, relativeTxtPath = "/absolute/song.txt")
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_INVALID_PATH", entry.invalidReasonCode)
    }

    @Test
    fun `given missing body notes, when indexing, then invalid`() {
        val rawText = "#TITLE:T\n#ARTIST:A\n#BPM:120\n#MP3:song.mp3\nE"
        
        val entry = index(rawText)
        
        assertFalse(entry.isValid)
        assertEquals("ERROR_CORRUPT_SONG_NO_NOTES", entry.invalidReasonCode)
    }
}
