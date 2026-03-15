package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.timing.NoteTimingWindow

enum class Difficulty { EASY, MEDIUM, HARD }

enum class ScoreAccumulator { SCORE, SCORE_GOLDEN, NONE }

data class ScoringConfig(
    val lineBonusEnabled: Boolean,
    val maxSongPoints: Int = if (lineBonusEnabled) 9000 else 10000,
    val maxLineBonusPool: Int = if (lineBonusEnabled) 1000 else 0,
    val difficulties: Map<Int, Difficulty> = emptyMap(),
)

data class PitchFrame(
    val midiNote: Int,
    val toneValid: Boolean,
)

data class TrackScoringProfile(
    val trackScoreValue: Double,
    val nonEmptyLineCount: Int,
    val lineBonusPerLine: Double,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val maxSongPoints: Int,
    val difficulty: Difficulty = DEFAULT_DIFFICULTY,
)

data class NoteResult(
    val noteTimingWindow: NoteTimingWindow,
    val hits: Int,
    val n: Int,
    val noteScore: Double,
    val maxNoteScore: Double,
    val accumulator: ScoreAccumulator,
)

data class PlayerScore(
    val score: Double = 0.0,
    val scoreGolden: Double = 0.0,
    val scoreLine: Double = 0.0,
    val scoreLast: Double = 0.0,
)

data class DisplayScore(
    val scoreInt: Int,
    val scoreGoldenInt: Int,
    val scoreLineInt: Int,
    val scoreTotalInt: Int,
)

data class LineBonusResult(
    val linePerfection: Double,
    val lineBonusAwarded: Double,
    val lineScore: Double,
    val maxLineScore: Double,
)
