package com.couchraoke.tv.domain.usdx.model

data class Line(
    val lineIndex: Int,
    val notes: List<NoteEvent>,
    val lineScoreValue: Long,
) {
    val startBeatFile: Int
        get() = notes.firstOrNull()?.startBeatFile ?: 0

    val endBeatFileExclusive: Int
        get() = notes.maxOfOrNull(NoteEvent::endBeatFileExclusive) ?: 0

    val isEmpty: Boolean
        get() = lineScoreValue == 0L
}
