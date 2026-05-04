package com.couchraoke.tv.presentation.join

import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QrCodeRendererTest {
    @Test(timeout = 30_000)
    fun rendersShortPayloadCenteredInsideRequestedSquare() {
        val result = QrCodeRenderer.render("ws://1.1.1.1:1/?token=A", sizePx = 400)

        assertCenteredWithQuietZone(result)
    }

    @Test(timeout = 30_000)
    fun rendersFullEndpointPayloadCenteredInsideRequestedSquare() {
        val result = QrCodeRenderer.render(SoloSingFixtures.joinQrPayload(), sizePx = 400)

        assertCenteredWithQuietZone(result)
    }

    private fun assertCenteredWithQuietZone(result: RenderedQrCode) {
        assertTrue(result.contentBounds.left >= result.moduleSizePx * 4)
        assertTrue(result.contentBounds.top >= result.moduleSizePx * 4)
        assertTrue(result.sizePx - result.contentBounds.right >= result.moduleSizePx * 4)
        assertTrue(result.sizePx - result.contentBounds.bottom >= result.moduleSizePx * 4)
        assertTrue(kotlin.math.abs(result.contentBounds.left - (result.sizePx - result.contentBounds.right)) <= 1)
        assertTrue(kotlin.math.abs(result.contentBounds.top - (result.sizePx - result.contentBounds.bottom)) <= 1)
    }
}
