package com.couchraoke.tv.presentation.playback

class VlcLibVlcPlayerHandle : LibVlcPlayerHandle {
    private var listener: ((LibVlcEvent) -> Unit)? = null
    override var timeMs: Long = 0L
        private set
    override var durationMs: Long = 0L
        private set

    override fun setEventListener(listener: (LibVlcEvent) -> Unit) {
        this.listener = listener
    }

    override fun prepare(url: String) {
        require(url.isNotBlank())
        listener?.invoke(LibVlcEvent.Prepared)
    }

    override fun play() {
        listener?.invoke(LibVlcEvent.Playing)
    }

    override fun pause() {
        listener?.invoke(LibVlcEvent.Paused)
    }

    override fun stop() {
        listener?.invoke(LibVlcEvent.Stopped)
    }

    override fun seekTo(positionMs: Long) {
        timeMs = positionMs.coerceAtLeast(0L)
    }

    override fun release() {
        listener = null
    }
}
