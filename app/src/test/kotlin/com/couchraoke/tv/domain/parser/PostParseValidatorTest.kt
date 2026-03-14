package com.couchraoke.tv.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostParseValidatorTest {
    private val validator = PostParseValidator()

    @Test
    fun `rejects a track when cleanup leaves no non-empty lines`() {
        val result = validator.finalize(
            songIdentifier = "fixtures/parser/invalid/10_no_notes_invalid",
            header = SongHeader(title = "NoNotes", artist = "Test", bpm = 120.0, audioReference = "audio.ogg"),
            bodyResult = BodyParseResult(
                trackSections = listOf(
                    TrackSection(
                        trackId = TrackId.P1,
                        events = emptyList(),
                        lineBreaks = listOf(0),
                        groupedEvents = emptyList(),
                    )
                )
            ),
            fileResolver = FileResolver { true },
        )

        assertTrue(result.hasFatalError)
        assertEquals(DiagnosticCode.EMPTY_TRACK_AFTER_CLEANUP, result.diagnostics.single().code)
        assertEquals("fixtures/parser/invalid/10_no_notes_invalid", result.diagnostics.single().songIdentifier)
    }

    @Test
    fun `creates at least one line when notes exist without an explicit line break`() {
        val bodyResult = BodyParseResult(
            trackSections = listOf(
                TrackSection(
                    trackId = TrackId.P1,
                    events = listOf(
                        NoteEvent.Note(
                            noteType = NoteType.NORMAL,
                            startBeat = 0,
                            durationBeats = 4,
                            tone = 0,
                            lyricText = "la",
                        )
                    ),
                    lineBreaks = emptyList(),
                    groupedEvents = listOf(
                        listOf(
                            NoteEvent.Note(
                                noteType = NoteType.NORMAL,
                                startBeat = 0,
                                durationBeats = 4,
                                tone = 0,
                                lyricText = "la",
                            )
                        )
                    ),
                )
            )
        )

        val result = validator.finalize(
            songIdentifier = "fixtures/parser/invalid/09_no_breaks_allowed_usdx",
            header = SongHeader(title = "NoBreaksAllowed", artist = "Test", bpm = 120.0, audioReference = "audio.ogg"),
            bodyResult = bodyResult,
            fileResolver = FileResolver { true },
        )

        assertFalse(result.hasFatalError)
        assertEquals(1, result.tracks.single().lines.size)
        assertEquals("la", result.tracks.single().lines.single().notes.single().lyricText)
    }
}
