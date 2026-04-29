package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import kotlin.math.max

interface SingingRenderModelBuilder {
    fun build(song: IndexedSong, parsedSong: ParsedSong, playerId: PlayerId): SingingRenderModel
}

class DefaultSingingRenderModelBuilder : SingingRenderModelBuilder {
    override fun build(song: IndexedSong, parsedSong: ParsedSong, playerId: PlayerId): SingingRenderModel {
        val track = parsedSong.tracks.firstOrNull { it.playerId == playerId } ?: parsedSong.tracks.first()
        val lines = track.lines.filter { !it.isEmpty }
        val currentLine = lines.firstOrNull()
        val nextLine = lines.drop(1).firstOrNull()
        val noteTargets = track.lines.flatMap { line ->
            line.notes.map { note -> note.toStaticNoteTarget(parsedSong) }
        }
        val stopAtLyricsTimeMs = parsedSong.header.endMs?.toLong()
            ?: noteTargets.maxOfOrNull { it.endTimeMs }
            ?: 0L

        return SingingRenderModel(
            songId = song.songId,
            title = song.title,
            artist = song.artist,
            background = song.toSingingBackground(),
            lanes = listOf(
                SingerLaneRenderModel(
                    playerId = playerId,
                    badgeText = playerId.name,
                    lane = LaneRenderState(
                        playerId = playerId,
                        noteTargets = noteTargets,
                        currentLyricsTimeMs = 0L,
                    ),
                ),
            ),
            lyrics = LyricsRenderState(
                currentLine = currentLine.toLyricsLine(parsedSong),
                nextLine = nextLine.toLyricsLine(parsedSong),
                highlightFraction = if (currentLine == null) 0f else 0f,
            ),
            elapsedTimeText = formatElapsedTime(0L),
            stopAtLyricsTimeMs = stopAtLyricsTimeMs,
        )
    }
}

fun formatElapsedTime(elapsedMs: Long): String {
    val totalSeconds = max(0L, elapsedMs) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun Line?.toLyricsLine(parsedSong: ParsedSong): LyricsLine = if (this == null) {
    LyricsLine(text = "", startTimeMs = null, endTimeMs = null)
} else {
    LyricsLine(
        text = notes.joinToString(separator = "") { it.lyric },
        startTimeMs = startBeatFile.toLyricsTimeMs(parsedSong),
        endTimeMs = endBeatFileExclusive.toLyricsTimeMs(parsedSong),
    )
}

private fun NoteEvent.toStaticNoteTarget(parsedSong: ParsedSong): StaticNoteTarget = StaticNoteTarget(
    startTimeMs = startBeatFile.toLyricsTimeMs(parsedSong),
    endTimeMs = endBeatFileExclusive.toLyricsTimeMs(parsedSong),
    toneSemitone = toneSemitone,
    lyric = lyric,
)

private fun Int.toLyricsTimeMs(parsedSong: ParsedSong): Long {
    val beatLengthMs = 60_000.0 / (parsedSong.timing.bpmFile * 4.0)
    return (this * beatLengthMs + parsedSong.header.gapMs).toLong()
}
