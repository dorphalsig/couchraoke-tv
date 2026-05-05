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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingingViewModelControlsTest {
    @Test(timeout = 30_000)
    fun backOpensPauseOverlayWithResumeDefaultFocus() = runBlocking {
        val coordinator = FakePlaybackCoordinator(PlaybackCoordinatorState(GamePhase.Live(plan(), 0L)))
        val viewModel = SingingViewModel(coordinator)

        viewModel.onBack()

        assertEquals(SingingModal.Pause, viewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Resume, viewModel.state.value.defaultFocus)
        assertEquals(1, coordinator.pauseCalls)
    }

    @Test(timeout = 30_000)
    fun resumeCallsCoordinatorAndClosesPauseOverlay() = runBlocking {
        val coordinator = FakePlaybackCoordinator(PlaybackCoordinatorState(GamePhase.Paused(plan(), 42L)))
        val viewModel = SingingViewModel(coordinator)
        viewModel.syncFromCoordinator()

        viewModel.onResume()

        assertEquals(1, coordinator.resumeCalls)
        assertEquals(null, viewModel.state.value.activeModal)
    }

    @Test(timeout = 30_000)
    fun restartAndQuitRequireConfirmationWithCancelDefaultFocus() = runBlocking {
        val viewModel = SingingViewModel(
            FakePlaybackCoordinator(
                PlaybackCoordinatorState(GamePhase.Paused(plan(), 42L)),
            ),
        )

        viewModel.onRestartRequested()
        assertEquals(SingingModal.RestartConfirm, viewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Cancel, viewModel.state.value.defaultFocus)

        viewModel.onQuitRequested()
        assertEquals(SingingModal.QuitConfirm, viewModel.state.value.activeModal)
        assertEquals(SingingControlFocus.Cancel, viewModel.state.value.defaultFocus)
    }

    @Test(timeout = 30_000)
    fun confirmRestartAndQuitCallCoordinatorControls() = runBlocking {
        val coordinator = FakePlaybackCoordinator(PlaybackCoordinatorState(GamePhase.Paused(plan(), 42L)))
        val viewModel = SingingViewModel(coordinator)

        viewModel.onRestartConfirmed()
        viewModel.onQuitConfirmed()

        assertEquals(1, coordinator.restartCalls)
        assertEquals(1, coordinator.quitCalls)
    }

    @Test(timeout = 30_000)
    fun backClosesOpenModalBeforeOpeningPauseOnSecondBack() = runBlocking {
        val viewModel = SingingViewModel(
            FakePlaybackCoordinator(
                PlaybackCoordinatorState(GamePhase.Live(plan(), 0L)),
            ),
        )
        viewModel.onRestartRequested()

        viewModel.onBack()
        assertEquals(null, viewModel.state.value.activeModal)

        viewModel.onBack()
        assertEquals(SingingModal.Pause, viewModel.state.value.activeModal)
    }

    @Test(timeout = 30_000)
    fun resultsStateReturnsToSongList() = runBlocking {
        val viewModel = SingingViewModel(
            FakePlaybackCoordinator(
                PlaybackCoordinatorState(GamePhase.Results(emptyMap())),
            ),
        )

        viewModel.syncFromCoordinator()

        assertTrue(viewModel.state.value.returnToSongList)
    }

    private fun plan(
        song: IndexedSong = SoloSingFixtures.indexedSong(),
        parsedSong: ParsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
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
        startMode = PlaybackStartMode.Countdown,
        countdownMs = 3_000,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        udpPort = SoloSingFixtures.UdpPort,
    )

    private class FakePlaybackCoordinator(
        initialState: PlaybackCoordinatorState,
    ) : PlaybackCoordinator {
        private val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<PlaybackCoordinatorState> = mutableState
        var pauseCalls = 0
        var resumeCalls = 0
        var restartCalls = 0
        var quitCalls = 0
        override suspend fun startSong(selection: SongStartSelection) = Unit
        override suspend fun pause() {
            pauseCalls++
        }
        override suspend fun resume() {
            resumeCalls++
        }
        override suspend fun restart() {
            restartCalls++
        }
        override suspend fun quitToSongList() {
            quitCalls++
        }
        override suspend fun onPlaybackEvent(event: PlaybackEvent) = Unit
    }
}
