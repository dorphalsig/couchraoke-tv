package com.couchraoke.tv.presentation.join

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.presentation.previews.PreviewSoloSingSample
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.BorderThin
import com.couchraoke.tv.ui.theme.DisplayAccentTitle
import com.couchraoke.tv.ui.theme.JoinCodeTopGap
import com.couchraoke.tv.ui.theme.JoinQrPanelPadding
import com.couchraoke.tv.ui.theme.PrimaryModalPadding
import com.couchraoke.tv.ui.theme.PrimaryModalWidth
import com.couchraoke.tv.ui.theme.RadiusLarge
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TV_PREVIEW_HEIGHT_DP
import com.couchraoke.tv.ui.theme.TV_PREVIEW_WIDTH_DP
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.constrainedTvQrSize
import com.couchraoke.tv.ui.theme.constrainedTvSurfaceWidth

private val ModalShape = RoundedCornerShape(RadiusLarge)
private val ModalSurface = SurfaceLevel2
private val Scrim = Color.Black.copy(alpha = 0.66f)
private val QrPanelShape = RoundedCornerShape(RadiusLarge)
private val QrPanelSurface = Color.White
private val QrPanelBorder = BorderSubtle.copy(alpha = 0.2f)

@NoCoverageGenerated
@Composable
fun JoinOverlay(
    qrPayload: String,
    joinCode: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Scrim)
            .testTag("join-overlay-scrim"),
        contentAlignment = Alignment.Center,
    ) {
        val modalWidth = constrainedTvSurfaceWidth(PrimaryModalWidth, maxWidth)
        val qrSize = constrainedTvQrSize(maxWidth, maxHeight)
        val density = LocalDensity.current
        val qrSizePx = with(density) { qrSize.roundToPx() }
        val renderedQr = remember(qrPayload, qrSizePx) {
            QrCodeRenderer.render(qrPayload, sizePx = qrSizePx)
        }

        Surface(
            modifier = Modifier.width(modalWidth),
            shape = ModalShape,
            colors = SurfaceDefaults.colors(containerColor = ModalSurface),
        ) {
            Column(
                modifier = Modifier.padding(PrimaryModalPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .testTag("join-overlay-qr-panel")
                        .clip(QrPanelShape)
                        .background(QrPanelSurface)
                        .border(width = BorderThin, color = QrPanelBorder, shape = QrPanelShape)
                        .padding(JoinQrPanelPadding),
                ) {
                    Image(
                        painter = renderedQr.painter,
                        contentDescription = "Join QR code",
                        modifier = Modifier.size(qrSize).clearAndSetSemantics { },
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(JoinCodeTopGap))
                Text(
                    text = joinCode,
                    style = DisplayAccentTitle,
                    color = TextPrimary,
                )
            }
        }
    }
}

@NoCoverageGenerated
@Preview(name = "Join Overlay", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun JoinOverlayPreview() {
    val qrPayload = "ws://${PreviewSoloSingSample.TvIpAddress}:${PreviewSoloSingSample.WebSocketPort}" +
        "/?token=${PreviewSoloSingSample.SessionToken}"
    JoinOverlay(
        qrPayload = qrPayload,
        joinCode = PreviewSoloSingSample.JoinCode,
    )
}
