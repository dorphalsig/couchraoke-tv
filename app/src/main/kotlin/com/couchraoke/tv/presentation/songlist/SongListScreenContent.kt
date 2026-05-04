package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.ui.theme.BodySecondary
import com.couchraoke.tv.ui.theme.BorderFocus
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.PreviewArtist
import com.couchraoke.tv.ui.theme.PreviewTitle
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextDisabled
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

private const val SONG_LIST_PLAYLIST_VISIBLE_ROWS = 5
private val APP_MARGIN_HORIZONTAL = 48.dp
private val APP_MARGIN_VERTICAL = 36.dp
private val HEADER_HEIGHT = 76.dp
private val RAIL_GRID_GAP = 32.dp
private val HEADER_TO_BODY_GAP = 24.dp
private val RANDOM_ROW_HEIGHT = 72.dp
private val RANDOM_ROW_GAP = 24.dp
private val GRID_GAP = 24.dp
private val PREVIEW_META_GAP = 16.dp
private val META_PLAYLIST_GAP = 24.dp
private val PLAYLIST_ROW_HEIGHT = 52.dp
private val PLAY_MEDLEY_TOP_GAP = 16.dp
private val FOCUS_BORDER_WIDTH = 3.dp
private val BORDER_THIN = 1.dp
private val CONTROL_SHAPE = RoundedCornerShape(14.dp)
private val SURFACE_MUTED = SurfaceLevel1
private val SURFACE_SEARCH = SurfaceLevel2
private val BUTTON_SURFACE = SurfaceLevel1
private val BUTTON_CONTENT = TextPrimary
private val DISABLED_TEXT = TextDisabled

@NoCoverageGenerated
@Composable
internal fun SongListContent(
    actions: SongListUiActions,
    state: SongListState,
    focusTargets: SongListFocusTargets,
    gridColumns: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = APP_MARGIN_HORIZONTAL, vertical = APP_MARGIN_VERTICAL),
    ) {
        HeaderRow(
            searchQuery = state.searchQuery,
            searchFocusRequester = focusTargets.search,
            onSearchOk = actions.onSearchOk,
            onJoin = actions.onJoin,
            onSettings = actions.onSettings,
        )
        Spacer(modifier = Modifier.height(HEADER_TO_BODY_GAP))
        Row(modifier = Modifier.fillMaxSize()) {
            LeftRail(focusedSong = state.focusedSong, playMedleyFocusRequester = focusTargets.playMedley)
            Spacer(modifier = Modifier.width(RAIL_GRID_GAP))
            RightBody(
                actions = actions,
                state = state,
                focusTargets = focusTargets,
                gridColumns = gridColumns,
            )
        }
    }
}

@Composable
private fun HeaderRow(
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchOk: () -> Unit,
    onJoin: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(searchQuery, searchFocusRequester, onSearchOk, Modifier.weight(1f))
        OutlinedButton(
            onClick = onJoin,
            modifier = Modifier.width(160.dp),
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) { Text("Join", style = ButtonLabel) }
        // Iteration 3 wires Settings; Iteration 1 intentionally opens no route/menu/screen/submenu.
        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier.width(160.dp),
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) { Text("Settings", style = ButtonLabel) }
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    focusRequester: FocusRequester,
    onSearchOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(64.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .clip(CONTROL_SHAPE)
            .background(if (focused) SURFACE_SEARCH.copy(alpha = 0.92f) else SURFACE_SEARCH)
            .border(
                BorderStroke(
                    width = if (focused) FOCUS_BORDER_WIDTH else BORDER_THIN,
                    color = if (focused) BorderFocus else BorderSubtle.copy(alpha = 0.2f),
                ),
                CONTROL_SHAPE,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSearchOk)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = if (searchQuery.isBlank()) "Search songs" else searchQuery,
            style = PanelTitle,
            color = if (searchQuery.isBlank()) TextSecondary else TextPrimary,
        )
    }
}

@Composable
private fun outlinedControlColors() = ButtonDefaults.colors(
    containerColor = BUTTON_SURFACE,
    contentColor = BUTTON_CONTENT,
    disabledContainerColor = BUTTON_SURFACE.copy(alpha = 0.55f),
    disabledContentColor = DISABLED_TEXT,
    focusedContainerColor = BUTTON_SURFACE,
    focusedContentColor = BUTTON_CONTENT,
    pressedContainerColor = BUTTON_SURFACE,
    pressedContentColor = BUTTON_CONTENT,
)

@Composable
private fun controlBorder() = ButtonDefaults.border(
    border = Border(BorderStroke(BORDER_THIN, BorderSubtle.copy(alpha = 0.2f))),
    focusedBorder = Border(BorderStroke(FOCUS_BORDER_WIDTH, BorderFocus)),
    pressedBorder = Border(BorderStroke(BORDER_THIN, BorderSubtle.copy(alpha = 0.2f))),
)

@Composable
private fun LeftRail(focusedSong: IndexedSong?, playMedleyFocusRequester: FocusRequester) {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.34f)) {
        PreviewPane(focusedSong)
        Spacer(modifier = Modifier.height(META_PLAYLIST_GAP))
        Text(text = "Medley playlist", style = PanelTitle)
        Spacer(modifier = Modifier.height(12.dp))
        repeat(SONG_LIST_PLAYLIST_VISIBLE_ROWS) { index ->
            Surface(modifier = Modifier.fillMaxWidth().height(PLAYLIST_ROW_HEIGHT), shape = RoundedCornerShape(12.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = if (index == 0) "No songs added" else "", style = BodySecondary, color = DISABLED_TEXT)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(PLAY_MEDLEY_TOP_GAP))
        // Iteration 4 wires medley execution; Iteration 1 keeps this action visible-disabled.
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.focusRequester(playMedleyFocusRequester)) {
            Text("Play Medley")
        }
    }
}

@Composable
private fun PreviewPane(focusedSong: IndexedSong?) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(SURFACE_MUTED)
                .testTag("songlist-preview-pane"),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Preview", style = BodySecondary, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(PREVIEW_META_GAP))
        Text(
            text = focusedSong?.title ?: "Focused song preview",
            style = PreviewTitle,
        )
        Text(
            text = focusedSong?.artist ?: "Artist",
            style = PreviewArtist,
            color = TextSecondary,
        )
    }
}

@Composable
private fun RightBody(
    actions: SongListUiActions,
    state: SongListState,
    focusTargets: SongListFocusTargets,
    gridColumns: Int,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RandomActionsRow(
            hasSongs = state.visibleSongs.isNotEmpty(),
            randomMedleyFocusRequester = focusTargets.randomMedley,
            onRandomSong = actions.onRandomSong,
            onRandomDuet = actions.onRandomDuet,
            onRandomMedley = actions.onRandomMedley,
        )
        Spacer(modifier = Modifier.height(RANDOM_ROW_GAP))
        state.emptyState?.let { EmptyStateCard(it) } ?: SongGrid(
            songs = state.visibleSongs,
            gridColumns = gridColumns,
            focusTargets = focusTargets,
            onSongFocused = actions.onSongFocused,
            onSongSelected = actions.onSongSelected,
        )
    }
}

@Composable
private fun RandomActionsRow(
    hasSongs: Boolean,
    randomMedleyFocusRequester: FocusRequester,
    onRandomSong: () -> Unit,
    onRandomDuet: () -> Unit,
    onRandomMedley: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(RANDOM_ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Button(
            onClick = onRandomSong,
            modifier = Modifier.weight(1f),
            enabled = hasSongs,
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) {
            Text("Random Song", style = ButtonLabel)
        }
        // Iteration 3 wires duet randomization; Iteration 1 keeps this action visible-disabled.
        OutlinedButton(
            onClick = onRandomDuet,
            modifier = Modifier.weight(1f),
            enabled = false,
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) {
            Text("Random Duet", style = ButtonLabel)
        }
        // Iteration 4 wires medley randomization; Iteration 1 keeps this action visible-disabled.
        OutlinedButton(
            onClick = onRandomMedley,
            modifier = Modifier.weight(1f).focusRequester(randomMedleyFocusRequester),
            enabled = false,
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) {
            Text("Random Medley", style = ButtonLabel)
        }
    }
}

@Composable
private fun SongGrid(
    songs: List<IndexedSong>,
    gridColumns: Int,
    focusTargets: SongListFocusTargets,
    onSongFocused: (String) -> Unit,
    onSongSelected: (IndexedSong) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {
        itemsIndexed(songs, key = { _, song -> song.songId }) { index, song ->
            SongCard(
                song = song,
                focusTargets = focusTargets,
                focusRequester = if (index == 0) focusTargets.firstCard else null,
                useLeftPanelTarget = index % gridColumns == 0,
                onFocused = { onSongFocused(song.songId) },
                onSelected = { onSongSelected(song) },
            )
        }
    }
}
