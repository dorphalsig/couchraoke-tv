package com.couchraoke.tv.domain.usdx.internal

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.Track

internal data class BodyParseResult(
    val tracks: List<Track>,
    val diagnostics: List<DiagnosticEntry>,
    val isDuet: Boolean,
    val hasEndMarker: Boolean,
)

internal object BodyParser {
    fun parse(lines: List<String>, startIndex: Int, txtUri: String): BodyParseResult =
        Session(lines, startIndex, txtUri).parse()

    private class Session(
        private val lines: List<String>,
        private val startIndex: Int,
        private val txtUri: String,
    ) {
        private val diagnostics = mutableListOf<DiagnosticEntry>()
        private val rawLines = mutableMapOf(
            PlayerId.P1 to mutableListOf<List<NoteEvent>>(),
            PlayerId.P2 to mutableListOf<List<NoteEvent>>(),
        )
        private val currentNotes = mutableMapOf(
            PlayerId.P1 to mutableListOf<NoteEvent>(),
            PlayerId.P2 to mutableListOf<NoteEvent>(),
        )

        private var currentPlayer = PlayerId.P1
        private var isDuet = false
        private var hasEndMarker = false
        private var invalidResult: BodyParseResult? = null

        fun parse(): BodyParseResult {
            var index = startIndex
            while (index < lines.size && invalidResult == null && !hasEndMarker) {
                parseLine(lineNumber = index + 1, line = lines[index].trim())
                index += 1
            }
            val invalid = invalidResult
            return if (invalid != null) {
                invalid
            } else {
                flushCurrentLines()
                buildResult()
            }
        }

        private fun parseLine(lineNumber: Int, line: String) {
            when {
                line.isEmpty() -> Unit
                line == "E" -> hasEndMarker = true
                line == "P1" -> {
                    isDuet = true
                    currentPlayer = PlayerId.P1
                }
                line == "P2" -> {
                    isDuet = true
                    currentPlayer = PlayerId.P2
                }
                line.startsWith("P") -> invalidBody(
                    DiagnosticFactory.ERROR_CORRUPT_SONG_INVALID_DUET_MARKER,
                    lineNumber,
                )
                line.startsWith("B") -> invalidBody(
                    DiagnosticFactory.ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM,
                    lineNumber,
                )
                line.startsWith('-') -> parseSentenceBreak(line, lineNumber)
                else -> parseNoteOrUnknown(line, lineNumber)
            }
        }

        private fun parseSentenceBreak(line: String, lineNumber: Int) {
            val tokens = line.whitespaceFields()
            when {
                tokens.size >= 3 -> invalidBody(DiagnosticFactory.ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE, lineNumber)
                tokens.size != 2 || tokens[1].toIntOrNull() == null -> {
                    invalidBody(DiagnosticFactory.ERROR_CORRUPT_SONG_MALFORMED_BODY, lineNumber)
                }
                else -> flushCurrentLines()
            }
        }

        private fun parseNoteOrUnknown(line: String, lineNumber: Int) {
            val noteType = noteTypeFor(line.first())
            if (noteType == null) {
                diagnostics += DiagnosticFactory.warn(
                    code = DiagnosticFactory.WARN_UNKNOWN_BODY_TOKEN,
                    txtUri = txtUri,
                    lineNumber = lineNumber,
                )
            } else {
                parseNote(line, lineNumber, noteType)
            }
        }

        private fun parseNote(line: String, lineNumber: Int, noteType: NoteType) {
            val tokens = line.whitespaceFields(limit = NOTE_FIELD_COUNT)
            val parsedNote = parsedNoteFrom(tokens)
            if (parsedNote == null || parsedNote.durationBeats < 0) {
                invalidBody(DiagnosticFactory.ERROR_CORRUPT_SONG_MALFORMED_BODY, lineNumber)
            } else {
                addNote(noteType, parsedNote, lineNumber)
            }
        }

        private fun addNote(
            noteType: NoteType,
            parsedNote: ParsedNote,
            lineNumber: Int,
        ) {
            val resolvedNoteType = if (parsedNote.durationBeats == 0) {
                diagnostics += DiagnosticFactory.warn(
                    code = DiagnosticFactory.WARN_ZERO_DURATION_CONVERTED_TO_FREESTYLE,
                    txtUri = txtUri,
                    lineNumber = lineNumber,
                )
                NoteType.Freestyle
            } else {
                noteType
            }
            currentNotes.getValue(currentPlayer) += NoteEvent(
                noteType = resolvedNoteType,
                startBeatFile = parsedNote.startBeatFile,
                durationBeats = parsedNote.durationBeats,
                toneSemitone = parsedNote.toneSemitone,
                lyric = parsedNote.lyric,
            )
        }

        private fun flushCurrentLines() {
            PlayerId.entries.forEach { playerId ->
                val notes = currentNotes.getValue(playerId)
                if (notes.isNotEmpty()) {
                    rawLines.getValue(playerId) += notes.toList()
                    notes.clear()
                }
            }
        }

        private fun buildResult(): BodyParseResult {
            val tracks = buildTracks()
            return if (tracks.isEmpty()) {
                diagnostics += DiagnosticFactory.invalid(
                    code = DiagnosticFactory.ERROR_CORRUPT_SONG_NO_NOTES,
                    txtUri = txtUri,
                )
                BodyParseResult(emptyList(), diagnostics, isDuet, hasEndMarker)
            } else {
                BodyParseResult(tracks, diagnostics, isDuet, hasEndMarker)
            }
        }

        private fun buildTracks(): List<Track> = buildList {
            val p1Lines = rawLines.getValue(PlayerId.P1)
            if (p1Lines.isNotEmpty() || !isDuet) {
                add(ScoreValueCalculator.buildTrack(PlayerId.P1, p1Lines))
            }
            if (isDuet) {
                add(ScoreValueCalculator.buildTrack(PlayerId.P2, rawLines.getValue(PlayerId.P2)))
            }
        }.filter { track -> track.lines.isNotEmpty() }

        private fun invalidBody(code: String, lineNumber: Int) {
            diagnostics += DiagnosticFactory.invalid(
                code = code,
                txtUri = txtUri,
                lineNumber = lineNumber,
            )
            invalidResult = BodyParseResult(
                tracks = listOf(Track(PlayerId.P1, listOf(Line(lineNumber, emptyList(), 0L)), 0L)),
                diagnostics = diagnostics.toList(),
                isDuet = isDuet,
                hasEndMarker = hasEndMarker,
            )
        }
    }

    private data class ParsedNote(
        val startBeatFile: Int,
        val durationBeats: Int,
        val toneSemitone: Int,
        val lyric: String,
    )

    private fun parsedNoteFrom(tokens: List<String>): ParsedNote? {
        val parsedNote = if (tokens.size >= NOTE_FIELD_COUNT) {
            ParsedNote(
                startBeatFile = tokens[1].toIntOrNull() ?: INVALID_INT,
                durationBeats = tokens[2].toIntOrNull() ?: INVALID_INT,
                toneSemitone = tokens[3].toIntOrNull() ?: INVALID_INT,
                lyric = tokens[4],
            )
        } else {
            ParsedNote(INVALID_INT, INVALID_INT, INVALID_INT, "")
        }
        val invalid = parsedNote.startBeatFile == INVALID_INT ||
            parsedNote.durationBeats == INVALID_INT ||
            parsedNote.toneSemitone == INVALID_INT
        return parsedNote.takeUnless { invalid }
    }

    private fun noteTypeFor(token: Char): NoteType? = when (token) {
        ':' -> NoteType.Normal
        '*' -> NoteType.Golden
        'F' -> NoteType.Freestyle
        'R' -> NoteType.Rap
        'G' -> NoteType.RapGolden
        else -> null
    }

    private fun String.whitespaceFields(limit: Int = Int.MAX_VALUE): List<String> {
        val fields = mutableListOf<String>()
        var index = skipWhitespace(0)
        while (index < length && fields.size < limit) {
            val lastAllowedField = fields.size == limit - 1
            if (lastAllowedField) {
                fields += substring(index)
                index = length
            } else {
                val fieldStart = index
                index = skipNonWhitespace(index)
                fields += substring(fieldStart, index)
                index = skipWhitespace(index)
            }
        }
        return fields
    }

    private fun String.skipWhitespace(start: Int): Int {
        var index = start
        while (index < length && this[index].isWhitespace()) {
            index += 1
        }
        return index
    }

    private fun String.skipNonWhitespace(start: Int): Int {
        var index = start
        while (index < length && !this[index].isWhitespace()) {
            index += 1
        }
        return index
    }

    private const val INVALID_INT = Int.MIN_VALUE
    private const val NOTE_FIELD_COUNT = 5
}
