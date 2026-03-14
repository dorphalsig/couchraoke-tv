package com.couchraoke.tv.domain.parser

enum class TrackId {
    P1,
    P2,
}

data class Track(
    val trackId: TrackId,
    val lines: List<Line> = emptyList(),
)
