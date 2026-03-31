package com.couchraoke.tv.presentation.songlist

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.presentation.songlist.components.ActionButtons
import com.couchraoke.tv.presentation.songlist.components.ContextualHintsBar
import com.couchraoke.tv.presentation.songlist.components.HeaderBar
import com.couchraoke.tv.presentation.songlist.components.MedleyPlaylist
import com.couchraoke.tv.presentation.songlist.components.PairingOverlay
import com.couchraoke.tv.presentation.songlist.components.PreviewPane
import com.couchraoke.tv.presentation.songlist.components.SelectPlayersModal
import com.couchraoke.tv.presentation.songlist.components.SongGrid

@Suppress("FunctionNaming")
@Composable
fun SongListScreen(
    viewModel: SongListViewModel = hiltViewModel()
) {
    DisposableEffect(Unit) {
        viewModel.onScreenVisible(true)
        onDispose {
            viewModel.onScreenVisible(false)
        }
    }

    val state by viewModel.uiState.collectAsState()

    SongListScreenContent(
        state = state,
        onBackPressed = { viewModel.onBackPressed() },
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onJoinPressed = { viewModel.onJoinPressed() },
        onPairingOverlayDismissed = { viewModel.onPairingOverlayDismissed() },
        onPlaylistRowSelected = { viewModel.onPlaylistRowSelected(it) },
        onPlaylistRowLongPressed = { viewModel.onPlaylistRowLongPressed(it) },
        onReorderConfirm = { from, to -> viewModel.onReorderConfirm(from, to) },
        onReorderCancel = { viewModel.onReorderCancel() },
        onPlayMedley = { viewModel.onPlayMedley() },
        onRandomSong = { viewModel.onRandomSong() },
        onRandomDuet = { viewModel.onRandomDuet() },
        onRandomMedley = { viewModel.onRandomMedley() },
        onSongSelected = { viewModel.onSongSelected(it) },
        onSongLongPressed = { viewModel.onSongLongPressed(it) },
        onSongFocused = { viewModel.onSongFocused(it) },
        onFocusZoneChanged = { viewModel.onFocusZoneChanged(it) },
        onSelectPlayersStart = { viewModel.onSelectPlayersStart() },
        onSelectPlayersCancel = { viewModel.onSelectPlayersCancel() },
        onErrorModalDismissed = { viewModel.onErrorModalDismissed() },
    )
}

// Backward-compat overload for tests using the old () -> Unit back callback
@Suppress("LongMethod", "FunctionNaming", "LongParameterList")
@Composable
internal fun SongListScreenContent(
    state: SongListUiState,
    onBackPressed: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPlaylistRowSelected: (Int) -> Unit,
    onPlaylistRowLongPressed: (Int) -> Unit,
    onReorderConfirm: (Int, Int) -> Unit,
    onReorderCancel: () -> Unit,
    onPlayMedley: () -> Unit,
    onRandomSong: () -> Unit,
    onRandomDuet: () -> Unit,
    onSongSelected: (SongEntry) -> Unit,
    onSongLongPressed: (SongEntry) -> Unit,
    onSongFocused: (String) -> Unit,
    onSelectPlayersStart: () -> Unit,
    onSelectPlayersCancel: () -> Unit,
    onErrorModalDismissed: () -> Unit,
) {
    SongListScreenContent(
        state = state,
        onBackPressed = {
            onBackPressed()
            BackResult.ClosedModal
        },
        onSearchQueryChanged = onSearchQueryChanged,
        onJoinPressed = {},
        onPairingOverlayDismissed = {},
        onPlaylistRowSelected = onPlaylistRowSelected,
        onPlaylistRowLongPressed = onPlaylistRowLongPressed,
        onReorderConfirm = onReorderConfirm,
        onReorderCancel = onReorderCancel,
        onPlayMedley = onPlayMedley,
        onRandomSong = onRandomSong,
        onRandomDuet = onRandomDuet,
        onRandomMedley = {},
        onSongSelected = onSongSelected,
        onSongLongPressed = onSongLongPressed,
        onSongFocused = onSongFocused,
        onFocusZoneChanged = {},
        onSelectPlayersStart = onSelectPlayersStart,
        onSelectPlayersCancel = onSelectPlayersCancel,
        onErrorModalDismissed = onErrorModalDismissed,
    )
}

// Composable: UI structure is inherently verbose; parameter count is a composable design constraint
@Suppress("LongMethod", "FunctionNaming", "LongParameterList")
@Composable
internal fun SongListScreenContent(
    state: SongListUiState,
    onBackPressed: () -> BackResult,
    onSearchQueryChanged: (String) -> Unit,
    onJoinPressed: () -> Unit,
    onPairingOverlayDismissed: () -> Unit,
    onPlaylistRowSelected: (Int) -> Unit,
    onPlaylistRowLongPressed: (Int) -> Unit,
    onReorderConfirm: (Int, Int) -> Unit,
    onReorderCancel: () -> Unit,
    onPlayMedley: () -> Unit,
    onRandomSong: () -> Unit,
    onRandomDuet: () -> Unit,
    onRandomMedley: () -> Unit,
    onSongSelected: (SongEntry) -> Unit,
    onSongLongPressed: (SongEntry) -> Unit,
    onSongFocused: (String) -> Unit,
    onFocusZoneChanged: (FocusZone) -> Unit,
    onSelectPlayersStart: () -> Unit,
    onSelectPlayersCancel: () -> Unit,
    onErrorModalDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val joinFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val firstTileFocusRequester = remember { FocusRequester() }

    BackHandler {
        when (onBackPressed()) {
            BackResult.ClosedModal -> Unit
            BackResult.MovedToSearch -> searchFocusRequester.requestFocus()
            BackResult.ClearedFilter -> Unit
            BackResult.ExitApp -> (context as? Activity)?.finish()
        }
    }

    LaunchedEffect(state.allSongs.isNotEmpty()) {
        if (state.allSongs.isNotEmpty()) {
            firstTileFocusRequester.requestFocus()
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderBar(
                joinToken = state.joinToken,
                onJoinPressed = onJoinPressed,
                searchQuery = state.searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                searchFocusRequester = searchFocusRequester,
                joinFocusRequester = joinFocusRequester,
                settingsFocusRequester = settingsFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.hasFocus || it.isFocused) {
                            onFocusZoneChanged(FocusZone.Header)
                        }
                    }
            )

            ActionButtons(
                hasFilteredSongs = state.filteredSongs.any { it.isValid },
                hasFilteredDuets = state.filteredSongs.any { it.isDuet },
                hasRandomMedleyEnabled = state.filteredSongs.count { it.canMedley } >= 2,
                onRandomSong = onRandomSong,
                onRandomDuet = onRandomDuet,
                onRandomMedley = onRandomMedley,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.hasFocus || it.isFocused) {
                            onFocusZoneChanged(FocusZone.Header)
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PreviewPane(
                        focusedSong = state.focusedSong,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MedleyPlaylist(
                        songs = state.medleyPlaylist,
                        reorderingIndex = state.isReorderingMedleyIndex,
                        onRowClick = {
                            onPlaylistRowSelected(it)
                            onFocusZoneChanged(FocusZone.LeftPanel)
                        },
                        onRowLongClick = {
                            onPlaylistRowLongPressed(it)
                            onFocusZoneChanged(FocusZone.LeftPanel)
                        },
                        onReorderUp = { if (it > 0) onReorderConfirm(it, it - 1) },
                        onReorderDown = {
                            if (it < state.medleyPlaylist.size - 1) {
                                onReorderConfirm(it, it + 1)
                            }
                        },
                        onReorderCancel = onReorderCancel,
                        onPlayMedley = {
                            onPlayMedley()
                            onFocusZoneChanged(FocusZone.LeftPanel)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.hasFocus || it.isFocused) {
                                    onFocusZoneChanged(FocusZone.LeftPanel)
                                }
                            },
                        showDuplicateFeedback = state.duplicateMedleyFeedback,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onFocusChanged {
                            if (it.hasFocus || it.isFocused) {
                                onFocusZoneChanged(FocusZone.Grid)
                            }
                        }
                ) {
                    if (state.allSongs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No songs yet. Connect a phone to get started.",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    } else if (state.filteredSongs.isEmpty() && state.searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No results for \"${state.searchQuery}\"",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    } else {
                        SongGrid(
                            songs = state.filteredSongs,
                            onSongClick = onSongSelected,
                            onSongLongClick = onSongLongPressed,
                            onSongFocused = {
                                onSongFocused(it.songId)
                                onFocusZoneChanged(FocusZone.Grid)
                            },
                            firstItemFocusRequester = firstTileFocusRequester,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            ContextualHintsBar(
                hint = state.currentHint,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.isPairingOverlayOpen) {
            PairingOverlay(
                joinToken = state.joinToken,
                joinUrl = "ws://localhost:8080/?token=${state.joinToken}",
                onDismiss = onPairingOverlayDismissed,
            )
        }

        state.selectPlayersDialog?.let { dialogState ->
            SelectPlayersModal(
                state = dialogState,
                onPlayer1Selected = { /* No-op */ },
                onPlayer1DifficultySelected = { /* No-op */ },
                onPlayer2Selected = { /* No-op */ },
                onPlayer2DifficultySelected = { /* No-op */ },
                onSoloDuetPartSelected = { /* No-op */ },
                onSwapParts = { /* No-op */ },
                onStart = onSelectPlayersStart,
                onCancel = onSelectPlayersCancel,
            )
        }

        state.errorModal?.let { errorState ->
            Dialog(onDismissRequest = onErrorModalDismissed) {
                Surface(
                    modifier = Modifier.widthIn(min = 320.dp, max = 480.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = errorState.title, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = errorState.bodyLine1, style = MaterialTheme.typography.bodyMedium)
                        errorState.bodyLine2?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onErrorModalDismissed,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
