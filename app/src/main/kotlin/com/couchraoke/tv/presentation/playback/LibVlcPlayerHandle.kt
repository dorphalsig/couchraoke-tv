package com.couchraoke.tv.presentation.playback

import kotlinx.coroutines.flow.SharedFlow

interface LibVlcPlayerHandle {
    val events: SharedFlow<LibVlcEvent>
    val timeMs: Long

    fun prepare(mediaUrl: String, seekToSec: Float)

    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun setVolume(percent: Int)

    fun release()
}

internal interface VideoSurfaceBinder {
    fun setVideoSurface(holder: android.view.SurfaceHolder?)
}

internal interface PreparedDurationProvider {
    val durationMs: Long?
}

sealed interface LibVlcEvent {
    data object Playing : LibVlcEvent
    data object Paused : LibVlcEvent
    data object EndReached : LibVlcEvent
    data class TimeChanged(
        val timeMs: Long,
    ) : LibVlcEvent
    data class EncounteredError(
        val lastWarningOrError: String? = null,
    ) : LibVlcEvent
}
