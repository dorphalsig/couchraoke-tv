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
        assertTrue(rect.bottom > rect.top)
    }

    @Test(timeout = 30_000)
    fun noteWidthIsProportionalToMsDuration() {
        // Note A: 1000ms, Note B: 2000ms — B should be twice as wide as A.
        val rects = noteMarkerRects(
            viewport = Rect(0f, 0f, 400f, 80f),
            state = LaneRenderState(
                playerId = PlayerId.P1,
                noteTargets = listOf(
                    StaticNoteTarget(startTimeMs = 0L, endTimeMs = 1_000L, toneSemitone = 1, lyric = "a"),
                    StaticNoteTarget(startTimeMs = 1_000L, endTimeMs = 3_000L, toneSemitone = 4, lyric = "b"),
                ),
                currentLyricsTimeMs = 0L,
            ),
        )

        val widthA = rects[0].right - rects[0].left
        val widthB = rects[1].right - rects[1].left
        assertEquals(2.0f, widthB / widthA, 0.01f)
    }

    @Test(timeout = 30_000)
    fun noteHeightScalesWithDifficultyThickness() {
        // Two notes spanning 3 semitones (toneSpan=3), viewport height 120px → pixelsPerSemitone = 40.
        // Hard (0): height = 2 * (0 + 0.5) * 40 = 40px
        // Medium (1): height = 2 * (1 + 0.5) * 40 = 120px
        // Easy (2): height = 2 * (2 + 0.5) * 40 = 200px
        fun rectFor(thickness: Int) = noteMarkerRects(
            viewport = Rect(0f, 0f, 400f, 120f),
            state = LaneRenderState(
                playerId = PlayerId.P1,
                noteTargets = listOf(
                    StaticNoteTarget(startTimeMs = 0L, endTimeMs = 1_000L, toneSemitone = 0, lyric = "anchor"),
                    StaticNoteTarget(startTimeMs = 0L, endTimeMs = 1_000L, toneSemitone = 3, lyric = "anchor"),
                    StaticNoteTarget(
                        startTimeMs = 100L,
                        endTimeMs = 900L,
                        toneSemitone = 1,
                        lyric = "test",
                        difficultyThicknessSemitones = thickness,
                    ),
                ),
                currentLyricsTimeMs = 0L,
            ),
        ).last()

        val hardHeight = rectFor(0).let { it.bottom - it.top }
        val mediumHeight = rectFor(1).let { it.bottom - it.top }
        val easyHeight = rectFor(2).let { it.bottom - it.top }

        assertEquals(40f, hardHeight, 0.01f)
        assertEquals(120f, mediumHeight, 0.01f)
        assertEquals(200f, easyHeight, 0.01f)
        assertTrue(hardHeight < mediumHeight)
        assertTrue(mediumHeight < easyHeight)
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
