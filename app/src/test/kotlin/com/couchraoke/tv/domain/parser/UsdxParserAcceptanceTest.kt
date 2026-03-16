package com.couchraoke.tv.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdxParserAcceptanceTest {
    private val parser = UsdxParser()

    @Test
    fun `parses valid minimal fixture into one valid track`() {
        val result = parseFixture("fixtures/parser/valid/01_valid_minimal_v1")

        assertTrue(result.parsedSong.isValid)
        assertEquals("Minimal", result.parsedSong.header.title)
        assertEquals(1, result.parsedSong.tracks.size)
        assertEquals(1, result.parsedSong.tracks.single().lines.size)
        assertEquals("la", result.parsedSong.tracks.single().lines.single().notes.single().lyricText)
        assertNull(result.invalidCode)
    }

    @Test
    fun `parses metadata rich fixture with optional metadata`() {
        val result = parseFixture("fixtures/parser/valid/metadata_rich")

        assertTrue(result.parsedSong.isValid)
        assertEquals("video.mp4", result.parsedSong.header.videoReference)
        assertEquals("cover.jpg", result.parsedSong.header.coverReference)
        assertEquals("bg.jpg", result.parsedSong.header.backgroundReference)
        assertEquals("inst.ogg", result.parsedSong.header.instrumentalReference)
        assertEquals("vocals.ogg", result.parsedSong.header.vocalsReference)
        assertEquals(12.5, requireNotNull(result.parsedSong.header.previewStartSec), 0.0)
        assertFalse(result.parsedSong.derivedSummary.isDuet)
    }

    @Test
    fun `parses duet fixture into two routed tracks`() {
        val result = parseFixture("fixtures/parser/duet/11_duet_valid")

        assertTrue(result.parsedSong.isValid)
        assertEquals(2, result.parsedSong.tracks.size)
        assertEquals(TrackId.P1, result.parsedSong.tracks[0].trackId)
        assertEquals(TrackId.P2, result.parsedSong.tracks[1].trackId)
        assertTrue(result.parsedSong.derivedSummary.isDuet)
        assertEquals("Alice", result.parsedSong.header.p1Name)
        assertEquals("Bob", result.parsedSong.header.p2Name)
    }

    @Test
    fun `rejects a fixture missing required artist`() {
        val result = parseFixture("fixtures/parser/invalid/05_missing_required_artist")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.MISSING_REQUIRED_ARTIST, result.invalidCode)
    }

    @Test
    fun `rejects a fixture missing required audio reference`() {
        val result = parseFixture("fixtures/parser/invalid/06_missing_required_audio")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.MISSING_REQUIRED_AUDIO, result.invalidCode)
    }

    @Test
    fun `rejects a fixture with missing required audio file`() {
        val result = parseFixture("fixtures/parser/invalid/07_missing_required_audio_file")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.REQUIRED_AUDIO_NOT_FOUND, result.invalidCode)
    }

    @Test
    fun `rejects a fixture with malformed body numeric fields`() {
        val result = parseFixture("fixtures/parser/invalid/08_malformed_body_numeric")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.MALFORMED_BODY_FIELD, result.invalidCode)
        assertEquals(6, result.parsedSong.diagnostics.first { it.code == DiagnosticCode.MALFORMED_BODY_FIELD }.lineNumber)
    }

    @Test
    fun `keeps a fixture with no line breaks valid when notes exist`() {
        val result = parseFixture("fixtures/parser/invalid/09_no_breaks_allowed_usdx")

        assertTrue(result.parsedSong.isValid)
        assertEquals(1, result.parsedSong.tracks.single().lines.size)
    }

    @Test
    fun `rejects a fixture with no usable notes after cleanup`() {
        val result = parseFixture("fixtures/parser/invalid/10_no_notes_invalid")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.EMPTY_TRACK_AFTER_CLEANUP, result.invalidCode)
    }

    @Test
    fun `rejects a fixture with an invalid duet marker`() {
        val result = parseFixture("fixtures/parser/invalid/12_duet_invalid_marker")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.INVALID_DUET_MARKER, result.invalidCode)
        assertEquals(6, result.parsedSong.diagnostics.first { it.code == DiagnosticCode.INVALID_DUET_MARKER }.lineNumber)
    }

    @Test
    fun `rejects a fixture with unsupported relative format in version one`() {
        val result = parseFixture("fixtures/parser/invalid/14_relative_in_v1_invalid")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.UNSUPPORTED_RELATIVE_FORMAT, result.invalidCode)
    }

    @Test
    fun `rejects a fixture with unsupported variable bpm`() {
        val result = parseFixture("fixtures/parser/invalid/15_bpm_change_non_relative")

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.UNSUPPORTED_VARIABLE_BPM, result.invalidCode)
        assertEquals(7, result.parsedSong.diagnostics.first { it.code == DiagnosticCode.UNSUPPORTED_VARIABLE_BPM }.lineNumber)
    }

    @Test
    fun `derives rap medley and preview fallback metadata`() {
        val result = parseFixture("fixtures/parser/derived/rap_preview_medley")

        assertTrue(result.parsedSong.isValid)
        assertTrue(result.parsedSong.derivedSummary.hasRap)
        assertEquals(MedleySource.EXPLICIT, result.parsedSong.derivedSummary.medleySource)
        assertEquals(32, result.parsedSong.derivedSummary.medleyStartBeat)
        assertEquals(64, result.parsedSong.derivedSummary.medleyEndBeat)
        assertNull(result.parsedSong.derivedSummary.previewStartSec)
    }

    @Test
    fun `preserves unknown tags and malformed optional headers in acceptance parsing`() {
        val result = parseFixture("fixtures/parser/edge/03_unknown_header_tags")

        assertTrue(result.parsedSong.isValid)
        assertEquals(3, result.parsedSong.header.customTags.size)
        assertEquals("NOVALUE", result.parsedSong.header.customTags[2].tagName)
        assertEquals(DiagnosticCode.MALFORMED_OPTIONAL_HEADER, result.parsedSong.diagnostics.last().code)
    }

    @Test
    fun `handles duplicate tags zero duration unknown tokens and external video correctly`() {
        val duplicates = parseFixture("fixtures/parser/edge/02_header_duplicates_last_wins")
        assertTrue(duplicates.parsedSong.isValid)
        assertEquals("New Title", duplicates.parsedSong.header.title)
        assertEquals("audio.ogg", duplicates.parsedSong.header.audioReference)

        val zeroDuration = parseFixture("fixtures/parser/derived/06_duration_zero_converts_to_freestyle")
        assertTrue(zeroDuration.parsedSong.isValid)
        assertEquals(NoteType.FREESTYLE, zeroDuration.parsedSong.tracks.single().lines.single().notes.single().noteType)

        val unknownBody = parseFixture("fixtures/parser/edge/07_unknown_body_token_ignored")
        assertTrue(unknownBody.parsedSong.isValid)
        assertEquals(DiagnosticCode.UNKNOWN_BODY_TOKEN, unknownBody.parsedSong.diagnostics.single().code)

        val externalVideo = parseFixture("fixtures/parser/edge/external_video")
        assertTrue(externalVideo.parsedSong.isValid)
        assertFalse(externalVideo.parsedSong.derivedSummary.hasVideo)
    }

    private fun parseFixture(path: String): ParseResult {
        val classLoader = requireNotNull(javaClass.classLoader)
        val songText = classLoader.getResource("$path/song.txt")!!.readText()
        return parser.parse(
            songIdentifier = path,
            rawText = songText,
            fileResolver = FileResolver { assetPath ->
                classLoader.getResource("$path/$assetPath") != null
            },
        )
    }
}
