package com.couchraoke.tv.domain.parser

data class HeaderParseResult(
    val header: SongHeader,
    val diagnostics: List<DiagnosticEntry> = emptyList(),
    val hasFatalError: Boolean = false,
)

class HeaderParser {
    fun parse(
        songIdentifier: String,
        lines: List<String>,
        fileResolver: FileResolver,
    ): HeaderParseResult {
        val diagnostics = mutableListOf<DiagnosticEntry>()
        val customTags = mutableListOf<CustomTag>()

        var version: SongVersion? = null
        var title: String? = null
        var artist: String? = null
        var bpm: Double? = null
        var gapMs: Int? = null
        var startSec: Double? = null
        var endMs: Int? = null
        var previewStartSec: Double? = null
        var audioReference: String? = null
        var audioTag: String? = null
        var videoReference: String? = null
        var videoGapSec: Double? = null
        var coverReference: String? = null
        var backgroundReference: String? = null
        var instrumentalReference: String? = null
        var vocalsReference: String? = null
        var medleyStartBeat: Int? = null
        var medleyEndBeat: Int? = null
        var calcMedleyEnabled = false
        var p1Name: String? = null
        var p2Name: String? = null

        var audioLineNumber: Int? = null
        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val trimmed = rawLine.trim()
            if (!trimmed.startsWith("#")) {
                return@forEachIndexed
            }

            val separatorIndex = trimmed.indexOf(':')
            if (separatorIndex <= 1) {
                val tagName = trimmed.removePrefix("#")
                if (tagName.isNotBlank()) {
                    customTags += CustomTag(tagName = tagName, content = "", lineNumber = lineNumber)
                    diagnostics += DiagnosticEntry(
                        severity = DiagnosticSeverity.WARNING,
                        code = DiagnosticCode.MALFORMED_OPTIONAL_HEADER,
                        message = "Header-like line has no separator",
                        songIdentifier = songIdentifier,
                        lineNumber = lineNumber,
                    )
                }
                return@forEachIndexed
            }

            val tag = trimmed.substring(1, separatorIndex).uppercase()
            val value = trimmed.substring(separatorIndex + 1)

            when (tag) {
                "VERSION" -> {
                    version = parseVersion(value)
                    if (version == null) {
                        diagnostics += error(
                            code = DiagnosticCode.MALFORMED_VERSION,
                            message = "Malformed version",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        )
                    }
                }
                "TITLE" -> title = value
                "ARTIST" -> artist = value
                "AUDIO", "MP3" -> {
                    audioReference = value
                    audioTag = tag
                    audioLineNumber = lineNumber
                }
                "BPM" -> {
                    val parsed = value.toDoubleOrNull()
                    if (parsed == null) {
                        diagnostics += error(
                            code = DiagnosticCode.MALFORMED_NUMERIC_FIELD,
                            message = "BPM must be numeric",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        )
                    } else {
                        bpm = parsed
                    }
                }
                "GAP" -> gapMs = value.toIntOrNull()
                "START" -> startSec = value.toDoubleOrNull()
                "END" -> endMs = value.toIntOrNull()
                "PREVIEWSTART" -> previewStartSec = value.toDoubleOrNull()
                "VIDEO" -> videoReference = value
                "VIDEOGAP" -> videoGapSec = value.toDoubleOrNull()
                "COVER" -> coverReference = value
                "BACKGROUND" -> backgroundReference = value
                "INSTRUMENTAL" -> instrumentalReference = value
                "VOCALS" -> vocalsReference = value
                "MEDLEYSTARTBEAT" -> medleyStartBeat = value.toIntOrNull()
                "MEDLEYENDBEAT" -> medleyEndBeat = value.toIntOrNull()
                "CALCMEDLEY" -> calcMedleyEnabled = value.equals("on", ignoreCase = true) || value.equals("yes", ignoreCase = true) || value.equals("true", ignoreCase = true)
                "P1" -> p1Name = value
                "P2" -> p2Name = value
                "RELATIVE" -> {
                    if (value.equals("yes", ignoreCase = true) || value.equals("on", ignoreCase = true) || value.equals("true", ignoreCase = true)) {
                        diagnostics += error(
                            code = DiagnosticCode.UNSUPPORTED_RELATIVE_FORMAT,
                            message = "Relative sentence format is unsupported",
                            songIdentifier = songIdentifier,
                            lineNumber = lineNumber,
                        )
                    }
                }
                else -> {
                    customTags += CustomTag(tagName = tag, content = value, lineNumber = lineNumber)
                    diagnostics += DiagnosticEntry(
                        severity = DiagnosticSeverity.WARNING,
                        code = DiagnosticCode.UNKNOWN_HEADER_TAG,
                        message = "Unknown header tag preserved",
                        songIdentifier = songIdentifier,
                        lineNumber = lineNumber,
                    )
                }
            }
        }

        if (title.isNullOrBlank()) {
            diagnostics += error(
                code = DiagnosticCode.MISSING_REQUIRED_TITLE,
                message = "Title is required",
                songIdentifier = songIdentifier,
            )
        }
        if (artist.isNullOrBlank()) {
            diagnostics += error(
                code = DiagnosticCode.MISSING_REQUIRED_ARTIST,
                message = "Artist is required",
                songIdentifier = songIdentifier,
            )
        }
        if (bpm == null && diagnostics.none { it.code == DiagnosticCode.MALFORMED_NUMERIC_FIELD }) {
            diagnostics += error(
                code = DiagnosticCode.MISSING_REQUIRED_BPM,
                message = "BPM is required",
                songIdentifier = songIdentifier,
            )
        } else if (bpm != null && bpm <= 0.0) {
            diagnostics += error(
                code = DiagnosticCode.MISSING_REQUIRED_BPM,
                message = "BPM must be greater than zero",
                songIdentifier = songIdentifier,
            )
        }

        val missingAudioReference = if (version == null) {
            audioTag != "MP3"
        } else {
            audioReference.isNullOrBlank()
        }

        if (missingAudioReference) {
            diagnostics += error(
                code = DiagnosticCode.MISSING_REQUIRED_AUDIO,
                message = "Audio reference is required",
                songIdentifier = songIdentifier,
            )
        } else if (!fileResolver.exists(requireNotNull(audioReference))) {
            diagnostics += error(
                code = DiagnosticCode.REQUIRED_AUDIO_NOT_FOUND,
                message = "Required audio asset was not found",
                songIdentifier = songIdentifier,
                lineNumber = audioLineNumber,
            )
        }

        val header = SongHeader(
            version = version,
            title = title,
            artist = artist,
            bpm = bpm,
            gapMs = gapMs,
            startSec = startSec,
            endMs = endMs,
            previewStartSec = previewStartSec,
            audioReference = audioReference,
            videoReference = videoReference,
            videoGapSec = videoGapSec,
            coverReference = coverReference,
            backgroundReference = backgroundReference,
            instrumentalReference = instrumentalReference,
            vocalsReference = vocalsReference,
            medleyStartBeat = medleyStartBeat,
            medleyEndBeat = medleyEndBeat,
            calcMedleyEnabled = calcMedleyEnabled,
            p1Name = p1Name,
            p2Name = p2Name,
            customTags = customTags.toList(),
        )

        return HeaderParseResult(
            header = header,
            diagnostics = diagnostics,
            hasFatalError = diagnostics.any { it.severity == DiagnosticSeverity.ERROR },
        )
    }

    private fun parseVersion(value: String): SongVersion? {
        val parts = value.split('.')
        if (parts.size != 3) {
            return null
        }

        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        return SongVersion(major = major, minor = minor, patch = patch)
    }

    private fun error(
        code: DiagnosticCode,
        message: String,
        songIdentifier: String,
        lineNumber: Int? = null,
    ): DiagnosticEntry = DiagnosticEntry(
        severity = DiagnosticSeverity.ERROR,
        code = code,
        message = message,
        songIdentifier = songIdentifier,
        lineNumber = lineNumber,
    )
}
