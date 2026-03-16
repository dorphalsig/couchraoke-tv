package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.DiagnosticCode
import com.couchraoke.tv.domain.parser.DiagnosticSeverity
import com.couchraoke.tv.domain.parser.ParseResult

object SongIndexer {
    fun fromParseResult(
        parseResult: ParseResult,
        phoneClientId: String,
        relativeTxtPath: String,
        modifiedTimeMs: Long,
        txtUrl: String,
        audioUrl: String?,
        videoUrl: String?,
        coverUrl: String?,
        backgroundUrl: String?,
        instrumentalUrl: String?,
        vocalsUrl: String?,
    ): SongEntry {
        val invalidPath = relativeTxtPath.contains("..") || relativeTxtPath.startsWith("/")
        val firstError = parseResult.parsedSong.diagnostics
            .firstOrNull { it.severity == DiagnosticSeverity.ERROR }
        val reasonCode = when {
            invalidPath -> "ERROR_CORRUPT_SONG_INVALID_PATH"
            firstError == null -> null
            else -> when (firstError.code) {
                DiagnosticCode.MISSING_REQUIRED_ARTIST,
                DiagnosticCode.MISSING_REQUIRED_TITLE,
                DiagnosticCode.MISSING_REQUIRED_BPM,
                DiagnosticCode.MISSING_REQUIRED_AUDIO,
                DiagnosticCode.MISSING_REQUIRED_FIELD -> "ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER"
                DiagnosticCode.REQUIRED_AUDIO_NOT_FOUND -> "ERROR_CORRUPT_SONG_FILE_NOT_FOUND"
                DiagnosticCode.MALFORMED_NUMERIC_FIELD -> "ERROR_CORRUPT_SONG_MALFORMED_HEADER"
                DiagnosticCode.MALFORMED_BODY_FIELD -> "ERROR_CORRUPT_SONG_MALFORMED_BODY"
                DiagnosticCode.EMPTY_TRACK_AFTER_CLEANUP,
                DiagnosticCode.NO_USABLE_NOTES -> "ERROR_CORRUPT_SONG_NO_NOTES"
                else -> null
            }
        }
        val header = parseResult.parsedSong.header
        return SongEntry(
            songId = "$phoneClientId::$relativeTxtPath",
            phoneClientId = phoneClientId,
            relativeTxtPath = relativeTxtPath,
            modifiedTimeMs = modifiedTimeMs,
            isValid = !invalidPath && parseResult.parsedSong.isValid,
            invalidReasonCode = reasonCode,
            invalidLineNumber = if (invalidPath) null else firstError?.lineNumber,
            artist = header.artist,
            title = header.title,
            album = null,
            txtUrl = txtUrl,
            audioUrl = audioUrl,
            videoUrl = videoUrl,
            coverUrl = coverUrl,
            backgroundUrl = backgroundUrl,
            instrumentalUrl = instrumentalUrl,
            vocalsUrl = vocalsUrl,
        )
    }
}
