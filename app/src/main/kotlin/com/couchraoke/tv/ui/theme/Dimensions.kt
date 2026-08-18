package com.couchraoke.tv.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Space8 = 8.dp
val Space12 = 12.dp
val Space16 = 16.dp
val Space24 = 24.dp
val Space32 = 32.dp
val Space48 = 48.dp

val RadiusSmall = 8.dp
val RadiusMedium = 12.dp
val RadiusLarge = 16.dp

val BorderThin = 1.dp
val FocusBorderWidth = 3.dp
val FocusBorderInset = 2.dp
const val UNFOCUSED_BORDER_OPACITY = 0.20f

const val FOCUS_IN_DURATION_MS = 150
const val FOCUS_OUT_DURATION_MS = 100
const val TV_PREVIEW_WIDTH_DP = 960
const val TV_PREVIEW_HEIGHT_DP = 540
const val JOIN_QR_MAX_VIEWPORT_HEIGHT_FRACTION = 0.55f

val AppMarginHorizontal = 48.dp
val AppMarginVertical = 36.dp
val HeaderHeight = 76.dp
val StandardButtonHeight = 72.dp
val StandardRowHeight = 76.dp
val DenseRowHeight = 56.dp
val PrimaryModalWidth = 960.dp
val PrimaryModalPadding = 32.dp
val JoinQRCodeSize = 400.dp
val QRCodeSize = JoinQRCodeSize
val JoinQrPanelPadding = 20.dp
val JoinCodeTopGap = 24.dp
val InterruptionModalWidth = 960.dp
val InterruptionModalTitleBottomGap = 16.dp
val InterruptionModalBodyBottomGap = 24.dp
val InterruptionActionRowHeight = 72.dp
val SelectPlayersActionButtonWidth = 176.dp

fun constrainedTvSurfaceWidth(targetWidth: Dp, viewportWidth: Dp): Dp = minOf(
    targetWidth,
    (viewportWidth - AppMarginHorizontal * 2).coerceAtLeast(0.dp),
)

fun constrainedTvQrSize(viewportWidth: Dp, viewportHeight: Dp): Dp = minOf(
    JoinQRCodeSize,
    (viewportWidth - AppMarginHorizontal * 2 - PrimaryModalPadding * 2 - JoinQrPanelPadding * 2).coerceAtLeast(0.dp),
    (viewportHeight * JOIN_QR_MAX_VIEWPORT_HEIGHT_FRACTION).coerceAtLeast(0.dp),
)

val SingingTopIntroStripHeight = 72.dp
val SingingTopMinimalStripHeight = 40.dp
val SingingBottomLyricsBandHeight = 160.dp
val SingingBodyToLyricsGap = 16.dp
val SingingSingleLaneHeight = 192.dp
val SingingLaneHorizontalPadding = 20.dp
val SingingLaneVerticalPadding = 16.dp
val SingingScoreBoxWidth = 144.dp
val SingingScoreBoxHeight = 88.dp
val SingingScoreBoxRightInset = 16.dp
val SingingScoreBoxToRatingGap = 8.dp
val SingingBadgeHeight = 40.dp
val SingingBadgeTopInset = 8.dp
val LyricsBandPaddingHorizontal = 24.dp
val LyricsBandPaddingTop = 20.dp
val LyricsBandLineGap = 8.dp

@Suppress("TopLevelPropertyNaming")
const val SongListLeftRailFraction = 0.34f

@Suppress("TopLevelPropertyNaming")
const val SongListGridFraction = 0.66f
val SongListRailGridGap = 32.dp
val SongListHeaderControlGap = 16.dp
val SongListHeaderButtonWidth = 160.dp
val SongListSearchHeight = 64.dp
val SongListHeaderToBodyGap = 24.dp
val SongListRandomRowHeight = 72.dp
val SongListRandomRowGap = 24.dp
val SongListGridColumnGap = 24.dp
val SongListGridRowGap = 24.dp

@Suppress("TopLevelPropertyNaming")
const val SongListGridColumnsWide = 4
val SongListCompactMaxWidth = 1200.dp

@Suppress("TopLevelPropertyNaming")
const val SongListCompactGridColumns = 3
val SongListCompactMarginHorizontal = 24.dp
val SongListCompactMarginVertical = 20.dp
val SongListCompactRailGridGap = 20.dp
val SongListCompactHeaderControlGap = 12.dp
val SongListCompactHeaderButtonWidth = 128.dp
val SongListCompactSearchHeight = 52.dp
val SongListCompactHeaderHeight = 56.dp
val SongListCompactHeaderToBodyGap = 12.dp
val SongListCompactRandomRowHeight = 52.dp
val SongListCompactRandomRowGap = 12.dp
val SongListCompactGridColumnGap = 16.dp
val SongListCompactGridRowGap = 16.dp

@Suppress("TopLevelPropertyNaming")
const val SongListPreviewAspect = 16f / 9f
val SongListPreviewToMetaGap = 16.dp
val SongListMetaToPlaylistGap = 24.dp
val SongListPlaylistRowHeight = 52.dp

@Suppress("TopLevelPropertyNaming")
const val SongListPlaylistVisibleRows = 5
val SongListPlayMedleyTopGap = 16.dp

@Suppress("TopLevelPropertyNaming")
const val SongListCompactPreviewAspect = 16f / 9f
val SongListCompactPreviewToMetaGap = 6.dp
val SongListCompactMetaToPlaylistGap = 8.dp
val SongListCompactPlaylistRowHeight = 36.dp

@Suppress("TopLevelPropertyNaming")
const val SongListCompactPlaylistVisibleRows = 3
val SongListCompactPlayMedleyTopGap = 8.dp

val SongCardHeight = 252.dp
val SongCardPadding = 12.dp
val SongCardImageHeight = 148.dp
val SongCardImageToTitleGap = 12.dp
val SongCardCompactHeight = 140.dp
val SongCardCompactPadding = 8.dp
val SongCardCompactImageSize = 72.dp
val SongCardCompactImageHeight = SongCardCompactImageSize
val SongCardCompactImageToTitleGap = 4.dp
val SongCardImageCornerRadius = 8.dp

@Suppress("TopLevelPropertyNaming")
const val SongCardTitleMaxLines = 2

@Suppress("TopLevelPropertyNaming")
const val SongCardCompactTitleMaxLines = 1
val SongCardFocusedArtistSlotHeight = 20.dp
val SongCardTitleToArtistGap = 4.dp
val SongCardTagCornerInset = 8.dp
val SongCardTagGap = 6.dp

@Suppress("TopLevelPropertyNaming")
const val SongCardMaxVisibleTags = 3
