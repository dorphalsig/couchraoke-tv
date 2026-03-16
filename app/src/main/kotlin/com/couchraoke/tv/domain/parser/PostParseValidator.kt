package com.couchraoke.tv.domain.parser

data class PostParseValidationResult(
    val tracks: List<Track>,
    val derivedSummary: DerivedSongSummary,
    val diagnostics: List<DiagnosticEntry> = emptyList(),
    val hasFatalError: Boolean = false,
)

class PostParseValidator {
    fun finalize(
        songIdentifier: String,
        header: SongHeader,
        bodyResult: BodyParseResult,
        fileResolver: FileResolver,
    ): PostParseValidationResult {
        val tracks = bodyResult.trackSections.map { section ->
            val lines = if (section.groupedEvents.isNotEmpty()) {
                section.groupedEvents
                    .filter { it.isNotEmpty() }
                    .map { notes -> Line(startBeat = notes.first().startBeat, notes = notes) }
            } else if (section.events.isNotEmpty()) {
                listOf(Line(startBeat = section.events.first().startBeat, notes = section.events))
            } else {
                emptyList()
            }
            Track(trackId = section.trackId, lines = lines)
        }

        val diagnostics = mutableListOf<DiagnosticEntry>()
        val emptyTrack = tracks.firstOrNull { it.lines.isEmpty() }
        if (emptyTrack != null) {
            diagnostics += DiagnosticEntry(
                severity = DiagnosticSeverity.ERROR,
                code = DiagnosticCode.EMPTY_TRACK_AFTER_CLEANUP,
                message = "Track has no remaining non-empty lines after cleanup",
                songIdentifier = songIdentifier,
                lineNumber = null,
            )
        }

        val previewStartSec = header.previewStartSec
        val hasLocalVideo = header.videoReference?.let { reference ->
            !reference.contains("://") && fileResolver.exists(reference)
        } == true

        val derivedSummary = DerivedSongSummary(
            isDuet = tracks.size > 1,
            hasRap = tracks.any { track ->
                track.lines.any { line ->
                    line.notes.any { note ->
                        note.noteType == NoteType.RAP || note.noteType == NoteType.RAP_GOLDEN
                    }
                }
            },
            hasVideo = hasLocalVideo,
            hasInstrumental = header.instrumentalReference?.let(fileResolver::exists) == true,
            previewStartSec = previewStartSec,
            medleySource = if (header.medleyStartBeat != null && header.medleyEndBeat != null) MedleySource.EXPLICIT else MedleySource.NONE,
            medleyStartBeat = header.medleyStartBeat,
            medleyEndBeat = header.medleyEndBeat,
            calcMedleyEnabled = header.calcMedleyEnabled,
        )

        return PostParseValidationResult(
            tracks = tracks,
            derivedSummary = derivedSummary,
            diagnostics = diagnostics,
            hasFatalError = diagnostics.any { it.severity == DiagnosticSeverity.ERROR },
        )
    }
}
