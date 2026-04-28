package com.couchraoke.tv.domain.usdx.model

data class NoteEvent(
    val noteType: NoteType,
    val startBeatFile: Int,
    val durationBeats: Int,
    val toneSemitone: Int,
    val lyric: String,
) {
    val endBeatFileExclusive: Int
        get() = startBeatFile + durationBeats
}
