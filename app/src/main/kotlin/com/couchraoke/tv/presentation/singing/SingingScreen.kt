@file:com.couchraoke.quality.NoCoverageGenerated

package com.couchraoke.tv.presentation.singing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.R
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.DisplayHeroNumber
import com.couchraoke.tv.ui.theme.LiveScore
import com.couchraoke.tv.ui.theme.LyricsBandLineGap
import com.couchraoke.tv.ui.theme.LyricsBandPaddingHorizontal
import com.couchraoke.tv.ui.theme.LyricsBandPaddingTop
import com.couchraoke.tv.ui.theme.LyricsCurrent
import com.couchraoke.tv.ui.theme.LyricsNext
import com.couchraoke.tv.ui.theme.Player1Accent
import com.couchraoke.tv.ui.theme.RadiusMedium
import com.couchraoke.tv.ui.theme.SentenceRating
import com.couchraoke.tv.ui.theme.SingerBadge
import com.couchraoke.tv.ui.theme.SingingBadgeHeight
import com.couchraoke.tv.ui.theme.SingingBadgeTopInset
import com.couchraoke.tv.ui.theme.SingingBodyToLyricsGap
import com.couchraoke.tv.ui.theme.SingingBottomLyricsBandHeight
import com.couchraoke.tv.ui.theme.SingingLaneHorizontalPadding
import com.couchraoke.tv.ui.theme.SingingLaneVerticalPadding
import com.couchraoke.tv.ui.theme.SingingScoreBoxHeight
import com.couchraoke.tv.ui.theme.SingingScoreBoxRightInset
import com.couchraoke.tv.ui.theme.SingingScoreBoxToRatingGap
import com.couchraoke.tv.ui.theme.SingingScoreBoxWidth
import com.couchraoke.tv.ui.theme.SingingSingleLaneHeight
import com.couchraoke.tv.ui.theme.SingingTopIntroStripHeight
import com.couchraoke.tv.ui.theme.SingingTopMinimalStripHeight
import com.couchraoke.tv.ui.theme.SurfaceLaneBand
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.SurfaceLyricsBand
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary
import com.couchraoke.tv.ui.theme.Timer
import com.couchraoke.tv.ui.theme.TopMetadataMinimal

@Composable
fun SingingScreen(state: SingingUiState) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.singing),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().testTag("singing-static-background"),
            contentScale = ContentScale.Crop,
        )
        state.backgroundImageUrl?.let { backgroundImageUrl ->
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().testTag("singing-song-background"),
                contentScale = ContentScale.Crop,
            )
        }
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("singing-video-surface"),
            factory = { context -> android.view.SurfaceView(context).apply { setZOrderMediaOverlay(true) } },
        )
        PitchLaneSurface(state = state.laneState)
        SingingBody(state = state)
        SingingMetadata(state = state, modifier = Modifier.align(Alignment.TopCenter))
        SingingLyrics(state = state, modifier = Modifier.align(Alignment.BottomCenter))
        SingingCountdown(state = state, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun PitchLaneSurface(state: LaneRenderState?) {
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag("singing-pitch-lane-surface"),
        factory = { context ->
            android.view.SurfaceView(context).apply {
                setZOrderMediaOverlay(true)
            }
        },
        update = { surfaceView ->
            val laneState = state ?: return@AndroidView
            surfaceView.post {
                val density = surfaceView.context.resources.displayMetrics.density
                val holder = surfaceView.holder
                val canvas = holder.lockCanvas() ?: return@post
                try {
                    // Scale so all renderer coordinates are in dp, consistent with Compose layout.
                    canvas.scale(density, density)
                    DefaultPitchLaneRenderer().drawPitchLane(
                        canvas = Canvas(canvas),
                        viewport = Rect(
                            left = SingingLaneHorizontalPadding.value,
                            top = surfaceView.height / density / 2f - SingingSingleLaneHeight.value / 2f,
                            right = surfaceView.width / density - SingingLaneHorizontalPadding.value,
                            bottom = surfaceView.height / density / 2f + SingingSingleLaneHeight.value / 2f,
                        ),
                        state = laneState,
                    )
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        },
    )
}

@Composable
private fun SingingBody(state: SingingUiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(
            modifier = Modifier.height(
                if (state.isPlaying) SingingTopMinimalStripHeight else SingingTopIntroStripHeight,
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = SingingBodyToLyricsGap),
            contentAlignment = Alignment.Center,
        ) {
            LaneWithBadge(state = state)
        }
        Spacer(modifier = Modifier.height(SingingBottomLyricsBandHeight))
    }
}

@Composable
private fun LaneWithBadge(state: SingingUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SingingSingleLaneHeight)
            .padding(horizontal = SingingLaneHorizontalPadding),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(SingingSingleLaneHeight)
                .padding(vertical = SingingLaneVerticalPadding)
                .clip(RoundedCornerShape(RadiusMedium))
                .background(SurfaceLaneBand),
        )
        SingerBadge(text = state.badgeText, modifier = Modifier.align(Alignment.TopStart))
        ScoreAndRating(state = state, modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun SingerBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = SingingBadgeTopInset)
            .height(SingingBadgeHeight)
            .clip(RoundedCornerShape(RadiusMedium))
            .background(Player1Accent)
            .padding(horizontal = SingingLaneVerticalPadding),
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
private fun ScoreAndRating(state: SingingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(end = SingingScoreBoxRightInset)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = SingingScoreBoxWidth, height = SingingScoreBoxHeight)
                .clip(RoundedCornerShape(RadiusMedium))
                .background(SurfaceLevel1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.scoreText,
                style = LiveScore,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(SingingScoreBoxToRatingGap))
        Text(
            text = "",
            style = SentenceRating,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SingingMetadata(state: SingingUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (state.isPlaying) SingingTopMinimalStripHeight else SingingTopIntroStripHeight)
            .background(SurfaceLevel2)
            .padding(horizontal = LyricsBandPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.title?.let {
            Text(text = it, style = TopMetadataMinimal, color = TextPrimary)
        }
        state.artist?.let {
            Spacer(modifier = Modifier.width(LyricsBandLineGap))
            Text(text = it, style = TopMetadataMinimal, color = TextSecondary)
        }
    }
}

@Composable
private fun SingingLyrics(state: SingingUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(SingingBottomLyricsBandHeight)
            .background(SurfaceLyricsBand)
            .padding(horizontal = LyricsBandPaddingHorizontal, vertical = LyricsBandPaddingTop),
    ) {
        Text(
            text = state.currentLyricsLine.orEmpty(),
            style = LyricsCurrent,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(LyricsBandLineGap))
        Text(
            text = state.nextLyricsLine.orEmpty(),
            style = LyricsNext,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = state.elapsedTimeText,
            modifier = Modifier.align(Alignment.End),
            style = Timer,
            color = TextPrimary,
        )
    }
}

@Composable
private fun SingingCountdown(state: SingingUiState, modifier: Modifier = Modifier) {
    state.countdownNumber?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = it.toString(),
                modifier = modifier,
                style = DisplayHeroNumber,
                color = TextPrimary,
            )
        }
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
