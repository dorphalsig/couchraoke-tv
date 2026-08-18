package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )
        val warning = SoloSingFixtures.longWarning()

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )

        controller.handle(PlaybackIntent.Play)

        assertEquals(0, audio.playCallCount)
        assertEquals(1, focus.abandonCallCount)
        assertEquals(PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied()), controller.events.single())
    }

    @Test(timeout = 30_000)
    fun transientLossPausesAndGainResumesOnlyFocusPausedPlayback() {
        val audio = FakeHandle()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = FakeAudioFocusController(),
        )
        controller.handle(PlaybackIntent.Play)

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
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = focus,
        )
        controller.handle(PlaybackIntent.Play)

        controller.onAudioFocusChanged(AudioFocusChange.PermanentLoss)

        assertEquals(1, audio.stopCallCount)
        assertEquals(1, focus.abandonCallCount)
        assertTrue(controller.events.last() is PlaybackEvent.Error)
    }

    @Test(timeout = 30_000)
    fun decorativeVideoErrorFallsBackWithoutStoppingAudioOrEmittingPlaybackError() {
        val audio = FakeHandle()
        val video = FakeHandle()
        val fallbackStates = mutableListOf<Boolean>()
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            videoHandle = video,
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = FakeAudioFocusController(),
            onDecorativeVideoAvailableChanged = fallbackStates::add,
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                SoloSingFixtures.PlaybackVideoUrl,
                null,
                0f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
        video.emit(LibVlcEvent.EncounteredError(SoloSingFixtures.VideoFailureMessage))

        assertFalse(controller.events.any { it is PlaybackEvent.Error })
        assertEquals(1, audio.playCallCount)
        assertEquals(0, audio.stopCallCount)
        assertEquals(1, video.stopCallCount)
        assertEquals(1, video.releaseCallCount)
        assertEquals(listOf(false, true, false), fallbackStates)
    }

    @Test(timeout = 30_000)
    fun medleyOnlyNoOpsAreSafeOnErrorPath() {
        val controller = DefaultPlaybackController(
            audioHandle = FakeHandle(),
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(PlaybackIntent.PrebufferNext(SoloSingFixtures.NextPreviewAudioUrl, seekToSec = 0f))
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

    private class FakeHandle : LibVlcPlayerHandle, PreparedDurationProvider {
        override var timeMs: Long = 0L
        override val durationMs: Long? = 180_000L
        override val events = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = 8)
        var playCallCount = 0
        var pauseCallCount = 0
        var stopCallCount = 0
        var releaseCallCount = 0
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
        override fun release() {
            releaseCallCount++
        }
        fun emit(event: LibVlcEvent) {
            events.tryEmit(event)
        }
    }
}
