package com.couchraoke.tv.presentation.playback

class DefaultPlaybackController(
    private val audioHandle: LibVlcPlayerHandle,
    private val videoHandle: LibVlcPlayerHandle? = null,
    private val clockMs: () -> Long,
    private val audioFocusController: AudioFocusController = AlwaysGrantedAudioFocusController,
) {
    private val mutableEvents = mutableListOf<PlaybackEvent>()
    private var preparedStopAtLyricsTimeMs = 0L
    private var playClockMs: Long? = null
    private var readyEmitted = false
    private var pauseCausedByFocusLoss = false

    val events: List<PlaybackEvent> get() = mutableEvents.toList()
    val currentPositionMs: Long get() = audioHandle.timeMs

    fun handle(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.Prepare -> prepare(intent)
            is PlaybackIntent.Play -> play(intent)
            PlaybackIntent.Pause -> pause(userInitiated = true)
            PlaybackIntent.Stop -> stop()
            is PlaybackIntent.Seek -> audioHandle.seekTo(intent.positionMs)
            is PlaybackIntent.PrebufferNext -> handleIteration1NoOp(intent)
            is PlaybackIntent.FadeOut -> handleIteration1NoOp(intent)
            is PlaybackIntent.Crossfade -> handleIteration1NoOp(intent)
        }
    }

    fun tick() {
        advanceReadyFallback()
        enforceStopBoundary()
    }

    fun onAudioFocusChanged(change: AudioFocusChange) {
        when (change) {
            AudioFocusChange.Gain -> if (pauseCausedByFocusLoss) {
                pauseCausedByFocusLoss = false
                audioHandle.play()
            }
            AudioFocusChange.TransientLoss -> pause(userInitiated = false)
            AudioFocusChange.PermanentLoss -> {
                stop()
                mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied())
            }
        }
    }

    private fun advanceReadyFallback() {
        val fallbackStartTvMs = playClockMs ?: return
        if (!readyEmitted && clockMs() - fallbackStartTvMs >= 500L) {
            readyEmitted = true
            mutableEvents += PlaybackEvent.Ready(fallbackStartTvMs)
        }
    }

    private fun enforceStopBoundary() {
        if (preparedStopAtLyricsTimeMs > 0L && audioHandle.timeMs >= preparedStopAtLyricsTimeMs) {
            stop()
            mutableEvents += PlaybackEvent.Ended
        }
    }

    private fun prepare(intent: PlaybackIntent.Prepare) {
        preparedStopAtLyricsTimeMs = intent.chartEndLyricsTimeMs ?: 0L
        readyEmitted = false
        playClockMs = null
        audioHandle.setEventListener { event -> onAudioEvent(event) }
        videoHandle?.setEventListener { event ->
            if (event is LibVlcEvent.EncounteredError) videoHandle.stop()
        }
        audioHandle.prepare(intent.audioUrl)
        intent.videoUrl?.let { url -> videoHandle?.prepare(url) }
        audioHandle.seekTo((intent.seekToSec * 1_000).toLong())
        videoHandle?.seekTo(((intent.videoGapSec ?: intent.seekToSec) * 1_000).toLong())
        val preparedDurationMs = audioHandle.durationMs
        val effectiveDurationMs = preparedDurationMs ?: intent.chartEndLyricsTimeMs
        if (effectiveDurationMs == null) {
            mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioUnavailable("No usable playback duration."))
        } else {
            mutableEvents += PlaybackEvent.Prepared(effectiveDurationMs)
        }
    }

    private fun play(intent: PlaybackIntent.Play) {
        preparedStopAtLyricsTimeMs = intent.stopAtLyricsTimeMs
        if (!readyEmitted) playClockMs = clockMs()
        pauseCausedByFocusLoss = false
        if (audioFocusController.requestAudioFocus()) {
            audioHandle.play()
            videoHandle?.play()
        } else {
            audioFocusController.abandonAudioFocus()
            mutableEvents += PlaybackEvent.Error(PlaybackErrorCause.AudioFocusDenied())
        }
    }

    private fun pause(userInitiated: Boolean) {
        pauseCausedByFocusLoss = !userInitiated
        audioHandle.pause()
    }

    private fun stop() {
        pauseCausedByFocusLoss = false
        audioHandle.stop()
        videoHandle?.stop()
        audioFocusController.abandonAudioFocus()
    }

    private fun onAudioEvent(event: LibVlcEvent) {
        when (event) {
            LibVlcEvent.Prepared -> Unit
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
            LibVlcEvent.Stopped,
            -> Unit
        }
    }
}
