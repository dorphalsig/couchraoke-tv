package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import com.couchraoke.tv.domain.library.SongEntry

@Suppress("LongParameterList", "FunctionNaming") // Composable: UI structure requires many parameters
@Composable
fun SongGrid(
    songs: List<SongEntry>,
    onSongClick: (SongEntry) -> Unit,
    onSongLongClick: (SongEntry) -> Unit,
    onSongFocused: (SongEntry) -> Unit,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    // TODO(SPEC: verify 4K threshold on real hardware)
    val columns = if (configuration.screenWidthDp >= SCREEN_WIDTH_4K_DP) COLUMNS_4K else COLUMNS_HD

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.songId },
        ) { index, song ->
            val focusRequester = if (index == 0) firstItemFocusRequester else remember { FocusRequester() }
            SongTile(
                song = song,
                onClick = { onSongClick(song) },
                onLongClick = { onSongLongClick(song) },
                onFocused = { onSongFocused(song) },
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
    }
}

private const val SCREEN_WIDTH_4K_DP = 3840
private const val COLUMNS_4K = 4
private const val COLUMNS_HD = 3
