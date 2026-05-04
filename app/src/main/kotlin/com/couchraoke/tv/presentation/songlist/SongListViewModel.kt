package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.presentation.playback.PreviewPlaybackState
import com.couchraoke.tv.presentation.playback.previewStartPositionMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val MEDLEY_DISABLED_MESSAGE = "This song can't be used in a medley. " +
    "Look for songs with an M tag in the lower right corner"

class SongListViewModel(
    songs: List<IndexedSong>,
    connectedPhoneCount: Int = 1,
    private val joinEndpointUrl: String = "",
    private val joinCode: String = "",
) {
    private val allSongs = songs.sortedWith(
        compareBy<IndexedSong, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album.orEmpty() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )
    private val mutableState = MutableStateFlow(
        SongListState(
            visibleSongs = allSongs,
            emptyState = songListEmptyStateFor(allSongs, connectedPhoneCount),
            randomDuetEnabled = false,
            randomMedleyEnabled = false,
        )
    )
    private var pendingSearchElapsedMs = 0L
    private var pendingFocusedSongId: String? = null
    private var pendingPreviewElapsedMs = 0L

    val state: StateFlow<SongListState> = mutableState

    fun updateSearch(query: String) {
        pendingSearchElapsedMs = 0L
        mutableState.value = mutableState.value.copy(searchQuery = query)
    }

    fun advanceSearchDebounce(ms: Long) {
        pendingSearchElapsedMs += ms
        if (pendingSearchElapsedMs >= 150L) {
            mutableState.value = mutableState.value.copy(
                visibleSongs = songsMatching(allSongs, state.value.searchQuery),
            )
        }
    }

    fun onBack(): Boolean {
        val shouldExit = state.value.focusArea == SongListFocusArea.Search && state.value.searchQuery.isBlank()
        if (!shouldExit) {
            handleConsumedBack()
        }
        return shouldExit
    }

    fun onSongFocused(songId: String) {
        val song = allSongs.firstOrNull { it.songId == songId } ?: return
        pendingFocusedSongId = songId
        pendingPreviewElapsedMs = 0L
        mutableState.value = mutableState.value.copy(focusedSong = song)
        stopPreview(clearPendingFocusedSong = false)
    }

    fun advancePreviewDebounce(ms: Long) {
        pendingPreviewElapsedMs += ms
        if (pendingPreviewElapsedMs >= 500L) {
            val song = allSongs.firstOrNull { it.songId == pendingFocusedSongId } ?: return
            mutableState.value = mutableState.value.copy(
                focusedSong = song,
                preview = PreviewPlaybackState(
                    songId = song.songId,
                    audioUrl = song.audioUrl,
                    startPositionMs = previewStartPositionMs(song.previewStartSec),
                )
            )
        }
    }

    fun onEvent(event: SongListEvent) {
        when (event) {
            SongListEvent.SearchOk -> mutableState.value = mutableState.value.copy(
                textInputRequest = TextInputRequest(state.value.searchQuery),
            )
            is SongListEvent.Inert -> Unit
            SongListEvent.GridFocusLost -> stopPreview()
            SongListEvent.OverlayOpened -> stopPreview()
            SongListEvent.ScreenExit -> stopPreview(previewReleased = true)
            SongListEvent.PreviewFailure -> mutableState.value = mutableState.value.copy(previewError = null)
            is SongListEvent.FocusAreaChanged -> mutableState.value = mutableState.value.copy(
                focusArea = event.focusArea,
            )
            is SongListEvent.SongLongPressed -> handleSongLongPressed(event.songId)
            SongListEvent.Join -> handleJoin()
            SongListEvent.DismissJoinOverlay -> mutableState.value = mutableState.value.copy(joinOverlay = null)
        }
    }

    private fun handleSongLongPressed(songId: String) {
        val song = allSongs.firstOrNull { it.songId == songId } ?: return
        if (!song.canMedley) {
            mutableState.value = mutableState.value.copy(blockingModal = BlockingModalState(MEDLEY_DISABLED_MESSAGE))
        }
    }

    private fun handleJoin() {
        onEvent(SongListEvent.OverlayOpened)
        mutableState.value = mutableState.value.copy(
            joinOverlay = JoinOverlayState(qrPayload = joinEndpointUrl, joinCode = joinCode),
        )
    }

    private fun handleConsumedBack() {
        if (state.value.focusArea == SongListFocusArea.Search) {
            updateSearch("")
            advanceSearchDebounce(150L)
        } else {
            mutableState.value = mutableState.value.copy(focusArea = SongListFocusArea.Search)
        }
    }

    private fun stopPreview(
        previewReleased: Boolean = state.value.previewReleased,
        clearPendingFocusedSong: Boolean = true,
    ) {
        if (clearPendingFocusedSong) pendingFocusedSongId = null
        mutableState.value = mutableState.value.copy(
            focusedSong = if (clearPendingFocusedSong) null else state.value.focusedSong,
            preview = null,
            previewReleased = previewReleased,
        )
    }
}

sealed interface SongListAction {
    data object SearchOk : SongListAction
    data class Inert(val action: SongListInertAction) : SongListAction
}

enum class SongListInertAction {
    Settings,
    RandomDuet,
    RandomMedley,
}

data class SongListState(
    val visibleSongs: List<IndexedSong>,
    val searchQuery: String = "",
    val textInputRequest: TextInputRequest? = null,
    val emptyState: SongListEmptyState? = null,
    val randomDuetEnabled: Boolean,
    val randomMedleyEnabled: Boolean,
    val openedRoute: String? = null,
    val focusedSong: IndexedSong? = null,
    val preview: PreviewPlaybackState? = null,
    val previewError: String? = null,
    val previewReleased: Boolean = false,
    val joinOverlay: JoinOverlayState? = null,
    val blockingModal: BlockingModalState? = null,
    val focusArea: SongListFocusArea = SongListFocusArea.Search,
)

data class SongListEmptyState(
    val title: String,
    val body: String,
)

data class TextInputRequest(
    val initialValue: String,
)

data class BlockingModalState(
    val body: String,
)

enum class SongListFocusArea {
    Search,
    Grid,
    LeftPanel,
}

sealed interface SongListEvent {
    data object SearchOk : SongListEvent
    data class Inert(val action: SongListInertAction) : SongListEvent
    data object GridFocusLost : SongListEvent
    data object OverlayOpened : SongListEvent
    data object ScreenExit : SongListEvent
    data object PreviewFailure : SongListEvent
    data object Join : SongListEvent
    data object DismissJoinOverlay : SongListEvent
    data class FocusAreaChanged(val focusArea: SongListFocusArea) : SongListEvent
    data class SongLongPressed(val songId: String) : SongListEvent
}

data class JoinOverlayState(
    val qrPayload: String,
    val joinCode: String,
)

private fun songsMatching(songs: List<IndexedSong>, query: String): List<IndexedSong> = if (query.isBlank()) {
    songs
} else {
    songs.filter { song -> song.matches(query) }
}

private fun IndexedSong.matches(query: String): Boolean = artist.contains(query, ignoreCase = true) ||
    album.orEmpty().contains(query, ignoreCase = true) ||
    title.contains(query, ignoreCase = true)

private fun songListEmptyStateFor(songs: List<IndexedSong>, connectedPhoneCount: Int): SongListEmptyState? = when {
    songs.isNotEmpty() -> null
    connectedPhoneCount == 0 -> SongListEmptyState(
        title = "No phones connected.",
        body = "Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code.",
    )
    else -> SongListEmptyState(
        title = "No songs found.",
        body = "Open the karaoke app on your phone and make sure the songs folder is set.",
    )
}
