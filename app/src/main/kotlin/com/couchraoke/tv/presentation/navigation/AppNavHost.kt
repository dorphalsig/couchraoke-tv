@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.navigation

import android.view.SurfaceHolder
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.data.library.ManifestLibraryManager
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.SessionState
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator
import com.couchraoke.tv.presentation.playback.DefaultPlaybackController
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersModal
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersRecoveryRequest
import com.couchraoke.tv.presentation.selectplayers.SelectPlayersViewModel
import com.couchraoke.tv.presentation.singing.SingingScreen
import com.couchraoke.tv.presentation.singing.SingingViewModel
import com.couchraoke.tv.presentation.songlist.SongListEvent
import com.couchraoke.tv.presentation.songlist.SongListScreen
import com.couchraoke.tv.presentation.songlist.SongListViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavHost(
    libraryManager: ManifestLibraryManager,
    networkController: NetworkController,
    sessionState: StateFlow<SessionState>,
    joinEndpointUrl: String,
    playbackCoordinator: DefaultPlaybackCoordinator,
    route: AppRoute = AppRoute.SongList,
    onExitApp: () -> Unit = {},
) {
    val routeInputs = rememberRouteInputs(libraryManager, networkController, sessionState, joinEndpointUrl)
    val navState = rememberNavState(route)
    val singingViewModel = remember(playbackCoordinator) { SingingViewModel(playbackCoordinator) }
    val singingState by singingViewModel.state.collectAsState()
    val playbackState by playbackCoordinator.state.collectAsState()
    val playbackController = rememberPlaybackController(LocalContext.current, playbackCoordinator, singingViewModel)
    var songListViewModel by remember { mutableStateOf<SongListViewModel?>(null) }

    PlaybackControllerEffects(navState.currentRoute, playbackCoordinator, networkController, playbackController)
    SingingCoordinatorSyncEffect(playbackState = playbackState, singingViewModel = singingViewModel)
    StartPendingSongEffect(
        pendingStartSong = navState.pendingStartSong,
        connectedPhones = routeInputs.connectedPhones,
        playbackCoordinator = playbackCoordinator,
        singingViewModel = singingViewModel,
        onStarted = { navState.currentRoute = AppRoute.Singing },
        onFinished = { navState.pendingStartSong = null },
    )
    PlaybackRecoveryRouting(
        playbackState = playbackState,
        onDisconnectedRecovery = { song ->
            navState.currentRoute = AppRoute.SongList
            navState.selectedSong = song
        },
        onErrorRecovery = {
            navState.currentRoute = AppRoute.SongList
            navState.selectedSong = null
        },
    )

    val routeActions = navState.routeActions(
        singingViewModel = singingViewModel,
        onViewModelReady = { songListViewModel = it },
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
        singingViewModel = singingViewModel,
        actions = routeActions,
        onVideoSurfaceAvailable = playbackController::setVideoSurface,
    )
    // Iteration 3 wires Settings; Iteration 1 intentionally has no Settings route/menu/screen/submenu.
}

@Composable
private fun rememberRouteInputs(
    libraryManager: ManifestLibraryManager,
    networkController: NetworkController,
    sessionStateFlow: StateFlow<SessionState>,
    joinEndpointUrl: String,
): RouteInputs {
    val songs by libraryManager.songs.collectAsState()
    val connectedPhones by networkController.connectedPhones.collectAsState()
    val sessionState by sessionStateFlow.collectAsState()
    return RouteInputs(
        songs = songs,
        connectedPhones = connectedPhones,
        sessionState = sessionState,
        joinEndpointUrl = joinEndpointUrl,
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

    fun routeActions(
        singingViewModel: SingingViewModel,
        onViewModelReady: (SongListViewModel) -> Unit,
    ): RouteActions = RouteActions(
        onViewModelReady = onViewModelReady,
        onSongSelected = { selectedSong = it },
        onSongCancelled = { selectedSong = null },
        onSongStarted = {
            pendingStartSong = it
            selectedSong = null
        },
        onReturnToSongList = {
            currentRoute = AppRoute.SongList
            singingViewModel.syncFromCoordinator()
        },
        onResultsShown = { currentRoute = AppRoute.SongList },
    )
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
    singingViewModel: SingingViewModel,
    actions: RouteActions,
    onVideoSurfaceAvailable: (SurfaceHolder?) -> Unit,
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
            singingViewModel = singingViewModel,
            onReturnToSongList = actions.onReturnToSongList,
            onVideoSurfaceAvailable = onVideoSurfaceAvailable,
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
    networkController: NetworkController,
    playbackController: DefaultPlaybackController,
) {
    LaunchedEffect(networkController, playbackCoordinator) {
        networkController.phoneEvents.collect(playbackCoordinator::onPhoneEvent)
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
            onOpenJoinQr = {
                selectPlayersViewModel.openJoinQr()
                when (selectPlayersViewModel.consumeRecoveryRequest()) {
                    SelectPlayersRecoveryRequest.OpenJoinQrOverlay -> songListViewModel.onEvent(SongListEvent.Join)
                    null -> Unit
                }
            },
        )
    }
}

@Composable
private fun SingingRoute(
    state: com.couchraoke.tv.presentation.singing.SingingUiState,
    singingViewModel: SingingViewModel,
    onReturnToSongList: () -> Unit,
    onVideoSurfaceAvailable: (SurfaceHolder?) -> Unit,
) {
    SingingScreen(
        state = state,
        overlayActions = com.couchraoke.tv.presentation.singing.SingingOverlayActions(
            onResume = singingViewModel::onResume,
            onRestartRequested = singingViewModel::onRestartRequested,
            onRestartConfirmed = singingViewModel::onRestartConfirmed,
            onQuitRequested = singingViewModel::onQuitRequested,
            onQuitConfirmed = singingViewModel::onQuitConfirmed,
            onCancel = singingViewModel::onBack,
        ),
        onVideoSurfaceAvailable = onVideoSurfaceAvailable,
    )
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
