package com.couchraoke.tv.presentation.selectplayers

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.SongStartSelection
import com.couchraoke.tv.domain.scoring.model.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SelectPlayersViewModel(
    private val song: IndexedSong,
    connectedPhones: List<ConnectedPhone>,
    countdownEnabled: Boolean = true,
    countdownSeconds: Int = 3,
) {
    private var pendingRecoveryRequest: SelectPlayersRecoveryRequest? = null

    private val playerOne = PlayerSelectionState(
        playerId = PlayerId.P1,
        selectedPhoneId = connectedPhones.firstOrNull()?.clientId,
        difficulty = Difficulty.Medium,
        enabled = true,
    )
    private val playerTwo = PlayerSelectionState(
        playerId = PlayerId.P2,
        selectedPhoneId = null,
        difficulty = Difficulty.Medium,
        enabled = false,
    )
    private val mutableState = MutableStateFlow(
        SelectPlayersState(
            title = "SELECT PLAYERS",
            subtitle = "${song.artist} — ${song.title}",
            playerOne = playerOne,
            playerTwo = playerTwo,
            showPlayerTwoDifficulty = false,
            canStart = playerOne.selectedPhoneId != null,
            countdownEnabled = countdownEnabled,
            countdownSeconds = countdownSeconds,
            noPhoneRecovery = if (connectedPhones.isEmpty()) {
                SelectPlayersNoPhoneRecovery()
            } else {
                null
            },
        )
    )

    val state: StateFlow<SelectPlayersState> = mutableState

    fun startSelection(): SongStartSelection? {
        val current = state.value
        val phoneId = current.playerOne.selectedPhoneId ?: return null
        return SongStartSelection(
            songId = song.songId,
            playerPhoneId = phoneId,
            playerId = PlayerId.P1,
            difficulty = current.playerOne.difficulty,
            countdownEnabled = current.countdownEnabled,
            countdownSeconds = current.countdownSeconds,
        )
    }

    fun openJoinQr() {
        if (state.value.noPhoneRecovery != null) {
            pendingRecoveryRequest = SelectPlayersRecoveryRequest.OpenJoinQrOverlay
        }
    }

    fun consumeRecoveryRequest(): SelectPlayersRecoveryRequest? {
        val request = pendingRecoveryRequest
        pendingRecoveryRequest = null
        return request
    }
}

data class SelectPlayersState(
    val title: String,
    val subtitle: String,
    val playerOne: PlayerSelectionState,
    val playerTwo: PlayerSelectionState,
    val showPlayerTwoDifficulty: Boolean,
    val canStart: Boolean,
    val countdownEnabled: Boolean,
    val countdownSeconds: Int,
    val noPhoneRecovery: SelectPlayersNoPhoneRecovery? = null,
)

data class PlayerSelectionState(
    val playerId: PlayerId,
    val selectedPhoneId: String?,
    val difficulty: Difficulty,
    val enabled: Boolean,
)

data class SelectPlayersNoPhoneRecovery(
    val title: String = "No phones connected",
    val body: String = "Open Join and scan the QR code with the phone app.",
    val primaryActionLabel: String = "Open Join QR",
) {
    val actionLabels: List<String> = listOf(primaryActionLabel)
}

sealed interface SelectPlayersRecoveryRequest {
    data object OpenJoinQrOverlay : SelectPlayersRecoveryRequest
}
