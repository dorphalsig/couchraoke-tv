package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.domain.session.SessionState

enum class Difficulty { Easy, Medium, Hard }

enum class DuetPart { P1, P2 }

data class PhoneOption(
    val clientId: String,
    val displayName: String,
)

sealed class SelectPlayersMode {
    data class SingleSong(val song: SongEntry) : SelectPlayersMode()
    data class Medley(val count: Int) : SelectPlayersMode()
}

// Back cascade result - returned by onBackPressed()
sealed class BackResult {
    object ClosedModal : BackResult()
    object MovedToSearch : BackResult()
    object ClearedFilter : BackResult()
    object ExitApp : BackResult()
}

// Focus zones for Back cascade
enum class FocusZone { Grid, LeftPanel, Header }

// Contextual hints for the hints bar (FR-044)
sealed class HintMode {
    object SongTile : HintMode()
    object MedleyRow : HintMode()
    object ReorderMode : HintMode()
}

data class SelectPlayersDialogState(
    val mode: SelectPlayersMode,
    val song: SongEntry?, // null for Medley mode
    val availablePhones: List<PhoneOption>,
    val player1Selection: PhoneOption? = null,
    val player1Difficulty: Difficulty = Difficulty.Medium,
    val player2Selection: PhoneOption? = null,
    val player2Difficulty: Difficulty = Difficulty.Medium,
    val soloPartSelection: DuetPart? = null,
    val isLoading: Boolean = false,
)

data class ErrorModalState(
    val title: String,
    val bodyLine1: String,
    val bodyLine2: String? = null,
)

data class SongListUiState(
    val allSongs: List<SongEntry> = emptyList(),
    val searchQuery: String = "",
    val filteredSongs: List<SongEntry> = emptyList(),
    val medleyPlaylist: List<SongEntry> = emptyList(),
    val isReorderingMedleyIndex: Int? = null,
    val selectPlayersDialog: SelectPlayersDialogState? = null,
    val errorModal: ErrorModalState? = null,
    val previewingSongId: String? = null,
    val focusedSong: SongEntry? = null, // sticky preview pane state
    val isPairingOverlayOpen: Boolean = false, // Join button overlay
    val currentHint: HintMode? = null, // contextual hints bar
    val duplicateMedleyFeedback: Boolean = false, // "Already in medley" toast
    val joinToken: String = "",
    val sessionState: SessionState = SessionState.Open,
)
