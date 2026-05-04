package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.AssignedSinger
import com.couchraoke.tv.domain.playback.GamePhase
import com.couchraoke.tv.domain.playback.PlaybackCoordinator
import com.couchraoke.tv.domain.playback.PlaybackCoordinatorState
import com.couchraoke.tv.domain.playback.PlaybackPlan
import com.couchraoke.tv.domain.playback.PlaybackStartMode
import com.couchraoke.tv.domain.playback.SongStartSelection
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import com.couchraoke.tv.presentation.playback.PlaybackEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingViewModelTest {
    @Test(timeout = 30_000)
    fun countdownStateShowsSongMetadataAndCountdownNumber() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Countdown(plan()),
                selectedSong = SoloSingFixtures.indexedSong(),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        val state = viewModel.state.value
        assertEquals(SoloSingFixtures.SongTitle, state.title)
        assertEquals(SoloSingFixtures.SongArtist, state.artist)
        assertEquals(3, state.countdownNumber)
    }

    @Test(timeout = 30_000)
    fun countdownTickReducesVisibleCountdownNumberAtOneHz() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Countdown(plan()),
                selectedSong = SoloSingFixtures.indexedSong(),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()
        viewModel.advanceCountdown(1_000)

        assertEquals(2, viewModel.state.value.countdownNumber)
    }

    @Test(timeout = 30_000)
    fun liveStateBuildsLyricsAndScorePlaceholderFromRenderModel() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Live(plan(), songStartTvMs = 0L),
                selectedSong = SoloSingFixtures.indexedSong(),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        val state = viewModel.state.value
        assertTrue(state.isPlaying)
        assertEquals("Hello", state.currentLyricsLine)
        assertEquals("couchraoke", state.nextLyricsLine)
        assertEquals("00000", state.scoreText)
        assertEquals(SoloSingFixtures.indexedSong().backgroundUrl, state.backgroundImageUrl)
        assertTrue(state.laneState?.noteTargets?.isNotEmpty() == true)
    }

    @Test(timeout = 30_000)
    fun liveStateForCountdownOffIsImmediatePlaybackState() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Live(
                    plan = plan(
                        countdownMs = null,
                        startMode = PlaybackStartMode.Live,
                    ),
                    songStartTvMs = 0L,
                ),
                selectedSong = SoloSingFixtures.indexedSong(),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        val state = viewModel.state.value
        assertTrue(state.isPlaying)
        assertEquals(null, state.countdownNumber)
    }

    @Test(timeout = 30_000)
    fun stoppedStateReturnsToSongList() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(phase = GamePhase.Stopped(plan())),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        assertTrue(viewModel.state.value.returnToSongList)
    }

    @Test(timeout = 30_000)
    fun resultsStateReturnsToSongList() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(phase = GamePhase.Results(emptyMap())),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        assertTrue(viewModel.state.value.returnToSongList)
    }

    @Test(timeout = 30_000)
    fun disconnectPausedStateKeepsScreenStable() {
        // Iteration 3 wires the disconnect auto-pause overlay; Iter 1 placeholder must not crash
        // and must not navigate away from the singing screen.
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.DisconnectPaused(plan(), disconnectedPlayer = PlayerId.P1),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()

        val state = viewModel.state.value
        assertTrue(!state.returnToSongList)
    }

    @Test(timeout = 30_000)
    fun openStateReturnsToSongListAndBackOpensPauseOverlay() {
        val coordinator = FakePlaybackCoordinator(PlaybackCoordinatorState(phase = GamePhase.Open))
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()
        assertTrue(viewModel.state.value.returnToSongList)

        viewModel.onBack()
        assertTrue(viewModel.state.value.pauseOverlay)
    }

    private fun plan(
        song: IndexedSong = SoloSingFixtures.indexedSong(),
        parsedSong: ParsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
        countdownMs: Int? = 3_000,
        startMode: PlaybackStartMode = PlaybackStartMode.Countdown,
    ): PlaybackPlan = PlaybackPlan(
        song = song,
        parsedSong = parsedSong,
        assignedSingers = listOf(
            AssignedSinger(
                phoneId = SoloSingFixtures.PhoneClientId,
                playerId = PlayerId.P1,
                difficulty = Difficulty.Medium,
            ),
        ),
        songInstanceSeq = 1L,
        startMode = startMode,
        countdownMs = countdownMs,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        udpPort = SoloSingFixtures.UdpPort,
    )

    private class FakePlaybackCoordinator(initialState: PlaybackCoordinatorState) : PlaybackCoordinator {
        private val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<PlaybackCoordinatorState> = mutableState

        override suspend fun startSong(selection: SongStartSelection) = Unit
        override suspend fun pause() = Unit
        override suspend fun resume() = Unit
        override suspend fun restart() = Unit
        override suspend fun quitToSongList() = Unit
        override suspend fun onPlaybackEvent(event: PlaybackEvent) = Unit
    }
}
