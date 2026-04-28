package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.scoring.internal.BeatWindowCalculator
import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.floor

class BeatCalculatorFixtureTest {
    @Test(timeout = 30_000)
    fun matchesF06BeatCursorFixture() {
        val expected = FixtureJson.decode<F06BeatCursorSnapshot>(
            FixturePaths.fixtureFile("F06_beat_time_conversion_static_bpm", "expected.beat_cursors.json"),
        )

        expected.samples.forEach { sample ->
            val highlightTimeSec = BeatWindowCalculator.lyricTimeSec(sample.lyricsTimeSec, expected.inputs.gapMs)
            val scoringTimeSec = BeatWindowCalculator.scoringTimeSec(
                sample.lyricsTimeSec,
                expected.inputs.gapMs,
                expected.inputs.micDelayMs.toInt(),
            )
            val bpmInternal = expected.inputs.bpmFile * 4
            val midBeatHighlight = BeatCalculator.timeSecToMidBeatInternal(highlightTimeSec, bpmInternal)
            val currentBeat = floor(midBeatHighlight).toInt()
            val midBeatScoring = BeatCalculator.timeSecToMidBeatInternal(scoringTimeSec, bpmInternal)
            val currentBeatD = BeatWindowCalculator.scoringCursorBeat(
                lyricsTimeSec = sample.lyricsTimeSec,
                gapMs = expected.inputs.gapMs,
                bpmFile = expected.inputs.bpmFile,
                micDelayMs = expected.inputs.micDelayMs.toInt(),
            )

            assertEquals(sample.highlightTimeSec, highlightTimeSec, 1e-9)
            assertEquals(sample.scoringTimeSec, scoringTimeSec, 1e-9)
            assertEquals(sample.midBeatHighlight, midBeatHighlight, 1e-9)
            assertEquals(sample.currentBeat, currentBeat)
            assertEquals(sample.midBeatScoring, midBeatScoring, 1e-9)
            assertEquals(sample.currentBeatD, currentBeatD)
        }
    }

    @Test(timeout = 30_000)
    fun computesNoteWindowsWithMicDelay() {
        val songStartTvMs = 1_000L
        val startTvMs = BeatWindowCalculator.noteStartTvMs(
            songStartTvMs = songStartTvMs,
            startBeatFile = 4,
            bpmFile = 120f,
            gapMs = 2_000f,
            micDelayMs = 100,
        )
        val endTvMs = BeatWindowCalculator.noteEndTvMs(
            songStartTvMs = songStartTvMs,
            startBeatFile = 4,
            durationBeats = 2,
            bpmFile = 120f,
            gapMs = 2_000f,
            micDelayMs = 100,
        )

        assertEquals(3_600L, startTvMs)
        assertEquals(3_850L, endTvMs)
    }

    @Test(timeout = 30_000)
    fun usesStartInclusiveEndExclusiveWindows() {
        val startTvMs = BeatWindowCalculator.noteStartTvMs(0L, 0, 120f, 0f, 0)
        val endTvMs = BeatWindowCalculator.noteEndTvMs(0L, 0, 4, 120f, 0f, 0)

        assertEquals(0L, startTvMs)
        assertEquals(500L, endTvMs)
        assertEquals(0, BeatWindowCalculator.currentBeatDAtTvTime(0L, 0L, 0f, 120f, 0))
        assertEquals(3, BeatWindowCalculator.currentBeatDAtTvTime(499L, 0L, 0f, 120f, 0))
        assertEquals(4, BeatWindowCalculator.currentBeatDAtTvTime(500L, 0L, 0f, 120f, 0))
    }

    @Test(timeout = 30_000)
    fun roundTripsBeatAndTimeWithinTolerance() {
        val bpmInternal = 480f
        val beat = 123.456789
        val timeSec = BeatCalculator.beatInternalToTimeSec(beat, bpmInternal)
        val roundTripBeat = BeatCalculator.timeSecToMidBeatInternal(timeSec, bpmInternal)
        val roundTripTimeSec = BeatCalculator.beatInternalToTimeSec(roundTripBeat, bpmInternal)

        assertEquals(beat, roundTripBeat, 1e-9)
        assertEquals(timeSec, roundTripTimeSec, 1e-9)
    }

    @Serializable
    private data class F06BeatCursorSnapshot(
        val description: String,
        val inputs: Inputs,
        val samples: List<Sample>,
    )

    @Serializable
    private data class Inputs(
        @kotlinx.serialization.SerialName("BPM_file")
        val bpmFile: Float,
        @kotlinx.serialization.SerialName("GAPms")
        val gapMs: Float,
        val micDelayMs: Double,
    )

    @Serializable
    private data class Sample(
        val lyricsTimeSec: Double,
        val highlightTimeSec: Double,
        val scoringTimeSec: Double,
        val midBeatHighlight: Double,
        val currentBeat: Int,
        val midBeatScoring: Double,
        val currentBeatD: Int,
    )
}
