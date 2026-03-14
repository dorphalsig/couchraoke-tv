package com.couchraoke.tv.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyParserTest {
    private val parser = BodyParser()

    @Test
    fun `routes duet content to the active track`() {
        val result = parser.parse(
            songIdentifier = "duet/11_duet_valid",
            lines = listOf(
                "P1",
                ": 0 4 0 la",
                "- 4",
                "P2",
                ": 0 4 0 la",
                "- 4",
                "E",
            ),
        )

        assertFalse(result.hasFatalError)
        assertEquals(2, result.trackSections.size)
        assertEquals(TrackId.P1, result.trackSections[0].trackId)
        assertEquals(1, result.trackSections[0].events.size)
        assertEquals("la", result.trackSections[0].events[0].lyricText)
        assertEquals(TrackId.P2, result.trackSections[1].trackId)
        assertEquals(1, result.trackSections[1].events.size)
        assertEquals("la", result.trackSections[1].events[0].lyricText)
    }

    @Test
    fun `parses a single track note stream in file order`() {
        val result = parser.parse(
            songIdentifier = "valid/01_valid_minimal_v1",
            lines = listOf(
                ": 0 4 0 la",
                "- 4",
                "E",
            ),
        )

        assertFalse(result.hasFatalError)
        assertEquals(1, result.trackSections.size)
        assertEquals(TrackId.P1, result.trackSections.single().trackId)
        assertEquals(1, result.trackSections.single().events.size)
        assertEquals(0, result.trackSections.single().events.single().startBeat)
        assertEquals(4, result.trackSections.single().lineBreaks.single())
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `converts zero duration notes into freestyle with a warning`() {
        val result = parser.parse(
            songIdentifier = "derived/06_duration_zero_converts_to_freestyle",
            lines = listOf(
                ": 0 0 0 la",
                "- 1",
                "E",
            ),
        )

        assertFalse(result.hasFatalError)
        assertEquals(NoteType.FREESTYLE, result.trackSections.single().events.single().noteType)
        assertEquals(DiagnosticCode.ZERO_DURATION_NOTE_CONVERTED, result.diagnostics.single().code)
    }

    @Test
    fun `continues past unknown body tokens with a warning`() {
        val result = parser.parse(
            songIdentifier = "edge/07_unknown_body_token_ignored",
            lines = listOf(
                "Z 0 1 2 foo",
                ": 0 4 0 la",
                "- 4",
                "E",
            ),
        )

        assertFalse(result.hasFatalError)
        assertEquals(1, result.trackSections.single().events.size)
        assertEquals(DiagnosticCode.UNKNOWN_BODY_TOKEN, result.diagnostics.single().code)
    }
}
