package com.couchraoke.tv.domain.parser

enum class NoteType {
    NORMAL,
    GOLDEN,
    FREESTYLE,
    RAP,
    RAP_GOLDEN,
}

sealed interface NoteEvent {
    val noteType: NoteType
    val startBeat: Int
    val durationBeats: Int
    val tone: Int
    val lyricText: String

    data class Note(
        override val noteType: NoteType,
        override val startBeat: Int,
        override val durationBeats: Int,
        override val tone: Int,
        override val lyricText: String,
    ) : NoteEvent
}
