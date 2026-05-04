package com.couchraoke.tv.presentation.join

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.DisplayAccentTitle
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextPrimary

private val ModalShape = RoundedCornerShape(24.dp)
private val ModalSurface = SurfaceLevel2
private val Scrim = Color.Black.copy(alpha = 0.66f)
private val QrPanelShape = RoundedCornerShape(20.dp)
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

    val renderedQr = remember(qrPayload) { QrCodeRenderer.render(qrPayload, sizePx = 400) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Scrim)
            .testTag("join-overlay-scrim"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(960.dp),
            shape = ModalShape,
            colors = SurfaceDefaults.colors(containerColor = ModalSurface),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .testTag("join-overlay-qr-panel")
                        .clip(QrPanelShape)
                        .background(QrPanelSurface)
                        .border(width = 1.dp, color = QrPanelBorder, shape = QrPanelShape)
                        .padding(20.dp),
                ) {
                    Image(
                        painter = renderedQr.painter,
                        contentDescription = "Join QR code",
                        modifier = Modifier.size(400.dp).clearAndSetSemantics { },
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
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
@Preview(name = "Join Overlay", widthDp = 1920, heightDp = 1080)
@Composable
fun JoinOverlayPreview() {
    JoinOverlay(
        qrPayload = "ws://192.168.1.10:8080/?token=ABCDEFGH",
        joinCode = "ABCDEFGH",
    )
}
