package com.couchraoke.tv.presentation.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerErrorTest {
    @Test(timeout = 30_000)
    fun audioLibVlcErrorTruncatesLatestWarningStopsAudioAndAbandonsFocus() {
        val audio = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )
        val warning = "w".repeat(140)

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        audio.emit(LibVlcEvent.EncounteredError(warning))

        val error = controller.events.last() as PlaybackEvent.Error
        val cause = error.cause as PlaybackErrorCause.PlayerError
        assertEquals(120, cause.lastWarningOrError?.length)
        assertEquals(warning.take(120), cause.lastWarningOrError)
        assertEquals(1, audio.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun audioEndStopsAudioAndAbandonsFocus() {
        val audio = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 0f, 120_000L))
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        audio.emit(LibVlcEvent.EndReached)

        assertEquals(PlaybackEvent.Ended, controller.events.last())
        assertEquals(1, audio.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun audioFocusDenialEmitsErrorWithoutPlayingAndAbandonsFocus() {
        val audio = FakeHandle()
        val focus = FakeAudioFocusController(shouldGrant = false)
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        assertEquals(0, audio.playCallCount)
        assertEquals(1, focus.abandonCallCount)
        assertEquals(PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied()), controller.events.single())
    }

    @Test(timeout = 30_000)
    fun transientLossPausesAndGainResumesOnlyFocusPausedPlayback() {
        val audio = FakeHandle()
        val controller = DefaultPlaybackController(audioHandle = audio, clockMs = { 1_000L })
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        controller.onAudioFocusChanged(AudioFocusChange.TransientLoss)
        controller.onAudioFocusChanged(AudioFocusChange.Gain)
        assertEquals(1, audio.pauseCallCount)
        assertEquals(2, audio.playCallCount)

        controller.handle(PlaybackIntent.Pause)
        controller.onAudioFocusChanged(AudioFocusChange.Gain)

        assertEquals(2, audio.pauseCallCount)
        assertEquals(2, audio.playCallCount)
    }

    @Test(timeout = 30_000)
    fun permanentLossStopsAudioAbandonsFocusAndEmitsError() {
        val audio = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        controller.onAudioFocusChanged(AudioFocusChange.PermanentLoss)

        assertEquals(1, audio.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
        assertTrue(controller.events.last() is PlaybackEvent.Error)
    }

    @Test(timeout = 30_000)
    fun decorativeVideoErrorFallsBackWithoutStoppingAudioOrEmittingPlaybackError() {
        val audio = FakeHandle()
        val video = FakeHandle()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            videoHandle = video,
            clockMs = { 1_000L },
        )

        controller.handle(
            PlaybackIntent.Prepare(
                "http://phone/audio.mp3",
                "http://phone/video.mp4",
                null,
                0f,
                120_000L,
            ),
        )
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        video.emit(LibVlcEvent.EncounteredError("video failed"))

        assertFalse(controller.events.any { it is PlaybackEvent.Error })
        assertEquals(0, audio.stopCallCount)
        assertEquals(1, video.stopCallCount)
    }

    @Test(timeout = 30_000)
    fun medleyOnlyNoOpsAreSafeOnErrorPath() {
        val controller = DefaultPlaybackController(audioHandle = FakeHandle(), clockMs = { 1_000L })

        controller.handle(PlaybackIntent.PrebufferNext("next.mp3", seekToSec = 0f))
        controller.handle(PlaybackIntent.FadeOut(durationSec = 0.2f))
        controller.handle(PlaybackIntent.Crossfade(fadeOutSec = 0.2f, fadeInSec = 0.2f))

        assertTrue(controller.events.isEmpty())
    }

    private class FakeAudioFocusController(
        private val shouldGrant: Boolean = true,
    ) : AudioFocusController {
        var abandonCallCount = 0
        override fun requestAudioFocus(): Boolean = shouldGrant
        override fun abandonAudioFocus() {
            abandonCallCount++
        }
    }

    private class FakeHandle : LibVlcPlayerHandle {
        override var timeMs: Long = 0L
        override val durationMs: Long? = 180_000L
        var playCallCount = 0
        var pauseCallCount = 0
        var stopCallCount = 0
        private var listener: ((LibVlcEvent) -> Unit)? = null
        override fun setEventListener(listener: (LibVlcEvent) -> Unit) {
            this.listener = listener
        }
        override fun prepare(url: String) = Unit
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
            timeMs = positionMs
        }
        override fun release() = Unit
        fun emit(event: LibVlcEvent) {
            listener?.invoke(event)
        }
    }
}
