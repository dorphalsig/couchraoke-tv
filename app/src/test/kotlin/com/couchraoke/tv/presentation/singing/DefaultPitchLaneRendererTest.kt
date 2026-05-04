package com.couchraoke.tv.presentation.singing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.couchraoke.tv.domain.model.PlayerId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultPitchLaneRendererTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun drawsPitchLaneWithinRealComposeCanvas() {
        val renderer = DefaultPitchLaneRenderer()
        val state = LaneRenderState(
            playerId = PlayerId.P1,
            noteTargets = listOf(
                StaticNoteTarget(startTimeMs = 0L, endTimeMs = 1_000L, toneSemitone = 1, lyric = "la"),
                StaticNoteTarget(startTimeMs = 1_200L, endTimeMs = 2_000L, toneSemitone = 4, lyric = "la"),
            ),
            currentLyricsTimeMs = 0L,
        )

        composeRule.setContent {
            Canvas(modifier = Modifier.testTag("lane").size(160.dp, 80.dp)) {
                renderer.drawPitchLane(
                    canvas = drawContext.canvas,
                    viewport = Rect(0f, 0f, size.width, size.height),
                    state = state,
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("lane").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun computesStableMarkerRectsForStaticNotes() {
        val rects = noteMarkerRects(
            viewport = Rect(0f, 0f, 320f, 160f),
            state = LaneRenderState(
                playerId = PlayerId.P1,
                noteTargets = listOf(
                    StaticNoteTarget(startTimeMs = 0L, endTimeMs = 1_000L, toneSemitone = 1, lyric = "la"),
                    StaticNoteTarget(startTimeMs = 1_200L, endTimeMs = 2_000L, toneSemitone = 4, lyric = "la"),
                ),
                currentLyricsTimeMs = 0L,
            ),
        )

        assertEquals(2, rects.size)
        assertTrue(rects[0].left < rects[1].left)
        assertTrue(rects[0].right > rects[0].left)
        assertTrue(rects[0].bottom > rects[0].top)
        assertTrue(rects[1].bottom < rects[0].bottom)
    }

    @Test(timeout = 30_000)
    fun returnsEmptyMarkerRectsWhenNoNotesExist() {
        val rects = noteMarkerRects(
            viewport = Rect(0f, 0f, 320f, 160f),
            state = LaneRenderState(
                playerId = PlayerId.P1,
                noteTargets = emptyList(),
                currentLyricsTimeMs = 0L,
            ),
        )

        assertTrue(rects.isEmpty())
    }

    @Test(timeout = 30_000)
    fun enforcesMinimumWidthAndHandlesSingleToneSpan() {
        val rect = noteMarkerRects(
            viewport = Rect(0f, 0f, 100f, 50f),
            state = LaneRenderState(
                playerId = PlayerId.P1,
                noteTargets = listOf(
                    StaticNoteTarget(startTimeMs = 10L, endTimeMs = 10L, toneSemitone = 7, lyric = "a"),
                    StaticNoteTarget(startTimeMs = 20L, endTimeMs = 20L, toneSemitone = 7, lyric = "b"),
                ),
                currentLyricsTimeMs = 0L,
            ),
        ).first()

        assertTrue(rect.right - rect.left >= 8f)
        assertEquals(12f, rect.bottom - rect.top, 0.001f)
        assertTrue(rect.bottom > rect.top)
    }

    @Test(timeout = 30_000)
    fun rejectsLivePitchAndScoringFeedbackState() {
        assertThrows(IllegalArgumentException::class.java) {
            validateIteration1LaneState(
                LaneRenderState(
                    playerId = PlayerId.P1,
                    noteTargets = emptyList(),
                    currentLyricsTimeMs = 0L,
                    hasLivePitch = true,
                ),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            validateIteration1LaneState(
                LaneRenderState(
                    playerId = PlayerId.P1,
                    noteTargets = emptyList(),
                    currentLyricsTimeMs = 0L,
                    hasScoringFeedback = true,
                ),
            )
        }
    }
}
