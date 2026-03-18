package com.couchraoke.tv.presentation.songlist.preview

import com.couchraoke.tv.domain.library.SongEntry

interface ISongPreviewController {
    fun startPreview(song: SongEntry)
    fun stopPreview()
    fun release()
}

class NoOpPreviewController : ISongPreviewController {
    override fun startPreview(song: SongEntry) = Unit
    override fun stopPreview() = Unit
    override fun release() = Unit
}
