package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackContractsTest {
    @Test(timeout = 30_000)
    fun prepareIntentCarriesStreamingAudioVideoSeekAndChartBoundary() {
        val intent = PlaybackIntent.Prepare(
            audioUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
            videoUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp4"),
            videoGapSec = 0.25f,
            seekToSec = SoloSingFixtures.StartSec,
            chartEndLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        )
        val play = PlaybackIntent.Play(stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs)

        assertEquals(SoloSingFixtures.StartSec, intent.seekToSec)
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, intent.chartEndLyricsTimeMs)
        assertEquals(SoloSingFixtures.StopAtLyricsTimeMs, play.stopAtLyricsTimeMs)
        assertFalse(intent.isIteration1NoOp())
        assertFalse(play.isIteration1NoOp())
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
        val prebuffer = PlaybackIntent.PrebufferNext("audio-a", videoUrl = null, videoGapSec = 0.5f, seekToSec = 1.25f)
        val fadeOut = PlaybackIntent.FadeOut(durationSec = 0.5f)
        val crossfade = PlaybackIntent.Crossfade(fadeOutSec = 1.0f, fadeInSec = 1.25f)

        assertTrue(prebuffer.isIteration1NoOp())
        assertTrue(fadeOut.isIteration1NoOp())
        assertTrue(crossfade.isIteration1NoOp())
        assertTrue(handleIteration1NoOp(prebuffer))
        assertTrue(handleIteration1NoOp(fadeOut))
        assertTrue(handleIteration1NoOp(crossfade))
        assertEquals("audio-a", prebuffer.audioUrl)
        assertNull(prebuffer.videoUrl)
        assertEquals(0.5f, prebuffer.videoGapSec)
        assertEquals(1.25f, prebuffer.seekToSec)
        assertEquals(0.5f, fadeOut.durationSec)
        assertEquals(1.0f, crossfade.fadeOutSec)
        assertEquals(1.25f, crossfade.fadeInSec)
    }

    @Test(timeout = 30_000)
    fun playbackErrorCausesCarryAudioFailureAndFocusDenialPayloads() {
        val audioUnavailable = PlaybackErrorCause.AudioUnavailable("duration missing")
        val focusDenied = PlaybackErrorCause.AudioFocusDenied("focus denied")
        val playerError = PlaybackErrorCause.PlayerError("vlc warning")

        assertEquals("duration missing", audioUnavailable.message)
        assertEquals("focus denied", focusDenied.message)
        assertEquals("vlc warning", playerError.lastWarningOrError)
        assertEquals(null, PlaybackErrorCause.AudioFocusDenied().message)
        assertEquals(null, PlaybackErrorCause.PlayerError().lastWarningOrError)
        assertNotEquals(audioUnavailable, PlaybackErrorCause.AudioUnavailable())
        assertNotEquals(focusDenied, PlaybackErrorCause.AudioFocusDenied())
        assertNotEquals(playerError, PlaybackErrorCause.PlayerError())
    }

    @Test(timeout = 30_000)
    fun playbackContractModelsKeepPublicCopyAndIdentitySemantics() {
        val prepare = PlaybackIntent.Prepare(
            audioUrl = "audio-a",
            videoUrl = "video-a",
            videoGapSec = 0.25f,
            seekToSec = 1f,
            chartEndLyricsTimeMs = 20_000L,
        )
        val seek = PlaybackIntent.Seek(positionMs = 4_000L)
        val preview = PreviewPlaybackState(
            songId = "song-a",
            audioUrl = "audio-a",
            startPositionMs = 2_000L,
        )
        val prebuffer = PlaybackIntent.PrebufferNext(
            audioUrl = "audio-a",
            videoUrl = "video-a",
            videoGapSec = 0.5f,
            seekToSec = 3f,
        )

        assertEquals("audio-b", prepare.copy(audioUrl = "audio-b").audioUrl)
        assertEquals(null, prepare.copy(videoUrl = null, chartEndLyricsTimeMs = null).chartEndLyricsTimeMs)
        assertEquals(4_000L, seek.positionMs)
        assertEquals(6_000L, seek.copy(positionMs = 6_000L).positionMs)
        assertEquals(500L, preview.debounceMs)
        assertEquals(750L, preview.copy(debounceMs = 750L).debounceMs)
        assertEquals("video-a", prebuffer.videoUrl)
        assertEquals("audio-b", prebuffer.copy(audioUrl = "audio-b").audioUrl)
        assertNotEquals(prepare, prepare.copy(seekToSec = 2f))
        assertTrue(prepare.toString().contains("audio-a"))
        assertTrue(preview.toString().contains("song-a"))
    }

    @Test(timeout = 30_000)
    fun libVlcSeamKeepsAudioHandleAuthoritativeForTime() {
        val handle = FakeLibVlcPlayerHandle(timeMs = 1_500L, durationMs = 18_000L)
        val events = mutableListOf<LibVlcEvent>()
        handle.setEventListener(events::add)

        handle.prepare("http://phone/song.mp3")
        handle.play()
        handle.release()

        assertEquals(1_500L, handle.timeMs)
        assertEquals(18_000L, handle.durationMs)
        assertTrue(handle.released)
        assertEquals(listOf(LibVlcEvent.Prepared, LibVlcEvent.Playing), events)
    }

    @Test(timeout = 30_000)
    fun vlcHandleAdapterExposesPreparePlayPauseStopSeekAndReleaseEvents() {
        val handle = VlcLibVlcPlayerHandle()
        val events = mutableListOf<LibVlcEvent>()
        handle.setEventListener(events::add)

        handle.prepare("http://phone/song.mp3")
        handle.seekTo(42_000L)
        handle.play()
        handle.pause()
        handle.stop()
        handle.release()
        handle.play()

        assertEquals(42_000L, handle.timeMs)
        assertEquals(0L, handle.durationMs)
        assertEquals(
            listOf(LibVlcEvent.Prepared, LibVlcEvent.Playing, LibVlcEvent.Paused, LibVlcEvent.Stopped),
            events,
        )
    }

    @Test(timeout = 30_000)
    fun songPreviewContractUsesDebouncedScreenScopedAudioPreviewState() {
        val state = PreviewPlaybackState(
            songId = SoloSingFixtures.SongId,
            audioUrl = SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"),
            startPositionMs = previewStartPositionMs(SoloSingFixtures.PreviewStartSec),
        )

        assertEquals(500L, state.debounceMs)
        assertEquals(12_000L, state.startPositionMs)
        assertEquals(0L, previewStartPositionMs(null))
        assertEquals(0L, previewStartPositionMs(0f))
        assertEquals(0L, previewStartPositionMs(-1f))
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
        var released = false

        override fun seekTo(positionMs: Long) {
            timeMs = positionMs
        }

        override fun release() {
            released = true
        }
    }
}
