package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingRenderModelBuilderTest {
    @Test(timeout = 30_000)
    fun buildsSentencePagedCurrentAndNextLyricsWithStableSlots() {
        val model = DefaultSingingRenderModelBuilder().build(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
        )

        assertEquals("Hello", model.lyrics.currentLine.text)
        assertEquals("couchraoke", model.lyrics.nextLine.text)
        assertTrue(model.lyrics.currentLine.startTimeMs != null)
        assertTrue(model.lyrics.nextLine.startTimeMs != null)
    }

    @Test(timeout = 30_000)
    fun instrumentalGapKeepsCompletedSentenceHighlightedInsteadOfBlankPaging() {
        val model = DefaultSingingRenderModelBuilder().buildAtLyricsTime(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            lyricsTimeMs = 1_499L,
        )

        assertEquals("Hello", model.lyrics.currentLine.text)
        assertEquals(1f, model.lyrics.highlightFraction)
    }

    @Test(timeout = 30_000)
    fun exposesClippedRevealHighlightModelAndStaticP1NoteTargetsOnly() {
        val model = DefaultSingingRenderModelBuilder().build(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
        )

        val lane = model.lanes.single().lane
        assertEquals(PlayerId.P1, lane.playerId)
        assertTrue(lane.noteTargets.isNotEmpty())
        assertFalse(lane.hasLivePitch)
        assertFalse(lane.hasScoringFeedback)
        assertEquals(0f, model.lyrics.highlightFraction)
    }

    @Test(timeout = 30_000)
    fun noteTargetThicknessReflectsSelectedDifficulty() {
        fun thicknessFor(difficulty: Difficulty) = DefaultSingingRenderModelBuilder().buildAtLyricsTime(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            playerId = PlayerId.P1,
            difficulty = difficulty,
            lyricsTimeMs = 0L,
        ).lanes.single().lane.noteTargets.first().difficultyThicknessSemitones

        assertEquals(2, thicknessFor(Difficulty.Easy))
        assertEquals(1, thicknessFor(Difficulty.Medium))
        assertEquals(0, thicknessFor(Difficulty.Hard))
    }
}
