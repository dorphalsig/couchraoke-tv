package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.usdx.model.NoteType

internal object NoteHitEvaluator {
    fun isHit(noteType: NoteType, targetToneSemitone: Int, difficulty: Difficulty, sample: ScoringSample): Boolean =
        when (noteType) {
            NoteType.Freestyle -> false
            NoteType.Rap, NoteType.RapGolden -> sample.toneValid
            NoteType.Normal, NoteType.Golden -> {
                if (!sample.toneValid) {
                    false
                } else {
                    val normalizedTone = OctaveNormalizer.normalize(sample.toneSemitone!!, targetToneSemitone)
                    kotlin.math.abs(normalizedTone - targetToneSemitone) <= tolerance(difficulty)
                }
            }
        }

    private fun tolerance(difficulty: Difficulty): Int =
        when (difficulty) {
            Difficulty.Easy -> 2
            Difficulty.Medium -> 1
            Difficulty.Hard -> 0
        }
}
