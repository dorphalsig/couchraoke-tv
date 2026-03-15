package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.parser.NoteType

val SCORE_FACTOR: Map<NoteType, Int> = mapOf(
    NoteType.FREESTYLE to 0,
    NoteType.NORMAL to 1,
    NoteType.GOLDEN to 2,
    NoteType.RAP to 1,
    NoteType.RAP_GOLDEN to 2,
)

val DIFFICULTY_TOLERANCE: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 2,
    Difficulty.MEDIUM to 1,
    Difficulty.HARD to 0,
)

val DEFAULT_DIFFICULTY: Difficulty = Difficulty.MEDIUM
