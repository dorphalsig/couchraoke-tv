package com.couchraoke.tv.domain.parser

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
}

enum class DiagnosticCode {
    UNKNOWN_HEADER_TAG,
    MISSING_REQUIRED_ARTIST,
    MISSING_REQUIRED_TITLE,
    MISSING_REQUIRED_BPM,
    MISSING_REQUIRED_AUDIO,
    MISSING_REQUIRED_FIELD,
    REQUIRED_AUDIO_NOT_FOUND,
    MALFORMED_NUMERIC_FIELD,
    UNSUPPORTED_VARIABLE_BPM,
    UNSUPPORTED_RELATIVE_FORMAT,
    INVALID_DUET_MARKER,
    NO_USABLE_NOTES,
    MALFORMED_VERSION,
    UNSUPPORTED_VERSION,
    EMPTY_TRACK_AFTER_CLEANUP,
    ZERO_DURATION_NOTE_CONVERTED,
    UNKNOWN_BODY_TOKEN,
    MALFORMED_OPTIONAL_HEADER,
    MALFORMED_BODY_FIELD,
}

data class DiagnosticEntry(
    val severity: DiagnosticSeverity,
    val code: DiagnosticCode,
    val message: String,
    val songIdentifier: String,
    val lineNumber: Int? = null,
)
