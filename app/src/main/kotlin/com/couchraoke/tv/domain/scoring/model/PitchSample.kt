package com.couchraoke.tv.domain.scoring.model

import com.couchraoke.tv.domain.model.PlayerId

data class PitchSample(
    val playerId: PlayerId,
    val midiNote: Int,
    val tvTimeMs: Long?
)
