package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.playback.GamePhase
import com.couchraoke.tv.domain.playback.PlaybackCoordinator
import com.couchraoke.tv.domain.playback.PlaybackModal
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.presentation.singing.SingingBackground.Static
import com.couchraoke.tv.presentation.singing.SingingBackground.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

class SingingViewModel(
    private val coordinator: PlaybackCoordinator,
) {
    private val mutableState = MutableStateFlow(SingingUiState())
    private var countdownRemainingMs: Int? = null
    val state: StateFlow<SingingUiState> = mutableState

    fun syncFromCoordinator() {
        val coordinatorState = coordinator.state.value
        mutableState.value = when (val phase = coordinatorState.phase) {
            is GamePhase.Countdown -> {
                countdownRemainingMs = phase.plan.countdownMs ?: 0
                buildState(
                    title = phase.plan.song.title,
                    artist = phase.plan.song.artist,
                    countdownNumber = countdownNumber(countdownRemainingMs ?: 0),
                )
            }
            is GamePhase.Live -> {
                val singer = phase.plan.assignedSingers.firstOrNull()
                val renderModel = DefaultSingingRenderModelBuilder().buildAtLyricsTime(
                    song = phase.plan.song,
                    parsedSong = phase.plan.parsedSong,
                    playerId = singer?.playerId ?: PlayerId.P1,
                    difficulty = singer?.difficulty ?: Difficulty.Medium,
                    lyricsTimeMs = 0L,
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
                    laneState = renderModel.lanes.firstOrNull()?.lane,
                    backgroundImageUrl = renderModel.background.fallbackImageUrl(),
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
        }.withCoordinatorModal(coordinatorState.modal)
    }

    fun advanceCountdown(elapsedMs: Int) {
        val current = countdownRemainingMs ?: return
        val next = (current - elapsedMs).coerceAtLeast(0)
        countdownRemainingMs = next
        mutableState.value = state.value.copy(countdownNumber = countdownNumber(next))
    }

    fun onBack() {
        val current = state.value
        mutableState.value = if (current.activeModal != null) {
            current.withClosedModal()
        } else {
            runBlocking { coordinator.pause() }
            current.copy(
                pauseOverlay = true,
                activeModal = SingingModal.Pause,
                defaultFocus = SingingControlFocus.Resume,
            )
        }
    }

    fun onResume() {
        runBlocking { coordinator.resume() }
        mutableState.value = state.value.copy(activeModal = null, pauseOverlay = false, defaultFocus = null)
    }

    fun onRestartRequested() {
        mutableState.value = state.value.copy(
            activeModal = SingingModal.RestartConfirm,
            pauseOverlay = false,
            defaultFocus = SingingControlFocus.Cancel,
        )
    }

    fun onRestartConfirmed() {
        runBlocking { coordinator.restart() }
        mutableState.value = state.value.copy(activeModal = null, pauseOverlay = false, defaultFocus = null)
    }

    fun onQuitRequested() {
        mutableState.value = state.value.copy(
            activeModal = SingingModal.QuitConfirm,
            pauseOverlay = false,
            defaultFocus = SingingControlFocus.Cancel,
        )
    }

    fun onQuitConfirmed() {
        runBlocking { coordinator.quitToSongList() }
        mutableState.value = state.value.copy(activeModal = null, pauseOverlay = false, defaultFocus = null)
    }

    private fun countdownNumber(remainingMs: Int): Int = (remainingMs + 999) / 1_000

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
        laneState: LaneRenderState? = null,
        backgroundImageUrl: String? = null,
        activeModal: SingingModal? = null,
        defaultFocus: SingingControlFocus? = null,
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
        laneState = laneState,
        backgroundImageUrl = backgroundImageUrl,
        activeModal = activeModal,
        defaultFocus = defaultFocus,
    )
}

private fun SingingUiState.withClosedModal(): SingingUiState = copy(
    activeModal = null,
    pauseOverlay = false,
    defaultFocus = null,
)

private fun SingingUiState.withCoordinatorModal(modal: PlaybackModal?): SingingUiState = when (modal) {
    PlaybackModal.Pause -> copy(
        pauseOverlay = true,
        activeModal = SingingModal.Pause,
        defaultFocus = SingingControlFocus.Resume,
    )
    PlaybackModal.RestartConfirm -> copy(
        activeModal = SingingModal.RestartConfirm,
        defaultFocus = SingingControlFocus.Cancel,
    )
    PlaybackModal.QuitConfirm -> copy(
        activeModal = SingingModal.QuitConfirm,
        defaultFocus = SingingControlFocus.Cancel,
    )
    is PlaybackModal.Disconnected,
    is PlaybackModal.Error,
    null,
    -> this
}

private fun SingingBackground.fallbackImageUrl(): String? = when (this) {
    is Static -> imageUrl
    is Video -> fallbackImageUrl
}

enum class SingingModal {
    Pause,
    RestartConfirm,
    QuitConfirm,
}

enum class SingingControlFocus {
    Resume,
    Cancel,
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
    val laneState: LaneRenderState? = null,
    val backgroundImageUrl: String? = null,
    val activeModal: SingingModal? = null,
    val defaultFocus: SingingControlFocus? = null,
)
