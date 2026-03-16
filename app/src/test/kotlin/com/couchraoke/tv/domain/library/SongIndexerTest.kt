package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.FileResolver
import com.couchraoke.tv.domain.parser.MedleySource
import com.couchraoke.tv.domain.parser.UsdxParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongIndexerTest {

    private val parser = UsdxParser()
    private val fileResolver = FileResolver { true }

    private fun indexSong(rawText: String, relativePath: String = "song.txt"): SongEntry {
        val parseResult = parser.parse("test-song", rawText, fileResolver)
        return SongIndexer.fromParseResult(
            parseResult = parseResult,
            phoneClientId = "phone1",
            relativeTxtPath = relativePath,
            modifiedTimeMs = 123456789L,
            txtUrl = "http://phone/song.txt",
            audioUrl = "http://phone/song.mp3",
            videoUrl = null,
            coverUrl = null,
            backgroundUrl = null,
            instrumentalUrl = null,
            vocalsUrl = null
        )
    }

    @Test
    fun `given P1 and P2 tracks when indexed then isDuet is true`() {
        val song = """
            #TITLE:Duet
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            P1
            : 0 1 60 Ly
            : 1 1 62 ric
            - 2
            P2
            : 4 1 64 O
            : 5 1 66 ther
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertTrue(entry.isDuet)
    }

    @Test
    fun `given only one track when indexed then isDuet is false`() {
        val song = """
            #TITLE:Solo
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            : 0 1 60 Ly
            : 1 1 62 ric
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertFalse(entry.isDuet)
    }

    @Test
    fun `given R or G notes when indexed then hasRap is true`() {
        val rapSong = """
            #TITLE:Rap
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            R 0 1 60 Rap
            G 2 1 62 Golden
            E
        """.trimIndent()

        val entry = indexSong(rapSong)
        assertTrue(entry.hasRap)
    }

    @Test
    fun `given no R or G notes when indexed then hasRap is false`() {
        val normalSong = """
            #TITLE:Normal
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            : 0 1 60 Ly
            * 2 1 62 ric
            E
        """.trimIndent()

        val entry = indexSong(normalSong)
        assertFalse(entry.hasRap)
    }

    @Test
    fun `given duet song when indexed then canMedley is false regardless of tags`() {
        val song = """
            #TITLE:Duet Medley
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            #MEDLEYSTARTBEAT:100
            #MEDLEYENDBEAT:200
            P1
            : 0 1 60 A
            P2
            : 4 1 60 B
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertFalse(entry.canMedley)
    }

    @Test
    fun `given non-duet song with medley tags when indexed then canMedley is true and beats stored`() {
        val song = """
            #TITLE:Medley
            #ARTIST:Artist
            #BPM:120
            #GAP:1000
            #AUDIO:song.mp3
            #MEDLEYSTARTBEAT:100
            #MEDLEYENDBEAT:200
            : 0 1 60 A
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertTrue(entry.canMedley)
        assertEquals(MedleySource.EXPLICIT, entry.medleySource)
        assertEquals(Integer.valueOf(100), entry.medleyStartBeat)
        assertEquals(Integer.valueOf(200), entry.medleyEndBeat)
    }

    @Test
    fun `given non-duet song with no medley tags when indexed then canMedley is false`() {
        val song = """
            #TITLE:No Medley
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            : 0 1 60 A
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertFalse(entry.canMedley)
        assertEquals(MedleySource.NONE, entry.medleySource)
    }

    @Test
    fun `given PREVIEWSTART tag when indexed then previewStartSec matches tag`() {
        val song = """
            #TITLE:Preview
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            #PREVIEWSTART:45.0
            : 0 1 60 A
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertEquals(45.0, entry.previewStartSec, 0.001)
    }

    @Test
    fun `given no PREVIEWSTART but medley tags present when indexed then previewStartSec is calculated from medleyStartBeat`() {
        // medleyStartBeat * 60.0 / (bpm * 4.0) + gapMs / 1000.0
        // 100 * 60.0 / (120 * 4.0) + 1000 / 1000.0
        // 6000.0 / 480.0 + 1.0 = 12.5 + 1.0 = 13.5
        val song = """
            #TITLE:Medley Fallback
            #ARTIST:Artist
            #BPM:120
            #GAP:1000
            #AUDIO:song.mp3
            #MEDLEYSTARTBEAT:100
            #MEDLEYENDBEAT:200
            : 0 1 60 A
            E
        """.trimIndent()

        val entry = indexSong(song)
        val expected = 13.5
        assertEquals(expected, entry.previewStartSec, 0.001)
    }

    @Test
    fun `given no PREVIEWSTART and no medley tags when indexed then previewStartSec is 0`() {
        val song = """
            #TITLE:No Preview
            #ARTIST:Artist
            #BPM:120
            #GAP:1000
            #AUDIO:song.mp3
            : 0 1 60 A
            E
        """.trimIndent()

        val entry = indexSong(song)
        assertEquals(0.0, entry.previewStartSec, 0.001)
    }

    @Test
    fun `given valid manifest entry, when fromManifestEntry, then SongEntry fields match`() {
        val entry = ManifestEntry(
            relativeTxtPath = "songs/artist/song.txt",
            isValid = true,
            invalidReasonCode = null,
            invalidLineNumber = null,
            modifiedTimeMs = 9876543210L,
            title = "My Song",
            artist = "My Artist",
            album = "My Album",
            isDuet = false,
            hasRap = false,
            hasVideo = true,
            hasInstrumental = false,
            canMedley = true,
            medleySource = "tag",
            medleyStartBeat = 50,
            medleyEndBeat = 150,
            previewStartSec = 30.5,
            txtUrl = "http://phone/songs/artist/song.txt",
            audioUrl = "http://phone/songs/artist/song.mp3",
        )

        val result = SongIndexer.fromManifestEntry(entry, "phone-1")

        assertEquals("phone-1::songs/artist/song.txt", result.songId)
        assertTrue(result.isValid)
        assertEquals("My Song", result.title)
        assertEquals("My Artist", result.artist)
        assertFalse(result.isDuet)
        assertTrue(result.canMedley)
        assertEquals(30.5, result.previewStartSec, 0.001)
        assertEquals("http://phone/songs/artist/song.mp3", result.audioUrl)
    }

    @Test
    fun `given manifest entry with path traversal, when fromManifestEntry, then isValid false and reason is INVALID_PATH`() {
        val entry = ManifestEntry(
            relativeTxtPath = "../evil/song.txt",
            isValid = true,
            modifiedTimeMs = 1000L,
            title = "Evil",
            artist = "Hacker",
            isDuet = false,
            hasRap = false,
            hasVideo = false,
            hasInstrumental = false,
            canMedley = false,
            txtUrl = "http://phone/evil/song.txt",
        )

        val result = SongIndexer.fromManifestEntry(entry, "phone-1")

        assertFalse(result.isValid)
        assertEquals("ERROR_CORRUPT_SONG_INVALID_PATH", result.invalidReasonCode)
    }

    @Test
    fun `given manifest entry with isValid=false from phone, when fromManifestEntry, then isValid false and invalidReasonCode preserved`() {
        val entry = ManifestEntry(
            relativeTxtPath = "songs/broken/song.txt",
            isValid = false,
            invalidReasonCode = "ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER",
            invalidLineNumber = 3,
            modifiedTimeMs = 5000L,
            title = "Broken",
            artist = "Nobody",
            isDuet = false,
            hasRap = false,
            hasVideo = false,
            hasInstrumental = false,
            canMedley = false,
            txtUrl = "http://phone/songs/broken/song.txt",
        )

        val result = SongIndexer.fromManifestEntry(entry, "phone-1")

        assertFalse(result.isValid)
        assertEquals("ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER", result.invalidReasonCode)
        assertEquals(Integer.valueOf(3), result.invalidLineNumber)
    }

    @Test
    fun `given PREVIEWSTART 0 or negative when indexed then treated as absent and falls back`() {
        // Fallback to medley if present
        val song0 = """
            #TITLE:Preview Zero
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            #PREVIEWSTART:0
            #MEDLEYSTARTBEAT:100
            #MEDLEYENDBEAT:200
            : 0 1 60 A
            E
        """.trimIndent()
        // 100 * 60 / (120 * 4) + 0 = 12.5
        assertEquals(12.5, indexSong(song0).previewStartSec, 0.001)

        val songNeg = """
            #TITLE:Preview Negative
            #ARTIST:Artist
            #BPM:120
            #GAP:0
            #AUDIO:song.mp3
            #PREVIEWSTART:-1.0
            #MEDLEYSTARTBEAT:100
            #MEDLEYENDBEAT:200
            : 0 1 60 A
            E
        """.trimIndent()
        assertEquals(12.5, indexSong(songNeg).previewStartSec, 0.001)
    }
}
