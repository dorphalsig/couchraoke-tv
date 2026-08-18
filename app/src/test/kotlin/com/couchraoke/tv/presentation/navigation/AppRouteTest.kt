package com.couchraoke.tv.presentation.navigation

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.AssignedSinger
import com.couchraoke.tv.domain.playback.GamePhase
import com.couchraoke.tv.domain.playback.PlaybackPlan
import com.couchraoke.tv.domain.playback.PlaybackStartMode
import com.couchraoke.tv.domain.playback.SongStartSelection
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test(timeout = 30_000)
    fun routeContractsIncludeSongListSingingAndInertResultsOnly() {
        val routes: List<AppRoute> = listOf(
            AppRoute.SongList,
            AppRoute.Singing,
            AppRoute.Results,
        )

        assertEquals(listOf("songList", "singing", "results"), routes.map { it.route })
    }

    @Test(timeout = 30_000)
    fun stopBoundaryIsReadOnlyForPlaybackPhasesOnly() {
        val plan = playbackPlan()

        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, GamePhase.Countdown(plan).stopAtLyricsTimeMsOrZero())
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, GamePhase.Live(plan, 0L).stopAtLyricsTimeMsOrZero())
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, GamePhase.Paused(plan, 0L).stopAtLyricsTimeMsOrZero())
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, GamePhase.Stopped(plan).stopAtLyricsTimeMsOrZero())
        assertEquals(
            SoloSingFixtures.StopAtLyricsTimeMs,
            GamePhase.DisconnectPaused(plan, PlayerId.P1).stopAtLyricsTimeMsOrZero(),
        )
        assertEquals(0L, GamePhase.Open.stopAtLyricsTimeMsOrZero())
        assertEquals(0L, GamePhase.Preparing(songStartSelection()).stopAtLyricsTimeMsOrZero())
        assertEquals(0L, GamePhase.Error("ERROR", emptyList()).stopAtLyricsTimeMsOrZero())
        assertEquals(0L, GamePhase.Results(emptyMap()).stopAtLyricsTimeMsOrZero())
    }

    private fun playbackPlan(): PlaybackPlan = PlaybackPlan(
        song = SoloSingFixtures.indexedSong(),
        parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
        assignedSingers = listOf(
            AssignedSinger(
                phoneId = SoloSingFixtures.PhoneClientId,
                playerId = PlayerId.P1,
                difficulty = Difficulty.Medium,
            ),
        ),
        songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
        startMode = PlaybackStartMode.Countdown,
        countdownMs = 3_000,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        udpPort = SoloSingFixtures.UdpPort,
    )

    private fun songStartSelection(): SongStartSelection = SongStartSelection(
        songId = SoloSingFixtures.SongId,
        playerPhoneId = SoloSingFixtures.PhoneClientId,
        playerId = PlayerId.P1,
        difficulty = Difficulty.Medium,
        countdownEnabled = true,
        countdownSeconds = 3,
    )
}
