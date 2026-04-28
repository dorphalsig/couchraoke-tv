package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.domain.usdx.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsdxParserRegressionTest {
    private val parser = DefaultUsdxParser()

    @Test(timeout = 30_000)
    fun missingRequiredAudioFailsWithMissingRequiredHeader() {
        val txt = """
            #TITLE:Missing Audio
            #ARTIST:Artist
            #BPM:120
            : 0 4 0 hi
            E
        """.trimIndent()

        val failure = parser
            .parse("missing_required_audio", txt.encodeToByteArray())
            .exceptionOrNull() as ParseException
        val invalid = failure.diagnostics.single { it.severity == Severity.Invalid }

        assertEquals("ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER", invalid.code)
        assertNull(invalid.lineNumber)
    }

    @Test(timeout = 30_000)
    fun malformedRequiredBpmFailsWithMalformedHeaderLineNumber() {
        val txt = """
            #TITLE:Malformed BPM
            #ARTIST:Artist
            #BPM:not-a-number
            #MP3:song.mp3
            : 0 4 0 hi
            E
        """.trimIndent()

        val failure = parser
            .parse("malformed_required_bpm", txt.encodeToByteArray())
            .exceptionOrNull() as ParseException
        val malformed = failure.diagnostics.first {
            it.severity == Severity.Invalid && it.code == "ERROR_CORRUPT_SONG_MALFORMED_HEADER"
        }
        val missing = failure.diagnostics.first {
            it.severity == Severity.Invalid && it.code == "ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER"
        }

        assertEquals(3, malformed.lineNumber)
        assertNull(missing.lineNumber)
    }

    @Test(timeout = 30_000)
    fun duplicateKnownHeaderTagsUseLastSuccessfullyParsedValue() {
        val txt = """
            #TITLE:First Title
            #TITLE:Final Title
            #ARTIST:First Artist
            #ARTIST:Final Artist
            #BPM:90
            #BPM:120
            #MP3:first.mp3
            #MP3:final.mp3
            : 0 4 0 hi
            E
        """.trimIndent()

        val parsed = parser
            .parse("duplicate_known_tags", txt.encodeToByteArray())
            .getOrThrow()

        assertEquals("Final Title", parsed.header.title)
        assertEquals("Final Artist", parsed.header.artist)
        assertEquals(120f, parsed.header.bpmFile)
        assertEquals("final.mp3", parsed.header.audio)
    }

    @Test(timeout = 30_000)
    fun malformedOptionalHeaderEmitsWarningAndTreatsValueAsAbsent() {
        val txt = """
            #TITLE:Optional Warning
            #ARTIST:Artist
            #BPM:120
            #MP3:song.mp3
            #YEAR:not-a-year
            #VIDEOGAP:not-a-gap
            : 0 4 0 hi
            E
        """.trimIndent()

        val parsed = parser
            .parse("malformed_optional_headers", txt.encodeToByteArray())
            .getOrThrow()
        val warnings = parsed.diagnostics.filter { it.severity == Severity.Warn }

        assertEquals(listOf(5, 6), warnings.map { it.lineNumber })
        assertEquals(
            listOf("WARN_MALFORMED_OPTIONAL_HEADER", "WARN_MALFORMED_OPTIONAL_HEADER"),
            warnings.map { it.code },
        )
        assertNull(parsed.header.year)
        assertNull(parsed.header.videoGapSec)
    }

    @Test(timeout = 30_000)
    fun rejectsDuetSongWhenEitherPlayerHasNoRemainingNotes() {
        val txt = """
            #TITLE:One Sided Duet
            #ARTIST:Artist
            #BPM:120
            #MP3:song.mp3
            P1
            : 0 4 0 hi
            P2
            E
        """.trimIndent()

        val failure = parser
            .parse("one_sided_duet", txt.encodeToByteArray())
            .exceptionOrNull() as ParseException
        val invalid = failure.diagnostics.single { it.severity == Severity.Invalid }

        assertEquals("ERROR_CORRUPT_SONG_NO_NOTES", invalid.code)
        assertNull(invalid.lineNumber)
    }
}
