package com.couchraoke.tv.presentation.songlist.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size
    )
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(
                x,
                y,
                if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            )
        }
    }
    return bmp
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun PairingOverlay(
    joinToken: String,
    joinUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qrSize = 320.dp
    val qrSizePx = with(LocalDensity.current) { qrSize.roundToPx() }
    val qrBitmap = remember(joinUrl, qrSizePx) {
        generateQrBitmap(content = joinUrl, size = qrSizePx)
    }

    Dialog(onDismissRequest = onDismiss) {
        BackHandler { onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(modifier = Modifier.background(Color.White)) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Pairing QR code",
                            modifier = Modifier
                                .sizeIn(minWidth = 320.dp, minHeight = 320.dp)
                                .size(320.dp)
                        )
                    }

                    Text(
                        text = joinToken,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp
                    )

                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
