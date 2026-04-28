package com.couchraoke.tv.domain.usdx.model

import com.couchraoke.tv.domain.model.PlayerId

data class Track(
    val playerId: PlayerId,
    val lines: List<Line>,
    val trackScoreValue: Long,
)
