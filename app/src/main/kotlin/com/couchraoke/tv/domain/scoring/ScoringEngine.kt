package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.ParsedSong
import com.couchraoke.tv.domain.timing.NoteTimingWindow

interface ScoringEngine {
    fun buildProfile(song: ParsedSong, trackIndex: Int, config: ScoringConfig): TrackScoringProfile
    fun isPitchMatch(midiNote: Int, toneValid: Boolean, noteType: NoteType, targetTone: Int, difficulty: Difficulty): Boolean
    fun evaluateNote(window: NoteTimingWindow, note: NoteEvent, frames: List<PitchFrame>, profile: TrackScoringProfile): NoteResult
    fun accumulateNote(current: PlayerScore, result: NoteResult): PlayerScore
    fun evaluateLine(lineScore: Double, maxLineScore: Double, profile: TrackScoringProfile): LineBonusResult
    fun computeDisplayScores(playerScore: PlayerScore): DisplayScore
}

class DefaultScoringEngine : ScoringEngine {
    override fun buildProfile(song: ParsedSong, trackIndex: Int, config: ScoringConfig): TrackScoringProfile {
        val track = song.tracks[trackIndex]
        val medleyStart = song.header.medleyStartBeat
        val medleyEnd = song.header.medleyEndBeat
        val hasMedley = medleyStart != null && medleyEnd != null

        var trackScoreValue = 0.0
        var nonEmptyLineCount = 0

        for (line in track.lines) {
            var lineSum = 0.0
            for (note in line.notes) {
                val inRange = if (hasMedley) {
                    note.startBeat >= medleyStart!! && note.startBeat < medleyEnd!!
                } else {
                    true
                }
                if (inRange) {
                    lineSum += note.durationBeats * SCORE_FACTOR[note.noteType]!!
                }
            }
            trackScoreValue += lineSum
            if (lineSum > 0) nonEmptyLineCount++
        }

        val lineBonusPerLine = if (nonEmptyLineCount > 0 && config.lineBonusEnabled)
            config.maxLineBonusPool.toDouble() / nonEmptyLineCount
        else 0.0

        return TrackScoringProfile(
            trackScoreValue = trackScoreValue,
            nonEmptyLineCount = nonEmptyLineCount,
            lineBonusPerLine = lineBonusPerLine,
            medleyStartBeat = medleyStart,
            medleyEndBeat = medleyEnd,
            maxSongPoints = config.maxSongPoints,
            difficulty = config.difficulties.getOrDefault(trackIndex, DEFAULT_DIFFICULTY),
        )
    }

    override fun isPitchMatch(midiNote: Int, toneValid: Boolean, noteType: NoteType, targetTone: Int, difficulty: Difficulty): Boolean {
        var tone = midiNote - 36
        while (kotlin.math.abs(tone - targetTone) > 6) {
            if (tone > targetTone) tone -= 12
            else tone += 12
        }
        return when (noteType) {
            NoteType.FREESTYLE -> false
            NoteType.RAP, NoteType.RAP_GOLDEN -> toneValid
            NoteType.NORMAL, NoteType.GOLDEN -> toneValid && kotlin.math.abs(tone - targetTone) <= DIFFICULTY_TOLERANCE[difficulty]!!
        }
    }

    override fun evaluateNote(window: NoteTimingWindow, note: NoteEvent, frames: List<PitchFrame>, profile: TrackScoringProfile): NoteResult {
        val noteNote = note as NoteEvent.Note

        if (noteNote.noteType == NoteType.FREESTYLE) {
            return NoteResult(window, hits = 0, n = frames.size, noteScore = 0.0, maxNoteScore = 0.0, accumulator = ScoreAccumulator.NONE)
        }

        val scoreFactor = SCORE_FACTOR[noteNote.noteType]!!
        val maxNoteScore = if (profile.trackScoreValue == 0.0) 0.0
            else (profile.maxSongPoints.toDouble() / profile.trackScoreValue) * scoreFactor * noteNote.durationBeats

        val n = frames.size
        val hits = frames.count { frame -> isPitchMatch(frame.midiNote, frame.toneValid, noteNote.noteType, noteNote.tone, profile.difficulty) }

        val noteScore = if (n > 0) maxNoteScore * (hits.toDouble() / n) else 0.0

        val accumulator = when (noteNote.noteType) {
            NoteType.GOLDEN, NoteType.RAP_GOLDEN -> ScoreAccumulator.SCORE_GOLDEN
            else -> ScoreAccumulator.SCORE
        }

        return NoteResult(window, hits, n, noteScore, maxNoteScore, accumulator)
    }

    override fun accumulateNote(current: PlayerScore, result: NoteResult): PlayerScore {
        return when (result.accumulator) {
            ScoreAccumulator.SCORE -> current.copy(score = current.score + result.noteScore, scoreLast = result.noteScore)
            ScoreAccumulator.SCORE_GOLDEN -> current.copy(scoreGolden = current.scoreGolden + result.noteScore, scoreLast = result.noteScore)
            ScoreAccumulator.NONE -> current.copy(scoreLast = result.noteScore)
        }
    }

    override fun evaluateLine(lineScore: Double, maxLineScore: Double, profile: TrackScoringProfile): LineBonusResult {
        val linePerfection = if (maxLineScore <= 2.0) 1.0
        else (lineScore / (maxLineScore - 2.0)).coerceIn(0.0, 1.0)
        val lineBonusAwarded = profile.lineBonusPerLine * linePerfection
        return LineBonusResult(
            linePerfection = linePerfection,
            lineBonusAwarded = lineBonusAwarded,
            lineScore = lineScore,
            maxLineScore = maxLineScore,
        )
    }

    override fun computeDisplayScores(playerScore: PlayerScore): DisplayScore {
        val scoreInt = (Math.round(playerScore.score / 10.0) * 10).toInt()
        val scoreGoldenInt = if (scoreInt < playerScore.score)
            (kotlin.math.ceil(playerScore.scoreGolden / 10.0) * 10).toInt()
        else
            (kotlin.math.floor(playerScore.scoreGolden / 10.0) * 10).toInt()
        val scoreLineInt = (kotlin.math.floor(kotlin.math.round(playerScore.scoreLine).toDouble() / 10.0) * 10).toInt()
        val scoreTotalInt = scoreInt + scoreGoldenInt + scoreLineInt
        return DisplayScore(scoreInt, scoreGoldenInt, scoreLineInt, scoreTotalInt)
    }
}
