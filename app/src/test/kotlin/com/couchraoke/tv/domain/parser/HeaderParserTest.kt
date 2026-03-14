package com.couchraoke.tv.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderParserTest {
    private val parser = HeaderParser()

    @Test
    fun `parses required and optional header tags for a modern song`() {
        val result = parser.parse(
            songIdentifier = "valid/metadata_rich",
            lines = listOf(
                "#VERSION:1.0.0",
                "#TITLE:MetadataRich",
                "#ARTIST:Test",
                "#AUDIO:audio.ogg",
                "#BPM:120",
                "#GAP:500",
                "#START:1.5",
                "#END:90000",
                "#PREVIEWSTART:12.5",
                "#VIDEO:video.mp4",
                "#VIDEOGAP:0.2",
                "#COVER:cover.jpg",
                "#BACKGROUND:bg.jpg",
                "#INSTRUMENTAL:inst.ogg",
                "#VOCALS:vocals.ogg",
                "#MEDLEYSTARTBEAT:32",
                "#MEDLEYENDBEAT:64",
                "#CALCMEDLEY:ON",
            ),
            fileResolver = FileResolver { true },
        )

        assertEquals("MetadataRich", result.header.title)
        assertEquals("Test", result.header.artist)
        assertEquals("audio.ogg", result.header.audioReference)
        assertEquals(120.0, requireNotNull(result.header.bpm), 0.0)
        assertEquals(500, result.header.gapMs)
        assertEquals(1.5, requireNotNull(result.header.startSec), 0.0)
        assertEquals(90000, result.header.endMs)
        assertEquals(12.5, requireNotNull(result.header.previewStartSec), 0.0)
        assertEquals("video.mp4", result.header.videoReference)
        assertEquals(0.2, requireNotNull(result.header.videoGapSec), 0.0)
        assertEquals("cover.jpg", result.header.coverReference)
        assertEquals("bg.jpg", result.header.backgroundReference)
        assertEquals("inst.ogg", result.header.instrumentalReference)
        assertEquals("vocals.ogg", result.header.vocalsReference)
        assertEquals(32, result.header.medleyStartBeat)
        assertEquals(64, result.header.medleyEndBeat)
        assertTrue(result.header.calcMedleyEnabled)
        assertEquals(SongVersion(1, 0, 0), result.header.version)
        assertTrue(result.diagnostics.isEmpty())
        assertFalse(result.hasFatalError)
    }

    @Test
    fun `keeps missing optional metadata absent`() {
        val result = parser.parse(
            songIdentifier = "valid/minimal",
            lines = listOf(
                "#VERSION:1.0.0",
                "#TITLE:Minimal",
                "#ARTIST:Test",
                "#AUDIO:audio.ogg",
                "#BPM:120",
                "#GAP:0",
            ),
            fileResolver = FileResolver { true },
        )

        assertNull(result.header.previewStartSec)
        assertNull(result.header.videoReference)
        assertNull(result.header.coverReference)
        assertNull(result.header.backgroundReference)
        assertNull(result.header.instrumentalReference)
        assertNull(result.header.vocalsReference)
        assertNull(result.header.medleyStartBeat)
        assertNull(result.header.medleyEndBeat)
        assertFalse(result.header.calcMedleyEnabled)
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `preserves unknown tags and no separator header lines in encounter order`() {
        val result = parser.parse(
            songIdentifier = "edge/03_unknown_header_tags",
            lines = listOf(
                "#VERSION:1.0.0",
                "#TITLE:UnknownTags",
                "#ARTIST:Test",
                "#AUDIO:audio.ogg",
                "#BPM:120",
                "#FOO:bar",
                "#X-TEST:1",
                "#NOVALUE",
            ),
            fileResolver = FileResolver { true },
        )

        assertEquals(3, result.header.customTags.size)
        assertEquals("FOO", result.header.customTags[0].tagName)
        assertEquals("bar", result.header.customTags[0].content)
        assertEquals("X-TEST", result.header.customTags[1].tagName)
        assertEquals("1", result.header.customTags[1].content)
        assertEquals("NOVALUE", result.header.customTags[2].tagName)
        assertEquals("", result.header.customTags[2].content)
        assertEquals(DiagnosticCode.UNKNOWN_HEADER_TAG, result.diagnostics[0].code)
        assertEquals(DiagnosticCode.UNKNOWN_HEADER_TAG, result.diagnostics[1].code)
        assertEquals(DiagnosticCode.MALFORMED_OPTIONAL_HEADER, result.diagnostics[2].code)
    }

    @Test
    fun `duplicate known tags use last valid value wins`() {
        val result = parser.parse(
            songIdentifier = "edge/02_header_duplicates_last_wins",
            lines = listOf(
                "#VERSION:1.0.0",
                "#TITLE:Old Title",
                "#TITLE:New Title",
                "#ARTIST:Old Artist",
                "#ARTIST:New Artist",
                "#AUDIO:old_audio.ogg",
                "#AUDIO:audio.ogg",
                "#BPM:100",
                "#BPM:120",
            ),
            fileResolver = FileResolver { true },
        )

        assertEquals("New Title", result.header.title)
        assertEquals("New Artist", result.header.artist)
        assertEquals("audio.ogg", result.header.audioReference)
        assertEquals(120.0, requireNotNull(result.header.bpm), 0.0)
    }
}
