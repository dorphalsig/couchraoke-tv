package com.couchraoke.tv.domain.usdx.model

data class ParsedSong(
    val songId: String,
    val header: SongHeader,
    val timing: SongTiming,
    val tracks: List<Track>,
    val diagnostics: List<DiagnosticEntry>,
)
