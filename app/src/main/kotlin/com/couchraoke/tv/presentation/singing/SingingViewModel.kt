package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.GamePhase
import com.couchraoke.tv.domain.playback.PlaybackCoordinator
import com.couchraoke.tv.domain.playback.PlaybackModal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SingingViewModel(
    private val coordinator: PlaybackCoordinator,
) {
    private val mutableState = MutableStateFlow(SingingUiState())
    val state: StateFlow<SingingUiState> = mutableState

    fun syncFromCoordinator() {
        val coordinatorState = coordinator.state.value
        mutableState.value = when (val phase = coordinatorState.phase) {
            is GamePhase.Countdown -> buildState(
                title = phase.plan.song.title,
                artist = phase.plan.song.artist,
                countdownNumber = (phase.plan.countdownMs ?: 0) / 1_000,
            )
            is GamePhase.Live -> {
                val renderModel = DefaultSingingRenderModelBuilder().build(
                    song = phase.plan.song,
                    parsedSong = phase.plan.parsedSong,
                    playerId = phase.plan.assignedSingers.firstOrNull()?.playerId ?: PlayerId.P1,
                )
                buildState(
                    isPlaying = true,
                    title = renderModel.title,
                    artist = renderModel.artist,
                    currentLyricsLine = renderModel.lyrics.currentLine.text,
                    nextLyricsLine = renderModel.lyrics.nextLine.text,
                    elapsedTimeText = renderModel.elapsedTimeText,
                    scoreText = renderModel.lanes.firstOrNull()?.scoreText ?: "00000",
                    badgeText = renderModel.lanes.firstOrNull()?.badgeText ?: "P1",
                )
            }
            GamePhase.Open -> buildState(returnToSongList = true)
            is GamePhase.Paused -> buildState(pauseOverlay = true)
            is GamePhase.Preparing -> buildState()
            is GamePhase.Error -> buildState(errorLines = phase.bodyLines)
            // Iteration 2 wires Results screen; return to song list as interim behavior.
            is GamePhase.Stopped -> buildState(returnToSongList = true)
            is GamePhase.Results -> buildState(returnToSongList = true)
            // Iteration 3 wires disconnect auto-pause overlay; Iter 1 keeps screen stable.
            is GamePhase.DisconnectPaused -> buildState()
        }.copy(pauseOverlay = coordinatorState.modal == PlaybackModal.Pause)
    }

    fun onBack() {
        mutableState.value = state.value.copy(pauseOverlay = true)
    }

    private fun buildState(
        isPlaying: Boolean = false,
        countdownNumber: Int? = null,
        returnToSongList: Boolean = false,
        pauseOverlay: Boolean = false,
        errorLines: List<String> = emptyList(),
        title: String? = null,
        artist: String? = null,
        currentLyricsLine: String? = null,
        nextLyricsLine: String? = null,
        elapsedTimeText: String = "00:00",
        scoreText: String = "00000",
        badgeText: String = "P1",
    ) = SingingUiState(
        isPlaying = isPlaying,
        countdownNumber = countdownNumber,
        returnToSongList = returnToSongList,
        pauseOverlay = pauseOverlay,
        errorLines = errorLines,
        title = title,
        artist = artist,
        currentLyricsLine = currentLyricsLine,
        nextLyricsLine = nextLyricsLine,
        elapsedTimeText = elapsedTimeText,
        scoreText = scoreText,
        badgeText = badgeText,
    )
}

data class SingingUiState(
    val isPlaying: Boolean = false,
    val countdownNumber: Int? = null,
    val returnToSongList: Boolean = false,
    val pauseOverlay: Boolean = false,
    val errorLines: List<String> = emptyList(),
    val title: String? = null,
    val artist: String? = null,
    val currentLyricsLine: String? = null,
    val nextLyricsLine: String? = null,
    val elapsedTimeText: String = "00:00",
    val scoreText: String = "00000",
    val badgeText: String = "P1",
)
