@file:com.couchraoke.quality.NoCoverageGenerated

package com.couchraoke.tv.presentation.singing

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.couchraoke.tv.ui.theme.DisplayHeroTitle
import com.couchraoke.tv.ui.theme.SingingTopIntroStripHeight
import com.couchraoke.tv.ui.theme.SingingTopMinimalStripHeight
import com.couchraoke.tv.ui.theme.TopMetadataMinimal

internal fun createSingingVideoSurface(context: android.content.Context): android.view.SurfaceView =
    android.view.SurfaceView(context).apply { setZOrderMediaOverlay(true) }

internal fun createPitchLaneSurface(context: android.content.Context): android.view.SurfaceView =
    android.view.SurfaceView(context)

internal fun metadataStripHeight(isPlaying: Boolean): Dp =
    if (isPlaying) SingingTopMinimalStripHeight else SingingTopIntroStripHeight

internal fun metadataTitleStyle(isPlaying: Boolean): TextStyle =
    if (isPlaying) TopMetadataMinimal else DisplayHeroTitle
