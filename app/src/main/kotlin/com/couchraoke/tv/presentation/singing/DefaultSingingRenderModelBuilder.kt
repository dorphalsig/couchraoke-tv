package com.couchraoke.tv.presentation.singing

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import kotlin.math.max

class DefaultSingingRenderModelBuilder : SingingRenderModelBuilder {
    override fun build(song: IndexedSong, parsedSong: ParsedSong, playerId: PlayerId): SingingRenderModel =
        buildAtLyricsTime(song, parsedSong, playerId, lyricsTimeMs = 0L)

    fun buildAtLyricsTime(
        song: IndexedSong,
        parsedSong: ParsedSong,
        playerId: PlayerId,
        lyricsTimeMs: Long,
    ): SingingRenderModel {
        val track = parsedSong.tracks.firstOrNull { it.playerId == playerId } ?: parsedSong.tracks.first()
        val lines = track.lines.filter { !it.isEmpty }
        val activeIndex = lines.indexOfLast { line -> line.startBeatFile.toLyricsTimeMs(parsedSong) <= lyricsTimeMs }
            .coerceAtLeast(0)
        val currentLine = lines.getOrNull(activeIndex)
        val nextLine = lines.getOrNull(activeIndex + 1)
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
            startSec = song.startSec,
            audioUrl = song.audioUrl,
            videoUrl = song.videoUrl,
            videoGapSec = null,
            background = song.toSingingBackground(),
            lanes = listOf(
                SingerLaneRenderModel(
                    playerId = playerId,
                    badgeText = playerId.name,
                    lane = LaneRenderState(
                        playerId = playerId,
                        noteTargets = noteTargets,
                        currentLyricsTimeMs = lyricsTimeMs,
                        visibleWindowMs = visibleWindowFor(lyricsTimeMs),
                    ),
                ),
            ),
            lyrics = LyricsRenderState(
                currentLine = currentLine.toLyricsLine(parsedSong),
                nextLine = nextLine.toLyricsLine(parsedSong),
                highlightFraction = currentLine.highlightAt(parsedSong, lyricsTimeMs),
            ),
            elapsedTimeText = formatElapsedTime(lyricsTimeMs),
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

private fun visibleWindowFor(lyricsTimeMs: Long): LongRange {
    val start = (lyricsTimeMs - 2_000L).coerceAtLeast(0L)
    return start..(start + 8_000L)
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

private fun Line?.highlightAt(parsedSong: ParsedSong, lyricsTimeMs: Long): Float {
    val line = this ?: return 0f
    val startMs = line.startBeatFile.toLyricsTimeMs(parsedSong)
    val endMs = line.endBeatFileExclusive.toLyricsTimeMs(parsedSong)
    return when {
        lyricsTimeMs >= endMs -> 1f
        lyricsTimeMs <= startMs -> 0f
        else -> ((lyricsTimeMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0f, 1f)
    }
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
