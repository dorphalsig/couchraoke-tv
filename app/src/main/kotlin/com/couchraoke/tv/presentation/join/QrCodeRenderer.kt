package com.couchraoke.tv.presentation.join

import androidx.compose.ui.graphics.Color
import com.couchraoke.quality.NoCoverageGenerated
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.solid

@NoCoverageGenerated
object QrCodeRenderer {
    fun render(payload: String, sizePx: Int): RenderedQrCode {
        require(payload.isNotBlank())
        require(sizePx > 0)

        val moduleSizePx = maxOf(1, sizePx / 100)
        val quietZonePx = moduleSizePx * 4
        val contentSize = sizePx - (quietZonePx * 2)
        val left = (sizePx - contentSize) / 2
        val top = (sizePx - contentSize) / 2
        val painter = QrCodePainter(
            data = payload,
            options = QrOptions {
                colors {
                    dark = QrBrush.solid(Color.Black)
                    light = QrBrush.solid(Color.White)
                }
                errorCorrectionLevel = QrErrorCorrectionLevel.Medium
                scale = QrContentScale
            },
        )

        return RenderedQrCode(
            painter = painter,
            sizePx = sizePx,
            moduleSizePx = moduleSizePx,
            contentBounds = QrContentBounds(
                left = left,
                top = top,
                right = left + contentSize,
                bottom = top + contentSize,
            ),
        )
    }

    private const val QrContentScale = 0.92f
}

data class RenderedQrCode(
    val painter: QrCodePainter,
    val sizePx: Int,
    val moduleSizePx: Int,
    val contentBounds: QrContentBounds,
)

data class QrContentBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)
