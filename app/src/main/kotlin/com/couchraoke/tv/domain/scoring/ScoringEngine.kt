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
    override fun buildProfile(song: ParsedSong, trackIndex: Int, config: ScoringConfig): TrackScoringProfile =
        throw NotImplementedError("not yet implemented")

    override fun isPitchMatch(midiNote: Int, toneValid: Boolean, noteType: NoteType, targetTone: Int, difficulty: Difficulty): Boolean =
        throw NotImplementedError("not yet implemented")

    override fun evaluateNote(window: NoteTimingWindow, note: NoteEvent, frames: List<PitchFrame>, profile: TrackScoringProfile): NoteResult =
        throw NotImplementedError("not yet implemented")

    override fun accumulateNote(current: PlayerScore, result: NoteResult): PlayerScore =
        throw NotImplementedError("not yet implemented")

    override fun evaluateLine(lineScore: Double, maxLineScore: Double, profile: TrackScoringProfile): LineBonusResult =
        throw NotImplementedError("not yet implemented")

    override fun computeDisplayScores(playerScore: PlayerScore): DisplayScore =
        throw NotImplementedError("not yet implemented")
}
