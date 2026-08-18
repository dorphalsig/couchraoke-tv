@file:com.couchraoke.quality.NoCoverageGenerated

package com.couchraoke.tv.presentation.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator
import com.couchraoke.tv.domain.playback.PlaybackCoordinatorState
import com.couchraoke.tv.presentation.playback.AndroidAudioFocusController
import com.couchraoke.tv.presentation.playback.DefaultPlaybackController
import com.couchraoke.tv.presentation.playback.VlcLibVlcPlayerHandle
import com.couchraoke.tv.presentation.singing.SingingViewModel

@Composable
internal fun SingingCoordinatorSyncEffect(
    playbackState: PlaybackCoordinatorState,
    singingViewModel: SingingViewModel,
) {
    LaunchedEffect(playbackState) {
        singingViewModel.syncFromCoordinator()
    }
}

@Composable
internal fun rememberPlaybackController(
    context: Context,
    playbackCoordinator: DefaultPlaybackCoordinator,
    singingViewModel: SingingViewModel,
): DefaultPlaybackController = remember(context, playbackCoordinator, singingViewModel) {
    lateinit var controller: DefaultPlaybackController
    val audioFocusController = AndroidAudioFocusController(context) { change ->
        controller.onAudioFocusChanged(change)
    }
    controller = DefaultPlaybackController(
        audioHandle = VlcLibVlcPlayerHandle(context),
        videoHandle = VlcLibVlcPlayerHandle(context, disableAudio = true),
        clockMs = { System.nanoTime() / 1_000_000 },
        audioFocusController = audioFocusController,
        stopAtLyricsTimeMs = { playbackCoordinator.state.value.phase.stopAtLyricsTimeMsOrZero() },
        onDecorativeVideoAvailableChanged = singingViewModel.onDecorativeVideoAvailableChanged,
    )
    controller
}
