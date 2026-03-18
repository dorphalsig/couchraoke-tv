package com.couchraoke.tv.presentation.songlist

import androidx.lifecycle.ViewModel
import com.couchraoke.tv.di.IoDispatcher
import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.domain.library.SongLibrary
import com.couchraoke.tv.domain.session.ISession
import com.couchraoke.tv.domain.session.SessionEvent
import com.couchraoke.tv.domain.session.SessionToken
import com.couchraoke.tv.presentation.songlist.preview.ISongPreviewController
import com.couchraoke.tv.presentation.songlist.preview.NoOpPreviewController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions") // ViewModel: each user story contributes event handlers
@HiltViewModel
class SongListViewModel @Inject constructor(
    private val library: SongLibrary,
    private val session: ISession,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val previewController: ISongPreviewController = NoOpPreviewController(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SongListUiState(
            joinToken = SessionToken.display(session.sessionId),
            sessionState = session.state,
        )
    )
    val uiState: StateFlow<SongListUiState> = _uiState.asStateFlow()

    private val _playerAssignmentEvents = MutableSharedFlow<PlayerAssignment>(extraBufferCapacity = 1)
    val playerAssignmentEvents: SharedFlow<PlayerAssignment> = _playerAssignmentEvents.asSharedFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private val focusedSongIdFlow = MutableStateFlow<String?>(null)
    private val vmScope = CoroutineScope(dispatcher + SupervisorJob())

    init {
        // UNDISPATCHED: establishes SharedFlow subscription synchronously before first suspension,
        // so events emitted immediately after ViewModel construction are never missed.
        vmScope.launch(start = CoroutineStart.UNDISPATCHED) {
            session.events.collect { event ->
                when (event) {
                    is SessionEvent.PhoneConnected,
                    is SessionEvent.PhoneDisconnected,
                    is SessionEvent.PhoneReconnected -> refreshSongs()
                    else -> Unit
                }
            }
        }
        @OptIn(FlowPreview::class)
        vmScope.launch {
            searchQueryFlow
                .debounce(SEARCH_DEBOUNCE_MS)
                .collect { query ->
                    _uiState.update { state ->
                        state.copy(
                            searchQuery = query,
                            filteredSongs = filterSongs(state.allSongs, query),
                        )
                    }
                }
        }
        @OptIn(FlowPreview::class)
        vmScope.launch {
            focusedSongIdFlow
                .debounce(PREVIEW_DEBOUNCE_MS)
                .collect { id ->
                    if (id != null) {
                        val song = library.getSongById(id)
                        if (song != null) {
                            previewController.startPreview(song)
                        } else {
                            previewController.stopPreview()
                        }
                    } else {
                        previewController.stopPreview()
                    }
                }
        }
    }

    private fun refreshSongs() {
        val songs = library.getSortedSongs()
        _uiState.update { state ->
            state.copy(
                allSongs = songs,
                filteredSongs = filterSongs(songs, state.searchQuery),
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    /** Returns true if the back press was consumed (cleared search query). */
    fun onBackPressed(): Boolean {
        return if (_uiState.value.searchQuery.isNotEmpty()) {
            searchQueryFlow.value = ""
            _uiState.update { it.copy(searchQuery = "", filteredSongs = filterSongs(it.allSongs, "")) }
            true
        } else {
            false
        }
    }

    // ---- Song selection (US1) ----
    fun onSongSelected(song: SongEntry) {
        val phones = session.connectedClientIds.mapNotNull { clientId ->
            session.displayNames[clientId]?.let { displayName ->
                PhoneOption(clientId, displayName)
            }
        }.sortedBy { it.displayName }

        _uiState.update { state ->
            state.copy(
                selectPlayersDialog = SelectPlayersDialogState(
                    mode = SelectPlayersMode.SingleSong(song),
                    song = song,
                    availablePhones = phones,
                )
            )
        }
    }

    fun onSongFocused(songId: String?) {
        _uiState.update { it.copy(previewingSongId = songId) }
        focusedSongIdFlow.value = songId
    }

    @Suppress("ReturnCount")
    fun onSelectPlayersStart() {
        val dialog = _uiState.value.selectPlayersDialog ?: return
        val song = dialog.song ?: return

        if (song.audioUrl == null) {
            _uiState.update {
                it.copy(
                    selectPlayersDialog = null,
                    errorModal = ErrorModalState(
                        title = "Song unavailable",
                        bodyLine1 = "This song cannot be played.",
                        bodyLine2 = "The phone may be offline or the file missing."
                    )
                )
            }
            return
        }

        val assignment = buildPlayerAssignment(dialog, song) ?: return
        _uiState.update { it.copy(selectPlayersDialog = null) }
        _playerAssignmentEvents.tryEmit(assignment)
    }

    private fun buildPlayerAssignment(
        dialog: SelectPlayersDialogState,
        song: SongEntry,
    ): PlayerAssignment? {
        val p1 = dialog.player1Selection ?: return null
        val p2 = dialog.player2Selection
        val isDuet = song.isDuet && p2 != null
        val p2Part = if (isDuet) resolvePlayer2Part(dialog.soloPartSelection) else null
        return PlayerAssignment(
            mode = dialog.mode,
            song = song,
            medleyPlaylist = null,
            player1 = p1,
            player1Difficulty = dialog.player1Difficulty,
            player2 = p2,
            player2Difficulty = if (isDuet) dialog.player2Difficulty else null,
            player1Part = dialog.soloPartSelection ?: DuetPart.P1,
            player2Part = p2Part,
        )
    }

    private fun resolvePlayer2Part(soloPartSelection: DuetPart?): DuetPart =
        if (soloPartSelection == DuetPart.P2) DuetPart.P1 else DuetPart.P2

    fun onSelectPlayersCancel() { _uiState.update { it.copy(selectPlayersDialog = null) } }
    fun onErrorModalDismissed() { _uiState.update { it.copy(errorModal = null) } }

    // ---- Medley (US5) ----
    fun onSongLongPressed(song: SongEntry) {
        if (!song.canMedley) {
            _uiState.update {
                it.copy(
                    errorModal = ErrorModalState(
                        title = "Cannot add to medley",
                        bodyLine1 = "This song can't be used in a medley.",
                        bodyLine2 = "Look for songs with an M tag in the lower right corner",
                    )
                )
            }
            return
        }
        _uiState.update { it.copy(medleyPlaylist = it.medleyPlaylist + song) }
    }

    fun onPlaylistRowLongPressed(index: Int) {
        _uiState.update { state ->
            state.copy(medleyPlaylist = state.medleyPlaylist.toMutableList().also { it.removeAt(index) })
        }
    }

    fun onPlaylistRowSelected(index: Int) {
        _uiState.update { it.copy(isReorderingMedleyIndex = if (it.isReorderingMedleyIndex == index) null else index) }
    }

    fun onReorderConfirm(from: Int, to: Int) {
        _uiState.update { state ->
            val list = state.medleyPlaylist.toMutableList()
            if (from in list.indices && to in 0..list.size) {
                val item = list.removeAt(from)
                list.add(to, item)
            }
            state.copy(medleyPlaylist = list, isReorderingMedleyIndex = null)
        }
    }

    fun onReorderCancel() {
        _uiState.update { it.copy(isReorderingMedleyIndex = null) }
    }

    fun onPlayMedley() {
        val playlist = _uiState.value.medleyPlaylist
        if (playlist.isEmpty()) return
        val phones = session.connectedClientIds.mapNotNull { clientId ->
            session.displayNames[clientId]?.let { displayName ->
                PhoneOption(clientId, displayName)
            }
        }.sortedBy { it.displayName }
        _uiState.update { state ->
            state.copy(
                selectPlayersDialog = SelectPlayersDialogState(
                    mode = SelectPlayersMode.Medley(playlist.size),
                    song = null,
                    availablePhones = phones,
                )
            )
        }
    }

    fun clearMedleyPlaylist() { _uiState.update { it.copy(medleyPlaylist = emptyList()) } }

    // ---- Random selection (US6) ----
    fun onRandomSong() {
        val song = _uiState.value.filteredSongs.filter { it.isValid }.randomOrNull() ?: return
        onSongSelected(song)
    }

    fun onRandomDuet() {
        val song = _uiState.value.filteredSongs.filter { it.isDuet }.randomOrNull()
        if (song == null) {
            _uiState.update {
                it.copy(
                    errorModal = ErrorModalState(
                        title = "No duets available",
                        bodyLine1 = "No duet songs match the current filter.",
                    )
                )
            }
            return
        }
        onSongSelected(song)
    }

    // ---- Preview (US4) ----
    fun onScreenVisible(visible: Boolean) {
        if (!visible) {
            previewController.stopPreview()
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewController.release()
        vmScope.cancel()
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 150L
        private const val PREVIEW_DEBOUNCE_MS = 500L
    }
}
