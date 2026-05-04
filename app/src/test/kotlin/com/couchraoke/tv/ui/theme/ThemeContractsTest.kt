package com.couchraoke.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContractsTest {
    @Test(timeout = 30_000)
    fun spacingRadiusBorderAndLayoutTokensMatchIteration1Spec() {
        assertEquals(8.dp, Space8)
        assertEquals(12.dp, Space12)
        assertEquals(16.dp, Space16)
        assertEquals(24.dp, Space24)
        assertEquals(32.dp, Space32)
        assertEquals(48.dp, Space48)

        assertEquals(8.dp, RadiusSmall)
        assertEquals(12.dp, RadiusMedium)
        assertEquals(16.dp, RadiusLarge)
        assertEquals(1.dp, BorderThin)
        assertEquals(3.dp, FocusBorderWidth)
        assertEquals(2.dp, FocusBorderInset)
        assertEquals(0.20f, UNFOCUSED_BORDER_OPACITY)

        assertEquals(48.dp, AppMarginHorizontal)
        assertEquals(36.dp, AppMarginVertical)
        assertEquals(76.dp, HeaderHeight)
        assertEquals(72.dp, StandardButtonHeight)
        assertEquals(76.dp, StandardRowHeight)
        assertEquals(56.dp, DenseRowHeight)
        assertEquals(960.dp, PrimaryModalWidth)
        assertEquals(32.dp, PrimaryModalPadding)
        assertEquals(400.dp, QRCodeSize)
        assertEquals(960.dp, InterruptionModalWidth)
        assertEquals(16.dp, InterruptionModalTitleBottomGap)
        assertEquals(24.dp, InterruptionModalBodyBottomGap)
        assertEquals(72.dp, InterruptionActionRowHeight)
    }

    @Test(timeout = 30_000)
    fun typographyTokensUseOperationalSansExceptDisplayTiers() {
        assertEquals(160.sp, DisplayHeroNumber.fontSize)
        assertEquals(56.sp, DisplayHeroTitle.fontSize)
        assertEquals(44.sp, DisplayAccentTitle.fontSize)
        assertEquals(40.sp, ScreenTitle.fontSize)
        assertEquals(32.sp, SectionTitle.fontSize)
        assertEquals(28.sp, PanelTitle.fontSize)
        assertEquals(24.sp, SongCardTitle.fontSize)
        assertEquals(18.sp, SongCardArtistFocused.fontSize)
        assertEquals(32.sp, PreviewTitle.fontSize)
        assertEquals(24.sp, PreviewArtist.fontSize)
        assertEquals(16.sp, TagChipLabel.fontSize)
        assertEquals(24.sp, BodyPrimary.fontSize)
        assertEquals(20.sp, BodySecondary.fontSize)
        assertEquals(22.sp, ButtonLabel.fontSize)
        assertEquals(20.sp, FieldLabel.fontSize)
        assertEquals(18.sp, Caption.fontSize)
        assertEquals(40.sp, LyricsCurrent.fontSize)
        assertEquals(32.sp, LyricsNext.fontSize)
        assertEquals(56.sp, LiveScore.fontSize)
        assertEquals(28.sp, SentenceRating.fontSize)
        assertEquals(20.sp, TopMetadataMinimal.fontSize)
        assertEquals(22.sp, SingerBadge.fontSize)
        assertEquals(24.sp, Timer.fontSize)
    }

    @Test(timeout = 30_000)
    fun colorAndSurfaceRolesAreDeclaredWithSpecAlphas() {
        assertEquals(Color(0xFF050A0F), AppBackground)
        assertEquals(Color(0xFF111821), SurfacePrimary)
        assertEquals(Color(0xFF182230), SurfaceElevated)
        assertEquals(0.68f, LANE_BAND_ALPHA)
        assertEquals(0.82f, LYRICS_BAND_ALPHA)
        assertEquals(SurfaceLaneBandBase.copy(alpha = LANE_BAND_ALPHA), SurfaceLaneBand)
        assertEquals(SurfaceLyricsBandBase.copy(alpha = LYRICS_BAND_ALPHA), SurfaceLyricsBand)

        assertEquals(Color(0xFF33D7FF), Player1Accent)
        assertEquals(Color(0xFFFF4FD8), Player2Accent)
        assertEquals(Color(0xFFFFC857), RewardAccent)
        assertEquals(Color(0xFF33D7FF), BorderFocus)
        assertFalse(BorderFocus == Player2Accent)
        assertFalse(BorderFocus == RewardAccent)
    }

    @Test(timeout = 30_000)
    fun focusPolicyUsesBorderPlusPlateOnlyAndDisabledItemsDoNotFocus() {
        assertEquals(BorderFocus, CouchraokeFocusPolicy.focusedBorderColor)
        assertEquals(FocusBorderWidth, CouchraokeFocusPolicy.focusedBorderWidth)
        assertEquals(FocusBorderInset, CouchraokeFocusPolicy.focusedBorderInset)
        assertEquals(BorderThin, CouchraokeFocusPolicy.unfocusedBorderWidth)
        assertEquals(BorderSubtle.copy(alpha = UNFOCUSED_BORDER_OPACITY), CouchraokeFocusPolicy.unfocusedBorderColor)
        assertTrue(CouchraokeFocusPolicy.usesFilledPlate)
        assertFalse(CouchraokeFocusPolicy.disabledElementsFocusable)
        assertFalse(CouchraokeFocusPolicy.usesShadow)
        assertFalse(CouchraokeFocusPolicy.usesBlur)
        assertFalse(CouchraokeFocusPolicy.usesGlow)
        assertFalse(CouchraokeFocusPolicy.usesElevationChange)
        assertFalse(CouchraokeFocusPolicy.usesScale)
        assertFalse(CouchraokeFocusPolicy.usesBackgroundPulse)
    }

    @Test(timeout = 30_000)
    fun motionBudgetsMatchIteration1AllowedPatternsAndProhibitedEffects() {
        assertEquals(150, FOCUS_IN_DURATION_MS)
        assertEquals(100, FOCUS_OUT_DURATION_MS)
        assertEquals(MotionBudget.V2, CouchraokeMotionPolicy.songListSettled)
        assertEquals(MotionBudget.V1, CouchraokeMotionPolicy.songListActiveNavigation)
        assertEquals(MotionBudget.V1, CouchraokeMotionPolicy.joinOverlay)
        assertEquals(MotionBudget.V1, CouchraokeMotionPolicy.selectPlayers)
        assertEquals(MotionBudget.V0, CouchraokeMotionPolicy.loading)
        assertEquals(MotionBudget.V2, CouchraokeMotionPolicy.countdown)
        assertEquals(MotionBudget.V0, CouchraokeMotionPolicy.singing)
        assertEquals(MotionBudget.V1, CouchraokeMotionPolicy.interruptionOverlay)

        assertFalse(CouchraokeMotionPolicy.allowsRuntimeBlur)
        assertFalse(CouchraokeMotionPolicy.allowsBloom)
        assertFalse(CouchraokeMotionPolicy.allowsGlow)
        assertFalse(CouchraokeMotionPolicy.allowsFullScreenShaderEffects)
        assertFalse(CouchraokeMotionPolicy.allowsGameplayParticles)
        assertFalse(CouchraokeMotionPolicy.allowsBackgroundAnimationDuringSinging)
        assertFalse(CouchraokeSurfacePolicy.allowsRuntimeBlur)
        assertFalse(CouchraokeSurfacePolicy.allowsGlow)
        assertFalse(CouchraokeSurfacePolicy.allowsShadow)
        assertFalse(CouchraokeSurfacePolicy.allowsElevationFocusTreatment)
    }
}
