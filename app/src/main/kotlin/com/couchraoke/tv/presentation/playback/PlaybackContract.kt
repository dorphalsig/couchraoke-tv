package com.couchraoke.tv.presentation.playback

sealed interface PlaybackIntent {
    data class Prepare(
        val audioUrl: String,
        val videoUrl: String?,
        val videoGapSec: Float?,
        val seekToSec: Float,
    ) : PlaybackIntent

    data object Play : PlaybackIntent
    data object Pause : PlaybackIntent
    data object Stop : PlaybackIntent

    data class Seek(
        val positionMs: Long,
    ) : PlaybackIntent

    data class PrebufferNext(
        val audioUrl: String,
        val videoUrl: String? = null,
        val videoGapSec: Float? = null,
        val seekToSec: Float,
    ) : PlaybackIntent

    data class FadeOut(
        val durationSec: Float,
    ) : PlaybackIntent

    data class Crossfade(
        val fadeOutSec: Float,
        val fadeInSec: Float,
    ) : PlaybackIntent
}

sealed interface PlaybackEvent {
    data class Prepared(
        val effectivePlaybackDurationMs: Long,
    ) : PlaybackEvent

    data class Ready(
        val songStartTvMs: Long,
    ) : PlaybackEvent

    data class Error(
        val cause: PlaybackErrorCause,
    ) : PlaybackEvent

    data object Ended : PlaybackEvent
}

sealed interface PlaybackErrorCause {
    data class AudioUnavailable(
        val message: String? = null,
    ) : PlaybackErrorCause

    data class AudioFocusDenied(
        val message: String? = null,
    ) : PlaybackErrorCause

    data class PlayerError(
        val lastWarningOrError: String? = null,
    ) : PlaybackErrorCause
}

fun PlaybackIntent.isIteration1NoOp(): Boolean = when (this) {
    is PlaybackIntent.PrebufferNext,
    is PlaybackIntent.FadeOut,
    is PlaybackIntent.Crossfade -> true
    else -> false
}

fun handleIteration1NoOp(intent: PlaybackIntent): Boolean {
    // Iteration 4 wires medley prebuffer/fade/crossfade execution.
    return intent.isIteration1NoOp()
}
