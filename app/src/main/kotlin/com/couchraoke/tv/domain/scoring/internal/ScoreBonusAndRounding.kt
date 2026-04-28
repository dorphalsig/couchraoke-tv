package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.model.PlayerScore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

internal object ScoreBonusAndRounding {
    fun lineBonusPerLine(maxLineBonusPool: Int, nonEmptyLines: Int): Double =
        if (nonEmptyLines == 0) 0.0 else maxLineBonusPool.toDouble() / nonEmptyLines

    fun linePerfection(lineScore: Double, maxLineScore: Double): Double =
        when {
            maxLineScore <= 2.0 -> 1.0
            else -> (lineScore / (maxLineScore - 2.0)).coerceIn(0.0, 1.0)
        }

    fun toPlayerScore(score: Double, scoreGolden: Double, scoreLine: Double, scoreLast: Double): PlayerScore {
        val scoreInt = (round(score / 10.0) * 10.0).toInt()
        val scoreLineInt = (floor(round(scoreLine) / 10.0) * 10.0).toInt()
        val scoreGoldenInt = if (scoreInt < score) {
            (ceil(scoreGolden / 10.0) * 10.0).toInt()
        } else {
            (floor(scoreGolden / 10.0) * 10.0).toInt()
        }
        val scoreTotalInt = (scoreInt + scoreGoldenInt + scoreLineInt).coerceAtMost(10_000)
        return PlayerScore(
            score = score,
            scoreGolden = scoreGolden,
            scoreLine = scoreLine,
            scoreLast = scoreLast,
            scoreInt = scoreInt,
            scoreGoldenInt = scoreGoldenInt,
            scoreLineInt = scoreLineInt,
            scoreTotalInt = scoreTotalInt,
        )
    }
}
