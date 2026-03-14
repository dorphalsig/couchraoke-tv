package com.couchraoke.tv.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsedSongModelTest {
    @Test
    fun `parsed song aggregates normalized header tracks derived summary and diagnostics`() {
        val diagnostic = DiagnosticEntry(
            severity = DiagnosticSeverity.WARNING,
            code = DiagnosticCode.UNKNOWN_HEADER_TAG,
            message = "Unknown header preserved",
            songIdentifier = "valid/minimal",
            lineNumber = 6,
        )
        val header = SongHeader(
            version = SongVersion(major = 1, minor = 0, patch = 0),
            title = "Minimal",
            artist = "Test",
            bpm = 120.0,
            gapMs = 0,
            audioReference = "audio.ogg",
            customTags = listOf(CustomTag("FOO", "bar", 6)),
        )
        val note = NoteEvent.Note(
            noteType = NoteType.NORMAL,
            startBeat = 0,
            durationBeats = 4,
            tone = 0,
            lyricText = "la",
        )
        val line = Line(startBeat = 0, notes = listOf(note))
        val track = Track(trackId = TrackId.P1, lines = listOf(line))
        val summary = DerivedSongSummary(
            isDuet = false,
            hasRap = false,
            hasVideo = false,
            hasInstrumental = false,
            previewStartSec = null,
            medleySource = MedleySource.NONE,
            medleyStartBeat = null,
            medleyEndBeat = null,
            calcMedleyEnabled = false,
        )

        val parsedSong = ParsedSong(
            songIdentifier = "valid/minimal",
            isValid = true,
            header = header,
            tracks = listOf(track),
            derivedSummary = summary,
            diagnostics = listOf(diagnostic),
        )

        assertTrue(parsedSong.isValid)
        assertEquals("Minimal", parsedSong.header.title)
        assertEquals(120.0, requireNotNull(parsedSong.header.bpm), 0.0)
        assertEquals(1, parsedSong.tracks.size)
        assertEquals(TrackId.P1, parsedSong.tracks.single().trackId)
        assertEquals("la", parsedSong.tracks.single().lines.single().notes.single().lyricText)
        assertFalse(parsedSong.derivedSummary.isDuet)
        assertEquals(DiagnosticCode.UNKNOWN_HEADER_TAG, parsedSong.diagnostics.single().code)
    }

    @Test
    fun `parse result exposes invalid parsed song and top level invalid code`() {
        val parsedSong = ParsedSong(
            songIdentifier = "invalid/missing-artist",
            isValid = false,
            header = SongHeader(),
            tracks = emptyList(),
            derivedSummary = DerivedSongSummary(),
            diagnostics = listOf(
                DiagnosticEntry(
                    severity = DiagnosticSeverity.ERROR,
                    code = DiagnosticCode.MISSING_REQUIRED_ARTIST,
                    message = "Artist is required",
                    songIdentifier = "invalid/missing-artist",
                    lineNumber = null,
                )
            ),
        )

        val result = ParseResult(
            parsedSong = parsedSong,
            invalidCode = DiagnosticCode.MISSING_REQUIRED_ARTIST,
        )

        assertFalse(result.parsedSong.isValid)
        assertEquals(DiagnosticCode.MISSING_REQUIRED_ARTIST, result.invalidCode)
        assertEquals("invalid/missing-artist", result.parsedSong.songIdentifier)
    }
}
