package com.couchraoke.tv.presentation.playback

interface LibVlcPlayerHandle {
    val timeMs: Long
    val durationMs: Long?

    fun setEventListener(listener: (LibVlcEvent) -> Unit)

    fun prepare(url: String)

    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)
}

sealed interface LibVlcEvent {
    data object Prepared : LibVlcEvent
    data object Playing : LibVlcEvent
    data object Paused : LibVlcEvent
    data object Stopped : LibVlcEvent
    data object EndReached : LibVlcEvent
    data class EncounteredError(
        val lastWarningOrError: String? = null,
    ) : LibVlcEvent
}
