package com.couchraoke.tv.domain.usdx.internal

import com.couchraoke.tv.domain.usdx.model.CustomHeaderTag
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.SongHeader

internal data class HeaderParseResult(
    val bodyStartIndex: Int,
    val draft: HeaderDraft,
    val diagnostics: List<DiagnosticEntry>,
)

internal data class HeaderMetadata(
    val title: String?,
    val artist: String?,
)

internal data class HeaderDraft(
    val title: String?,
    val artist: String?,
    val bpmFile: Float?,
    val gapMs: Float,
    val resolvedAudio: String?,
    val resolvedAudioLineNumber: Int?,
    val metadata: HeaderMetadata,
    val startSec: Float?,
    val endMs: Int?,
    val videoGapSec: Float?,
    val previewStartSec: Float?,
    val video: String?,
    val cover: String?,
    val background: String?,
    val instrumental: String?,
    val vocals: String?,
    val version: String,
    val year: Int?,
    val genre: String?,
    val album: String?,
    val p1Name: String?,
    val p2Name: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val customTags: List<CustomHeaderTag>,
) {
    fun toSongHeader(isDuet: Boolean): SongHeader =
        SongHeader(
            title = requireNotNull(title),
            artist = requireNotNull(artist),
            bpmFile = requireNotNull(bpmFile),
            gapMs = gapMs,
            audio = resolvedAudio,
            startSec = startSec,
            endMs = endMs,
            videoGapSec = videoGapSec,
            previewStartSec = previewStartSec,
            video = video,
            cover = cover,
            background = background,
            instrumental = instrumental,
            vocals = vocals,
            version = version,
            year = year,
            genre = genre,
            album = album,
            isDuet = isDuet,
            p1Name = p1Name,
            p2Name = p2Name,
            medleyStartBeat = medleyStartBeat,
            medleyEndBeat = medleyEndBeat,
            customTags = customTags,
        )
}

internal object HeaderParser {
    fun parse(lines: List<String>, txtUri: String): HeaderParseResult =
        Session(lines, txtUri).parse()

    private class Session(
        private val lines: List<String>,
        private val txtUri: String,
    ) {
        private val diagnostics = mutableListOf<DiagnosticEntry>()
        private val customTags = mutableListOf<CustomHeaderTag>()
        private val knownTags = KnownHeaderTags()
        private var bodyStartIndex = lines.size

        fun parse(): HeaderParseResult {
            parseHeaderLines()
            return HeaderParseResult(
                bodyStartIndex = bodyStartIndex,
                draft = buildDraft(),
                diagnostics = diagnostics,
            )
        }

        private fun parseHeaderLines() {
            var index = 0
            var readingHeaders = true
            while (index < lines.size && readingHeaders) {
                val line = lines[index]
                if (line.startsWith("#")) {
                    parseHeaderLine(line, index + 1)
                    index += 1
                } else {
                    bodyStartIndex = index
                    readingHeaders = false
                }
            }
        }

        private fun parseHeaderLine(line: String, lineNumber: Int) {
            val raw = line.removePrefix("#")
            val separatorIndex = raw.indexOf(':')
            if (separatorIndex < 0) {
                customTags += CustomHeaderTag(tag = "", content = raw.trim())
            } else {
                parseSeparatedHeader(raw, separatorIndex, lineNumber)
            }
        }

        private fun parseSeparatedHeader(raw: String, separatorIndex: Int, lineNumber: Int) {
            val rawName = raw.substring(0, separatorIndex).trim()
            val value = raw.substring(separatorIndex + 1)
            when (rawName.uppercase()) {
                "TITLE" -> knownTags.title = value.takeUnless(String::isBlank)
                "ARTIST" -> knownTags.artist = value.takeUnless(String::isBlank)
                "BPM" -> parseBpm(value, lineNumber)
                "GAP" -> knownTags.gapMs = value.toFloatOrWarn(lineNumber) ?: knownTags.gapMs
                "AUDIO" -> knownTags.setAudio(value.takeUnless(String::isBlank), lineNumber)
                "MP3" -> knownTags.setMp3(value.takeUnless(String::isBlank), lineNumber)
                "START" -> knownTags.startSec = value.toFloatOrWarn(lineNumber)
                "END" -> knownTags.endMs = value.toIntOrWarn(lineNumber)
                "VIDEOGAP" -> knownTags.videoGapSec = value.toFloatOrWarn(lineNumber)
                "PREVIEWSTART" -> knownTags.previewStartSec = value.toFloatOrWarn(lineNumber)
                "VIDEO" -> knownTags.video = value.takeUnless(String::isBlank)
                "COVER" -> knownTags.cover = value.takeUnless(String::isBlank)
                "BACKGROUND" -> knownTags.background = value.takeUnless(String::isBlank)
                "INSTRUMENTAL" -> knownTags.instrumental = value.takeUnless(String::isBlank)
                "VOCALS" -> knownTags.setVocals(value.takeUnless(String::isBlank), lineNumber)
                "VERSION" -> parseVersion(value, lineNumber)
                "YEAR" -> knownTags.year = value.toIntOrWarn(lineNumber)
                "GENRE" -> knownTags.genre = value.takeUnless(String::isBlank)
                "ALBUM" -> knownTags.album = value.takeUnless(String::isBlank)
                "P1" -> knownTags.p1Name = value.takeUnless(String::isBlank)
                "P2" -> knownTags.p2Name = value.takeUnless(String::isBlank)
                "MEDLEYSTARTBEAT" -> knownTags.medleyStartBeat = value.toIntOrWarn(lineNumber)
                "MEDLEYENDBEAT" -> knownTags.medleyEndBeat = value.toIntOrWarn(lineNumber)
                else -> customTags += CustomHeaderTag(tag = rawName, content = value)
            }
        }

        private fun parseBpm(value: String, lineNumber: Int) {
            val parsed = value.trim().toFloatOrNull()
            if (parsed == null || parsed <= 0f) {
                diagnostics += DiagnosticFactory.invalid(
                    code = DiagnosticFactory.ERROR_CORRUPT_SONG_MALFORMED_HEADER,
                    txtUri = txtUri,
                    lineNumber = lineNumber,
                )
            } else {
                knownTags.bpmFile = parsed
            }
        }

        private fun parseVersion(value: String, lineNumber: Int) {
            val parsedVersion = value.trim()
            if (!parsedVersion.isSemanticVersion() || parsedVersion.isUnsupportedVersion()) {
                diagnostics += DiagnosticFactory.invalid(
                    code = DiagnosticFactory.ERROR_CORRUPT_SONG_INVALID_VERSION,
                    txtUri = txtUri,
                    lineNumber = lineNumber,
                )
            } else {
                knownTags.version = parsedVersion
            }
        }

        private fun buildDraft(): HeaderDraft {
            val resolvedVersion = knownTags.version ?: "0.3.0"
            val resolvedAudio = resolvedAudio(resolvedVersion)
            if (knownTags.vocals != null && knownTags.instrumental == null) {
                diagnostics += vocalsWithoutInstrumental(txtUri, knownTags.vocalsLineNumber)
            }
            // Base audio is optional when an #INSTRUMENTAL+#VOCALS pair is authored; the phone
            // serves the generated mix as the effective audioUrl (tv_app.md §2.4.7, §2.5.5).
            val audioRequirementSatisfied = resolvedAudio.value != null || knownTags.hasAuthoredMixPair
            val missingCount = listOf(
                knownTags.title,
                knownTags.artist,
                knownTags.bpmFile,
            ).count { value -> value == null } + if (audioRequirementSatisfied) 0 else 1
            repeat(missingCount) {
                diagnostics += DiagnosticFactory.invalid(
                    code = DiagnosticFactory.ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER,
                    txtUri = txtUri,
                )
            }
            return HeaderDraft(
                title = knownTags.title,
                artist = knownTags.artist,
                bpmFile = knownTags.bpmFile,
                gapMs = knownTags.gapMs,
                resolvedAudio = resolvedAudio.value,
                resolvedAudioLineNumber = resolvedAudio.lineNumber,
                metadata = HeaderMetadata(knownTags.title, knownTags.artist),
                startSec = knownTags.startSec,
                endMs = knownTags.endMs,
                videoGapSec = knownTags.videoGapSec,
                previewStartSec = knownTags.previewStartSec,
                video = knownTags.video,
                cover = knownTags.cover,
                background = knownTags.background,
                instrumental = knownTags.instrumental,
                vocals = knownTags.vocals,
                version = resolvedVersion,
                year = knownTags.year,
                genre = knownTags.genre,
                album = knownTags.album,
                p1Name = knownTags.p1Name,
                p2Name = knownTags.p2Name,
                medleyStartBeat = knownTags.medleyStartBeat,
                medleyEndBeat = knownTags.medleyEndBeat,
                customTags = customTags,
            )
        }

        private fun resolvedAudio(version: String = knownTags.version ?: "0.3.0"): HeaderValue =
            if (version.isLegacyVersion()) {
                HeaderValue(knownTags.mp3, knownTags.mp3LineNumber)
            } else {
                HeaderValue(
                    value = knownTags.audio ?: knownTags.mp3,
                    lineNumber = knownTags.audioLineNumber ?: knownTags.mp3LineNumber,
                )
            }

        private fun String.toFloatOrWarn(lineNumber: Int): Float? =
            trim().toFloatOrNull().also { parsed ->
                if (parsed == null) diagnostics += malformedOptionalHeader(txtUri, lineNumber)
            }

        private fun String.toIntOrWarn(lineNumber: Int): Int? =
            trim().toIntOrNull().also { parsed ->
                if (parsed == null) diagnostics += malformedOptionalHeader(txtUri, lineNumber)
            }
    }

    private fun malformedOptionalHeader(txtUri: String, lineNumber: Int): DiagnosticEntry =
        DiagnosticFactory.warn(
            code = DiagnosticFactory.WARN_MALFORMED_OPTIONAL_HEADER,
            txtUri = txtUri,
            lineNumber = lineNumber,
        )

    /**
     * `#VOCALS` without `#INSTRUMENTAL` is ignored for playback resolution (tv_app.md §2.4.7).
     *
     * This is the only mix-pair diagnostic derivable from the chart text alone. The two
     * `WARN_MIX_PAIR_INELIGIBLE_*` codes compare decoded sample rate and channel count, which is
     * phone-side work — the TV never decodes or mixes those sources (tv_app.md §2.6.7).
     */
    private fun vocalsWithoutInstrumental(txtUri: String, lineNumber: Int?): DiagnosticEntry =
        DiagnosticFactory.warn(
            code = DiagnosticFactory.WARN_VOCALS_WITHOUT_INSTRUMENTAL,
            txtUri = txtUri,
            lineNumber = lineNumber,
        )
    private data class KnownHeaderTags(
        var title: String? = null,
        var artist: String? = null,
        var bpmFile: Float? = null,
        var gapMs: Float = 0f,
        var audio: String? = null,
        var audioLineNumber: Int? = null,
        var mp3: String? = null,
        var mp3LineNumber: Int? = null,
        var startSec: Float? = null,
        var endMs: Int? = null,
        var videoGapSec: Float? = null,
        var previewStartSec: Float? = null,
        var video: String? = null,
        var cover: String? = null,
        var background: String? = null,
        var instrumental: String? = null,
        var vocals: String? = null,
        var vocalsLineNumber: Int? = null,
        var version: String? = null,
        var year: Int? = null,
        var genre: String? = null,
        var album: String? = null,
        var p1Name: String? = null,
        var p2Name: String? = null,
        var medleyStartBeat: Int? = null,
        var medleyEndBeat: Int? = null,
    ) {
        /**
         * Base audio is optional when both mix sources are authored; whether the pair is actually
         * accepted depends on decoded sample rate and channel count, which is phone-side
         * (tv_app.md §2.4.7, §2.6.7).
         */
        val hasAuthoredMixPair: Boolean
            get() = instrumental != null && vocals != null

        fun setAudio(value: String?, lineNumber: Int) {
            audio = value
            audioLineNumber = lineNumber
        }

        fun setMp3(value: String?, lineNumber: Int) {
            mp3 = value
            mp3LineNumber = lineNumber
        }

        fun setVocals(value: String?, lineNumber: Int) {
            vocals = value
            vocalsLineNumber = lineNumber
        }
    }

    private data class HeaderValue(
        val value: String? = null,
        val lineNumber: Int? = null,
    )

    private fun String.isSemanticVersion(): Boolean =
        Regex("\\d+\\.\\d+\\.\\d+").matches(trim())

    private fun String.isUnsupportedVersion(): Boolean {
        val parts = trim().split('.').map(String::toInt)
        return parts[0] >= 2
    }

    private fun String.isLegacyVersion(): Boolean {
        val parts = trim().split('.').map(String::toInt)
        return parts[0] < 1
    }
}
