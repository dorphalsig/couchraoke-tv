package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.usdx.model.ParsedSong

interface SingingRenderModelBuilder {
    fun build(song: IndexedSong, parsedSong: ParsedSong, playerId: PlayerId): SingingRenderModel
}
