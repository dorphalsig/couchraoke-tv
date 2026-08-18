package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.AssignedSinger
import com.couchraoke.tv.domain.playback.GamePhase
import com.couchraoke.tv.domain.playback.PlaybackCoordinator
import com.couchraoke.tv.domain.playback.PlaybackCoordinatorState
import com.couchraoke.tv.domain.playback.PlaybackModal
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
        assertEquals(SoloSingFixtures.indexedSong().videoUrl, state.videoUrl)
        assertTrue(state.laneState?.noteTargets?.isNotEmpty() == true)
    }

    @Test(timeout = 30_000)
    fun decorativeVideoUnavailableRemovesVideoButKeepsStaticFallback() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Live(plan(), songStartTvMs = 0L),
                selectedSong = SoloSingFixtures.indexedSong(),
            ),
        )
        val viewModel = SingingViewModel(coordinator = coordinator)

        viewModel.syncFromCoordinator()
        viewModel.onDecorativeVideoAvailableChanged(false)

        val state = viewModel.state.value
        assertTrue(state.isPlaying)
        assertEquals(null, state.videoUrl)
        assertEquals(SoloSingFixtures.indexedSong().backgroundUrl, state.backgroundImageUrl)
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

    @Test(timeout = 30_000)
    fun pausedAndErrorPhasesMapToUiState() {
        val pausedViewModel = SingingViewModel(
            FakePlaybackCoordinator(PlaybackCoordinatorState(phase = GamePhase.Paused(plan(), positionMs = 42L))),
        )
        pausedViewModel.syncFromCoordinator()
        assertTrue(pausedViewModel.state.value.pauseOverlay)

        val errorLines = listOf("This song can't be played.", "Check the song's phone.")
        val errorViewModel = SingingViewModel(
            FakePlaybackCoordinator(PlaybackCoordinatorState(phase = GamePhase.Error("ERROR", errorLines))),
        )
        errorViewModel.syncFromCoordinator()
        assertEquals(errorLines, errorViewModel.state.value.errorLines)
    }

    @Test(timeout = 30_000)
    fun coordinatorModalsMapToActiveModalAndDefaultFocus() {
        val pauseViewModel = modalViewModel(PlaybackModal.Pause)
        assertEquals(SingingModal.Pause, pauseViewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Resume, pauseViewModel.state.value.defaultFocus)

        val restartViewModel = modalViewModel(PlaybackModal.RestartConfirm)
        assertEquals(SingingModal.RestartConfirm, restartViewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Cancel, restartViewModel.state.value.defaultFocus)

        val quitViewModel = modalViewModel(PlaybackModal.QuitConfirm)
        assertEquals(SingingModal.QuitConfirm, quitViewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Cancel, quitViewModel.state.value.defaultFocus)
    }

    @Test(timeout = 30_000)
    fun overlayActionsInvokeCoordinatorAndUpdateModalState() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(
                phase = GamePhase.Live(
                    plan = plan(),
                    songStartTvMs = 0L,
                ),
            ),
        )
        val viewModel = SingingViewModel(coordinator)
        viewModel.syncFromCoordinator()

        viewModel.onRestartRequested()
        assertEquals(SingingModal.RestartConfirm, viewModel.state.value.activeModal)
        viewModel.onBack()
        assertEquals(null, viewModel.state.value.activeModal)

        viewModel.onResume()
        assertEquals(1, coordinator.resumeCallCount)
        assertEquals(null, viewModel.state.value.activeModal)

        viewModel.onRestartRequested()
        viewModel.onRestartConfirmed()
        assertEquals(1, coordinator.restartCallCount)
        assertEquals(null, viewModel.state.value.activeModal)

        viewModel.onQuitRequested()
        assertEquals(SingingModal.QuitConfirm, viewModel.state.value.activeModal)
        viewModel.onQuitConfirmed()
        assertEquals(1, coordinator.quitCallCount)
        assertEquals(null, viewModel.state.value.activeModal)
    }

    private fun modalViewModel(modal: PlaybackModal): SingingViewModel {
        val viewModel = SingingViewModel(
            FakePlaybackCoordinator(
                PlaybackCoordinatorState(phase = GamePhase.Live(plan(), songStartTvMs = 0L), modal = modal),
            ),
        )
        viewModel.syncFromCoordinator()
        return viewModel
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
        var pauseCallCount = 0
        var resumeCallCount = 0
        var restartCallCount = 0
        var quitCallCount = 0

        override suspend fun startSong(selection: SongStartSelection) = Unit
        override suspend fun pause() {
            pauseCallCount++
        }
        override suspend fun resume() {
            resumeCallCount++
        }
        override suspend fun restart() {
            restartCallCount++
        }
        override suspend fun quitToSongList() {
            quitCallCount++
        }
        override suspend fun onPlaybackEvent(event: PlaybackEvent) = Unit
    }
}
