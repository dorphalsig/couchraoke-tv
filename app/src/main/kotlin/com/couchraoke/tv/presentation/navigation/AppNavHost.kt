@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.data.library.ManifestLibraryManager
import com.couchraoke.tv.data.network.KtorNetworkController
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersModal
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersViewModel
import com.couchraoke.tv.presentation.singing.SingingScreen
import com.couchraoke.tv.presentation.singing.SingingViewModel
import com.couchraoke.tv.presentation.songlist.SongListScreen
import com.couchraoke.tv.presentation.songlist.SongListViewModel

@Composable
fun AppNavHost(
    libraryManager: ManifestLibraryManager,
    networkController: KtorNetworkController,
    playbackCoordinator: DefaultPlaybackCoordinator,
    route: AppRoute = AppRoute.SongList,
) {
    val songs by libraryManager.songs.collectAsState()
    val connectedPhones by networkController.connectedPhones.collectAsState()
    val sessionState by networkController.sessionState.collectAsState()
    var selectedSong by remember { mutableStateOf<IndexedSong?>(null) }
    var pendingStartSong by remember { mutableStateOf<IndexedSong?>(null) }
    var currentRoute by remember { mutableStateOf(route) }
    val singingViewModel = remember(playbackCoordinator) { SingingViewModel(playbackCoordinator) }
    val singingState by singingViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        libraryManager.refreshFromConnectedPhones()
    }
    LaunchedEffect(pendingStartSong) {
        startPendingSong(
            pendingStartSong = pendingStartSong,
            connectedPhones = connectedPhones,
            playbackCoordinator = playbackCoordinator,
            singingViewModel = singingViewModel,
            onStarted = { currentRoute = AppRoute.Singing },
            onFinished = { pendingStartSong = null },
        )
    }

    when (currentRoute) {
        AppRoute.SongList -> SongListRoute(
            state = SongListRouteState(
                songs = songs,
                connectedPhones = connectedPhones,
                sessionState = sessionState,
                joinEndpointUrl = networkController.joinEndpointUrl,
                selectedSong = selectedSong,
            ),
            onSongSelected = { selectedSong = it },
            onSongCancelled = { selectedSong = null },
            onSongStarted = {
                pendingStartSong = it
                selectedSong = null
            },
        )
        AppRoute.Singing -> SingingRoute(
            state = singingState,
            onReturnToSongList = {
                currentRoute = AppRoute.SongList
                singingViewModel.syncFromCoordinator()
            },
        )
        AppRoute.Results -> Unit // Iteration 2 wires Results; Iteration 1 returns to Song List.
    }
    // Iteration 3 wires Settings; Iteration 1 intentionally has no Settings route/menu/screen/submenu.
}

@Composable
private fun SongListRoute(
    state: SongListRouteState,
    onSongSelected: (IndexedSong) -> Unit,
    onSongCancelled: () -> Unit,
    onSongStarted: (IndexedSong) -> Unit,
) {
    val songListViewModel = remember(state) {
        SongListViewModel(
            songs = state.songs,
            connectedPhoneCount = state.connectedPhones.size,
            joinEndpointUrl = state.joinEndpointUrl,
            joinCode = state.sessionState.joinCode,
        )
    }
    SongListScreen(viewModel = songListViewModel, onSongSelected = onSongSelected)
    state.selectedSong?.let { song ->
        val selectPlayersViewModel = remember(song, state.connectedPhones) {
            SelectPlayersViewModel(song = song, connectedPhones = state.connectedPhones)
        }
        val selectState by selectPlayersViewModel.state.collectAsState()
        SelectPlayersModal(
            state = selectState,
            onStart = { onSongStarted(song) },
            onCancel = onSongCancelled,
        )
    }
}

@Composable
private fun SingingRoute(
    state: com.couchraoke.tv.presentation.singing.SingingUiState,
    onReturnToSongList: () -> Unit,
) {
    SingingScreen(state = state)
    if (state.returnToSongList) {
        onReturnToSongList()
    }
}

private data class SongListRouteState(
    val songs: List<IndexedSong>,
    val connectedPhones: List<com.couchraoke.tv.data.network.ConnectedPhone>,
    val sessionState: com.couchraoke.tv.data.network.SessionState,
    val joinEndpointUrl: String,
    val selectedSong: IndexedSong?,
)

private suspend fun startPendingSong(
    pendingStartSong: IndexedSong?,
    connectedPhones: List<com.couchraoke.tv.data.network.ConnectedPhone>,
    playbackCoordinator: DefaultPlaybackCoordinator,
    singingViewModel: SingingViewModel,
    onStarted: () -> Unit,
    onFinished: () -> Unit,
) {
    val song = pendingStartSong ?: return
    val selection = SelectPlayersViewModel(song = song, connectedPhones = connectedPhones).startSelection()
    if (selection != null) {
        playbackCoordinator.startSong(selection)
        singingViewModel.syncFromCoordinator()
        onStarted()
    }
    onFinished()
}
