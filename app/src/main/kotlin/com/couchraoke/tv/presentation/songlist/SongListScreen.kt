package com.couchraoke.tv.presentation.songlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.presentation.songlist.components.ActionButtons
import com.couchraoke.tv.presentation.songlist.components.JoinWidget
import com.couchraoke.tv.presentation.songlist.components.MedleyPlaylist
import com.couchraoke.tv.presentation.songlist.components.SearchField
import com.couchraoke.tv.presentation.songlist.components.SelectPlayersModal
import com.couchraoke.tv.presentation.songlist.components.SongGrid

@Suppress("LongMethod", "CyclomaticComplexMethod", "FunctionNaming") // Composable: UI structure is inherently verbose
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
    val firstTileFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler {
        viewModel.onBackPressed()
    }

    LaunchedEffect(state.allSongs.isNotEmpty()) {
        if (state.allSongs.isNotEmpty()) {
            firstTileFocusRequester.requestFocus()
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel (320.dp)
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JoinWidget(
                    joinToken = state.joinToken,
                    wsUrl = "", // Placeholder as per instructions
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Medley Playlist
                Box(modifier = Modifier.weight(1f)) {
                    MedleyPlaylist(
                        songs = state.medleyPlaylist,
                        reorderingIndex = state.isReorderingMedleyIndex,
                        onRowClick = { viewModel.onPlaylistRowSelected(it) },
                        onRowLongClick = { viewModel.onPlaylistRowLongPressed(it) },
                        onReorderUp = { if (it > 0) viewModel.onReorderConfirm(it, it - 1) },
                        onReorderDown = {
                            if (it < state.medleyPlaylist.size - 1) viewModel.onReorderConfirm(it, it + 1)
                        },
                        onReorderCancel = { viewModel.onReorderCancel() },
                        onPlayMedley = { viewModel.onPlayMedley() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ActionButtons(
                    hasFilteredSongs = state.filteredSongs.any { it.isValid },
                    hasFilteredDuets = state.filteredSongs.any { it.isDuet },
                    onRandomSong = { viewModel.onRandomSong() },
                    onRandomDuet = { viewModel.onRandomDuet() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Right panel
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
                    .weight(1f)
            ) {
                SearchField(
                    query = state.searchQuery,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        onSongClick = { viewModel.onSongSelected(it) },
                        onSongLongClick = { viewModel.onSongLongPressed(it) },
                        onSongFocused = { viewModel.onSongFocused(it.songId) },
                        firstItemFocusRequester = firstTileFocusRequester,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Overlay modals
        state.selectPlayersDialog?.let { dialogState ->
            SelectPlayersModal(
                state = dialogState,
                onPlayer1Selected = { /* No-op */ },
                onPlayer1DifficultySelected = { /* No-op */ },
                onPlayer2Selected = { /* No-op */ },
                onPlayer2DifficultySelected = { /* No-op */ },
                onSoloDuetPartSelected = { /* No-op */ },
                onSwapParts = { /* No-op */ },
                onStart = { viewModel.onSelectPlayersStart() },
                onCancel = { viewModel.onSelectPlayersCancel() }
            )
        }

        state.errorModal?.let { errorState ->
            Dialog(onDismissRequest = { viewModel.onErrorModalDismissed() }) {
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
                            onClick = { viewModel.onErrorModalDismissed() },
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
