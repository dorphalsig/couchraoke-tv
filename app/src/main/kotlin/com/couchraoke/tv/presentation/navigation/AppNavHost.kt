@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.navigation

import androidx.activity.compose.BackHandler
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
import com.couchraoke.tv.presentation.playback.DefaultPlaybackController
import com.couchraoke.tv.presentation.playback.VlcLibVlcPlayerHandle
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
    onExitApp: () -> Unit = {},
) {
    val routeInputs = rememberRouteInputs(libraryManager, networkController)
    val navState = rememberNavState(route)
    val singingViewModel = remember(playbackCoordinator) { SingingViewModel(playbackCoordinator) }
    val singingState by singingViewModel.state.collectAsState()
    var songListViewModel by remember { mutableStateOf<SongListViewModel?>(null) }

    LaunchedEffect(Unit) {
        libraryManager.refreshFromConnectedPhones()
    }
    PlaybackControllerEffects(
        currentRoute = navState.currentRoute,
        playbackCoordinator = playbackCoordinator,
    )
    StartPendingSongEffect(
        pendingStartSong = navState.pendingStartSong,
        connectedPhones = routeInputs.connectedPhones,
        playbackCoordinator = playbackCoordinator,
        singingViewModel = singingViewModel,
        onStarted = { navState.currentRoute = AppRoute.Singing },
        onFinished = { navState.pendingStartSong = null },
    )

    val routeActions = RouteActions(
        onViewModelReady = { songListViewModel = it },
        onSongSelected = { navState.selectedSong = it },
        onSongCancelled = { navState.selectedSong = null },
        onSongStarted = {
            navState.pendingStartSong = it
            navState.selectedSong = null
        },
        onReturnToSongList = {
            navState.currentRoute = AppRoute.SongList
            singingViewModel.syncFromCoordinator()
        },
        onResultsShown = { navState.currentRoute = AppRoute.SongList },
    )
    RouteBackHandler(
        currentRoute = navState.currentRoute,
        songListViewModel = songListViewModel,
        singingViewModel = singingViewModel,
        onResultsBack = routeActions.onResultsShown,
        onExitApp = onExitApp,
    )

    CurrentRoute(
        currentRoute = navState.currentRoute,
        songListState = SongListRouteState(
            songs = routeInputs.songs,
            connectedPhones = routeInputs.connectedPhones,
            sessionState = routeInputs.sessionState,
            joinEndpointUrl = routeInputs.joinEndpointUrl,
            selectedSong = navState.selectedSong,
        ),
        singingState = singingState,
        actions = routeActions,
    )
    // Iteration 3 wires Settings; Iteration 1 intentionally has no Settings route/menu/screen/submenu.
}

@Composable
private fun rememberRouteInputs(
    libraryManager: ManifestLibraryManager,
    networkController: KtorNetworkController,
): RouteInputs {
    val songs by libraryManager.songs.collectAsState()
    val connectedPhones by networkController.connectedPhones.collectAsState()
    val sessionState by networkController.sessionState.collectAsState()
    return RouteInputs(
        songs = songs,
        connectedPhones = connectedPhones,
        sessionState = sessionState,
        joinEndpointUrl = networkController.joinEndpointUrl,
    )
}

private data class RouteInputs(
    val songs: List<IndexedSong>,
    val connectedPhones: List<com.couchraoke.tv.data.network.ConnectedPhone>,
    val sessionState: com.couchraoke.tv.data.network.SessionState,
    val joinEndpointUrl: String,
)

@Composable
private fun rememberNavState(route: AppRoute): NavState {
    var selectedSong by remember { mutableStateOf<IndexedSong?>(null) }
    var pendingStartSong by remember { mutableStateOf<IndexedSong?>(null) }
    var currentRoute by remember { mutableStateOf(route) }
    return NavState(
        selectedSong = selectedSong,
        pendingStartSong = pendingStartSong,
        currentRoute = currentRoute,
        setSelectedSong = { selectedSong = it },
        setPendingStartSong = { pendingStartSong = it },
        setCurrentRoute = { currentRoute = it },
    )
}

private class NavState(
    selectedSong: IndexedSong?,
    pendingStartSong: IndexedSong?,
    currentRoute: AppRoute,
    private val setSelectedSong: (IndexedSong?) -> Unit,
    private val setPendingStartSong: (IndexedSong?) -> Unit,
    private val setCurrentRoute: (AppRoute) -> Unit,
) {
    var selectedSong: IndexedSong?
        get() = selectedSongValue
        set(value) {
            selectedSongValue = value
            setSelectedSong(value)
        }
    var pendingStartSong: IndexedSong?
        get() = pendingStartSongValue
        set(value) {
            pendingStartSongValue = value
            setPendingStartSong(value)
        }
    var currentRoute: AppRoute
        get() = currentRouteValue
        set(value) {
            currentRouteValue = value
            setCurrentRoute(value)
        }

    private var selectedSongValue = selectedSong
    private var pendingStartSongValue = pendingStartSong
    private var currentRouteValue = currentRoute
}

private data class RouteActions(
    val onViewModelReady: (SongListViewModel) -> Unit,
    val onSongSelected: (IndexedSong) -> Unit,
    val onSongCancelled: () -> Unit,
    val onSongStarted: (IndexedSong) -> Unit,
    val onReturnToSongList: () -> Unit,
    val onResultsShown: () -> Unit,
)

@Composable
private fun RouteBackHandler(
    currentRoute: AppRoute,
    songListViewModel: SongListViewModel?,
    singingViewModel: SingingViewModel,
    onResultsBack: () -> Unit,
    onExitApp: () -> Unit,
) {
    BackHandler {
        when (currentRoute) {
            AppRoute.SongList -> if (songListViewModel?.onBack() == true) onExitApp()
            AppRoute.Singing -> singingViewModel.onBack()
            AppRoute.Results -> onResultsBack()
        }
    }
}

@Composable
private fun CurrentRoute(
    currentRoute: AppRoute,
    songListState: SongListRouteState,
    singingState: com.couchraoke.tv.presentation.singing.SingingUiState,
    actions: RouteActions,
) {
    when (currentRoute) {
        AppRoute.SongList -> SongListRoute(
            state = songListState,
            onViewModelReady = actions.onViewModelReady,
            onSongSelected = actions.onSongSelected,
            onSongCancelled = actions.onSongCancelled,
            onSongStarted = actions.onSongStarted,
        )
        AppRoute.Singing -> SingingRoute(
            state = singingState,
            onReturnToSongList = actions.onReturnToSongList,
        )
        AppRoute.Results -> actions.onResultsShown()
    }
}

@Composable
private fun StartPendingSongEffect(
    pendingStartSong: IndexedSong?,
    connectedPhones: List<com.couchraoke.tv.data.network.ConnectedPhone>,
    playbackCoordinator: DefaultPlaybackCoordinator,
    singingViewModel: SingingViewModel,
    onStarted: () -> Unit,
    onFinished: () -> Unit,
) {
    LaunchedEffect(pendingStartSong) {
        startPendingSong(
            pendingStartSong = pendingStartSong,
            connectedPhones = connectedPhones,
            playbackCoordinator = playbackCoordinator,
            singingViewModel = singingViewModel,
            onStarted = onStarted,
            onFinished = onFinished,
        )
    }
}

@Composable
private fun PlaybackControllerEffects(
    currentRoute: AppRoute,
    playbackCoordinator: DefaultPlaybackCoordinator,
) {
    val playbackController = remember {
        DefaultPlaybackController(
            audioHandle = VlcLibVlcPlayerHandle(),
            clockMs = { System.nanoTime() / 1_000_000 },
        )
    }
    val playbackIntents by playbackCoordinator.intents.collectAsState()
    var handledPlaybackIntentCount by remember { mutableStateOf(0) }
    var handledPlaybackEventCount by remember { mutableStateOf(0) }

    LaunchedEffect(playbackIntents) {
        playbackIntents.drop(handledPlaybackIntentCount).forEach(playbackController::handle)
        handledPlaybackIntentCount = playbackIntents.size
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute == AppRoute.Singing) {
            while (true) {
                kotlinx.coroutines.delay(50L)
                playbackController.tick()
                playbackController.events
                    .drop(handledPlaybackEventCount)
                    .forEach { playbackCoordinator.onPlaybackEvent(it) }
                handledPlaybackEventCount = playbackController.events.size
            }
        }
    }
}

@Composable
private fun SongListRoute(
    state: SongListRouteState,
    onViewModelReady: (SongListViewModel) -> Unit,
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
    LaunchedEffect(songListViewModel) {
        onViewModelReady(songListViewModel)
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
