package com.couchraoke.tv.presentation.songlist.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * A non-focusable widget that displays a QR code and join token for session pairing.
 *
 * @param joinToken The human-readable join token (e.g. "ABCDE-FGHIJ").
 * @param wsUrl The full WebSocket URL to be encoded in the QR code.
 * @param modifier The modifier to be applied to the widget.
 */
@Suppress("FunctionNaming")
@Composable
fun JoinWidget(
    joinToken: String,
    wsUrl: String,
    modifier: Modifier = Modifier,
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(wsUrl) {
        qrBitmap = try {
            BarcodeEncoder().encodeBitmap(wsUrl, BarcodeFormat.QR_CODE, QR_BITMAP_SIZE_PX, QR_BITMAP_SIZE_PX)
        } catch (e: com.google.zxing.WriterException) {
            android.util.Log.w("JoinWidget", "QR encoding failed", e)
            null // QR encoding failed — widget shows no image
        }
    }

    Box(
        modifier = modifier
            .focusable(false)
            .widthIn(min = 280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR code to join",
                    modifier = Modifier
                        .fillMaxHeight(QR_HEIGHT_FRACTION)
                        .aspectRatio(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = joinToken,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val QR_BITMAP_SIZE_PX = 512
private const val QR_HEIGHT_FRACTION = 0.16f
