package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackContractsTest {
    @Test(timeout = 30_000)
    fun prepareIntentCarriesStreamingAudioVideoSeekAndStopBoundary() {
        val intent = PlaybackIntent.Prepare(
            audioUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
            videoUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp4"),
            videoGapSec = 0.25f,
            seekToSec = SoloSingFixtures.StartSec,
            stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        )

        assertEquals(SoloSingFixtures.StartSec, intent.seekToSec)
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, intent.stopAtLyricsTimeMs)
        assertFalse(intent.isIteration1NoOp())
    }

    @Test(timeout = 30_000)
    fun playbackEventsExposePreparedReadyErrorAndEndedContracts() {
        val prepared = PlaybackEvent.Prepared(effectivePlaybackDurationMs = 17_000L)
        val ready = PlaybackEvent.Ready(songStartTvMs = 1_234L)
        val error = PlaybackEvent.Error(PlaybackErrorCause.PlayerError("vlc warning"))

        assertEquals(17_000L, prepared.effectivePlaybackDurationMs)
        assertEquals(1_234L, ready.songStartTvMs)
        val ended: PlaybackEvent = PlaybackEvent.Ended

        assertEquals("vlc warning", (error.cause as PlaybackErrorCause.PlayerError).lastWarningOrError)
        assertEquals(PlaybackEvent.Ended, ended)
    }

    @Test(timeout = 30_000)
    fun medleyOnlyIntentsAreIteration1NoOps() {
        val prebuffer = PlaybackIntent.PrebufferNext("audio-a", null)
        val fadeOut = PlaybackIntent.FadeOut(durationMs = 500L)
        val crossfade = PlaybackIntent.Crossfade("audio-b", durationMs = 1_000L)

        assertTrue(prebuffer.isIteration1NoOp())
        assertTrue(fadeOut.isIteration1NoOp())
        assertTrue(crossfade.isIteration1NoOp())
        assertTrue(handleIteration1NoOp(prebuffer))
        assertTrue(handleIteration1NoOp(fadeOut))
        assertTrue(handleIteration1NoOp(crossfade))
        assertEquals("audio-a", prebuffer.audioUrl)
        assertNull(prebuffer.videoUrl)
        assertEquals(500L, fadeOut.durationMs)
        assertEquals("audio-b", crossfade.nextAudioUrl)
        assertEquals(1_000L, crossfade.durationMs)
    }

    @Test(timeout = 30_000)
    fun libVlcSeamKeepsAudioHandleAuthoritativeForTime() {
        val handle = FakeLibVlcPlayerHandle(timeMs = 1_500L, durationMs = 18_000L)
        val events = mutableListOf<LibVlcEvent>()
        handle.setEventListener(events::add)

        handle.prepare("http://phone/song.mp3")
        handle.play()

        assertEquals(1_500L, handle.timeMs)
        assertEquals(18_000L, handle.durationMs)
        assertEquals(listOf(LibVlcEvent.Prepared, LibVlcEvent.Playing), events)
    }

    private class FakeLibVlcPlayerHandle(
        override var timeMs: Long,
        override val durationMs: Long?,
    ) : LibVlcPlayerHandle {
        private var listener: (LibVlcEvent) -> Unit = {}

        override fun setEventListener(listener: (LibVlcEvent) -> Unit) {
            this.listener = listener
        }

        override fun prepare(url: String) {
            listener(LibVlcEvent.Prepared)
        }

        override fun play() {
            listener(LibVlcEvent.Playing)
        }

        override fun pause() = Unit
        override fun stop() = Unit
        override fun seekTo(positionMs: Long) {
            timeMs = positionMs
        }
    }
}
