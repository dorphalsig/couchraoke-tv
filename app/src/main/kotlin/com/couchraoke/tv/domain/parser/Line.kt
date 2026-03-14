package com.couchraoke.tv.domain.parser

data class Line(
    val startBeat: Int,
    val notes: List<NoteEvent> = emptyList(),
)
