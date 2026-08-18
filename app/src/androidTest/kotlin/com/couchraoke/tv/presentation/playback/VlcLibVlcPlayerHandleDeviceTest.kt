package com.couchraoke.tv.presentation.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VlcLibVlcPlayerHandleDeviceTest {
    @Test(timeout = 45_000)
    fun realMediaCapturesLibVlcEventsAndKeepsAudioVideoPlayersInSync() {
        val arguments = InstrumentationRegistry.getArguments()
        val audioUri = arguments.getString("audioUri").orEmpty()
        val videoUri = arguments.getString("videoUri").orEmpty()
        assumeTrue("audioUri instrumentation argument is required", audioUri.isNotBlank())
        assumeTrue("videoUri instrumentation argument is required", videoUri.isNotBlank())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        verifyRealMediaEvents(context, audioUri, videoUri)
        verifyControllerSyncsVideoToAudio(context, audioUri, videoUri)
    }

    private fun verifyRealMediaEvents(
        context: android.content.Context,
        audioUri: String,
        videoUri: String,
    ) {
        val audio = VlcLibVlcPlayerHandle(context)
        val video = VlcLibVlcPlayerHandle(context, disableAudio = true)
        val audioPlaying = CountDownLatch(1)
        val videoPlaying = CountDownLatch(1)
        val audioEvents = mutableListOf<LibVlcEvent>()
        val videoEvents = mutableListOf<LibVlcEvent>()

        try {
            audio.setEventListener { event ->
                audioEvents += event
                if (event is LibVlcEvent.Playing) audioPlaying.countDown()
            }
            video.setEventListener { event ->
                videoEvents += event
                if (event is LibVlcEvent.Playing) videoPlaying.countDown()
            }

            audio.prepare(audioUri)
            video.prepare(videoUri)
            audio.play()
            video.play()

            assertTrue("audio player did not emit Playing; events=$audioEvents", audioPlaying.await(10, TimeUnit.SECONDS))
            assertTrue("video player did not emit Playing; events=$videoEvents", videoPlaying.await(10, TimeUnit.SECONDS))
        } finally {
            audio.release()
            video.release()
        }
    }

    private fun verifyControllerSyncsVideoToAudio(
        context: android.content.Context,
        audioUri: String,
        videoUri: String,
    ) {
        val audio = VlcLibVlcPlayerHandle(context)
        val video = VlcLibVlcPlayerHandle(context, disableAudio = true)
        val controller = DefaultPlaybackController(
            audioHandle = audio,
            videoHandle = video,
            clockMs = { System.nanoTime() / 1_000_000 },
            audioFocusController = GrantingAudioFocusController,
        )

        try {
            controller.handle(PlaybackIntent.Prepare(audioUri, videoUri, PainkillerVideoGapSec, 0f, 300_000L))
            controller.handle(PlaybackIntent.Play(stopAtLyricsTimeMs = 300_000L))
            assertTrue("audio/video did not both advance", waitUntilBothAdvance(audio, video))
            runControllerTicks(controller, durationMs = 2_000L)
            val audioTimeMs = audio.timeMs
            val videoTimeMs = video.timeMs
            val expectedVideoTimeMs = audioTimeMs + PainkillerVideoOffsetMs
            assertTrue(
                "audio/video LibVLC players drifted too far: audio=$audioTimeMs video=$videoTimeMs expectedVideo=$expectedVideoTimeMs events=${controller.events}",
                abs(expectedVideoTimeMs - videoTimeMs) <= SyncToleranceMs,
            )
        } finally {
            audio.release()
            video.release()
        }
    }

    private fun runControllerTicks(
        controller: DefaultPlaybackController,
        durationMs: Long,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(durationMs)
        while (System.nanoTime() < deadline) {
            controller.tick()
            Thread.sleep(100L)
        }
    }

    private fun waitUntilBothAdvance(
        audio: LibVlcPlayerHandle,
        video: LibVlcPlayerHandle,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline) {
            if (audio.timeMs > 0L && video.timeMs > 0L) return true
            Thread.sleep(100L)
        }
        return false
    }

    private object GrantingAudioFocusController : AudioFocusController {
        override fun requestAudioFocus(): Boolean = true
        override fun abandonAudioFocus() = Unit
    }

    private companion object {
        const val PainkillerVideoGapSec = -4.1f
        const val PainkillerVideoOffsetMs = 4_100L
        const val SyncToleranceMs = 750L
    }
}
