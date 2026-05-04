package com.couchraoke.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CouchraokeTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = TextPrimary,
        secondary = TextSecondary,
        tertiary = Player1Accent,
        background = AppBackground,
        surface = SurfacePrimary,
        onPrimary = AppBackground,
        onSecondary = AppBackground,
        onTertiary = AppBackground,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        error = Error,
        onError = AppBackground,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
