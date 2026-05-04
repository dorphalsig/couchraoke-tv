package com.couchraoke.tv.presentation.playback

class DefaultPlaybackController(
    private val audioHandle: LibVlcPlayerHandle,
    private val clockMs: () -> Long,
) {
    private val mutableEvents = mutableListOf<PlaybackEvent>()
    private var preparedStopAtLyricsTimeMs = 0L
    private var playClockMs: Long? = null
    private var readyEmitted = false

    val events: List<PlaybackEvent> get() = mutableEvents.toList()
    val currentPositionMs: Long get() = audioHandle.timeMs

    fun handle(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.Prepare -> prepare(intent)
            is PlaybackIntent.Play -> play(intent)
            PlaybackIntent.Pause -> audioHandle.pause()
            PlaybackIntent.Stop -> audioHandle.stop()
            is PlaybackIntent.Seek -> audioHandle.seekTo(intent.positionMs)
            is PlaybackIntent.PrebufferNext -> handleIteration1NoOp(intent)
            is PlaybackIntent.FadeOut -> handleIteration1NoOp(intent)
            is PlaybackIntent.Crossfade -> handleIteration1NoOp(intent)
        }
    }

    fun advanceReadyFallback() {
        val fallbackStartTvMs = playClockMs ?: return
        if (!readyEmitted && clockMs() - fallbackStartTvMs >= 500L) {
            readyEmitted = true
            mutableEvents += PlaybackEvent.Ready(fallbackStartTvMs)
        }
    }

    fun enforceStopBoundary() {
        if (preparedStopAtLyricsTimeMs > 0L && audioHandle.timeMs >= preparedStopAtLyricsTimeMs) {
            audioHandle.stop()
            mutableEvents += PlaybackEvent.Ended
        }
    }

    private fun prepare(intent: PlaybackIntent.Prepare) {
        preparedStopAtLyricsTimeMs = intent.chartEndLyricsTimeMs ?: 0L
        audioHandle.setEventListener { event -> onAudioEvent(event) }
        audioHandle.prepare(intent.audioUrl)
        audioHandle.seekTo((intent.seekToSec * 1_000).toLong())
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
        playClockMs = clockMs()
        readyEmitted = false
        audioHandle.play()
    }

    private fun onAudioEvent(event: LibVlcEvent) {
        when (event) {
            LibVlcEvent.Prepared -> Unit
            LibVlcEvent.Playing -> if (!readyEmitted) {
                readyEmitted = true
                mutableEvents += PlaybackEvent.Ready(clockMs() - audioHandle.timeMs)
            }
            LibVlcEvent.EndReached -> mutableEvents += PlaybackEvent.Ended
            is LibVlcEvent.EncounteredError -> mutableEvents += PlaybackEvent.Error(
                PlaybackErrorCause.PlayerError(event.lastWarningOrError),
            )
            LibVlcEvent.Paused,
            LibVlcEvent.Stopped,
            -> Unit
        }
    }
}
