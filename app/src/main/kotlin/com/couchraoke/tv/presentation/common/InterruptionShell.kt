@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.ui.theme.BodyPrimary
import com.couchraoke.tv.ui.theme.InterruptionModalBodyBottomGap
import com.couchraoke.tv.ui.theme.InterruptionModalTitleBottomGap
import com.couchraoke.tv.ui.theme.InterruptionModalWidth
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.PrimaryModalPadding
import com.couchraoke.tv.ui.theme.RadiusLarge
import com.couchraoke.tv.ui.theme.Space16
import com.couchraoke.tv.ui.theme.SurfaceLevel0
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary
import com.couchraoke.tv.ui.theme.constrainedTvSurfaceWidth

@Composable
fun InterruptionShell(
    title: String,
    bodyLines: List<String>,
    modifier: Modifier = Modifier,
    background: (@Composable (Modifier) -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (background != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("interruption-background-focus-locked"),
            ) {
                background(Modifier.fillMaxSize())
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .background(SurfaceLevel0.copy(alpha = 0.66f))
                .testTag("interruption-shell-scrim"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(constrainedTvSurfaceWidth(InterruptionModalWidth, maxWidth))
                    .clip(RoundedCornerShape(RadiusLarge))
                    .background(SurfaceLevel2)
                    .padding(PrimaryModalPadding)
                    .testTag("interruption-shell"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, style = PanelTitle, color = TextPrimary)
                Spacer(modifier = Modifier.height(InterruptionModalTitleBottomGap))
                bodyLines.forEach { bodyLine ->
                    Text(text = bodyLine, style = BodyPrimary, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(InterruptionModalBodyBottomGap))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space16),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
