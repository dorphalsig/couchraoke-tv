package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
            stopAtLyricsTimeMs = { SoloSingFixtures.PreparedDurationMs },
        )

        controller.handle(PlaybackIntent.Play)
        controller.handle(PlaybackIntent.Pause)
        controller.handle(PlaybackIntent.Seek(positionMs = SoloSingFixtures.RestartSeekPositionMs))
        controller.handle(PlaybackIntent.Stop)

        assertEquals(1, handle.playCallCount)
        assertEquals(1, handle.pauseCallCount)
        assertEquals(SoloSingFixtures.RestartSeekPositionMs, handle.timeMs)
        assertEquals(1, handle.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun playRequestsAudioFocusAndDenialEmitsErrorWithoutPlaying() {
        val handle = FakeHandle()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = FakeAudioFocusController(shouldGrant = false),
        )

        controller.handle(PlaybackIntent.Play)

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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play)

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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Play)
        assertEquals(1, focus.requestCallCount)
        assertEquals(0, focus.abandonCallCount)

        controller.handle(PlaybackIntent.Stop)
        assertEquals(1, focus.abandonCallCount)

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                1.5f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
        assertEquals(2, focus.requestCallCount)
        assertEquals(1, focus.abandonCallCount)
    }

    @Test(timeout = 30_000)
    fun permanentAudioFocusLossAbandonsFocusAndEmitsError() {
        val handle = FakeHandle()
        val focus = FakeAudioFocusController()
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play)

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

    private class FakeHandle : LibVlcPlayerHandle, PreparedDurationProvider {
        override var timeMs: Long = 0L
        override val durationMs: Long? = 180_000L
        override val events = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = 8)
        var playCallCount = 0
        var pauseCallCount = 0
        var stopCallCount = 0
        override fun prepare(mediaUrl: String, seekToSec: Float) = Unit
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
        override fun setVolume(percent: Int) = Unit
        override fun release() = Unit
    }
}
