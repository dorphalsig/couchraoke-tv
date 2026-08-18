package com.couchraoke.tv.presentation.playback

import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VideoSurfaceBindingTest {
    @Test(timeout = 30_000)
    fun setVideoSurfaceDelegatesHolderSurfaceToVlcPlayer() {
        val holder = SurfaceView(context()).holder
        val player = FakeVlcPlayer()
        val handle = VlcLibVlcPlayerHandle(
            player = player,
            mediaFactory = FakeVlcMediaFactory(),
            disableAudio = true,
        )

        handle.setVideoSurface(holder)
        handle.setVideoSurface(null)

        assertSame(holder.surface, player.surfaces[0])
        assertSame(holder, player.holders[0])
        assertEquals(null, player.surfaces[1])
        assertEquals(null, player.holders[1])
    }

    @Test(timeout = 30_000)
    fun controllerSetVideoSurfaceDelegatesToDecorativeVlcHandle() {
        val holder = SurfaceView(context()).holder
        val videoPlayer = FakeVlcPlayer()
        val videoHandle = VlcLibVlcPlayerHandle(
            player = videoPlayer,
            mediaFactory = FakeVlcMediaFactory(),
            disableAudio = true,
        )
        val controller = DefaultPlaybackController(
            audioHandle = FakeHandle(),
            videoHandle = videoHandle,
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.setVideoSurface(holder)

        assertSame(holder.surface, videoPlayer.surfaces.single())
        assertSame(holder, videoPlayer.holders.single())
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private class FakeAudioFocusController : AudioFocusController {
        override fun requestAudioFocus(): Boolean = true
        override fun abandonAudioFocus() = Unit
    }

    private class FakeHandle : LibVlcPlayerHandle, PreparedDurationProvider {
        override val events = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = 8)
        override val timeMs: Long = 0L
        override val durationMs: Long? = 180_000L
        override fun prepare(mediaUrl: String, seekToSec: Float) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(percent: Int) = Unit
        override fun release() = Unit
    }

    private class FakeVlcMediaFactory : VlcMediaFactory {
        override fun create(url: String): VlcMedia = FakeVlcMedia(url)
    }

    private class FakeVlcMedia(
        override val url: String,
    ) : VlcMedia {
        override fun addOption(option: String) = Unit
        override fun parse(flags: Int, timeoutMs: Long): Boolean = true
        override fun release() = Unit
    }

    private class FakeVlcPlayer : VlcPlayer {
        override var listener: ((VlcPlayerCallback) -> Unit)? = null
        override val timeMs: Long = 0L
        override val durationMs: Long? = 180_000L
        val surfaces = mutableListOf<Surface?>()
        val holders = mutableListOf<SurfaceHolder?>()
        override fun setMedia(media: VlcMedia) = Unit
        override fun play() = Unit
        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) = Unit
        override fun setVolume(percent: Int) = Unit
        override fun release() = Unit
        override fun setSurface(surface: Surface?, holder: SurfaceHolder?) {
            surfaces += surface
            holders += holder
        }
    }
}
