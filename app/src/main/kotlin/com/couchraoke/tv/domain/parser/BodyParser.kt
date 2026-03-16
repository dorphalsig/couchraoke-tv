package com.couchraoke.tv.domain.parser

data class TrackSection(
    val trackId: TrackId,
    val events: List<NoteEvent.Note>,
    val lineBreaks: List<Int>,
    val groupedEvents: List<List<NoteEvent.Note>> = emptyList(),
)

data class BodyParseResult(
    val trackSections: List<TrackSection>,
    val diagnostics: List<DiagnosticEntry> = emptyList(),
    val hasFatalError: Boolean = false,
)

private class TrackAccumulator(val trackId: TrackId) {
    val events = mutableListOf<NoteEvent.Note>()
    val lineBreaks = mutableListOf<Int>()
    val groupedEvents = mutableListOf<List<NoteEvent.Note>>()
    val currentLine = mutableListOf<NoteEvent.Note>()

    fun finalizeCurrentLine() {
        if (currentLine.isNotEmpty()) {
            groupedEvents += currentLine.toList()
            currentLine.clear()
        }
    }

    fun toTrackSection(): TrackSection = TrackSection(
        trackId = trackId,
        events = events.toList(),
        lineBreaks = lineBreaks.toList(),
        groupedEvents = groupedEvents.toList(),
    )
}

class BodyParser {
    fun parse(
        songIdentifier: String,
        lines: List<String>,
        lineNumberOffset: Int = 0,
    ): BodyParseResult {
        val diagnostics = mutableListOf<DiagnosticEntry>()
        val trackAccumulators = linkedMapOf<TrackId, TrackAccumulator>()
        var currentTrack = TrackId.P1
        trackAccumulators.getOrPut(currentTrack) { TrackAccumulator(currentTrack) }

        for ((index, rawLine) in lines.withIndex()) {
            val lineNumber = lineNumberOffset + index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) {
                continue
            }

            when {
                trimmed == "P1" -> {
                    currentTrack = TrackId.P1
                    trackAccumulators.getOrPut(currentTrack) { TrackAccumulator(currentTrack) }
                }
                trimmed == "P2" -> {
                    currentTrack = TrackId.P2
                    trackAccumulators.getOrPut(currentTrack) { TrackAccumulator(currentTrack) }
                }
                trimmed.startsWith("P") -> {
                    return fatalResult(
                        trackAccumulators = trackAccumulators,
                        diagnostics = diagnostics,
                        diagnostic = error(
                            code = DiagnosticCode.INVALID_DUET_MARKER,
                            message = "Invalid duet marker",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        ),
                    )
                }
                trimmed == "E" -> {
                    trackAccumulators.values.forEach { it.finalizeCurrentLine() }
                    break
                }
                trimmed.startsWith("B") -> {
                    return fatalResult(
                        trackAccumulators = trackAccumulators,
                        diagnostics = diagnostics,
                        diagnostic = error(
                            code = DiagnosticCode.UNSUPPORTED_VARIABLE_BPM,
                            message = "Variable BPM is unsupported",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        ),
                    )
                }
                trimmed.startsWith(":") || trimmed.startsWith("*") || trimmed.startsWith("F") || trimmed.startsWith("R") || trimmed.startsWith("G") -> {
                    val note = parseNote(trimmed)
                    if (note == null) {
                        return fatalResult(
                            trackAccumulators = trackAccumulators,
                            diagnostics = diagnostics,
                            diagnostic = error(
                                code = DiagnosticCode.MALFORMED_BODY_FIELD,
                                message = "Malformed note numeric field",
                                songIdentifier = songIdentifier,
                                lineNumber = lineNumber,
                            ),
                        )
                    }
                    if (note.durationBeats == 0) {
                        diagnostics += DiagnosticEntry(
                            severity = DiagnosticSeverity.WARNING,
                            code = DiagnosticCode.ZERO_DURATION_NOTE_CONVERTED,
                            message = "Zero-duration note converted to freestyle",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        )
                    }
                    val accumulator = trackAccumulators.getValue(currentTrack)
                    accumulator.events += note
                    accumulator.currentLine += note
                }
                trimmed.startsWith("-") -> {
                    val breakBeat = trimmed.removePrefix("-").trim().split(Regex("\\s+"))[0].toIntOrNull()
                        ?: return fatalResult(
                            trackAccumulators = trackAccumulators,
                            diagnostics = diagnostics,
                            diagnostic = error(
                                code = DiagnosticCode.MALFORMED_BODY_FIELD,
                                message = "Malformed line break numeric field",
                                songIdentifier = songIdentifier,
                                lineNumber = lineNumber,
                            ),
                        )
                    val accumulator = trackAccumulators.getValue(currentTrack)
                    accumulator.lineBreaks += breakBeat
                    accumulator.finalizeCurrentLine()
                }
                trimmed.first().isLetter() -> {
                    diagnostics += DiagnosticEntry(
                        severity = DiagnosticSeverity.WARNING,
                        code = DiagnosticCode.UNKNOWN_BODY_TOKEN,
                        message = "Unknown body token ignored",
                        songIdentifier = songIdentifier,
                        lineNumber = lineNumber,
                    )
                }
            }
        }

        trackAccumulators.values.forEach { it.finalizeCurrentLine() }

        return BodyParseResult(
            trackSections = trackAccumulators.values.map { it.toTrackSection() },
            diagnostics = diagnostics,
            hasFatalError = false,
        )
    }

    private fun parseNote(line: String): NoteEvent.Note? {
        val token = line.first()
        val parts = line.substring(1).trim().split(Regex("\\s+"), limit = 4)
        if (parts.size < 4) {
            return null
        }

        val startBeat = parts[0].toIntOrNull() ?: return null
        val durationBeats = parts[1].toIntOrNull() ?: return null
        val tone = parts[2].toIntOrNull() ?: return null
        val lyricText = parts[3]

        val noteType = when {
            durationBeats == 0 -> NoteType.FREESTYLE
            token == '*' -> NoteType.GOLDEN
            token == 'F' -> NoteType.FREESTYLE
            token == 'R' -> NoteType.RAP
            token == 'G' -> NoteType.RAP_GOLDEN
            else -> NoteType.NORMAL
        }

        return NoteEvent.Note(
            noteType = noteType,
            startBeat = startBeat,
            durationBeats = durationBeats,
            tone = tone,
            lyricText = lyricText,
        )
    }

    private fun fatalResult(
        trackAccumulators: Map<TrackId, TrackAccumulator>,
        diagnostics: List<DiagnosticEntry>,
        diagnostic: DiagnosticEntry,
    ): BodyParseResult = BodyParseResult(
        trackSections = trackAccumulators.values.map { it.toTrackSection() },
        diagnostics = diagnostics + diagnostic,
        hasFatalError = true,
    )

    private fun error(
        code: DiagnosticCode,
        message: String,
        songIdentifier: String,
        lineNumber: Int,
    ): DiagnosticEntry = DiagnosticEntry(
        severity = DiagnosticSeverity.ERROR,
        code = code,
        message = message,
        songIdentifier = songIdentifier,
        lineNumber = lineNumber,
    )
}
