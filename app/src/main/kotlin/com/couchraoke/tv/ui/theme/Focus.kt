package com.couchraoke.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

data class Focus(
    val focusedBorderColor: Color = BorderFocus,
    val focusedBorderWidth: Dp = FocusBorderWidth,
    val focusedBorderInset: Dp = FocusBorderInset,
    val unfocusedBorderWidth: Dp = BorderThin,
    val unfocusedBorderColor: Color = BorderSubtle.copy(alpha = UNFOCUSED_BORDER_OPACITY),
    val usesFilledPlate: Boolean = true,
    val disabledElementsFocusable: Boolean = false,
    val usesShadow: Boolean = false,
    val usesBlur: Boolean = false,
    val usesGlow: Boolean = false,
    val usesElevationChange: Boolean = false,
    val usesScale: Boolean = false,
    val usesBackgroundPulse: Boolean = false,
)

val CouchraokeFocusPolicy = Focus()
