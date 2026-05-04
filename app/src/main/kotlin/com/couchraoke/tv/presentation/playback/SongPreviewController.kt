package com.couchraoke.tv.presentation.playback

interface SongPreviewController {
    fun preparePreview(audioUrl: String, startPositionMs: Long)

    fun play()

    fun stop()

    fun release()
}

data class PreviewPlaybackState(
    val songId: String,
    val audioUrl: String,
    val startPositionMs: Long,
    val debounceMs: Long = 500L,
)

fun previewStartPositionMs(previewStartSec: Float?): Long = if (previewStartSec != null && previewStartSec > 0f) {
    (previewStartSec * 1_000).toLong()
} else {
    0L
}
