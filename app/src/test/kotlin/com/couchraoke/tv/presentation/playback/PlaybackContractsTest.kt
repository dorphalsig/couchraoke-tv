package com.couchraoke.tv.presentation.playback

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackContractsTest {
    @Test(timeout = 30_000)
    fun prepareIntentCarriesOnlyDocumentedStreamingMediaAndSeekFields() {
        val intent = PlaybackIntent.Prepare(
            audioUrl = SoloSingFixtures.PlaybackAudioUrl,
            videoUrl = SoloSingFixtures.PlaybackVideoUrl,
            videoGapSec = 0.25f,
            seekToSec = SoloSingFixtures.StartSec,
        )
        val play = PlaybackIntent.Play

        assertEquals(SoloSingFixtures.PlaybackAudioUrl, intent.audioUrl)
        assertEquals(SoloSingFixtures.PlaybackVideoUrl, intent.videoUrl)
        assertEquals(0.25f, intent.videoGapSec)
        assertEquals(SoloSingFixtures.StartSec, intent.seekToSec)
        assertFalse(intent.toString().contains("chartEndLyricsTimeMs"))
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
        assertEquals(null, prebuffer.videoUrl)
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
        assertEquals(null, prepare.copy(videoUrl = null).videoUrl)
        assertEquals(PlaybackIntent.Play, PlaybackIntent.Play)
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
    fun libVlcSeamExposesSpecEventStreamAndPlaybackOperations() {
        val handle = FakeLibVlcPlayerHandle(timeMs = 1_500L, durationMs = 18_000L)

        handle.prepare("http://phone/song.mp3", seekToSec = 2.5f)
        handle.play()
        handle.pause()
        handle.stop()
        handle.seekTo(4_000L)
        handle.setVolume(75)
        handle.release()

        assertEquals(4_000L, handle.timeMs)
        assertEquals(18_000L, handle.durationMs)
        assertEquals("http://phone/song.mp3", handle.preparedUrl)
        assertEquals(2.5f, handle.preparedSeekSec)
        assertEquals(75, handle.volumePercent)
        assertTrue(handle.released)
    }

    @Test(timeout = 30_000)
    fun songPreviewContractUsesDebouncedScreenScopedAudioPreviewState() {
        val state = PreviewPlaybackState(
            songId = SoloSingFixtures.SongId,
            audioUrl = SoloSingFixtures.PlaybackAudioUrl,
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
    ) : LibVlcPlayerHandle, PreparedDurationProvider {
        override val events = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = 8)
        var preparedUrl: String? = null
        var preparedSeekSec: Float? = null
        var volumePercent: Int? = null
        var released = false

        override fun prepare(mediaUrl: String, seekToSec: Float) {
            preparedUrl = mediaUrl
            preparedSeekSec = seekToSec
        }

        override fun play() {
            events.tryEmit(LibVlcEvent.Playing)
        }

        override fun pause() = Unit
        override fun stop() = Unit

        override fun seekTo(positionMs: Long) {
            timeMs = positionMs
        }

        override fun setVolume(percent: Int) {
            volumePercent = percent
        }

        override fun release() {
            released = true
        }
    }
}
