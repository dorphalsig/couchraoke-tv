package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingRenderContractsTest {
    @Test(timeout = 30_000)
    fun layoutTokensMatchSingleSingerWireframeContract() {
        val layout = SingingLayout()

        assertEquals(72, layout.topIntroStripHeightDp)
        assertEquals(40, layout.topMinimalStripHeightDp)
        assertEquals(160, layout.bottomLyricsBandHeightDp)
        assertEquals(192, layout.singleLaneHeightDp)
        assertEquals(SingingLaneVerticalPosition.Centered, layout.singleLaneVerticalPosition)
        assertEquals(144, layout.scoreBoxWidthDp)
        assertEquals(88, layout.scoreBoxHeightDp)
    }

    @Test(timeout = 30_000)
    fun builderCreatesOneCenteredStaticLaneTwoLyricsLinesAndScorePlaceholder() {
        val model = DefaultSingingRenderModelBuilder().build(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
        )

        assertEquals(1, model.lanes.size)
        assertEquals(PlayerId.P1, model.lanes.single().playerId)
        assertEquals("00000", model.lanes.single().scoreText)
        assertEquals("Hello", model.lyrics.currentLine.text)
        assertEquals("couchraoke", model.lyrics.nextLine.text)
        assertTrue(model.lanes.single().lane.noteTargets.isNotEmpty())
    }

    @Test(timeout = 30_000)
    fun laneStateExcludesLivePitchAndScoringFeedbackForIteration1() {
        val model = DefaultSingingRenderModelBuilder().build(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
        )
        val lane = model.lanes.single().lane

        assertFalse(lane.hasLivePitch)
        assertFalse(lane.hasScoringFeedback)
    }

    @Test(timeout = 30_000)
    fun optionalVideoBecomesBestEffortBackgroundWithStaticFallback() {
        val model = DefaultSingingRenderModelBuilder().build(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
        )

        val background = model.background as SingingBackground.Video

        val staticBackground = SoloSingFixtures.indexedSong()
            .copy(videoUrl = null)
            .toSingingBackground()

        assertEquals(SoloSingFixtures.indexedSong().backgroundUrl, background.fallbackImageUrl)
        assertEquals(SoloSingFixtures.indexedSong().videoUrl, background.videoUrl)
        assertTrue(staticBackground is SingingBackground.Static)
    }
}
