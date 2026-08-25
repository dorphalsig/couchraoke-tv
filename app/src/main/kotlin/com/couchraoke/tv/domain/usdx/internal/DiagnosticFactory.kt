package com.couchraoke.tv.domain.usdx.internal

import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.Severity

internal object DiagnosticFactory {
    const val ERROR_CORRUPT_SONG_FILE_NOT_FOUND = "ERROR_CORRUPT_SONG_FILE_NOT_FOUND"
    const val ERROR_CORRUPT_SONG_NO_NOTES = "ERROR_CORRUPT_SONG_NO_NOTES"
    const val ERROR_CORRUPT_SONG_NO_BREAKS = "ERROR_CORRUPT_SONG_NO_BREAKS"
    const val ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER = "ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER"
    const val ERROR_CORRUPT_SONG_MALFORMED_HEADER = "ERROR_CORRUPT_SONG_MALFORMED_HEADER"
    const val ERROR_CORRUPT_SONG_MALFORMED_BODY = "ERROR_CORRUPT_SONG_MALFORMED_BODY"
    const val ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM = "ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM"
    const val ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE = "ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE"
    const val ERROR_CORRUPT_SONG_INVALID_VERSION = "ERROR_CORRUPT_SONG_INVALID_VERSION"
    const val ERROR_CORRUPT_SONG_INVALID_DUET_MARKER = "ERROR_CORRUPT_SONG_INVALID_DUET_MARKER"

    const val WARN_ZERO_DURATION_CONVERTED_TO_FREESTYLE = "WARN_ZERO_DURATION_CONVERTED_TO_FREESTYLE"
    const val WARN_UNKNOWN_BODY_TOKEN = "WARN_UNKNOWN_BODY_TOKEN"
    const val WARN_MALFORMED_OPTIONAL_HEADER = "WARN_MALFORMED_OPTIONAL_HEADER"

    // Mix-pair diagnostics (tv_app.md §2.4.7). Only the first is derivable from chart text and so
    // emitted by HeaderParser; the two ineligibility codes require decoded audio, which is
    // phone-side work, and are produced by the discovery layer.
    const val WARN_VOCALS_WITHOUT_INSTRUMENTAL = "WARN_VOCALS_WITHOUT_INSTRUMENTAL"
    const val WARN_MIX_PAIR_INELIGIBLE_SAMPLE_RATE = "WARN_MIX_PAIR_INELIGIBLE_SAMPLE_RATE"
    const val WARN_MIX_PAIR_INELIGIBLE_CHANNELS = "WARN_MIX_PAIR_INELIGIBLE_CHANNELS"

    fun invalid(code: String, txtUri: String, lineNumber: Int? = null): DiagnosticEntry =
        DiagnosticEntry(
            severity = Severity.Invalid,
            code = code,
            txtUri = txtUri,
            lineNumber = lineNumber,
        )

    fun warn(code: String, txtUri: String, lineNumber: Int? = null): DiagnosticEntry =
        DiagnosticEntry(
            severity = Severity.Warn,
            code = code,
            txtUri = txtUri,
            lineNumber = lineNumber,
        )
}
