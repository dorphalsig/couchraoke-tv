package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId

data class SingingRenderModel(
    val songId: String,
    val title: String,
    val artist: String,
    val startSec: Float,
    val audioUrl: String,
    val videoUrl: String?,
    val videoGapSec: Float?,
    val background: SingingBackground,
    val layout: SingingLayout = SingingLayout(),
    val lanes: List<SingerLaneRenderModel>,
    val lyrics: LyricsRenderState,
    val elapsedTimeText: String,
    val stopAtLyricsTimeMs: Long,
)

data class SingerLaneRenderModel(
    val playerId: PlayerId,
    val badgeText: String,
    val scoreText: String = "00000",
    val sentenceRatingText: String? = null,
    val lane: LaneRenderState,
)

data class LaneRenderState(
    val playerId: PlayerId,
    val noteTargets: List<StaticNoteTarget>,
    val currentLyricsTimeMs: Long,
    val visibleWindowMs: LongRange = 0L..0L,
    val showNoteLines: Boolean = true,
    val hasLivePitch: Boolean = false,
    val hasScoringFeedback: Boolean = false,
)

data class StaticNoteTarget(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val toneSemitone: Int,
    val lyric: String,
    val noteType: StaticNoteType = StaticNoteType.Regular,
)

enum class StaticNoteType {
    Regular,
    Golden,
    Freestyle,
    Rap,
}

data class LyricsRenderState(
    val currentLine: LyricsLine,
    val nextLine: LyricsLine,
    val highlightFraction: Float,
    val highlightMode: LyricsHighlightMode = LyricsHighlightMode.ClippedReveal,
)

enum class LyricsHighlightMode {
    ClippedReveal,
}

data class LyricsLine(
    val text: String,
    val startTimeMs: Long?,
    val endTimeMs: Long?,
)

sealed interface SingingBackground {
    data class Static(
        val imageUrl: String?,
    ) : SingingBackground

    data class Video(
        val videoUrl: String,
        val fallbackImageUrl: String?,
    ) : SingingBackground
}

data class SingingLayout(
    val topIntroStripHeightDp: Int = 72,
    val topMinimalStripHeightDp: Int = 40,
    val bottomLyricsBandHeightDp: Int = 160,
    val bodyToLyricsGapDp: Int = 16,
    val singleLaneHeightDp: Int = 192,
    val laneHorizontalPaddingDp: Int = 20,
    val laneVerticalPaddingDp: Int = 16,
    val scoreBoxWidthDp: Int = 144,
    val scoreBoxHeightDp: Int = 88,
    val scoreBoxRightInsetDp: Int = 16,
    val scoreBoxToRatingGapDp: Int = 8,
    val badgeHeightDp: Int = 40,
    val badgeTopInsetDp: Int = 8,
    val lyricsBandPaddingHorizontalDp: Int = 24,
    val lyricsBandPaddingTopDp: Int = 20,
    val lyricsBandLineGapDp: Int = 8,
    val singleLaneVerticalPosition: SingingLaneVerticalPosition = SingingLaneVerticalPosition.Centered,
)

enum class SingingLaneVerticalPosition {
    Centered,
}

fun IndexedSong.toSingingBackground(): SingingBackground = videoUrl?.let {
    SingingBackground.Video(videoUrl = it, fallbackImageUrl = backgroundUrl)
} ?: SingingBackground.Static(imageUrl = backgroundUrl)
