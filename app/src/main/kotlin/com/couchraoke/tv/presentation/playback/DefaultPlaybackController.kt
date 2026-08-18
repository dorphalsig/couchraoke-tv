package com.couchraoke.tv.presentation.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class DefaultPlaybackController(
    private val audioHandle: LibVlcPlayerHandle,
    private val videoHandle: LibVlcPlayerHandle? = null,
    private val clockMs: () -> Long,
    private val audioFocusController: AudioFocusController,
    private val stopAtLyricsTimeMs: () -> Long = { 0L },
    private val onDecorativeVideoAvailableChanged: (Boolean) -> Unit = {},
    scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
) {
    private val mutableEvents = mutableListOf<PlaybackEvent>()
    private val audioDurationProvider = audioHandle as? PreparedDurationProvider
    private var preparedStopAtLyricsTimeMs = 0L
    private var playClockMs: Long? = null
    private var readyEmitted = false
    private var pauseCausedByFocusLoss = false
    private var playing = false
    private var videoOffsetMs = 0L

    val events: List<PlaybackEvent> get() = mutableEvents.toList()
    val currentPositionMs: Long get() = audioHandle.timeMs

    init {
        scope.launch { audioHandle.events.collect(::onAudioEvent) }
        videoHandle?.let { handle ->
            scope.launch { handle.events.collect { event -> onVideoEvent(handle, event) } }
        }
    }

    fun handle(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.Prepare -> prepare(intent)
            PlaybackIntent.Play -> play()
            PlaybackIntent.Pause -> pause(userInitiated = true)
            PlaybackIntent.Stop -> stop()
            is PlaybackIntent.Seek -> audioHandle.seekTo(intent.positionMs)
            is PlaybackIntent.PrebufferNext -> handleIteration1NoOp(intent)
            is PlaybackIntent.FadeOut -> handleIteration1NoOp(intent)
            is PlaybackIntent.Crossfade -> handleIteration1NoOp(intent)
        }
    }

    fun tick() {
        val fallbackStartTvMs = playClockMs
        if (fallbackStartTvMs != null && !readyEmitted && clockMs() - fallbackStartTvMs >= 500L) {
            readyEmitted = true
            mutableEvents += PlaybackEvent.Ready(fallbackStartTvMs)
        }
        videoHandle?.let { video ->
            val targetVideoTimeMs = (audioHandle.timeMs + videoOffsetMs).coerceAtLeast(0L)
            if (playing && abs(targetVideoTimeMs - video.timeMs) > VideoSyncToleranceMs) {
                video.seekTo(targetVideoTimeMs)
            }
        }
        if (preparedStopAtLyricsTimeMs > 0L && audioHandle.timeMs >= preparedStopAtLyricsTimeMs) {
            stop()
            mutableEvents += PlaybackEvent.Ended
        }
    }

    fun setVideoSurface(holder: android.view.SurfaceHolder?) {
        (videoHandle as? VideoSurfaceBinder)?.setVideoSurface(holder)
    }

    fun onAudioFocusChanged(change: AudioFocusChange) {
        when (change) {
            AudioFocusChange.Gain -> if (pauseCausedByFocusLoss) {
                pauseCausedByFocusLoss = false
                playing = true
                audioHandle.play()
            }
            AudioFocusChange.TransientLoss -> pause(userInitiated = false)
            AudioFocusChange.PermanentLoss -> {
                stop()
                mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied())
            }
        }
    }

    private fun prepare(intent: PlaybackIntent.Prepare) {
        preparedStopAtLyricsTimeMs = 0L
        readyEmitted = false
        playClockMs = null
        playing = false
        videoOffsetMs = (-(intent.videoGapSec ?: 0f) * 1_000).toLong()
        onDecorativeVideoAvailableChanged(false)
        audioHandle.prepare(intent.audioUrl, intent.seekToSec)
        val videoSeekToSec = if ((intent.videoGapSec ?: 0f) < 0f) {
            intent.seekToSec - (intent.videoGapSec ?: 0f)
        } else {
            intent.seekToSec
        }
        intent.videoUrl?.let { url ->
            videoHandle?.prepare(url, videoSeekToSec)
            if (videoHandle != null) onDecorativeVideoAvailableChanged(true)
        }
        val effectiveDurationMs = audioDurationProvider?.durationMs
        if (effectiveDurationMs == null) {
            mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioUnavailable("No usable playback duration."))
        } else {
            mutableEvents += PlaybackEvent.Prepared(effectiveDurationMs)
        }
    }

    private fun play() {
        preparedStopAtLyricsTimeMs = stopAtLyricsTimeMs()
        if (!readyEmitted) playClockMs = clockMs()
        pauseCausedByFocusLoss = false
        if (audioFocusController.requestAudioFocus()) {
            playing = true
            audioHandle.play()
            videoHandle?.play()
        } else {
            audioFocusController.abandonAudioFocus()
            mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied())
        }
    }

    private fun pause(userInitiated: Boolean) {
        pauseCausedByFocusLoss = !userInitiated
        playing = false
        audioHandle.pause()
    }

    private fun stop() {
        pauseCausedByFocusLoss = false
        playing = false
        audioHandle.stop()
        videoHandle?.stop()
        audioFocusController.abandonAudioFocus()
    }

    private fun onAudioEvent(event: LibVlcEvent) {
        when (event) {
            LibVlcEvent.Playing -> if (!readyEmitted) {
                readyEmitted = true
                mutableEvents += PlaybackEvent.Ready(clockMs() - audioHandle.timeMs)
            }
            LibVlcEvent.EndReached -> {
                stop()
                mutableEvents += PlaybackEvent.Ended
            }
            is LibVlcEvent.EncounteredError -> {
                stop()
                mutableEvents += PlaybackEvent.Error(
                    PlaybackErrorCause.PlayerError(event.lastWarningOrError?.take(120)),
                )
            }
            LibVlcEvent.Paused,
            is LibVlcEvent.TimeChanged,
            -> Unit
        }
    }

    private fun onVideoEvent(videoHandle: LibVlcPlayerHandle, event: LibVlcEvent) {
        when (event) {
            LibVlcEvent.Playing -> videoHandle.seekTo((audioHandle.timeMs + videoOffsetMs).coerceAtLeast(0L))
            is LibVlcEvent.EncounteredError -> {
                videoHandle.stop()
                videoHandle.release()
                onDecorativeVideoAvailableChanged(false)
            }
            LibVlcEvent.Paused,
            LibVlcEvent.EndReached,
            is LibVlcEvent.TimeChanged,
            -> Unit
        }
    }

    private companion object {
        const val VideoSyncToleranceMs = 250L
    }
}
