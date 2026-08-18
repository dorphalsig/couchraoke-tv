package com.couchraoke.tv.presentation.playback

import android.view.Surface
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.videolan.libvlc.interfaces.IMedia

class VlcLibVlcPlayerHandleTest {
    @Test(timeout = 30_000)
    fun prepareAttachesStreamedMediaSeeksAndReadsPlayerDurationAndTime() {
        val player = FakeVlcPlayer(timeMs = 2_500L, durationMs = 123_000L)
        val mediaFactory = FakeVlcMediaFactory()
        val handle = VlcLibVlcPlayerHandle(
            player = player,
            mediaFactory = mediaFactory,
        )

        handle.prepare("http://phone/audio.mp3", seekToSec = 2.5f)

        assertEquals("http://phone/audio.mp3", mediaFactory.createdUrls.single())
        assertEquals(mediaFactory.createdMedia.single(), player.attachedMedia.single())
        assertEquals(listOf(2_500L), player.seekCalls)
        assertEquals(2_500L, handle.timeMs)
        assertEquals(123_000L, handle.durationMs)
    }

    @Test(timeout = 30_000)
    fun prepareCallsParseOnAttachedMedia() {
        val mediaFactory = FakeVlcMediaFactory()
        val handle = VlcLibVlcPlayerHandle(
            player = FakeVlcPlayer(),
            mediaFactory = mediaFactory,
        )

        handle.prepare("http://phone/audio.mp3", seekToSec = 0f)

        assertEquals(1, mediaFactory.createdMedia.single().parseCallCount)
        assertEquals(listOf(IMedia.Parse.ParseNetwork to 5_000L), mediaFactory.createdMedia.single().parseCalls)
    }

    @Test(timeout = 30_000)
    fun decorativeVideoPrepareDisablesAudioOnAttachedMedia() {
        val mediaFactory = FakeVlcMediaFactory()
        val handle = VlcLibVlcPlayerHandle(
            player = FakeVlcPlayer(),
            mediaFactory = mediaFactory,
            disableAudio = true,
        )

        handle.prepare("http://phone/video.mp4", seekToSec = 0f)

        assertEquals(listOf(":no-audio"), mediaFactory.createdMedia.single().options)
    }

    @Test(timeout = 30_000)
    fun playPauseStopSeekVolumeAndReleaseDelegateToMediaPlayerSeam() {
        val player = FakeVlcPlayer()
        val ownerReleaseCalls = mutableListOf<Unit>()
        val handle = VlcLibVlcPlayerHandle(
            player = player,
            mediaFactory = FakeVlcMediaFactory(),
            releaseOwner = { ownerReleaseCalls += Unit },
        )

        handle.play()
        handle.pause()
        handle.stop()
        handle.seekTo(42_000L)
        handle.setVolume(75)
        handle.release()

        assertEquals(1, player.playCallCount)
        assertEquals(1, player.pauseCallCount)
        assertEquals(1, player.stopCallCount)
        assertEquals(listOf(42_000L), player.seekCalls)
        assertEquals(listOf(75), player.volumeCalls)
        assertEquals(1, player.releaseCallCount)
        assertEquals(1, ownerReleaseCalls.size)
    }

    @Test(timeout = 30_000)
    fun releaseReleasesAttachedMediaAndClearsListener() {
        val player = FakeVlcPlayer()
        val mediaFactory = FakeVlcMediaFactory()
        val handle = VlcLibVlcPlayerHandle(player, mediaFactory)

        handle.prepare("http://phone/audio.mp3", seekToSec = 0f)
        handle.release()
        player.emit(VlcPlayerCallback.Playing)

        assertEquals(1, mediaFactory.createdMedia.single().releaseCallCount)
        assertNull(player.listener)
    }

    @Test(timeout = 30_000)
    fun mediaPlayerCallbacksMapToPublicLibVlcEvents() = runBlocking {
        val player = FakeVlcPlayer()
        val handle = VlcLibVlcPlayerHandle(player, FakeVlcMediaFactory())
        val deferredEvents = mutableListOf<LibVlcEvent>()

        deferredEvents += nextEvent(handle) { player.emit(VlcPlayerCallback.Playing) }
        deferredEvents += nextEvent(handle) { player.emit(VlcPlayerCallback.Paused) }
        deferredEvents += nextEvent(handle) { player.emit(VlcPlayerCallback.EndReached) }
        deferredEvents += nextEvent(handle) { player.emit(VlcPlayerCallback.TimeChanged(42_000L)) }
        deferredEvents += nextEvent(handle) { player.emit(VlcPlayerCallback.EncounteredError("latest warning")) }

        assertEquals(
            listOf(
                LibVlcEvent.Playing,
                LibVlcEvent.Paused,
                LibVlcEvent.EndReached,
                LibVlcEvent.TimeChanged(42_000L),
                LibVlcEvent.EncounteredError("latest warning"),
            ),
            deferredEvents,
        )
    }

    @Test(timeout = 30_000)
    fun nonPositivePlayerDurationIsReportedAsUnavailable() {
        val handle = VlcLibVlcPlayerHandle(
            player = FakeVlcPlayer(durationMs = 0L),
            mediaFactory = FakeVlcMediaFactory(),
        )

        assertNull(handle.durationMs)
    }

    private suspend fun nextEvent(handle: VlcLibVlcPlayerHandle, emit: () -> Unit): LibVlcEvent =
        kotlinx.coroutines.coroutineScope {
            val event = async(start = CoroutineStart.UNDISPATCHED) { handle.events.first() }
            emit()
            event.await()
        }

    private class FakeVlcMediaFactory : VlcMediaFactory {
        val createdUrls = mutableListOf<String>()
        val createdMedia = mutableListOf<FakeVlcMedia>()
        override fun create(url: String): VlcMedia {
            createdUrls += url
            return FakeVlcMedia(url).also(createdMedia::add)
        }
    }

    private class FakeVlcMedia(
        override val url: String,
    ) : VlcMedia {
        val options = mutableListOf<String>()
        val parseCalls = mutableListOf<Pair<Int, Long>>()
        val parseCallCount: Int get() = parseCalls.size
        var releaseCallCount = 0
        override fun addOption(option: String) {
            options += option
        }
        override fun parse(flags: Int, timeoutMs: Long): Boolean {
            parseCalls += flags to timeoutMs
            return true
        }
        override fun release() {
            releaseCallCount++
        }
    }

    private class FakeVlcPlayer(
        override var timeMs: Long = 0L,
        override var durationMs: Long? = 180_000L,
    ) : VlcPlayer {
        override var listener: ((VlcPlayerCallback) -> Unit)? = null
        val attachedMedia = mutableListOf<VlcMedia>()
        val seekCalls = mutableListOf<Long>()
        val volumeCalls = mutableListOf<Int>()
        var playCallCount = 0
        var pauseCallCount = 0
        var stopCallCount = 0
        var releaseCallCount = 0
        override fun setMedia(media: VlcMedia) {
            attachedMedia += media
        }
        override fun play() {
            playCallCount++
        }
        override fun pause() {
            pauseCallCount++
        }
        override fun stop() {
            stopCallCount++
        }
        override fun seekTo(positionMs: Long) {
            seekCalls += positionMs
            timeMs = positionMs
        }
        override fun setVolume(percent: Int) {
            volumeCalls += percent
        }
        override fun setSurface(surface: Surface?, holder: SurfaceHolder?) = Unit
        override fun release() {
            releaseCallCount++
        }
        fun emit(callback: VlcPlayerCallback) {
            listener?.invoke(callback)
        }
    }
}
