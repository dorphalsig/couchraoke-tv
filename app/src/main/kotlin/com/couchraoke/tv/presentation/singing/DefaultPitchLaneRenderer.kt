@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.singing

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import com.couchraoke.quality.NoCoverageGenerated

private val LaneBandColor = Color(0x66222222)
private val NoteMarkerColor = Color(0xFF5CD6FF)

@NoCoverageGenerated
class DefaultPitchLaneRenderer : PitchLaneRenderer {
    override fun drawPitchLane(canvas: Canvas, viewport: Rect, state: LaneRenderState) {
        validateIteration1LaneState(state)

        canvas.drawRoundRect(
            left = viewport.left,
            top = viewport.top,
            right = viewport.right,
            bottom = viewport.bottom,
            radiusX = 24f,
            radiusY = 24f,
            paint = Paint().apply {
                color = LaneBandColor
                style = PaintingStyle.Fill
            },
        )

        noteMarkerRects(viewport, state).forEach { rect ->
            canvas.drawRoundRect(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom,
                radiusX = 6f,
                radiusY = 6f,
                paint = Paint().apply {
                    color = NoteMarkerColor
                    style = PaintingStyle.Fill
                },
            )
        }
    }
}

internal fun validateIteration1LaneState(state: LaneRenderState) {
    require(!state.hasLivePitch)
    require(!state.hasScoringFeedback)
}

internal fun noteMarkerRects(viewport: Rect, state: LaneRenderState): List<Rect> {
    if (state.noteTargets.isEmpty()) return emptyList()

    val minStart = state.noteTargets.minOf { it.startTimeMs }
    val maxEnd = state.noteTargets.maxOf { it.endTimeMs }.coerceAtLeast(minStart + 1L)
    val minTone = state.noteTargets.minOf { it.toneSemitone }
    val maxTone = state.noteTargets.maxOf { it.toneSemitone }.coerceAtLeast(minTone + 1)
    val durationSpan = (maxEnd - minStart).toFloat()
    val toneSpan = (maxTone - minTone).toFloat()

    val dpPerSemitone = viewport.height / toneSpan
    return state.noteTargets.map { note ->
        val left = viewport.left + viewport.width * ((note.startTimeMs - minStart).toFloat() / durationSpan)
        val right = viewport.left + viewport.width * ((note.endTimeMs - minStart).toFloat() / durationSpan)
        val toneFraction = (note.toneSemitone - minTone).toFloat() / toneSpan
        val centerY = viewport.bottom - (viewport.height * toneFraction)
        val halfHeight = (note.difficultyThicknessSemitones + 0.5f) * dpPerSemitone
        Rect(
            left = left,
            top = centerY - halfHeight,
            right = right.coerceAtLeast(left + 8f),
            bottom = centerY + halfHeight,
        )
    }
}
