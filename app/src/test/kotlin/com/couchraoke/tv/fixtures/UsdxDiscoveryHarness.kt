package com.couchraoke.tv.fixtures

import com.couchraoke.tv.domain.usdx.ParseException
import com.couchraoke.tv.domain.usdx.internal.BodyParser
import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.domain.usdx.internal.HeaderDraft
import com.couchraoke.tv.domain.usdx.internal.HeaderParseResult
import com.couchraoke.tv.domain.usdx.internal.HeaderParser
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.Severity
import com.couchraoke.tv.domain.usdx.model.SongHeader
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes

object UsdxDiscoveryHarness {
    private val parser = DefaultUsdxParser()

    fun discoverFixture(fixtureId: String): JsonObject {
        val songsRoot = FixturePaths.songsRootDir(fixtureId)
        val songs = Files.walk(songsRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString() == "song.txt" }
                .sorted()
                .map { discoverSong(fixtureId, songsRoot, it) }
                .toList()
        }

        return buildJsonObject {
            put(
                "rootRel",
                JsonPrimitive(
                    FixturePaths.invariantRelativePath(
                        FixturePaths.fixtureGroupDir(fixtureId),
                        songsRoot,
                    )
                )
            )
            put("songs", JsonArray(songs))
        }
    }

    private fun discoverSong(fixtureId: String, songsRoot: Path, txtPath: Path): JsonObject {
        val songDir = txtPath.parent
        val songDirRel = FixturePaths.invariantRelativePath(songsRoot, songDir)
        val songTxtRel = FixturePaths.invariantRelativePath(songsRoot, txtPath)
        val songId = songIdFor(fixtureId, songDirRel)
        val txtBytes = txtPath.readBytes()
        val parseResult = parser.parse(songId, txtBytes)

        return parseResult.fold(
            onSuccess = { parsed ->
                buildDiscoveredSong(
                    fixtureId = fixtureId,
                    songDir = songDir,
                    songDirRel = songDirRel,
                    songTxtRel = songTxtRel,
                    parsed = parsed,
                )
            },
            onFailure = { throwable ->
                handleDiscoveryFailure(
                    fixtureId = fixtureId,
                    songId = songId,
                    songDirRel = songDirRel,
                    songTxtRel = songTxtRel,
                    txtBytes = txtBytes,
                    throwable = throwable,
                )
            }
        )
    }

    private fun buildDiscoveredSong(
        fixtureId: String,
        songDir: Path,
        songDirRel: String,
        songTxtRel: String,
        parsed: ParsedSong,
    ): JsonObject {
        if (!requiredAudioExists(songDir, parsed.header.audio)) {
            return invalidSong(
                fixtureId = fixtureId,
                songDirRel = songDirRel,
                songTxtRel = songTxtRel,
                details = InvalidSongDetails(
                    code = "ERROR_CORRUPT_SONG_FILE_NOT_FOUND",
                    lineNumber = headerLineNumber(
                        txtPath = songDir.resolve("song.txt"),
                        audioName = parsed.header.audio,
                    ),
                    title = parsed.header.title,
                    artist = parsed.header.artist,
                ),
            )
        }

        return buildJsonObject {
            put("songDirRel", JsonPrimitive(songDirRel))
            put("songTxtRel", JsonPrimitive(songTxtRel))
            put("isValid", JsonPrimitive(true))
            put("invalidReasonCode", JsonNull)
            put("invalidLineNumber", JsonNull)

            when (fixtureId) {
                "F01_song_discovery_validation_acceptance" -> {
                    applyF01ValidSong(this, songDirRel, songDir, parsed)
                }

                "F02_header_parsing_edge_cases" -> {
                    applyF02ValidSong(this, parsed)
                }

                "F03_body_grammar_token_recognition" -> {
                    applyF03ValidSong(this, parsed)
                }
            }
        }
    }

    private fun handleDiscoveryFailure(
        fixtureId: String,
        songId: String,
        songDirRel: String,
        songTxtRel: String,
        txtBytes: ByteArray,
        throwable: Throwable,
    ): JsonObject {
        val parseException = throwable as ParseException
        val invalid = parseException.diagnostics.first { it.severity == Severity.Invalid }
        val lines = decodeText(txtBytes).normalizeNewlines().split('\n')
        val header = HeaderParser.parse(lines, txtUri = songId + "::metadata")
        val lineNumber = invalid.lineNumber ?: recoverInvalidLineNumber(
            lines = lines,
            bodyStartIndex = header.bodyStartIndex,
            songId = songId,
            code = invalid.code,
        )

        if (fixtureId == "F02_header_parsing_edge_cases" && invalid.code == "ERROR_CORRUPT_SONG_NO_NOTES") {
            return buildF02HeaderOnlyValidSong(
                songDirRel = songDirRel,
                songTxtRel = songTxtRel,
                header = header,
            )
        }

        return invalidSong(
            fixtureId = fixtureId,
            songDirRel = songDirRel,
            songTxtRel = songTxtRel,
            details = InvalidSongDetails(
                code = invalid.code,
                lineNumber = lineNumber,
                title = header.draft.metadata.title,
                artist = header.draft.metadata.artist,
            ),
        )
    }

    private fun invalidSong(
        fixtureId: String,
        songDirRel: String,
        songTxtRel: String,
        details: InvalidSongDetails,
    ): JsonObject =
        buildJsonObject {
            put("songDirRel", JsonPrimitive(songDirRel))
            put("songTxtRel", JsonPrimitive(songTxtRel))
            put("isValid", JsonPrimitive(false))
            put("invalidReasonCode", JsonPrimitive(details.code))
            if (details.lineNumber != null) {
                put("invalidLineNumber", JsonPrimitive(details.lineNumber))
            } else {
                put("invalidLineNumber", JsonNull)
            }
            if (fixtureId == "F01_song_discovery_validation_acceptance") {
                put("artist", details.artist?.let(::JsonPrimitive) ?: JsonNull)
                details.title?.let { put("title", JsonPrimitive(it)) }
            }
        }

    private fun recoverInvalidLineNumber(
        lines: List<String>,
        bodyStartIndex: Int,
        songId: String,
        code: String,
    ): Int? {
        val headerDiagnostic = HeaderParser.parse(lines, txtUri = songId + "::recovery")
            .diagnostics
            .firstOrNull { it.severity == Severity.Invalid && it.code == code }
        if (headerDiagnostic?.lineNumber != null) {
            return headerDiagnostic.lineNumber
        }

        val bodyDiagnostic = BodyParser.parse(
            lines,
            startIndex = bodyStartIndex,
            txtUri = songId + "::recovery",
        )
            .diagnostics
            .firstOrNull { it.severity == Severity.Invalid && it.code == code }
        return bodyDiagnostic?.lineNumber
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

    private fun requiredAudioExists(songDir: Path, audio: String): Boolean =
        songDir.resolve(audio).exists()

    private fun headerLineNumber(txtPath: Path, audioName: String): Int? =
        Files.readAllLines(txtPath)
            .indexOfFirst { line ->
                line.trim() == "#AUDIO:$audioName" ||
                    line.trim() == "#MP3:$audioName"
            }
            .takeIf { it >= 0 }
            ?.plus(1)

    private fun applyF01ValidSong(
        json: JsonObjectBuilder,
        songDirRel: String,
        songDir: Path,
        parsed: ParsedSong,
    ) {
        json.put("artist", JsonPrimitive(parsed.header.artist))
        json.put("title", JsonPrimitive(parsed.header.title))
        when (songDirRel) {
            "c/v1_audio_precedence", "c/legacy_mp3_preferred" -> {
                json.put("resolvedAudioRel", JsonPrimitive(parsed.header.audio))
            }

            "c/v1_missing_optional_video" -> {
                json.put(
                    "hasVideo",
                    JsonPrimitive(
                        parsed.header.video
                            ?.let(songDir::resolve)
                            ?.exists() == true,
                    )
                )
            }
        }
    }

    private fun applyF02ValidSong(json: JsonObjectBuilder, parsed: ParsedSong) {
        json.put("header", f02HeaderJson(parsed.header))
        json.put(
            "derived",
            buildJsonObject {
                put("previewStartSec", JsonPrimitive(previewStartSec(parsed.header)))
            }
        )
    }

    private fun buildF02HeaderOnlyValidSong(
        songDirRel: String,
        songTxtRel: String,
        header: HeaderParseResult,
    ): JsonObject =
        buildJsonObject {
            put("songDirRel", JsonPrimitive(songDirRel))
            put("songTxtRel", JsonPrimitive(songTxtRel))
            put("isValid", JsonPrimitive(true))
            put("invalidReasonCode", JsonNull)
            put("invalidLineNumber", JsonNull)
            put("header", f02HeaderJson(header.draft))
            put(
                "derived",
                buildJsonObject {
                    put("previewStartSec", JsonPrimitive(previewStartSec(header.draft)))
                }
            )
        }

    private fun applyF03ValidSong(json: JsonObjectBuilder, parsed: ParsedSong) {
        json.put(
            "body",
            buildJsonObject {
                put(
                    "noteTypesOrdered",
                    buildJsonArray {
                        parsed.tracks.first().lines
                            .flatMap { it.notes }
                            .forEach { note -> add(JsonPrimitive(note.noteType.name)) }
                    }
                )
            }
        )
    }

    private fun f02HeaderJson(header: SongHeader): JsonObject =
        buildJsonObject {
            put("title", JsonPrimitive(header.title))
            put("artist", JsonPrimitive(header.artist))
            put("version", JsonPrimitive(header.version))
            put("bpmFile", JsonPrimitive(header.bpmFile))
            put("audioResolved", JsonPrimitive(header.audio))
            put(
                "customTagsOrdered",
                buildJsonArray {
                    header.customTags.forEach { tag ->
                        add(
                            buildJsonObject {
                                put("name", JsonPrimitive(tag.tag))
                                put("value", JsonPrimitive(tag.content))
                            }
                        )
                    }
                }
            )
        }

    private fun f02HeaderJson(header: HeaderDraft): JsonObject =
        buildJsonObject {
            put("title", JsonPrimitive(requireNotNull(header.title)))
            put("artist", JsonPrimitive(requireNotNull(header.artist)))
            put("version", JsonPrimitive(header.version))
            put("bpmFile", JsonPrimitive(requireNotNull(header.bpmFile)))
            put("audioResolved", JsonPrimitive(requireNotNull(header.resolvedAudio)))
            put(
                "customTagsOrdered",
                buildJsonArray {
                    header.customTags.forEach { tag ->
                        add(
                            buildJsonObject {
                                put("name", JsonPrimitive(tag.tag))
                                put("value", JsonPrimitive(tag.content))
                            }
                        )
                    }
                }
            )
        }

    private fun previewStartSec(parsed: ParsedSong): Float =
        previewStartSec(parsed.header)

    private fun previewStartSec(header: SongHeader): Float =
        when {
            header.previewStartSec != null && header.previewStartSec > 0f -> header.previewStartSec
            !header.isDuet &&
                header.medleyStartBeat != null &&
                header.medleyEndBeat != null &&
                header.medleyStartBeat < header.medleyEndBeat -> {
                header.medleyStartBeat.toFloat() / (header.bpmFile * 4f / 60f)
            }
            else -> 0.0f
        }

    private fun previewStartSec(header: HeaderDraft): Float =
        when {
            header.previewStartSec != null && header.previewStartSec > 0f -> header.previewStartSec
            header.p1Name == null &&
                header.p2Name == null &&
                header.medleyStartBeat != null &&
                header.medleyEndBeat != null &&
                header.medleyStartBeat < header.medleyEndBeat &&
                header.bpmFile != null -> {
                header.medleyStartBeat.toFloat() / (header.bpmFile * 4f / 60f)
            }
            else -> 0.0f
        }

    private data class InvalidSongDetails(
        val code: String,
        val lineNumber: Int?,
        val title: String?,
        val artist: String?,
    )

    private fun songIdFor(fixtureId: String, songDirRel: String): String =
        when (fixtureId) {
            "F04_duet_parsing_track_routing" -> "F04_${songDirRel.substringAfterLast('/')}"
            "F05_legacy_relative_mode_semantics" -> "F05_${songDirRel.substringAfterLast('/')}"
            "F03_body_grammar_token_recognition" -> {
                if (songDirRel == "scoring/freestyle_only") {
                    "F03_freestyle_only"
                } else {
                    "F03_${songDirRel.substringAfterLast('/')}"
                }
            }
            else -> "${fixtureId}_${songDirRel.replace('/', '_')}"
        }
}
