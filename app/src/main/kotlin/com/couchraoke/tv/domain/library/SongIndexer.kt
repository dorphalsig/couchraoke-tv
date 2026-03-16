package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.DiagnosticCode
import com.couchraoke.tv.domain.parser.DiagnosticSeverity
import com.couchraoke.tv.domain.parser.MedleySource
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
        val ds = parseResult.parsedSong.derivedSummary
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
            isDuet = ds.isDuet,
            hasRap = ds.hasRap,
            hasVideo = ds.hasVideo,
            hasInstrumental = ds.hasInstrumental,
            medleySource = ds.medleySource,
            medleyStartBeat = ds.medleyStartBeat,
            medleyEndBeat = ds.medleyEndBeat,
            calcMedleyEnabled = ds.calcMedleyEnabled,
            canMedley = !ds.isDuet && ds.medleySource == MedleySource.EXPLICIT,
            previewStartSec = when {
                (ds.previewStartSec ?: 0.0) > 0.0 -> ds.previewStartSec!!
                !ds.isDuet && ds.medleySource == MedleySource.EXPLICIT -> {
                    val bpm = parseResult.parsedSong.header.bpm ?: 1.0
                    val gapMs = parseResult.parsedSong.header.gapMs ?: 0
                    val startBeat = ds.medleyStartBeat ?: 0
                    startBeat * 60.0 / (bpm * 4.0) + gapMs / 1000.0
                }
                else -> 0.0
            },
            txtUrl = txtUrl,
            audioUrl = audioUrl,
            videoUrl = videoUrl,
            coverUrl = coverUrl,
            backgroundUrl = backgroundUrl,
            instrumentalUrl = instrumentalUrl,
            vocalsUrl = vocalsUrl,
        )
    }

    fun fromManifestEntry(
        entry: ManifestEntry,
        phoneClientId: String,
    ): SongEntry {
        val invalidPath = entry.relativeTxtPath.contains("..") || entry.relativeTxtPath.startsWith("/")
        return SongEntry(
            songId = "$phoneClientId::${entry.relativeTxtPath}",
            phoneClientId = phoneClientId,
            relativeTxtPath = entry.relativeTxtPath,
            modifiedTimeMs = entry.modifiedTimeMs,
            isValid = !invalidPath && entry.isValid,
            invalidReasonCode = if (invalidPath) "ERROR_CORRUPT_SONG_INVALID_PATH" else entry.invalidReasonCode,
            invalidLineNumber = if (invalidPath) null else entry.invalidLineNumber,
            artist = entry.artist,
            title = entry.title,
            album = entry.album,
            isDuet = entry.isDuet,
            hasRap = entry.hasRap,
            hasVideo = entry.hasVideo,
            hasInstrumental = entry.hasInstrumental,
            canMedley = entry.canMedley,
            medleySource = if (entry.medleySource == "tag") MedleySource.EXPLICIT else MedleySource.NONE,
            medleyStartBeat = entry.medleyStartBeat,
            medleyEndBeat = entry.medleyEndBeat,
            calcMedleyEnabled = true,
            previewStartSec = entry.previewStartSec,
            txtUrl = entry.txtUrl ?: "",
            audioUrl = entry.audioUrl,
            videoUrl = entry.videoUrl,
            coverUrl = entry.coverUrl,
            backgroundUrl = entry.backgroundUrl,
            instrumentalUrl = entry.instrumentalUrl,
            vocalsUrl = entry.vocalsUrl,
        )
    }
}
