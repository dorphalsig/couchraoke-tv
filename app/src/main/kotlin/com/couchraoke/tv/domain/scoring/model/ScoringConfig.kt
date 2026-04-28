package com.couchraoke.tv.domain.scoring.model

import com.couchraoke.tv.domain.model.PlayerId

data class ScoringConfig(
    val playerDifficulties: Map<PlayerId, Difficulty>,
    val lineBonusEnabled: Boolean
)
