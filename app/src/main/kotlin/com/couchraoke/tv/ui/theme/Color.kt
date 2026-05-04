package com.couchraoke.tv.ui.theme

import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF050A0F)
val SurfacePrimary = Color(0xFF111821)
val SurfaceElevated = Color(0xFF182230)
val SurfaceLaneBandBase = Color(0xFF111821)
val SurfaceLyricsBandBase = Color(0xFF050A0F)
val BorderSubtle = Color(0xFF8A98A8)
val BorderFocus = Color(0xFF33D7FF)
val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xFFB8C4D2)
val TextDisabled = Color(0xFF6F7B89)
val Player1Accent = Color(0xFF33D7FF)
val Player2Accent = Color(0xFFFF4FD8)
val RewardAccent = Color(0xFFFFC857)
val Success = Color(0xFF57D68D)
val Warning = Color(0xFFFFB84D)
val Error = Color(0xFFFF5C5C)

const val LANE_BAND_ALPHA = 0.68f
const val LYRICS_BAND_ALPHA = 0.82f

val SurfaceLevel0 = AppBackground
val SurfaceLevel1 = SurfacePrimary
val SurfaceLevel2 = SurfaceElevated
val SurfaceLaneBand = SurfaceLaneBandBase.copy(alpha = LANE_BAND_ALPHA)
val SurfaceLyricsBand = SurfaceLyricsBandBase.copy(alpha = LYRICS_BAND_ALPHA)
