package com.couchraoke.tv.presentation.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.couchraoke.tv.ui.theme.CouchraokeTheme

/** Stable Compose test tag so later tests can assert this node's bounds by tag. */
const val QR_CODE_IMAGE_TAG = "qr_code_image"

/**
 * T039 (FR-030): renders [payload] as a dark-on-light, static QR code. [QrBitmapRenderer]
 * produces the underlying bitmap at one pixel per module; [FilterQuality.None] keeps every
 * module edge crisp when Compose scales that small bitmap up to [modifier]'s size, rather than
 * blurring it the way bilinear filtering would.
 */
@Composable
fun QrCode(payload: String, modifier: Modifier = Modifier) {
    val render = remember(payload) { QrBitmapRenderer.render(payload) }
    Image(
        bitmap = render.image,
        contentDescription = "QR code to join the session",
        modifier = modifier.testTag(QR_CODE_IMAGE_TAG),
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None,
    )
}

@Preview(name = "QR Code", widthDp = 300, heightDp = 300)
@Composable
fun QrCodePreview() {
    CouchraokeTheme {
        QrCode(payload = "ws://192.168.1.42:51900/?token=SWIFT-PANDA", modifier = Modifier.size(300.dp))
    }
}
