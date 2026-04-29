package com.couchraoke.tv.presentation.singing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas

interface PitchLaneRenderer {
    fun drawPitchLane(canvas: Canvas, viewport: Rect, state: LaneRenderState)
}

class DefaultPitchLaneRenderer : PitchLaneRenderer {
    override fun drawPitchLane(canvas: Canvas, viewport: Rect, state: LaneRenderState) {
        require(!state.hasLivePitch)
        require(!state.hasScoringFeedback)
    }
}
