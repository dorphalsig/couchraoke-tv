package com.couchraoke.tv.presentation.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.couchraoke.tv.presentation.singing.SingingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingingCoordinatorSyncEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun coordinatorStateChangesRefreshSingingViewModelState() {
        val coordinator = FakePlaybackCoordinator(
            PlaybackCoordinatorState(GamePhase.Countdown(plan()), selectedSong = SoloSingFixtures.indexedSong()),
        )
        val viewModel = SingingViewModel(coordinator)

        composeRule.setContent {
            val playbackState by coordinator.state.collectAsState()
            SingingCoordinatorSyncEffect(playbackState = playbackState, singingViewModel = viewModel)
        }

        composeRule.waitForIdle()
        assertEquals(3, viewModel.state.value.countdownNumber)
        assertFalse(viewModel.state.value.isPlaying)

        composeRule.runOnIdle {
            coordinator.mutableState.value = PlaybackCoordinatorState(
                phase = GamePhase.Live(plan(), songStartTvMs = SoloSingFixtures.ReadySongStartTvMs),
                selectedSong = SoloSingFixtures.indexedSong(),
            )
        }

        composeRule.waitForIdle()
        assertTrue(viewModel.state.value.isPlaying)
        assertEquals(SoloSingFixtures.SongTitle, viewModel.state.value.title)

        composeRule.runOnIdle {
            coordinator.mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Open)
        }

        composeRule.waitForIdle()
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
        songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
        startMode = PlaybackStartMode.Countdown,
        countdownMs = 3_000,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        udpPort = SoloSingFixtures.UdpPort,
    )

    private class FakePlaybackCoordinator(
        initialState: PlaybackCoordinatorState,
    ) : PlaybackCoordinator {
        val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<PlaybackCoordinatorState> = mutableState
        override suspend fun startSong(selection: SongStartSelection) = Unit
        override suspend fun pause() = Unit
        override suspend fun resume() = Unit
        override suspend fun restart() = Unit
        override suspend fun quitToSongList() = Unit
        override suspend fun onPlaybackEvent(event: PlaybackEvent) = Unit
    }
}
