package com.couchraoke.tv.presentation.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerTest {
    @Test(timeout = 30_000)
    fun prepareRegistersSingleListenerBeforePlayAndEmitsPrepared() = runBlocking {
        val handle = FakeHandle(duration = 123_000L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 1_000L })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.handle(PlaybackIntent.Play(120_000L))

        assertEquals(1, handle.listenerRegistrationCount)
        assertTrue(handle.listenerRegisteredBeforePlay)
        assertEquals(PlaybackEvent.Prepared(123_000L), controller.events.single())
    }

    @Test(timeout = 30_000)
    fun firstPlayingComputesSongStartTvMsFromClockMinusHandleTime() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 10_000L })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 2.5f, 120_000L))
        controller.handle(PlaybackIntent.Play(120_000L))
        handle.emit(LibVlcEvent.Playing)

        assertEquals(PlaybackEvent.Ready(7_500L), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun fallbackReadyUsesPlayTimeWhenPlayingDoesNotArriveWithin500Ms() = runBlocking {
        var now = 5_000L
        val controller = DefaultPlaybackController(audioHandle = FakeHandle(), clockMs = { now })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.handle(PlaybackIntent.Play(120_000L))
        now = 5_500L
        controller.tick()

        assertEquals(PlaybackEvent.Ready(5_000L), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun tickEmitsFallbackReadyAndEnforcesStopBoundary() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        var now = 1_000L
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { now })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, null))
        controller.handle(PlaybackIntent.Play(120_000L))
        now = 1_500L
        controller.tick()
        handle.timeMs = 120_000L
        controller.tick()

        assertTrue(handle.stopped)
        assertEquals(
            listOf(PlaybackEvent.Prepared(180_000L), PlaybackEvent.Ready(1_000L), PlaybackEvent.Ended),
            controller.events,
        )
    }

    @Test(timeout = 30_000)
    fun pauseStopAndSeekIntentsCallUnderlyingHandle() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 0L })

        controller.handle(PlaybackIntent.Pause)
        controller.handle(PlaybackIntent.Seek(positionMs = 42_000L))
        controller.handle(PlaybackIntent.Stop)

        assertTrue(handle.paused)
        assertTrue(handle.stopped)
        assertEquals(42_000L, handle.timeMs)
        assertEquals(42_000L, controller.currentPositionMs)
    }

    @Test(timeout = 30_000)
    fun medleyOnlyIntentsRemainNoOpsInIteration1() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 0L })

        controller.handle(PlaybackIntent.PrebufferNext("next.mp3", videoUrl = null, seekToSec = 0f))
        controller.handle(PlaybackIntent.FadeOut(durationSec = 0.3f))
        controller.handle(PlaybackIntent.Crossfade(fadeOutSec = 0.3f, fadeInSec = 0.3f))

        assertTrue(controller.events.isEmpty())
        assertEquals(0, handle.playCallCount)
    }

    @Test(timeout = 30_000)
    fun fallbackDoesNothingBeforePlayOrWhenReadyAlreadyEmitted() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        var now = 10_000L
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { now })

        controller.tick()
        assertTrue(controller.events.isEmpty())

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.handle(PlaybackIntent.Play(120_000L))
        handle.emit(LibVlcEvent.Playing)
        now = 11_000L
        controller.tick()

        assertEquals(2, controller.events.size)
        assertEquals(PlaybackEvent.Ready(10_000L), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun audioEventsEmitEndedAndPlayerErrorAndIgnorePreparedPausedStopped() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 10_000L })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        handle.emit(LibVlcEvent.Prepared)
        handle.emit(LibVlcEvent.Paused)
        handle.emit(LibVlcEvent.Stopped)
        handle.emit(LibVlcEvent.EndReached)
        handle.emit(LibVlcEvent.EncounteredError("last warning"))

        assertEquals(PlaybackEvent.Ended, controller.events[1])
        assertEquals(
            PlaybackEvent.Error(
                PlaybackErrorCause.PlayerError(lastWarningOrError = "last warning"),
            ),
            controller.events[2],
        )
    }

    @Test(timeout = 30_000)
    fun stopBoundaryDoesNothingBeforePreparedBoundaryOrBelowThreshold() = runBlocking {
        val handle = FakeHandle(timeMs = 5_000L)
        val controller = DefaultPlaybackController(audioHandle = handle, clockMs = { 0L })

        controller.tick()
        assertFalse(handle.stopped)

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.tick()
        assertFalse(handle.stopped)
    }

    @Test(timeout = 30_000)
    fun prepareFailsWhenNeitherChartBoundaryNorAudioDurationIsUsable() = runBlocking {
        val controller = DefaultPlaybackController(audioHandle = FakeHandle(duration = null), clockMs = { 0L })

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, null))

        assertEquals(
            PlaybackEvent.Error(PlaybackErrorCause.AudioUnavailable("No usable playback duration.")),
            controller.events.single(),
        )
    }

    @Test(timeout = 30_000)
    fun vlcHandleAdapterEmitsEventsAndClampsSeekUntilReleased() {
        val handle = VlcLibVlcPlayerHandle()
        val events = mutableListOf<LibVlcEvent>()
        handle.setEventListener(events::add)

        handle.prepare("http://phone/audio.mp3")
        handle.seekTo(-1_000L)
        handle.play()
        handle.pause()
        handle.stop()
        handle.release()
        handle.play()

        assertEquals(0L, handle.timeMs)
        assertEquals(0L, handle.durationMs)
        assertEquals(
            listOf(LibVlcEvent.Prepared, LibVlcEvent.Playing, LibVlcEvent.Paused, LibVlcEvent.Stopped),
            events,
        )
    }

    @Test(timeout = 30_000)
    fun vlcHandleRejectsBlankPrepareUrl() {
        val handle = VlcLibVlcPlayerHandle()

        try {
            handle.prepare("")
            throw AssertionError("Expected blank URL rejection")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("Failed requirement") == true)
        }
    }

    private class FakeHandle(
        override var timeMs: Long = 0L,
        duration: Long? = 180_000L,
    ) : LibVlcPlayerHandle {
        override var durationMs: Long? = duration
        var listenerRegistrationCount = 0
        var listenerRegisteredBeforePlay = false
        var playCallCount = 0
        var paused = false
        var stopped = false
        private var listener: ((LibVlcEvent) -> Unit)? = null
        override fun setEventListener(listener: (LibVlcEvent) -> Unit) {
            listenerRegistrationCount++
            this.listener = listener
        }
        override fun prepare(url: String) = Unit
        override fun play() {
            playCallCount++
            listenerRegisteredBeforePlay = listener != null
        }
        override fun pause() {
            paused = true
        }
        override fun stop() {
            stopped = true
        }
        override fun seekTo(positionMs: Long) {
            timeMs = positionMs
        }
        override fun release() = Unit
        fun emit(event: LibVlcEvent) {
            listener?.invoke(event)
        }
    }
}
