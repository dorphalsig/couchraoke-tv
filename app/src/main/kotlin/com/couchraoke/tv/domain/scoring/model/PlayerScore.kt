package com.couchraoke.tv.domain.scoring.model

data class PlayerScore(
    val score: Double,
    val scoreGolden: Double,
    val scoreLine: Double,
    val scoreLast: Double,
    val scoreInt: Int,
    val scoreGoldenInt: Int,
    val scoreLineInt: Int,
    val scoreTotalInt: Int
)
