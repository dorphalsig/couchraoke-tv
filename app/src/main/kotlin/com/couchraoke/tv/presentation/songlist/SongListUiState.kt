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
    val joinToken: String = "",
    val sessionState: SessionState = SessionState.Open,
)
