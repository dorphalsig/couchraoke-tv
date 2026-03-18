package com.couchraoke.tv.presentation.songlist.preview

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.couchraoke.tv.domain.library.SongEntry

/**
 * Wraps ExoPlayer to provide song preview playback.
 * Lifecycle is managed by SongListViewModel (released in onCleared).
 */
class SongPreviewController(private val player: ExoPlayer) : ISongPreviewController {

    override fun startPreview(song: SongEntry) {
        val url = song.audioUrl ?: return
        player.setMediaItem(MediaItem.fromUri(url))
        player.seekTo((song.previewStartSec * MS_PER_SECOND).toLong())
        player.prepare()
        player.play()
    }

    override fun stopPreview() {
        player.stop()
    }

    override fun release() {
        player.release()
    }

    companion object {
        private const val MS_PER_SECOND = 1000
    }
}
