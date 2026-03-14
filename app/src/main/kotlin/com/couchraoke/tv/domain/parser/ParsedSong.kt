package com.couchraoke.tv.domain.parser

data class ParsedSong(
    val songIdentifier: String,
    val isValid: Boolean,
    val header: SongHeader,
    val tracks: List<Track>,
    val derivedSummary: DerivedSongSummary,
    val diagnostics: List<DiagnosticEntry> = emptyList(),
)
