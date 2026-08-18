package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerTest {
    @Test(timeout = 30_000)
    fun prepareDelegatesDocumentedMediaSeekAndEmitsPrepared() = runBlocking {
        val handle = FakeHandle(duration = SoloSingFixtures.ReportedPlaybackDurationMs)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { SoloSingFixtures.PlaybackClockMs },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                2.5f,
            ),
        )
        controller.handle(PlaybackIntent.Play)

        assertEquals(SoloSingFixtures.PlaybackAudioUrl, handle.preparedUrl)
        assertEquals(2.5f, handle.preparedSeekSec)
        assertTrue(handle.preparedBeforePlay)
        assertEquals(
            PlaybackEvent.Prepared(SoloSingFixtures.ReportedPlaybackDurationMs),
            controller.events.single(),
        )
    }

    @Test(timeout = 30_000)
    fun firstPlayingComputesSongStartTvMsFromClockMinusHandleTime() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { SoloSingFixtures.PlaybackReadyClockMs },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                2.5f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
        handle.emit(LibVlcEvent.Playing)

        assertEquals(PlaybackEvent.Ready(7_500L), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun fallbackReadyUsesPlayTimeWhenPlayingDoesNotArriveWithin500Ms() = runBlocking {
        var now = SoloSingFixtures.PlaybackFallbackStartMs
        val controller = DefaultPlaybackController(
            audioHandle = FakeHandle(),
            clockMs = { now },
            audioFocusController = FakeAudioFocusController(),
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
        now = SoloSingFixtures.PlaybackFallbackReadyMs
        controller.tick()

        assertEquals(PlaybackEvent.Ready(SoloSingFixtures.PlaybackFallbackStartMs), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun tickEmitsFallbackReadyAndEnforcesStopBoundaryFromCoordinatorState() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        var now = SoloSingFixtures.PlaybackClockMs
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { now },
            audioFocusController = FakeAudioFocusController(),
            stopAtLyricsTimeMs = { SoloSingFixtures.PreparedDurationMs },
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
        now = SoloSingFixtures.PlaybackClockMs + 500L
        controller.tick()
        handle.timeMs = SoloSingFixtures.PreparedDurationMs
        controller.tick()

        assertTrue(handle.stopped)
        assertEquals(
            listOf(
                PlaybackEvent.Prepared(SoloSingFixtures.FallbackPlaybackDurationMs),
                PlaybackEvent.Ready(SoloSingFixtures.PlaybackClockMs),
                PlaybackEvent.Ended,
            ),
            controller.events,
        )
    }

    @Test(timeout = 30_000)
    fun pauseStopAndSeekIntentsCallUnderlyingHandle() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(PlaybackIntent.Pause)
        controller.handle(PlaybackIntent.Seek(positionMs = SoloSingFixtures.SeekPositionMs))
        controller.handle(PlaybackIntent.Stop)

        assertTrue(handle.paused)
        assertTrue(handle.stopped)
        assertEquals(SoloSingFixtures.SeekPositionMs, handle.timeMs)
        assertEquals(SoloSingFixtures.SeekPositionMs, controller.currentPositionMs)
    }

    @Test(timeout = 30_000)
    fun medleyOnlyIntentsRemainNoOpsInIteration1() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(
            PlaybackIntent.PrebufferNext(
                SoloSingFixtures.NextPreviewAudioUrl,
                videoUrl = null,
                seekToSec = 0f,
            ),
        )
        controller.handle(PlaybackIntent.FadeOut(durationSec = 0.3f))
        controller.handle(PlaybackIntent.Crossfade(fadeOutSec = 0.3f, fadeInSec = 0.3f))

        assertTrue(controller.events.isEmpty())
        assertEquals(0, handle.playCallCount)
    }

    @Test(timeout = 30_000)
    fun fallbackDoesNothingBeforePlayOrWhenReadyAlreadyEmitted() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        var now = SoloSingFixtures.PlaybackReadyClockMs
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { now },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.tick()
        assertTrue(controller.events.isEmpty())

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
        handle.emit(LibVlcEvent.Playing)
        now = SoloSingFixtures.PlaybackReadyClockMs + SoloSingFixtures.PlaybackClockMs
        controller.tick()

        assertEquals(2, controller.events.size)
        assertEquals(PlaybackEvent.Ready(SoloSingFixtures.PlaybackReadyClockMs), controller.events.last())
    }

    @Test(timeout = 30_000)
    fun audioEventsEmitEndedAndPlayerErrorAndIgnorePausedAndTimeChanged() = runBlocking {
        val handle = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { SoloSingFixtures.PlaybackReadyClockMs },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )
        handle.emit(LibVlcEvent.Paused)
        handle.emit(LibVlcEvent.TimeChanged(123L))
        handle.emit(LibVlcEvent.EndReached)
        handle.emit(LibVlcEvent.EncounteredError(SoloSingFixtures.LastWarningLine))

        assertEquals(PlaybackEvent.Ended, controller.events[1])
        assertEquals(
            PlaybackEvent.Error(
                PlaybackErrorCause.PlayerError(lastWarningOrError = SoloSingFixtures.LastWarningLine),
            ),
            controller.events[2],
        )
    }

    @Test(timeout = 30_000)
    fun stopBoundaryDoesNothingBeforePreparedBoundaryOrBelowThreshold() = runBlocking {
        val handle = FakeHandle(timeMs = SoloSingFixtures.PlaybackFallbackStartMs)
        val controller = DefaultPlaybackController(
            audioHandle = handle,
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
            stopAtLyricsTimeMs = { SoloSingFixtures.PreparedDurationMs },
        )

        controller.tick()
        assertFalse(handle.stopped)

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )
        controller.tick()
        assertFalse(handle.stopped)
    }

    @Test(timeout = 30_000)
    fun tickResyncsDecorativeVideoToAuthoritativeAudioTime() = runBlocking {
        val audio = FakeHandle(timeMs = SoloSingFixtures.AuthoritativeAudioTimeMs)
        val video = FakeHandle(timeMs = 0L)
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            videoHandle = video,
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
            stopAtLyricsTimeMs = { SoloSingFixtures.PreparedDurationMs },
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                SoloSingFixtures.PlaybackAviVideoUrl,
                -4.1f,
                0f,
            ),
        )
        controller.handle(PlaybackIntent.Play)
        audio.timeMs = SoloSingFixtures.AuthoritativeAudioTimeMs
        video.timeMs = SoloSingFixtures.InitialVideoTimeMs
        controller.tick()

        assertEquals(SoloSingFixtures.SyncedVideoTimeMs, video.timeMs)
    }

    @Test(timeout = 30_000)
    fun prepareWithoutVideoClearsDecorativeVideoAvailability() = runBlocking {
        val fallbackStates = mutableListOf<Boolean>()
        val controller = DefaultPlaybackController(
            audioHandle = FakeHandle(),
            videoHandle = FakeHandle(),
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
            onDecorativeVideoAvailableChanged = fallbackStates::add,
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )

        assertEquals(listOf(false), fallbackStates)
    }

    @Test(timeout = 30_000)
    fun prepareFailsWhenAudioDurationIsUnavailable() = runBlocking {
        val controller = DefaultPlaybackController(
            audioHandle = FakeHandle(duration = null),
            clockMs = { 0L },
            audioFocusController = FakeAudioFocusController(),
        )

        controller.handle(
            PlaybackIntent.Prepare(
                SoloSingFixtures.PlaybackAudioUrl,
                null,
                null,
                0f,
            ),
        )

        assertEquals(
            PlaybackEvent.Error(PlaybackErrorCause.AudioUnavailable("No usable playback duration.")),
            controller.events.single(),
        )
    }

    private class FakeAudioFocusController : AudioFocusController {
        override fun requestAudioFocus(): Boolean = true
        override fun abandonAudioFocus() = Unit
    }

    private class FakeHandle(
        override var timeMs: Long = 0L,
        duration: Long? = SoloSingFixtures.FallbackPlaybackDurationMs,
    ) : LibVlcPlayerHandle, PreparedDurationProvider {
        override var durationMs: Long? = duration
        override val events = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = 8)
        var preparedUrl: String? = null
        var preparedSeekSec: Float? = null
        var preparedBeforePlay = false
        var playCallCount = 0
        var paused = false
        var stopped = false
        override fun prepare(mediaUrl: String, seekToSec: Float) {
            preparedUrl = mediaUrl
            preparedSeekSec = seekToSec
            timeMs = (seekToSec * 1_000).toLong()
        }
        override fun play() {
            preparedBeforePlay = preparedUrl != null
            playCallCount++
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
        override fun setVolume(percent: Int) = Unit
        override fun release() = Unit
        fun emit(event: LibVlcEvent) {
            events.tryEmit(event)
        }
    }
}
