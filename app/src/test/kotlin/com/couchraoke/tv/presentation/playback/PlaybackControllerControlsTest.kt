package com.couchraoke.tv.presentation.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerControlsTest {
    @Test(timeout = 30_000)
    fun pauseResumeStopAndRestartSeekControlUnderlyingHandle() {
        val handle = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        controller.handle(PlaybackIntent.Pause)
        controller.handle(PlaybackIntent.Seek(positionMs = 1_500L))
        controller.handle(PlaybackIntent.Stop)

        assertEquals(1, handle.playCallCount)
        assertEquals(1, handle.pauseCallCount)
        assertEquals(1_500L, handle.timeMs)
        assertEquals(1, handle.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun playRequestsAudioFocusAndDenialEmitsErrorWithoutPlaying() {
        val handle = FakeHandle()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 1_000L },
            audioFocusController = FakeAudioFocusController(shouldGrant = false),
        )

        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        assertEquals(0, handle.playCallCount)
        assertEquals(
            PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied()),
            controller.events.single(),
        )
    }

    @Test(timeout = 30_000)
    fun transientAudioFocusLossPausesAndGainResumesOnlyFocusPausedPlayback() {
        val handle = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        controller.onAudioFocusChanged(AudioFocusChange.TransientLoss)
        controller.onAudioFocusChanged(AudioFocusChange.Gain)

        assertEquals(1, handle.pauseCallCount)
        assertEquals(2, handle.playCallCount)

        controller.handle(PlaybackIntent.Pause)
        controller.onAudioFocusChanged(AudioFocusChange.Gain)

        assertEquals(2, handle.playCallCount)
    }

    @Test(timeout = 30_000)
    fun restartAbandonsAudioFocusThenReRequestsOnSubsequentPlay() {
        val handle = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        assertEquals(1, focus.requestCallCount)
        assertEquals(0, focus.abandonCallCount)

        // Restart: Stop abandons, Prepare resets timing, Play re-requests.
        controller.handle(PlaybackIntent.Stop)
        assertEquals(1, focus.abandonCallCount)

        controller.handle(PlaybackIntent.Prepare("http://phone/audio.mp3", null, null, 1.5f, 120_000L))
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))
        assertEquals(2, focus.requestCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun permanentAudioFocusLossAbandonsFocusAndEmitsError() {
        val handle = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 1_000L },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 120_000L))

        controller.onAudioFocusChanged(AudioFocusChange.PermanentLoss)

        assertEquals(1, handle.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
        assertTrue(controller.events.last() is PlaybackEvent.Error)
    }

    private class FakeAudioFocusController(
        private val shouldGrant: Boolean = true,
    ) : AudioFocusController {
        var requestCallCount = 0
        var abandonCallCount = 0
        override fun requestAudioFocus(): Boolean {
            requestCallCount++
            return shouldGrant
        }
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
        override fun setEventListener(listener: (LibVlcEvent) -> Unit) = Unit
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
    }
}
