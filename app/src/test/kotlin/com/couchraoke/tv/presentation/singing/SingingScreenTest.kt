package com.couchraoke.tv.presentation.singing

import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.DisplayHeroTitle
import com.couchraoke.tv.ui.theme.SingingTopIntroStripHeight
import com.couchraoke.tv.ui.theme.SingingTopMinimalStripHeight
import com.couchraoke.tv.ui.theme.TopMetadataMinimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun videoSurfaceIsMediaOverlayAndPitchLaneSurfaceIsNot() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val video = createSingingVideoSurface(context)
        val pitchLane = createPitchLaneSurface(context)

        assertEquals(SurfaceView::class.java, video.javaClass)
        assertFalse(TextureView::class.java.isAssignableFrom(video.javaClass))
        assertEquals(SurfaceView::class.java, pitchLane.javaClass)
        assertFalse(TextureView::class.java.isAssignableFrom(pitchLane.javaClass))
        assertTrue(video.subLayer() > pitchLane.subLayer())
    }

    @Test(timeout = 30_000)
    fun metadataUsesIntroAndActiveHeightsAndTypography() {
        assertEquals(SingingTopIntroStripHeight, metadataStripHeight(isPlaying = false))
        assertEquals(DisplayHeroTitle, metadataTitleStyle(isPlaying = false))
        assertEquals(SingingTopMinimalStripHeight, metadataStripHeight(isPlaying = true))
        assertEquals(TopMetadataMinimal, metadataTitleStyle(isPlaying = true))
    }

    @Test(timeout = 30_000)
    fun videoSurfaceIsAbsentWhenVideoUrlIsNull() {
        composeRule.setContent {
            CouchraokeTheme {
                SingingScreen(state = SingingUiState(videoUrl = null))
            }
        }

        composeRule.onAllNodes(hasTestTag("singing-video-surface")).assertCountEquals(0)
    }

    @Test(timeout = 30_000)
    fun videoSurfaceIsPresentWhenVideoUrlIsNonNull() {
        composeRule.setContent {
            CouchraokeTheme {
                SingingScreen(state = SingingUiState(videoUrl = "http://127.0.0.1/video.mp4"))
            }
        }

        composeRule.onAllNodes(hasTestTag("singing-video-surface")).assertCountEquals(1)
    }

    @Test(timeout = 30_000)
    fun rendersBundledBackgroundAndExactlyTwoLyricsLines() {
        composeRule.setContent {
            CouchraokeTheme {
                SingingScreen(
                    state = SingingUiState(
                        title = "Song",
                        artist = "Artist",
                        currentLyricsLine = "Current line",
                        nextLyricsLine = "Next line",
                        elapsedTimeText = "00:35",
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("singing-static-background").assertIsDisplayed()
        composeRule.onAllNodes(hasTestTag("singing-lyrics-line")).assertCountEquals(2)
        composeRule.onNodeWithText("Current line").assertIsDisplayed()
        composeRule.onNodeWithText("Next line").assertIsDisplayed()
    }

    private fun SurfaceView.subLayer(): Int {
        val field = SurfaceView::class.java.getDeclaredField("mSubLayer")
        field.isAccessible = true
        return field.getInt(this)
    }
}
