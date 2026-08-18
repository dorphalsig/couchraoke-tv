@file:NoCoverageGenerated
@file:Suppress("TooManyFunctions")

package com.couchraoke.tv.presentation.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.presentation.common.InterruptionShell
import com.couchraoke.tv.presentation.join.JoinOverlay
import com.couchraoke.tv.presentation.selectplayers.PlayerSelectionState
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersModal
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersNoPhoneRecovery
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersState
import com.couchraoke.tv.presentation.singing.SingingModal
import com.couchraoke.tv.presentation.singing.SingingOverlayActions
import com.couchraoke.tv.presentation.singing.SingingOverlays
import com.couchraoke.tv.presentation.singing.SingingScreen
import com.couchraoke.tv.presentation.singing.SingingUiState
import com.couchraoke.tv.presentation.songlist.SongListScreen
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.TV_PREVIEW_HEIGHT_DP
import com.couchraoke.tv.ui.theme.TV_PREVIEW_WIDTH_DP

@NoCoverageGenerated
@Preview(name = "Song List", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSongList() {
    CouchraokeTheme {
        SongListScreen(state = previewSongListState())
    }
}

@Preview(name = "Join Overlay", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewJoinOverlay() {
    val qrPayload = "ws://${PreviewSoloSingSample.TvIpAddress}:${PreviewSoloSingSample.WebSocketPort}" +
        "/?token=${PreviewSoloSingSample.SessionToken}"
    JoinOverlay(
        qrPayload = qrPayload,
        joinCode = PreviewSoloSingSample.JoinCode,
    )
}

@Preview(name = "Select Players", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSelectPlayers() {
    CouchraokeTheme {
        SelectPlayersModal(
            state = SelectPlayersState(
                title = "SELECT PLAYERS",
                subtitle = "${PreviewSoloSingSample.SongArtist} — ${PreviewSoloSingSample.SongTitle}",
                playerOne = PlayerSelectionState(
                    playerId = PlayerId.P1,
                    selectedPhoneId = PreviewSoloSingSample.PhoneDeviceName,
                    difficulty = Difficulty.Medium,
                    enabled = true,
                ),
                playerTwo = PlayerSelectionState(
                    playerId = PlayerId.P2,
                    selectedPhoneId = null,
                    difficulty = Difficulty.Medium,
                    enabled = false,
                ),
                showPlayerTwoDifficulty = false,
                canStart = true,
                countdownEnabled = true,
                countdownSeconds = 3,
                noPhoneRecovery = null,
            ),
            onStart = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Select Players No Phone", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSelectPlayersNoPhone() {
    CouchraokeTheme {
        SelectPlayersModal(
            state = SelectPlayersState(
                title = "SELECT PLAYERS",
                subtitle = "${PreviewSoloSingSample.SongArtist} — ${PreviewSoloSingSample.SongTitle}",
                playerOne = PlayerSelectionState(
                    playerId = PlayerId.P1,
                    selectedPhoneId = null,
                    difficulty = Difficulty.Medium,
                    enabled = true,
                ),
                playerTwo = PlayerSelectionState(
                    playerId = PlayerId.P2,
                    selectedPhoneId = null,
                    difficulty = Difficulty.Medium,
                    enabled = false,
                ),
                showPlayerTwoDifficulty = false,
                canStart = false,
                countdownEnabled = true,
                countdownSeconds = 3,
                noPhoneRecovery = SelectPlayersNoPhoneRecovery(),
            ),
            onStart = {},
            onCancel = {},
        )
    }
}

@Preview(name = "Singing Single Singer", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingSingleSinger() {
    CouchraokeTheme {
        SingingScreen(
            state = SingingUiState(
                isPlaying = true,
                title = PreviewSoloSingSample.SongTitle,
                artist = PreviewSoloSingSample.SongArtist,
                currentLyricsLine = "Hello",
                nextLyricsLine = "couchraoke",
                elapsedTimeText = "00:35",
                scoreText = "00000",
                badgeText = "P1",
            ),
        )
    }
}

@Preview(name = "Singing Countdown", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingCountdown() {
    CouchraokeTheme {
        SingingScreen(
            state = SingingUiState(
                isPlaying = false,
                countdownNumber = 3,
                title = PreviewSoloSingSample.SongTitle,
                artist = PreviewSoloSingSample.SongArtist,
            ),
        )
    }
}

@Preview(name = "Singing Pause Overlay", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingPause() {
    CouchraokeTheme {
        SingingOverlays(
            state = SingingUiState(activeModal = SingingModal.Pause),
            actions = SingingOverlayActions(),
        )
    }
}

@Preview(name = "Singing Restart Confirm", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingRestartConfirm() {
    CouchraokeTheme {
        SingingOverlays(
            state = SingingUiState(activeModal = SingingModal.RestartConfirm),
            actions = SingingOverlayActions(),
        )
    }
}

@Preview(name = "Singing Quit Confirm", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingQuitConfirm() {
    CouchraokeTheme {
        SingingOverlays(
            state = SingingUiState(activeModal = SingingModal.QuitConfirm),
            actions = SingingOverlayActions(),
        )
    }
}

@Preview(name = "Singing Disconnect", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingDisconnect() {
    CouchraokeTheme {
        InterruptionShell(
            title = "DISCONNECTED",
            bodyLines = listOf(
                "A required singer disconnected",
                "during countdown.",
                "Please reconnect and start again.",
            ),
        ) {
            Button(onClick = {}) { Text("OK") }
        }
    }
}

@Preview(name = "Singing Error", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun PreviewSingingError() {
    CouchraokeTheme {
        InterruptionShell(
            title = "ERROR",
            bodyLines = listOf(
                "This song can't be played.",
                "LibVLC: http connection failed: 404 Not Found",
            ),
        ) {
            Button(onClick = {}) { Text("OK") }
        }
    }
}
