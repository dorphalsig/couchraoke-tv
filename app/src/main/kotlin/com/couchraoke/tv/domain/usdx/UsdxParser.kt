package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.domain.usdx.model.ParsedSong

interface UsdxParser {
    fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong>
}
