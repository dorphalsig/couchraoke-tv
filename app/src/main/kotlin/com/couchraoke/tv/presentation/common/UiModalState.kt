package com.couchraoke.tv.presentation.common

sealed interface UiModalState {
    data object None : UiModalState

    data class JoinOverlay(
        val qrPayload: String,
        val joinCode: String,
    ) : UiModalState

    data class SelectPlayers(
        val songId: String,
        val title: String,
        val subtitle: String,
    ) : UiModalState

    data class Pause(
        val defaultFocusedAction: ModalAction = ModalAction.Resume,
    ) : UiModalState

    data class Confirm(
        val title: String,
        val body: String,
        val defaultFocusedAction: ModalAction = ModalAction.Cancel,
    ) : UiModalState

    data class Blocking(
        val title: String,
        val bodyLines: List<String>,
        val action: ModalAction = ModalAction.Ok,
    ) : UiModalState
}

enum class ModalAction {
    Resume,
    RestartSong,
    QuitToSongList,
    Cancel,
    Ok,
    OpenJoinQr,
}
