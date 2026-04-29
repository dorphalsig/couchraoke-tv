package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackContractsTest {
    @Test(timeout = 30_000)
    fun songStartSelectionCapturesSoloSingerAndCountdownChoice() {
        val selection = SongStartSelection(
            songId = SoloSingFixtures.SongId,
            playerPhoneId = SoloSingFixtures.PhoneClientId,
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            countdownEnabled = true,
            countdownSeconds = 3,
        )

        assertEquals(SoloSingFixtures.SongId, selection.songId)
        assertEquals(SoloSingFixtures.PhoneClientId, selection.playerPhoneId)
        assertEquals(PlayerId.P1, selection.playerId)
        assertEquals(Difficulty.Medium, selection.difficulty)
        assertTrue(selection.countdownEnabled)
        assertEquals(3, selection.countdownSeconds)
    }

    @Test(timeout = 30_000)
    fun playbackPlanSeparatesCountdownAndLiveStartContracts() {
        val countdownPlan = PlaybackPlan(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            assignedSingers = listOf(
                AssignedSinger(SoloSingFixtures.PhoneClientId, PlayerId.P1, Difficulty.Medium),
            ),
            songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
            startMode = PlaybackStartMode.Countdown,
            countdownMs = 3_000,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            udpPort = SoloSingFixtures.UdpPort,
        )
        val livePlan = countdownPlan.copy(startMode = PlaybackStartMode.Live, countdownMs = null)

        assertEquals(PlaybackStartMode.Countdown, countdownPlan.startMode)
        assertEquals(3_000, countdownPlan.countdownMs)
        assertEquals(PlaybackStartMode.Live, livePlan.startMode)
        assertNull(livePlan.countdownMs)
    }

    @Test(timeout = 30_000)
    fun gamePhaseModelsOpenPreparingCountdownLivePausedAndErrorStates() {
        val selection = SongStartSelection(
            songId = SoloSingFixtures.SongId,
            playerPhoneId = SoloSingFixtures.PhoneClientId,
            playerId = PlayerId.P1,
            difficulty = Difficulty.Medium,
            countdownEnabled = false,
            countdownSeconds = 0,
        )
        val plan = PlaybackPlan(
            song = SoloSingFixtures.indexedSong(),
            parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
            assignedSingers = listOf(
                AssignedSinger(SoloSingFixtures.PhoneClientId, PlayerId.P1, Difficulty.Medium),
            ),
            songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
            startMode = PlaybackStartMode.Live,
            countdownMs = null,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
            udpPort = SoloSingFixtures.UdpPort,
        )

        val openPhase: GamePhase = GamePhase.Open

        assertEquals(GamePhase.Open, openPhase)
        assertEquals(selection, (GamePhase.Preparing(selection) as GamePhase.Preparing).selection)
        assertEquals(plan, (GamePhase.Countdown(plan) as GamePhase.Countdown).plan)
        assertEquals(1234L, (GamePhase.Live(plan, 1234L) as GamePhase.Live).songStartTvMs)
        assertEquals(500L, (GamePhase.Paused(plan, 500L) as GamePhase.Paused).positionMs)
        val errorPhase = GamePhase.Error("ERROR", listOf("This song can't be played."))

        assertEquals("ERROR", errorPhase.title)
    }
}
