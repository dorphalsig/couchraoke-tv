@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.playback.PlaybackCoordinatorState
import com.couchraoke.tv.domain.playback.PlaybackModal
import com.couchraoke.tv.presentation.common.InterruptionShell
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.SelectPlayersActionButtonWidth

@Composable
fun PlaybackRecoveryRouting(
    playbackState: PlaybackCoordinatorState,
    onDisconnectedRecovery: (IndexedSong?) -> Unit,
    onErrorRecovery: () -> Unit,
) {
    var dismissedModal by remember { mutableStateOf<PlaybackModal?>(null) }
    LaunchedEffect(playbackState.modal) {
        dismissedModal = null
        when (playbackState.modal) {
            is PlaybackModal.Disconnected -> onDisconnectedRecovery(playbackState.selectedSong)
            is PlaybackModal.Error -> onErrorRecovery()
            PlaybackModal.Pause,
            PlaybackModal.RestartConfirm,
            PlaybackModal.QuitConfirm,
            null,
            -> Unit
        }
    }
    RecoveryModal(
        modal = playbackState.modal.takeUnless { it == dismissedModal },
        onDismiss = { dismissedModal = playbackState.modal },
    )
}

@Composable
private fun RecoveryModal(modal: PlaybackModal?, onDismiss: () -> Unit) {
    when (modal) {
        is PlaybackModal.Disconnected -> InterruptionShell(
            title = "DISCONNECTED",
            bodyLines = listOf(modal.message),
        ) {
            RecoveryOkButton(onDismiss)
        }
        is PlaybackModal.Error -> InterruptionShell(
            title = "ERROR",
            bodyLines = modal.bodyLines,
        ) {
            RecoveryOkButton(onDismiss)
        }
        PlaybackModal.Pause,
        PlaybackModal.RestartConfirm,
        PlaybackModal.QuitConfirm,
        null,
        -> Unit
    }
}

@Composable
private fun RecoveryOkButton(onDismiss: () -> Unit) {
    Button(onClick = onDismiss, modifier = Modifier.width(SelectPlayersActionButtonWidth)) {
        Text("OK", style = ButtonLabel)
    }
}
