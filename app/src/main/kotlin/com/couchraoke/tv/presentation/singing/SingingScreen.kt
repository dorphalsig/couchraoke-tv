@file:com.couchraoke.quality.NoCoverageGenerated

package com.couchraoke.tv.presentation.singing

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.R
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.DisplayHeroNumber
import com.couchraoke.tv.ui.theme.LiveScore
import com.couchraoke.tv.ui.theme.LyricsCurrent
import com.couchraoke.tv.ui.theme.LyricsNext
import com.couchraoke.tv.ui.theme.Player1Accent
import com.couchraoke.tv.ui.theme.SingerBadge
import com.couchraoke.tv.ui.theme.SurfaceLaneBand
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLyricsBand
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary
import com.couchraoke.tv.ui.theme.Timer
import com.couchraoke.tv.ui.theme.TopMetadataMinimal

private val BADGE_HEIGHT = 40.dp
private val BADGE_TOP_INSET = 8.dp

@Composable
fun SingingScreen(state: SingingUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.singing),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("singing-surface"),
            factory = { context -> SurfaceView(context).apply { setZOrderMediaOverlay(true) } },
        )
        LaneWithBadge(state = state, modifier = Modifier.align(Alignment.Center))
        SingingMetadata(state = state, modifier = Modifier.align(Alignment.TopCenter))
        SingingLyrics(state = state, modifier = Modifier.align(Alignment.BottomCenter))
        SingingTimerAndScore(state = state)
        SingingCountdown(state = state, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun LaneWithBadge(state: SingingUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SingerBadge(text = state.badgeText)
        LaneBand()
    }
}

@Composable
private fun SingerBadge(text: String) {
    Box(
        modifier = Modifier
            .padding(top = BADGE_TOP_INSET)
            .height(BADGE_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(Player1Accent)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SingerBadge,
            color = Color.Black,
        )
    }
}

@Composable
private fun LaneBand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(192.dp)
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLaneBand),
    )
}

@Composable
private fun SingingMetadata(state: SingingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        state.title?.let {
            Text(text = it, style = TopMetadataMinimal, color = TextPrimary)
        }
        state.artist?.let {
            Text(text = it, style = TopMetadataMinimal, color = TextSecondary)
        }
    }
}

@Composable
private fun SingingLyrics(state: SingingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLyricsBand)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text(
            text = state.currentLyricsLine.orEmpty(),
            style = LyricsCurrent,
            color = TextPrimary,
        )
        Text(
            text = state.nextLyricsLine.orEmpty(),
            style = LyricsNext,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SingingTimerAndScore(state: SingingUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = state.elapsedTimeText,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            style = Timer,
            color = TextPrimary,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(width = 144.dp, height = 88.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLevel1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.scoreText,
                style = LiveScore.copy(fontSize = 48.sp),
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun SingingCountdown(state: SingingUiState, modifier: Modifier = Modifier) {
    state.countdownNumber?.let {
        Text(
            text = it.toString(),
            modifier = modifier,
            style = DisplayHeroNumber,
            color = TextPrimary,
        )
    }
}

@NoCoverageGenerated
@Preview(name = "Singing Single Singer", widthDp = 1920, heightDp = 1080)
@Composable
fun SingingScreenPreview() {
    CouchraokeTheme {
        SingingScreen(
            state = SingingUiState(
                isPlaying = true,
                title = "Demo Song",
                artist = "Demo Artist",
                currentLyricsLine = "Hello",
                nextLyricsLine = "couchraoke",
                elapsedTimeText = "00:35",
                scoreText = "00000",
                badgeText = "P1",
            ),
        )
    }
}
