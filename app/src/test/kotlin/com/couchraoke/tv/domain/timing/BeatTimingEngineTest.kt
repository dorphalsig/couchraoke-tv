package com.couchraoke.tv.domain.timing

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.TrackId
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.floor

class BeatTimingEngineTest {

    // US1 — Beat cursor: baseline conversion
    @Test
    fun `beat cursor baseline conversion returns expected currentBeat`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            micDelayMs = 0,
        )
        val cursor = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec = 5.0)
        // highlightTimeSec = 5.0 - 2000/1000.0 = 3.0
        // bpmInternal = 120 * 4 = 480
        // midBeat = 3.0 * 480 / 60 = 24.0
        // currentBeat = floor(24.0) = 24
        assertEquals(3.0, cursor.highlightTimeSec, 0.0)
        assertEquals(24.0, cursor.midBeat, 0.0)
        assertEquals(24, cursor.currentBeat)
    }

    // US1 — Beat cursor: negative pre-roll
    @Test
    fun `beat cursor pre-roll returns negative highlightTimeSec`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            micDelayMs = 0,
        )
        // lyricsTimeSec=1.0, gap elapses at 2.0s → highlightTimeSec = 1.0 - 2.0 = -1.0
        val cursor = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec = 1.0)
        assertTrue(cursor.highlightTimeSec < 0.0)
        assertEquals(-1.0, cursor.highlightTimeSec, 0.0)
    }

    // US1 — Beat cursor: round-trip
    @Test
    fun `beat cursor round-trip is repeatable for same inputs`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            micDelayMs = 0,
        )
        val cursor1 = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec = 5.0)
        val cursor2 = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec = 5.0)
        assertEquals(cursor1.lyricsTimeSec, cursor2.lyricsTimeSec, 0.0)
        assertEquals(cursor1.highlightTimeSec, cursor2.highlightTimeSec, 0.0)
        assertEquals(cursor1.midBeat, cursor2.midBeat, 0.0)
        assertEquals(cursor1.currentBeat, cursor2.currentBeat)
    }

    // US2 — Note window: derivation
    @Test
    fun `note window derivation produces start end and finalization boundaries`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            micDelayMs = 0,
            songStartTvMs = 10_000L,
        )
        val note = NoteEvent.Note(
            noteType = NoteType.NORMAL,
            startBeat = 0,
            durationBeats = 4,
            tone = 0,
            lyricText = "la",
        )
        // bpmInternal = 480
        // beatsToMs(0) = 0 * 60000 / 480 = 0.0
        // beatsToMs(4) = 4 * 60000 / 480 = 500.0
        // noteStartTvMs = 10_000 + 2000 + 0 + 0 = 12_000
        // noteEndTvMs   = 10_000 + 2000 + 500 + 0 = 12_500
        // finalizationTvMs = 12_500 + 450 = 12_950
        val window = BeatTimingEngine.computeNoteTimingWindow(context, TrackId.P1, 0, note)
        assertEquals(12_000L, window.noteStartTvMs)
        assertEquals(12_500L, window.noteEndTvMs)
        assertEquals(12_950L, window.finalizationTvMs)
    }

    // US2 — Note window: zero-duration note
    @Test
    fun `zero-duration note produces zero-width timing window`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            micDelayMs = 0,
            songStartTvMs = 10_000L,
        )
        val note = NoteEvent.Note(
            noteType = NoteType.NORMAL,
            startBeat = 8,
            durationBeats = 0,
            tone = 0,
            lyricText = "la",
        )
        // beatsToMs(8) = 8 * 60000 / 480 = 1000.0
        // beatsToMs(8+0) = 1000.0
        // noteStartTvMs = 10_000 + 2000 + 1000 + 0 = 13_000
        // noteEndTvMs   = 10_000 + 2000 + 1000 + 0 = 13_000
        val window = BeatTimingEngine.computeNoteTimingWindow(context, TrackId.P1, 0, note)
        assertEquals(window.noteStartTvMs, window.noteEndTvMs)
    }

    // US2 — Note window: start-inclusive end-exclusive membership
    @Test
    fun `note membership is start-inclusive and end-exclusive`() {
        val window = NoteTimingWindow(
            trackId = TrackId.P1,
            lineStartBeat = 0,
            noteType = NoteType.NORMAL,
            startBeat = 0,
            durationBeats = 4,
            noteStartTvMs = 12_000L,
            noteEndTvMs = 12_500L,
        )

        // Frame exactly at start (12_000) → eligible
        val frameAtStart = PitchFrameTiming(
            frameTimestampTvMs = 12_000L,
            arrivalTimeTvMs = 12_000L,
            eligibleForCollection = true,
        )
        assertTrue(BeatTimingEngine.isPitchFrameEligible(frameAtStart, window))

        // Frame within window (12_499) → eligible
        val frameWithin = PitchFrameTiming(
            frameTimestampTvMs = 12_499L,
            arrivalTimeTvMs = 12_499L,
            eligibleForCollection = true,
        )
        assertTrue(BeatTimingEngine.isPitchFrameEligible(frameWithin, window))

        // Frame at end (12_500) → NOT eligible (end-exclusive)
        val frameAtEnd = PitchFrameTiming(
            frameTimestampTvMs = 12_500L,
            arrivalTimeTvMs = 12_500L,
            eligibleForCollection = true,
        )
        assertFalse(BeatTimingEngine.isPitchFrameEligible(frameAtEnd, window))
    }

    // US3 — Calibration: micDelayMs default
    @Test
    fun `micDelayMs defaults to zero when unset`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
        )
        assertEquals(0, context.micDelayMs)
    }

    // US3 — Calibration: micDelayMs range validation
    @Test
    fun `micDelayMs outside 0-400 range is rejected`() {
        try {
            TimingContext(songIdentifier = "x", bpmFile = 120.0, gapMs = 2000, micDelayMs = 401)
            fail("Expected IllegalArgumentException was not thrown for micDelayMs=401")
        } catch (e: IllegalArgumentException) {
            // expected
        }

        try {
            TimingContext(songIdentifier = "x", bpmFile = 120.0, gapMs = 2000, micDelayMs = -1)
            fail("Expected IllegalArgumentException was not thrown for micDelayMs=-1")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    // US3 — Playback bounds: explicit end vs media duration fallback
    @Test
    fun `explicit endMs is used when present and positive`() {
        val context = TimingContext(
            songIdentifier = "test",
            bpmFile = 120.0,
            gapMs = 2000,
            endMs = 60_000,
        )
        val bounds = BeatTimingEngine.computePlaybackBounds(context)
        assertEquals(60.0, bounds.effectiveSongEndSec!!, 0.0)
        assertTrue(bounds.endsFromHeader)
    }
}
