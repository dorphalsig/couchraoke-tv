package com.couchraoke.tv.presentation.join

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.couchraoke.tv.presentation.qr.QrBitmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val VIEWPORT_BOUNDS = DpRect(left = 0.dp, top = 0.dp, right = 960.dp, bottom = 540.dp)
private const val MIN_QR_SHARE_OF_SHORTER_SIDE = 0.30f
private const val MAX_QR_SHARE_OF_SHORTER_SIDE = 0.55f

/**
 * The four-module quiet zone [QrBitmapRenderer] passes to ZXing as `EncodeHintType.MARGIN`
 * (research.md R1, FR-031). This test does not assume the renderer honoured it -- see
 * [theRenderedQrCarriesAtLeastAFourModuleQuietZoneFr031], which reads it back off the pixels.
 */
private const val QUIET_ZONE_MODULES = 4

private val TAG_LABELS = mapOf(
    JOIN_OVERLAY_ROOT_TAG to "root",
    JOIN_OVERLAY_QR_TAG to "QR code",
    JOIN_OVERLAY_CODE_TAG to "join code",
    JOIN_OVERLAY_CONNECTED_COUNT_TAG to "connected count",
    JOIN_OVERLAY_DISMISS_ACTION_TAG to "dismiss action",
)

private fun testUiState(): JoinUiState = JoinUiState(
    joinCodeDisplay = "SWIFT-PANDA",
    qrPayload = "ws://192.168.1.42:51900/?token=SWIFT-PANDA",
    connectedCount = 3,
)

private fun DpRect.overlaps(other: DpRect): Boolean =
    left < other.right && other.left < right && top < other.bottom && other.top < bottom

private val DpRect.width get() = right - left
private val DpRect.height get() = bottom - top

/**
 * T061 (research.md R9, FR-030, FR-031): numeric layout assertions for [JoinOverlay] under the
 * pinned `w960dp-h540dp-land-television-xhdpi-notouch` qualifier. This stands in for a
 * screenshot baseline, which this slice deliberately does not record (research.md R9, plan.md
 * Phase E) -- proportion and presence are numeric relationships that belong in CI, not a visual
 * judgement call.
 *
 * One correction to R9, which asks for "the QR's bounds expanded by its quiet zone". The quiet
 * zone is not outside those bounds: [QrBitmapRenderer] passes `EncodeHintType.MARGIN = 4` to
 * ZXing and reports `moduleCount` as `BitMatrix.width`, the side length *including* the margin,
 * so the node tagged [JOIN_OVERLAY_QR_TAG] already contains its own quiet zone. Expanding those
 * bounds by four more modules would double-count the margin and would fail on the join code for
 * a reason FR-030 explicitly requires ("directly beneath"). The QR's measured bounds are
 * therefore used as-is, and the margin's existence is proved separately, off the pixels, by
 * [theRenderedQrCarriesAtLeastAFourModuleQuietZoneFr031].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi-notouch")
class JoinOverlayBoundsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test(timeout = 30_000)
    fun everyExpectedElementIsPresentAndDisplayed() {
        setJoinOverlayContent()

        TAG_LABELS.keys.forEach { tag -> composeRule.onNodeWithTag(tag).assertIsDisplayed() }
    }

    @Test(timeout = 30_000)
    fun nothingExceedsThe960By540ViewportFr030() {
        setJoinOverlayContent()

        boundsByTag().forEach { (tag, rect) ->
            val label = TAG_LABELS.getValue(tag)
            assertTrue(
                "$label left ${rect.left} must be within the 960x540dp viewport (FR-030)",
                rect.left >= VIEWPORT_BOUNDS.left,
            )
            assertTrue(
                "$label top ${rect.top} must be within the 960x540dp viewport (FR-030)",
                rect.top >= VIEWPORT_BOUNDS.top,
            )
            assertTrue(
                "$label right ${rect.right} must be within the 960x540dp viewport (FR-030)",
                rect.right <= VIEWPORT_BOUNDS.right,
            )
            assertTrue(
                "$label bottom ${rect.bottom} must be within the 960x540dp viewport (FR-030)",
                rect.bottom <= VIEWPORT_BOUNDS.bottom,
            )
        }
    }

    @Test(timeout = 30_000)
    fun qrIsBetween30And55PercentOfTheShorterViewportDimensionFr030() {
        setJoinOverlayContent()

        val qr = qrBounds()
        val shorterSide = minOf(VIEWPORT_BOUNDS.width, VIEWPORT_BOUNDS.height)
        val minQrSize = shorterSide * MIN_QR_SHARE_OF_SHORTER_SIDE
        val maxQrSize = shorterSide * MAX_QR_SHARE_OF_SHORTER_SIDE

        assertTrue(
            "QR width ${qr.width} must be >= $minQrSize, the FR-030 dominant-element floor",
            qr.width >= minQrSize,
        )
        assertTrue(
            "QR width ${qr.width} must be <= $maxQrSize, the FR-030 dominant-element ceiling",
            qr.width <= maxQrSize,
        )
        assertTrue(
            "QR height ${qr.height} must be >= $minQrSize, the FR-030 dominant-element floor",
            qr.height >= minQrSize,
        )
        assertTrue(
            "QR height ${qr.height} must be <= $maxQrSize, the FR-030 dominant-element ceiling",
            qr.height <= maxQrSize,
        )
    }

    @Test(timeout = 30_000)
    fun joinCodeSitsDirectlyBeneathTheQrWithNothingBetweenFr030() {
        setJoinOverlayContent()

        val qr = qrBounds()
        val code = codeBounds()
        assertTrue(
            "join code top ${code.top} must sit at/below the QR's bottom ${qr.bottom} (FR-030)",
            code.top >= qr.bottom,
        )

        val gapBetweenQrAndCode = DpRect(left = qr.left, top = qr.bottom, right = qr.right, bottom = code.top)
        otherNodeBounds(exclude = setOf(JOIN_OVERLAY_QR_TAG, JOIN_OVERLAY_CODE_TAG)).forEach { (tag, rect) ->
            val label = TAG_LABELS.getValue(tag)
            assertTrue(
                "$label must not sit between the QR and the join code (FR-030 nothing-between requirement)",
                !rect.overlaps(gapBetweenQrAndCode),
            )
        }
    }

    @Test(timeout = 30_000)
    fun noNodeOverlapsTheQrsQuietZoneFr031() {
        setJoinOverlayContent()

        val qr = qrBounds()
        otherNodeBounds(exclude = setOf(JOIN_OVERLAY_QR_TAG)).forEach { (tag, rect) ->
            val label = TAG_LABELS.getValue(tag)
            assertTrue("$label must not overlap the QR's quiet zone (FR-031)", !rect.overlaps(qr))
        }
    }

    /**
     * FR-031's other half: the quiet zone must actually be there. [QrBitmapRenderer] encodes at
     * `0x0`, which pins ZXing's scale to one pixel per module, so the render is exactly
     * `moduleCount` pixels square and the outermost [QUIET_ZONE_MODULES] rings of pixels *are*
     * the margin. Reading them back proves the renderer honoured `EncodeHintType.MARGIN` rather
     * than assuming it, which is what research.md R1 asks of this test ("asserts a measured
     * margin rather than an assumed one").
     */
    @Test(timeout = 30_000)
    fun theRenderedQrCarriesAtLeastAFourModuleQuietZoneFr031() {
        val render = QrBitmapRenderer.render(testUiState().qrPayload)
        val pixels = render.image.toPixelMap()
        val last = render.moduleCount - 1

        for (ring in 0 until QUIET_ZONE_MODULES) {
            for (i in 0 until render.moduleCount) {
                listOf(
                    "top" to pixels[i, ring],
                    "bottom" to pixels[i, last - ring],
                    "left" to pixels[ring, i],
                    "right" to pixels[last - ring, i],
                ).forEach { (edge, color) ->
                    assertEquals(
                        "$edge edge module $i of quiet-zone ring $ring must be light (FR-031)",
                        Color.White,
                        color,
                    )
                }
            }
        }
    }

    private fun setJoinOverlayContent() {
        composeRule.setContent { JoinOverlay(uiState = testUiState(), onDismissRequest = {}) }
    }

    private fun boundsByTag(): Map<String, DpRect> =
        TAG_LABELS.keys.associateWith { tag -> composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot() }

    private fun qrBounds(): DpRect = composeRule.onNodeWithTag(JOIN_OVERLAY_QR_TAG).getUnclippedBoundsInRoot()

    private fun codeBounds(): DpRect = composeRule.onNodeWithTag(JOIN_OVERLAY_CODE_TAG).getUnclippedBoundsInRoot()

    private fun otherNodeBounds(exclude: Set<String>): Map<String, DpRect> =
        boundsByTag().filterKeys { it !in exclude && it != JOIN_OVERLAY_ROOT_TAG }
}
