package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.DemoSoloSingSeed
import com.couchraoke.tv.R
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.presentation.join.JoinOverlay
import com.couchraoke.tv.ui.theme.CouchraokeTheme

private const val GRID_COLUMNS_1080 = 3
private const val GRID_COLUMNS_4K = 4
private val FOUR_K_WIDTH = 3000.dp

@NoCoverageGenerated
@Composable
fun SongListScreen(
    viewModel: SongListViewModel,
    modifier: Modifier = Modifier,
    onSongSelected: (IndexedSong) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    PreviewDebounceTicker(viewModel)
    DisposableEffect(viewModel) {
        onDispose { viewModel.onEvent(SongListEvent.ScreenExit) }
    }
    SongListScreen(
        state = state,
        modifier = modifier,
        onJoin = { viewModel.onEvent(SongListEvent.Join) },
        onDismissJoin = { viewModel.onEvent(SongListEvent.DismissJoinOverlay) },
        onSearchOk = { viewModel.onEvent(SongListEvent.SearchOk) },
        onSettings = { viewModel.onEvent(SongListEvent.Inert(SongListInertAction.Settings)) },
        onRandomSong = { state.visibleSongs.firstOrNull()?.let(onSongSelected) },
        onRandomDuet = { viewModel.onEvent(SongListEvent.Inert(SongListInertAction.RandomDuet)) },
        onRandomMedley = { viewModel.onEvent(SongListEvent.Inert(SongListInertAction.RandomMedley)) },
        onSongFocused = viewModel::onSongFocused,
        onSongSelected = onSongSelected,
    )
}

@Composable
private fun PreviewDebounceTicker(viewModel: SongListViewModel) {
    LaunchedEffect(viewModel) {
        while (true) {
            kotlinx.coroutines.delay(50L)
            viewModel.tick(50L)
        }
    }
}

@NoCoverageGenerated
@Composable
fun SongListScreen(
    state: SongListState,
    modifier: Modifier = Modifier,
    onJoin: () -> Unit = {},
    onDismissJoin: () -> Unit = {},
    onSearchOk: () -> Unit = {},
    onSettings: () -> Unit = {},
    onRandomSong: () -> Unit = {},
    onRandomDuet: () -> Unit = {},
    onRandomMedley: () -> Unit = {},
    onSongFocused: (String) -> Unit = {},
    onSongSelected: (IndexedSong) -> Unit = {},
) {
    val focusTargets = rememberFocusTargets()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gridColumns = if (maxWidth >= FOUR_K_WIDTH) GRID_COLUMNS_4K else GRID_COLUMNS_1080
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.songlist),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("songlist-background"),
                contentScale = ContentScale.Crop,
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)))
        }
        InitialSongListFocus(state.visibleSongs.isEmpty(), focusTargets)
        SongListContent(
            actions = SongListUiActions(
                onSearchOk = onSearchOk,
                onJoin = onJoin,
                onSettings = onSettings,
                onRandomSong = onRandomSong,
                onRandomDuet = onRandomDuet,
                onRandomMedley = onRandomMedley,
                onSongFocused = onSongFocused,
                onSongSelected = onSongSelected,
            ),
            state = state,
            focusTargets = focusTargets,
            gridColumns = gridColumns,
        )
        state.joinOverlay?.let { join ->
            JoinOverlay(
                qrPayload = join.qrPayload,
                joinCode = join.joinCode,
                onDismiss = onDismissJoin,
            )
        }
    }
}

@NoCoverageGenerated
@Composable
private fun InitialSongListFocus(isEmpty: Boolean, focusTargets: SongListFocusTargets) {
    LaunchedEffect(isEmpty) {
        if (isEmpty) {
            focusTargets.search.requestFocus()
        } else {
            focusTargets.firstCard.requestFocus()
        }
    }
}

@Composable
private fun rememberFocusTargets(): SongListFocusTargets = remember {
    SongListFocusTargets(
        search = FocusRequester(),
        firstCard = FocusRequester(),
        playMedley = FocusRequester(),
        randomMedley = FocusRequester(),
    )
}

@NoCoverageGenerated
@Preview(name = "Song List", widthDp = 1920, heightDp = 1080)
@Composable
fun SongListScreenPreview() {
    CouchraokeTheme {
        SongListScreen(
            state = previewSongListState(),
        )
    }
}

internal fun previewSongListState(): SongListState {
    val songs = listOf(
        previewSong(
            songId = "song-1",
            title = "Demo Song",
            artist = "Demo Artist",
            coverUrl = null,
        ),
        previewSong(
            songId = "song-2",
            title = "Duet Demo",
            artist = "Second Artist",
            coverUrl = "cover",
            isDuet = true,
        ),
        previewSong(
            songId = "song-3",
            title = "Rap Medley",
            artist = "Third Artist",
            hasRap = true,
            canMedley = true,
        ),
    )
    return SongListState(
        visibleSongs = songs,
        focusedSong = songs.first(),
        randomDuetEnabled = false,
        randomMedleyEnabled = false,
    )
}

private fun previewSong(
    songId: String,
    title: String,
    artist: String,
    coverUrl: String? = null,
    isDuet: Boolean = false,
    hasRap: Boolean = false,
    canMedley: Boolean = false,
): IndexedSong = IndexedSong(
    songId = songId,
    phoneClientId = DemoSoloSingSeed.PhoneClientId,
    relativeTxtPath = DemoSoloSingSeed.RelativeTxtPath,
    modifiedTimeMs = DemoSoloSingSeed.ModifiedTimeMs,
    title = title,
    artist = artist,
    album = DemoSoloSingSeed.SongAlbum,
    year = DemoSoloSingSeed.SongYear,
    genre = DemoSoloSingSeed.SongGenre,
    txtUrl = DemoSoloSingSeed.TxtUrl,
    audioUrl = DemoSoloSingSeed.AudioUrl,
    videoUrl = DemoSoloSingSeed.VideoUrl,
    coverUrl = coverUrl,
    backgroundUrl = null,
    isDuet = isDuet,
    hasRap = hasRap,
    hasVideo = true,
    canMedley = canMedley,
    medleySource = null,
    medleyStartBeat = null,
    medleyEndBeat = null,
    startSec = DemoSoloSingSeed.StartSec,
    previewStartSec = DemoSoloSingSeed.PreviewStartSec,
)

internal data class SongListFocusTargets(
    val search: FocusRequester,
    val firstCard: FocusRequester,
    val playMedley: FocusRequester,
    val randomMedley: FocusRequester,
)

internal data class SongListUiActions(
    val onSearchOk: () -> Unit,
    val onJoin: () -> Unit,
    val onSettings: () -> Unit,
    val onRandomSong: () -> Unit,
    val onRandomDuet: () -> Unit,
    val onRandomMedley: () -> Unit,
    val onSongFocused: (String) -> Unit,
    val onSongSelected: (IndexedSong) -> Unit,
)
