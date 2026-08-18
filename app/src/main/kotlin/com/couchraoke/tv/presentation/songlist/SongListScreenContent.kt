package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.R
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.ui.theme.AppMarginHorizontal
import com.couchraoke.tv.ui.theme.AppMarginVertical
import com.couchraoke.tv.ui.theme.BodySecondary
import com.couchraoke.tv.ui.theme.BorderFocus
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.BorderThin
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.FocusBorderWidth
import com.couchraoke.tv.ui.theme.HeaderHeight
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.PreviewArtist
import com.couchraoke.tv.ui.theme.PreviewTitle
import com.couchraoke.tv.ui.theme.RadiusMedium
import com.couchraoke.tv.ui.theme.SongCardCompactHeight
import com.couchraoke.tv.ui.theme.SongCardCompactImageHeight
import com.couchraoke.tv.ui.theme.SongCardCompactImageSize
import com.couchraoke.tv.ui.theme.SongCardCompactImageToTitleGap
import com.couchraoke.tv.ui.theme.SongCardCompactPadding
import com.couchraoke.tv.ui.theme.SongCardCompactTitle
import com.couchraoke.tv.ui.theme.SongCardCompactTitleMaxLines
import com.couchraoke.tv.ui.theme.SongCardHeight
import com.couchraoke.tv.ui.theme.SongCardImageHeight
import com.couchraoke.tv.ui.theme.SongCardImageToTitleGap
import com.couchraoke.tv.ui.theme.SongCardPadding
import com.couchraoke.tv.ui.theme.SongCardTitle
import com.couchraoke.tv.ui.theme.SongCardTitleMaxLines
import com.couchraoke.tv.ui.theme.SongListCompactGridColumnGap
import com.couchraoke.tv.ui.theme.SongListCompactGridRowGap
import com.couchraoke.tv.ui.theme.SongListCompactHeaderButtonWidth
import com.couchraoke.tv.ui.theme.SongListCompactHeaderControlGap
import com.couchraoke.tv.ui.theme.SongListCompactHeaderHeight
import com.couchraoke.tv.ui.theme.SongListCompactHeaderToBodyGap
import com.couchraoke.tv.ui.theme.SongListCompactMarginHorizontal
import com.couchraoke.tv.ui.theme.SongListCompactMarginVertical
import com.couchraoke.tv.ui.theme.SongListCompactMetaToPlaylistGap
import com.couchraoke.tv.ui.theme.SongListCompactPlayMedleyTopGap
import com.couchraoke.tv.ui.theme.SongListCompactPlaylistRowHeight
import com.couchraoke.tv.ui.theme.SongListCompactPlaylistVisibleRows
import com.couchraoke.tv.ui.theme.SongListCompactPreviewAspect
import com.couchraoke.tv.ui.theme.SongListCompactPreviewToMetaGap
import com.couchraoke.tv.ui.theme.SongListCompactRailGridGap
import com.couchraoke.tv.ui.theme.SongListCompactRandomRowGap
import com.couchraoke.tv.ui.theme.SongListCompactRandomRowHeight
import com.couchraoke.tv.ui.theme.SongListCompactSearchHeight
import com.couchraoke.tv.ui.theme.SongListGridColumnGap
import com.couchraoke.tv.ui.theme.SongListGridRowGap
import com.couchraoke.tv.ui.theme.SongListHeaderButtonWidth
import com.couchraoke.tv.ui.theme.SongListHeaderControlGap
import com.couchraoke.tv.ui.theme.SongListHeaderToBodyGap
import com.couchraoke.tv.ui.theme.SongListLeftRailFraction
import com.couchraoke.tv.ui.theme.SongListMetaToPlaylistGap
import com.couchraoke.tv.ui.theme.SongListPlayMedleyTopGap
import com.couchraoke.tv.ui.theme.SongListPlaylistRowHeight
import com.couchraoke.tv.ui.theme.SongListPlaylistVisibleRows
import com.couchraoke.tv.ui.theme.SongListPreviewAspect
import com.couchraoke.tv.ui.theme.SongListPreviewToMetaGap
import com.couchraoke.tv.ui.theme.SongListRailGridGap
import com.couchraoke.tv.ui.theme.SongListRandomRowGap
import com.couchraoke.tv.ui.theme.SongListRandomRowHeight
import com.couchraoke.tv.ui.theme.SongListSearchHeight
import com.couchraoke.tv.ui.theme.Space16
import com.couchraoke.tv.ui.theme.Space8
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextDisabled
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

private val CONTROL_SHAPE = RoundedCornerShape(RadiusMedium)
private val SURFACE_MUTED = SurfaceLevel1
private val SURFACE_SEARCH = SurfaceLevel2
private val BUTTON_SURFACE = SurfaceLevel1
private val BUTTON_CONTENT = TextPrimary
private val DISABLED_TEXT = TextDisabled

internal data class SongListLayoutMetrics(
    val marginHorizontal: Dp,
    val marginVertical: Dp,
    val railGridGap: Dp,
    val headerControlGap: Dp,
    val headerButtonWidth: Dp,
    val searchHeight: Dp,
    val headerHeight: Dp,
    val headerToBodyGap: Dp,
    val randomRowHeight: Dp,
    val randomRowGap: Dp,
    val gridColumnGap: Dp,
    val gridRowGap: Dp,
    val cardHeight: Dp,
    val cardImageHeight: Dp,
    val cardImageWidth: Dp?,
    val cardPadding: Dp,
    val cardImageToTitleGap: Dp,
    val cardTitleStyle: TextStyle,
    val cardTitleMaxLines: Int,
    val previewAspect: Float,
    val previewToMetaGap: Dp,
    val metaToPlaylistGap: Dp,
    val playlistRowHeight: Dp,
    val playlistVisibleRows: Int,
    val playMedleyTopGap: Dp,
)

internal val StandardSongListLayoutMetrics = SongListLayoutMetrics(
    marginHorizontal = AppMarginHorizontal,
    marginVertical = AppMarginVertical,
    railGridGap = SongListRailGridGap,
    headerControlGap = SongListHeaderControlGap,
    headerButtonWidth = SongListHeaderButtonWidth,
    searchHeight = SongListSearchHeight,
    headerHeight = HeaderHeight,
    headerToBodyGap = SongListHeaderToBodyGap,
    randomRowHeight = SongListRandomRowHeight,
    randomRowGap = SongListRandomRowGap,
    gridColumnGap = SongListGridColumnGap,
    gridRowGap = SongListGridRowGap,
    cardHeight = SongCardHeight,
    cardImageHeight = SongCardImageHeight,
    cardImageWidth = null,
    cardPadding = SongCardPadding,
    cardImageToTitleGap = SongCardImageToTitleGap,
    cardTitleStyle = SongCardTitle,
    cardTitleMaxLines = SongCardTitleMaxLines,
    previewAspect = SongListPreviewAspect,
    previewToMetaGap = SongListPreviewToMetaGap,
    metaToPlaylistGap = SongListMetaToPlaylistGap,
    playlistRowHeight = SongListPlaylistRowHeight,
    playlistVisibleRows = SongListPlaylistVisibleRows,
    playMedleyTopGap = SongListPlayMedleyTopGap,
)

internal val CompactSongListLayoutMetrics = SongListLayoutMetrics(
    marginHorizontal = SongListCompactMarginHorizontal,
    marginVertical = SongListCompactMarginVertical,
    railGridGap = SongListCompactRailGridGap,
    headerControlGap = SongListCompactHeaderControlGap,
    headerButtonWidth = SongListCompactHeaderButtonWidth,
    searchHeight = SongListCompactSearchHeight,
    headerHeight = SongListCompactHeaderHeight,
    headerToBodyGap = SongListCompactHeaderToBodyGap,
    randomRowHeight = SongListCompactRandomRowHeight,
    randomRowGap = SongListCompactRandomRowGap,
    gridColumnGap = SongListCompactGridColumnGap,
    gridRowGap = SongListCompactGridRowGap,
    cardHeight = SongCardCompactHeight,
    cardImageHeight = SongCardCompactImageHeight,
    cardImageWidth = SongCardCompactImageSize,
    cardPadding = SongCardCompactPadding,
    cardImageToTitleGap = SongCardCompactImageToTitleGap,
    cardTitleStyle = SongCardCompactTitle,
    cardTitleMaxLines = SongCardCompactTitleMaxLines,
    previewAspect = SongListCompactPreviewAspect,
    previewToMetaGap = SongListCompactPreviewToMetaGap,
    metaToPlaylistGap = SongListCompactMetaToPlaylistGap,
    playlistRowHeight = SongListCompactPlaylistRowHeight,
    playlistVisibleRows = SongListCompactPlaylistVisibleRows,
    playMedleyTopGap = SongListCompactPlayMedleyTopGap,
)

@NoCoverageGenerated
@Composable
internal fun SongListContent(
    actions: SongListUiActions,
    state: SongListState,
    focusTargets: SongListFocusTargets,
    gridColumns: Int,
    layoutMetrics: SongListLayoutMetrics,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = layoutMetrics.marginHorizontal, vertical = layoutMetrics.marginVertical),
    ) {
        HeaderRow(
            searchQuery = state.searchQuery,
            searchFocusRequester = focusTargets.search,
            onSearchOk = actions.onSearchOk,
            onJoin = actions.onJoin,
            onSettings = actions.onSettings,
            layoutMetrics = layoutMetrics,
        )
        Spacer(modifier = Modifier.height(layoutMetrics.headerToBodyGap))
        Row(modifier = Modifier.fillMaxSize()) {
            LeftRail(
                focusedSong = state.focusedSong,
                playMedleyFocusRequester = focusTargets.playMedley,
                layoutMetrics = layoutMetrics,
            )
            Spacer(modifier = Modifier.width(layoutMetrics.railGridGap))
            RightBody(
                actions = actions,
                state = state,
                focusTargets = focusTargets,
                gridColumns = gridColumns,
                layoutMetrics = layoutMetrics,
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
    layoutMetrics: SongListLayoutMetrics,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(layoutMetrics.headerHeight),
        horizontalArrangement = Arrangement.spacedBy(layoutMetrics.headerControlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchField(searchQuery, searchFocusRequester, onSearchOk, layoutMetrics, Modifier.weight(1f))
        OutlinedButton(
            onClick = onJoin,
            modifier = Modifier.width(layoutMetrics.headerButtonWidth),
            colors = outlinedControlColors(),
            border = controlBorder(),
        ) { Text("Join", style = ButtonLabel) }
        // Iteration 3 wires Settings; Iteration 1 intentionally opens no route/menu/screen/submenu.
        var settingsFocused by remember { mutableStateOf(false) }
        val settingsInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .onFocusChanged { settingsFocused = it.isFocused }
                .focusable(interactionSource = settingsInteraction)
                .clip(CONTROL_SHAPE)
                .background(BUTTON_SURFACE)
                .border(
                    BorderStroke(
                        if (settingsFocused) FocusBorderWidth else BorderThin,
                        if (settingsFocused) BorderFocus else BorderSubtle.copy(alpha = 0.2f),
                    ),
                    CONTROL_SHAPE,
                )
                .clickable(interactionSource = settingsInteraction, indication = null, onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    focusRequester: FocusRequester,
    onSearchOk: () -> Unit,
    layoutMetrics: SongListLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(layoutMetrics.searchHeight)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .clip(CONTROL_SHAPE)
            .background(if (focused) SURFACE_SEARCH.copy(alpha = 0.92f) else SURFACE_SEARCH)
            .border(
                BorderStroke(
                    width = if (focused) FocusBorderWidth else BorderThin,
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
    border = Border(BorderStroke(BorderThin, BorderSubtle.copy(alpha = 0.2f))),
    focusedBorder = Border(BorderStroke(FocusBorderWidth, BorderFocus)),
    pressedBorder = Border(BorderStroke(BorderThin, BorderSubtle.copy(alpha = 0.2f))),
)

@Composable
private fun LeftRail(
    focusedSong: IndexedSong?,
    playMedleyFocusRequester: FocusRequester,
    layoutMetrics: SongListLayoutMetrics,
) {
    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(SongListLeftRailFraction)) {
        PreviewPane(focusedSong, layoutMetrics.previewAspect, layoutMetrics.previewToMetaGap)
        Spacer(modifier = Modifier.height(layoutMetrics.metaToPlaylistGap))
        Text(text = "Medley playlist", style = PanelTitle)
        Spacer(modifier = Modifier.height(Space8))
        repeat(layoutMetrics.playlistVisibleRows) { index ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutMetrics.playlistRowHeight),
                shape = RoundedCornerShape(RadiusMedium),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Space16),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = if (index == 0) "No songs added" else "", style = BodySecondary, color = DISABLED_TEXT)
                }
            }
            if (index != layoutMetrics.playlistVisibleRows - 1) {
                Spacer(modifier = Modifier.height(Space8))
            }
        }
        Spacer(modifier = Modifier.height(layoutMetrics.playMedleyTopGap))
        // Iteration 4 wires medley execution; Iteration 1 keeps this action visible-disabled.
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutMetrics.playlistRowHeight)
                .focusRequester(playMedleyFocusRequester),
        ) {
            Text("Play Medley")
        }
    }
}

@Composable
private fun PreviewPane(focusedSong: IndexedSong?, previewAspect: Float, previewToMetaGap: Dp) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(previewAspect)
                .clip(RoundedCornerShape(RadiusMedium))
                .background(SURFACE_MUTED)
                .testTag("songlist-preview-pane"),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Preview", style = BodySecondary, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(previewToMetaGap))
        Text(
            text = focusedSong?.title ?: "Focused song preview",
            style = PreviewTitle,
            color = TextPrimary,
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
    layoutMetrics: SongListLayoutMetrics,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RandomActionsRow(
            hasSongs = state.visibleSongs.isNotEmpty(),
            randomMedleyFocusRequester = focusTargets.randomMedley,
            onRandomSong = actions.onRandomSong,
            onRandomDuet = actions.onRandomDuet,
            onRandomMedley = actions.onRandomMedley,
            layoutMetrics = layoutMetrics,
        )
        Spacer(modifier = Modifier.height(layoutMetrics.randomRowGap))
        state.emptyState?.let { EmptyStateCard(it) } ?: SongGrid(
            songs = state.visibleSongs,
            gridColumns = gridColumns,
            focusTargets = focusTargets,
            onSongFocused = actions.onSongFocused,
            onSongSelected = actions.onSongSelected,
            layoutMetrics = layoutMetrics,
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
    layoutMetrics: SongListLayoutMetrics,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(layoutMetrics.randomRowHeight),
        horizontalArrangement = Arrangement.spacedBy(layoutMetrics.gridColumnGap),
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
    layoutMetrics: SongListLayoutMetrics,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        horizontalArrangement = Arrangement.spacedBy(layoutMetrics.gridColumnGap),
        verticalArrangement = Arrangement.spacedBy(layoutMetrics.gridRowGap),
    ) {
        itemsIndexed(songs, key = { _, song -> song.songId }) { index, song ->
            SongCard(
                song = song,
                focusTargets = focusTargets,
                focusRequester = if (index == 0) focusTargets.firstCard else null,
                useLeftPanelTarget = index % gridColumns == 0,
                onFocused = { onSongFocused(song.songId) },
                onSelected = { onSongSelected(song) },
                cardHeight = layoutMetrics.cardHeight,
                cardImageHeight = layoutMetrics.cardImageHeight,
                cardImageWidth = layoutMetrics.cardImageWidth,
                cardPadding = layoutMetrics.cardPadding,
                cardImageToTitleGap = layoutMetrics.cardImageToTitleGap,
                titleStyle = layoutMetrics.cardTitleStyle,
                titleMaxLines = layoutMetrics.cardTitleMaxLines,
            )
        }
    }
}
