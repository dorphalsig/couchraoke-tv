package com.couchraoke.tv.domain.scoring

object BeatCalculator {
    fun timeSecToMidBeatInternal(tSec: Double, bpmInternal: Float): Double =
        tSec * (bpmInternal / 60.0)

    fun beatInternalToTimeSec(beatInternal: Double, bpmInternal: Float): Double =
        beatInternal * (60.0 / bpmInternal)
}
