package com.couchraoke.tv.domain.usdx.internal

import com.couchraoke.tv.domain.usdx.ParseException
import com.couchraoke.tv.domain.usdx.UsdxParser
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.Severity
import com.couchraoke.tv.domain.usdx.model.SongTiming
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal class DefaultUsdxParser : UsdxParser {
    override fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong> {
        val text = decodeText(txtBytes)
        val lines = text.normalizeNewlines().split('\n')
        val header = HeaderParser.parse(lines, txtUri = songId)
        val headerDiagnostics = header.diagnostics
        val headerFailure = headerDiagnostics.takeIf { diagnostics -> diagnostics.anyInvalid() }
        if (headerFailure != null) {
            return Result.failure(ParseException(headerFailure))
        }

        val body = BodyParser.parse(lines, startIndex = header.bodyStartIndex, txtUri = songId)
        val diagnostics = headerDiagnostics + body.diagnostics
        val invalidDiagnostics = diagnostics.takeIf { combinedDiagnostics -> combinedDiagnostics.anyInvalid() }
        return if (invalidDiagnostics != null) {
            Result.failure(ParseException(invalidDiagnostics))
        } else {
            val songHeader = header.draft.toSongHeader(
                isDuet = body.isDuet,
            )
            Result.success(
                ParsedSong(
                    songId = songId,
                    header = songHeader,
                    timing = SongTiming(bpmFile = songHeader.bpmFile),
                    tracks = body.tracks,
                    diagnostics = diagnostics,
                )
            )
        }
    }

    private fun decodeText(bytes: ByteArray): String =
        try {
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            String(bytes, StandardCharsets.ISO_8859_1)
        }

    private fun String.normalizeNewlines(): String =
        replace("\r\n", "\n").replace('\r', '\n')

    private fun List<DiagnosticEntry>.anyInvalid(): Boolean =
        any { it.severity == Severity.Invalid }
}
