package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.usdx.model.ParsedSong

data class SongStartSelection(
    val songId: String,
    val playerPhoneId: String,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val countdownEnabled: Boolean,
    val countdownSeconds: Int,
)

data class AssignedSinger(
    val phoneId: String,
    val playerId: PlayerId,
    val difficulty: Difficulty,
)

data class PlaybackPlan(
    val song: IndexedSong,
    val parsedSong: ParsedSong,
    val assignedSingers: List<AssignedSinger>,
    val songInstanceSeq: Long,
    val startMode: PlaybackStartMode,
    val countdownMs: Int?,
    val stopAtLyricsTimeMs: Long,
    val udpPort: Int,
    val nextRevision: Long = 1L,
)

enum class PlaybackStartMode {
    Countdown,
    Live,
}

sealed interface GamePhase {
    data object Open : GamePhase
    data class Preparing(
        val selection: SongStartSelection,
    ) : GamePhase

    data class Countdown(
        val plan: PlaybackPlan,
    ) : GamePhase

    data class Live(
        val plan: PlaybackPlan,
        val songStartTvMs: Long,
    ) : GamePhase

    data class Paused(
        val plan: PlaybackPlan,
        val positionMs: Long,
    ) : GamePhase

    data class Error(
        val title: String,
        val bodyLines: List<String>,
    ) : GamePhase

    // Iteration 2: song reached stopAtLyricsTimeMs; triggers scoring finalization before Results.
    data class Stopped(val plan: PlaybackPlan) : GamePhase

    // Iteration 2: Results screen visible; returns to Open when user navigates back to Song List.
    data class Results(val scores: Map<PlayerId, PlayerScore>) : GamePhase

    // Iteration 3: required singer WebSocket dropped during Live; auto-pauses until resolved.
    data class DisconnectPaused(val plan: PlaybackPlan, val disconnectedPlayer: PlayerId) : GamePhase
}

data class PlaybackCoordinatorState(
    val phase: GamePhase = GamePhase.Open,
    val selectedSong: IndexedSong? = null,
    val modal: PlaybackModal? = null,
)

sealed interface PlaybackModal {
    data object Pause : PlaybackModal
    data object RestartConfirm : PlaybackModal
    data object QuitConfirm : PlaybackModal
    data class Disconnected(
        val message: String = "A required singer disconnected during countdown. Please reconnect and start again.",
    ) : PlaybackModal

    data class Error(
        val bodyLines: List<String>,
    ) : PlaybackModal
}
