package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.ui.theme.BorderFocus
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.SongCardArtistFocused
import com.couchraoke.tv.ui.theme.SongCardTitle
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.TagChipLabel
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

private val CARD_HEIGHT = 252.dp
private val CARD_PADDING = 12.dp
private val IMAGE_HEIGHT = 148.dp
private val ARTIST_SLOT_HEIGHT = 20.dp
private val TITLE_ARTIST_GAP = 4.dp
private val TAG_CORNER_INSET = 8.dp
private val TAG_GAP = 6.dp
private val FOCUS_BORDER_WIDTH = 3.dp
private val FOCUS_BORDER_INSET = 2.dp
private val BORDER_THIN = 1.dp
private val CARD_SHAPE = RoundedCornerShape(16.dp)
private val IMAGE_SHAPE = RoundedCornerShape(8.dp)
private val BORDER_SUBTLE = BorderSubtle.copy(alpha = 0.2f)
private val SURFACE_MUTED = SurfaceLevel1
private val FOCUS_PLATE = TextPrimary.copy(alpha = 0.08f)
private val CHIP_BACKGROUND = SurfaceLevel1.copy(alpha = 0.8f)

@NoCoverageGenerated
@Composable
internal fun SongCard(
    song: IndexedSong,
    focusTargets: SongListFocusTargets,
    focusRequester: FocusRequester?,
    useLeftPanelTarget: Boolean,
    onFocused: () -> Unit,
    onSelected: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val weakArtwork = song.coverUrl.isNullOrBlank()
    val cardFocusModifier = if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester)
    Surface(
        modifier = Modifier
            .then(cardFocusModifier)
            .height(CARD_HEIGHT)
            .testTag("song-card-${song.songId}")
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (focusState.isFocused) onFocused()
            }
            .focusProperties {
                if (useLeftPanelTarget) left = focusTargets.playMedley
            }
            .focusable(interactionSource = interactionSource)
            .border(cardBorder(focused), CARD_SHAPE)
            .padding(FOCUS_BORDER_INSET),
        shape = CARD_SHAPE,
        colors = SurfaceDefaults.colors(containerColor = SURFACE_MUTED),
    ) {
        SongCardBody(song, focused, weakArtwork, interactionSource, onSelected)
    }
}

@Composable
private fun SongCardBody(
    song: IndexedSong,
    focused: Boolean,
    weakArtwork: Boolean,
    interactionSource: MutableInteractionSource,
    onSelected: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (focused) FOCUS_PLATE else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelected)
            .padding(CARD_PADDING),
    ) {
        SongArtwork(song = song, weakArtwork = weakArtwork)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.title,
            modifier = Modifier.testTag("song-card-title-${song.songId}"),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = SongCardTitle,
        )
        Spacer(modifier = Modifier.height(TITLE_ARTIST_GAP))
        Box(modifier = Modifier.height(ARTIST_SLOT_HEIGHT)) {
            if (focused || weakArtwork) ArtistText(song = song, weakArtwork = weakArtwork)
        }
    }
}

@Composable
private fun SongArtwork(song: IndexedSong, weakArtwork: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IMAGE_HEIGHT)
            .clip(IMAGE_SHAPE)
            .background(SURFACE_MUTED),
    ) {
        CoverImage(song = song, weakArtwork = weakArtwork)
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(TAG_CORNER_INSET)) {
            SongTags(song)
        }
    }
}

@Composable
private fun CoverImage(song: IndexedSong, weakArtwork: Boolean) {
    if (weakArtwork) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = song.artist, style = SongCardArtistFocused, color = TextSecondary, maxLines = 1)
        }
    } else {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ArtistText(song: IndexedSong, weakArtwork: Boolean) {
    Text(
        text = song.artist,
        modifier = Modifier.testTag("song-card-artist-${song.songId}"),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = SongCardArtistFocused,
        color = if (weakArtwork) TextPrimary else TextSecondary,
    )
}

@Composable
private fun SongTags(song: IndexedSong) {
    val tags = buildList {
        if (song.isDuet) add("D")
        if (song.canMedley) add("M")
        if (song.hasRap) add("R")
        if (song.genre.equals("Instrumental", ignoreCase = true)) add("I")
        if (song.hasVideo) add("V")
    }.take(3)
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(TAG_GAP)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CHIP_BACKGROUND)
                    .border(BorderStroke(BORDER_THIN, BORDER_SUBTLE), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(text = tag, style = TagChipLabel)
            }
        }
    }
}

private fun cardBorder(focused: Boolean): BorderStroke = BorderStroke(
    width = if (focused) FOCUS_BORDER_WIDTH else BORDER_THIN,
    color = if (focused) BorderFocus else BORDER_SUBTLE,
)
