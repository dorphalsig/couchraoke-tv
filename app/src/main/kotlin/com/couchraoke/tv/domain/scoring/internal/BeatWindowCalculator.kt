package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.BeatCalculator
import kotlin.math.floor
import kotlin.math.roundToLong

internal object BeatWindowCalculator {
    fun lyricTimeSec(lyricsTimeSec: Double, gapMs: Float): Double =
        lyricsTimeSec - (gapMs / 1000.0)

    fun scoringTimeSec(
        lyricsTimeSec: Double,
        gapMs: Float,
        micDelayMs: Int,
    ): Double = lyricTimeSec(lyricsTimeSec, gapMs) - (micDelayMs / 1000.0)

    fun currentBeat(
        lyricsTimeSec: Double,
        gapMs: Float,
        bpmFile: Float,
    ): Int = floor(midBeat(lyricTimeSec(lyricsTimeSec, gapMs), bpmFile)).toInt()

    fun scoringCursorBeat(
        lyricsTimeSec: Double,
        gapMs: Float,
        bpmFile: Float,
        micDelayMs: Int,
    ): Int = floor(midBeat(scoringTimeSec(lyricsTimeSec, gapMs, micDelayMs), bpmFile)).toInt()

    /**
     * Returns the stepped scoring beat at a captured sample time.
     *
     * Runtime scoring evaluates beats in (oldBeatD, currentBeatD], so a sample exactly at a note's
     * exclusive end boundary may advance the beat cursor while the note is no longer active.
     */
    fun currentBeatDAtTvTime(
        sampleTvTimeMs: Long,
        songStartTvMs: Long,
        gapMs: Float,
        bpmFile: Float,
        micDelayMs: Int,
    ): Int {
        val relativeTimeSec =
            ((sampleTvTimeMs - songStartTvMs).toDouble() - gapMs - micDelayMs) / 1000.0
        return floor(midBeat(relativeTimeSec, bpmFile)).toInt()
    }

    fun noteStartTvMs(
        songStartTvMs: Long,
        startBeatFile: Int,
        bpmFile: Float,
        gapMs: Float,
        micDelayMs: Int,
    ): Long =
        songStartTvMs +
            beatOffsetMs(startBeatFile.toDouble(), bpmFile) +
            gapMs.roundToLong() +
            micDelayMs

    fun noteEndTvMs(
        songStartTvMs: Long,
        startBeatFile: Int,
        durationBeats: Int,
        bpmFile: Float,
        gapMs: Float,
        micDelayMs: Int,
    ): Long =
        songStartTvMs +
            beatOffsetMs(startBeatFile + durationBeats.toDouble(), bpmFile) +
            gapMs.roundToLong() +
            micDelayMs

    private fun midBeat(timeSec: Double, bpmFile: Float): Double =
        BeatCalculator.timeSecToMidBeatInternal(timeSec, bpmFile * 4)

    private fun beatOffsetMs(beatFile: Double, bpmFile: Float): Long =
        (beatFile * 15000.0 / bpmFile).roundToLong()
}
