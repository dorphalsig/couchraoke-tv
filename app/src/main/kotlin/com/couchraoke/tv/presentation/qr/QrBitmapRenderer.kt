package com.couchraoke.tv.presentation.qr

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Color as AndroidColor

/**
 * The result of rendering a QR payload: the bitmap itself, plus the exact module count of the
 * underlying [com.google.zxing.common.BitMatrix] (side length, quiet zone included). Exposing
 * [moduleCount] lets a later bounds test (T061, research.md R9) measure the rendered quiet
 * zone in modules rather than assume one, without duplicating the ZXing call itself.
 */
data class QrRender(val image: ImageBitmap, val moduleCount: Int)

/**
 * T039 (research.md R1, FR-030): owns the ZXing call and the four-module quiet zone. Pure
 * payload-string construction lives in [QrPayloadEncoder] instead, so that class stays
 * JVM-testable with no ZXing/Android type in its signature.
 *
 * `QRCodeWriter.encode` is called with `width = height = 0` deliberately: ZXing's
 * `renderResult` computes `outputWidth = max(requestedWidth, moduleCount)` and a `multiple`
 * scale factor of `min(outputWidth / moduleCount, ...)`, so requesting `0x0` is the one input
 * that always yields `multiple = 1` -- a matrix rendered at exactly one pixel per module, with
 * [com.google.zxing.common.BitMatrix.getWidth] equal to the true module count including the
 * quiet zone. [QrCode] then scales that small, crisp matrix up to fill the space Compose gives
 * it, keeping the code dark-on-light and static (FR-030).
 */
object QrBitmapRenderer {
    private const val QUIET_ZONE_MODULES = 4

    fun render(payload: String): QrRender {
        val hints = mapOf(EncodeHintType.MARGIN to QUIET_ZONE_MODULES)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)

        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        return QrRender(image = bitmap.asImageBitmap(), moduleCount = matrix.width)
    }
}
