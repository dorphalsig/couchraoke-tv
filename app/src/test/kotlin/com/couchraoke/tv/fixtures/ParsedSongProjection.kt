package com.couchraoke.tv.fixtures

import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.SongHeader
import com.couchraoke.tv.domain.usdx.model.Track
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Projects a parsed chart into the canonical `ParsedSong` JSON the `expected.parsedSong.json`
 * fixtures assert. The fixtures mirror the domain model field for field, so this projection is
 * deliberately total: every field is emitted, including nulls, so that a field added to
 * [SongHeader] without a matching fixture update fails loudly instead of passing silently.
 */
object ParsedSongProjection {
    fun project(parsed: ParsedSong): JsonObject = buildJsonObject {
        put("songId", JsonPrimitive(parsed.songId))
        put("header", header(parsed.header))
        put("timing", buildJsonObject { put("bpmFile", JsonPrimitive(parsed.timing.bpmFile)) })
        put("tracks", JsonArray(parsed.tracks.map(::track)))
        put(
            "diagnostics",
            JsonArray(
                parsed.diagnostics.map { diagnostic ->
                    Phase0Assertions.diagnostic(
                        severity = diagnostic.severity.name,
                        code = diagnostic.code,
                        txtUri = diagnostic.txtUri,
                        lineNumber = diagnostic.lineNumber,
                    )
                }
            )
        )
    }

    private fun header(header: SongHeader): JsonObject = buildJsonObject {
        put("title", JsonPrimitive(header.title))
        put("artist", JsonPrimitive(header.artist))
        put("bpmFile", JsonPrimitive(header.bpmFile))
        put("gapMs", JsonPrimitive(header.gapMs))
        put("audio", header.audio.orJsonNull())
        put("startSec", header.startSec.orJsonNull())
        put("endMs", header.endMs.orJsonNull())
        put("videoGapSec", header.videoGapSec.orJsonNull())
        put("previewStartSec", header.previewStartSec.orJsonNull())
        put("video", header.video.orJsonNull())
        put("cover", header.cover.orJsonNull())
        put("background", header.background.orJsonNull())
        put("instrumental", header.instrumental.orJsonNull())
        put("vocals", header.vocals.orJsonNull())
        put("version", JsonPrimitive(header.version))
        put("year", header.year.orJsonNull())
        put("genre", header.genre.orJsonNull())
        put("album", header.album.orJsonNull())
        put("isDuet", JsonPrimitive(header.isDuet))
        put("p1Name", header.p1Name.orJsonNull())
        put("p2Name", header.p2Name.orJsonNull())
        put("medleyStartBeat", header.medleyStartBeat.orJsonNull())
        put("medleyEndBeat", header.medleyEndBeat.orJsonNull())
        put(
            "customTags",
            JsonArray(
                header.customTags.map { tag ->
                    buildJsonObject {
                        put("tag", JsonPrimitive(tag.tag))
                        put("content", JsonPrimitive(tag.content))
                    }
                }
            )
        )
    }

    private fun track(track: Track): JsonObject = buildJsonObject {
        put("playerId", JsonPrimitive(track.playerId.name))
        put("lines", JsonArray(track.lines.map(::line)))
        put("trackScoreValue", JsonPrimitive(track.trackScoreValue))
    }

    private fun line(line: Line): JsonObject = buildJsonObject {
        put("lineIndex", JsonPrimitive(line.lineIndex))
        put("notes", JsonArray(line.notes.map(::note)))
        put("lineScoreValue", JsonPrimitive(line.lineScoreValue))
        put("startBeatFile", JsonPrimitive(line.startBeatFile))
        put("endBeatFileExclusive", JsonPrimitive(line.endBeatFileExclusive))
        put("isEmpty", JsonPrimitive(line.isEmpty))
    }

    private fun note(note: NoteEvent): JsonObject = buildJsonObject {
        put("noteType", JsonPrimitive(note.noteType.name))
        put("startBeatFile", JsonPrimitive(note.startBeatFile))
        put("durationBeats", JsonPrimitive(note.durationBeats))
        put("toneSemitone", JsonPrimitive(note.toneSemitone))
        put("lyric", JsonPrimitive(note.lyric))
        put("endBeatFileExclusive", JsonPrimitive(note.endBeatFileExclusive))
    }

    private fun String?.orJsonNull(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull

    private fun Int?.orJsonNull(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull

    private fun Float?.orJsonNull(): JsonElement = this?.let(::JsonPrimitive) ?: JsonNull
}
