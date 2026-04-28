package com.couchraoke.tv.fixtures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class FixtureManifestSnapshot(
    val specVersion: String,
    val fixtures: List<FixtureManifestEntry>,
)

@Serializable
data class FixtureManifestEntry(
    val id: String,
    val covers: List<String>,
)

@Serializable
data class DiscoverySnapshot(
    val rootRel: String,
    val songs: List<DiscoverySong>,
) {
    fun ordered(): DiscoverySnapshot = copy(
        songs = songs.sortedBy(DiscoverySong::songTxtRel),
    )
}

@Serializable
data class DiscoverySong(
    val songDirRel: String,
    val songTxtRel: String,
    val isValid: Boolean,
    val invalidReasonCode: String? = null,
    val invalidLineNumber: Int? = null,
    val artist: String? = null,
    val title: String? = null,
    val resolvedAudioRel: String? = null,
    val hasVideo: Boolean? = null,
    val header: DiscoveryHeader? = null,
    val derived: DiscoveryDerived? = null,
    val body: DiscoveryBody? = null,
)

@Serializable
data class DiscoveryHeader(
    val title: String,
    val artist: String,
    val version: String,
    val bpmFile: Float,
    val audioResolved: String,
    val customTagsOrdered: List<String>,
)

@Serializable
data class DiscoveryDerived(
    val previewStartSec: Float,
)

@Serializable
data class DiscoveryBody(
    val noteTypesOrdered: List<String>,
)

@Serializable
data class ParsedSongSnapshot(
    val songId: String,
    val header: ParsedSongHeader,
    val timing: ParsedSongTiming,
    val tracks: List<ParsedSongTrack>,
    val diagnostics: List<ParsedSongDiagnostic>,
) {
    fun ordered(): ParsedSongSnapshot = copy(
        header = header.ordered(),
        tracks = tracks.sortedBy(ParsedSongTrack::trackIndex).map(ParsedSongTrack::ordered),
        diagnostics = diagnostics.sortedWith(
            compareBy<ParsedSongDiagnostic> { it.lineNumber ?: Int.MAX_VALUE }
                .thenBy { it.severity }
                .thenBy { it.code }
                .thenBy { it.txtUri },
        ),
    )
}

@Serializable
data class ParsedSongHeader(
    val title: String,
    val artist: String,
    val bpmFile: Float,
    val gapMs: Float,
    val audio: String,
    val video: String? = null,
    val cover: String? = null,
    val background: String? = null,
    val p1Name: String? = null,
    val p2Name: String? = null,
    val relativeMode: Boolean? = null,
    val version: String,
    val customTags: JsonObject,
) {
    fun ordered(): ParsedSongHeader = copy(
        customTags = FixtureSnapshotOrdering.orderedJsonObject(customTags),
    )
}

@Serializable
data class ParsedSongTiming(
    val bpmChanges: List<ParsedSongBpmChange>,
    val startBeatFile: Int? = null,
    val endBeatFile: Int? = null,
    val notesGapBeatsFile: Int? = null,
)

@Serializable
data class ParsedSongBpmChange(
    val startBeatFile: Int,
    val bpmFile: Float,
)

@Serializable
data class ParsedSongTrack(
    val trackIndex: Int,
    val lines: List<ParsedSongLine>,
) {
    fun ordered(): ParsedSongTrack = copy(
        lines = lines.sortedBy(ParsedSongLine::lineIndex),
    )
}

@Serializable
data class ParsedSongLine(
    val lineIndex: Int,
    val notes: List<ParsedSongNote>,
)

@Serializable
data class ParsedSongNote(
    val noteType: String,
    val startBeatFile: Int,
    val durationBeats: Int,
    val toneUsdx: Int,
    val lyric: String,
)

@Serializable
data class ParsedSongDiagnostic(
    val severity: String,
    val code: String,
    val txtUri: String,
    val lineNumber: Int? = null,
)

@Serializable
data class ScoreSnapshot(
    val description: String,
    val assumptions: ScoreAssumptions? = null,
    val derived: JsonObject? = null,
    val perBeat: List<ScorePerBeat>,
    val lineBonus: ScoreLineBonus? = null,
    val expectedTotals: ScoreExpectedTotals,
) {
    fun ordered(): ScoreSnapshot = copy(
        derived = derived?.let(FixtureSnapshotOrdering::orderedJsonObject),
        perBeat = perBeat.sortedBy(ScorePerBeat::beat),
        lineBonus = lineBonus?.ordered(),
    )
}

@Serializable
data class ScoreAssumptions(
    val playerId: String,
    val difficulty: String,
    @SerialName("LineBonusEnabled")
    val lineBonusEnabled: Boolean,
)

@Serializable
data class ScorePerBeat(
    val beat: Int,
    val activeNoteType: String? = null,
    val toneValid: Boolean,
    val expected: ScorePerBeatExpected,
)

@Serializable
data class ScorePerBeatExpected(
    val scoreDelta: Int,
    val scoreGoldenDelta: Int,
)

@Serializable
data class ScoreLineBonus(
    @SerialName("NonEmptyLines")
    val nonEmptyLines: Int? = null,
    @SerialName("LineBonusPerLine")
    val lineBonusPerLine: Int? = null,
    val expectedScoreLine: Int,
    val lines: List<ScoreLineBonusLine>? = null,
) {
    fun ordered(): ScoreLineBonus = copy(
        lines = lines?.sortedBy(ScoreLineBonusLine::lineIndex),
    )
}

@Serializable
data class ScoreLineBonusLine(
    val lineIndex: Int,
    val lineType: String,
    val maxLineScore: Int,
    val lineScore: Int,
    val linePerfection: Double,
    val lineBonus: Int,
)

@Serializable
data class ScoreExpectedTotals(
    val score: Int? = null,
    val scoreGolden: Int? = null,
    val scoreLine: Double? = null,
    val scoreInt: Int? = null,
    val scoreGoldenInt: Int? = null,
    val scoreLineInt: Int? = null,
    val scoreTotalInt: Int,
)

@Serializable
data class MedleyTotalSnapshot(
    val description: String,
    val expectedTotals: MedleyExpectedTotals,
)

@Serializable
data class MedleyExpectedTotals(
    val scoreInt: Int,
    val scoreGoldenInt: Int,
    val scoreLineInt: Int,
    val scoreTotalInt: Int,
)

object FixtureSnapshotOrdering {
    fun orderedJsonObject(value: JsonObject): JsonObject =
        FixtureJson.ordered(value) as JsonObject

    fun orderedJsonElement(value: JsonElement): JsonElement =
        FixtureJson.ordered(value)

    fun orderedJsonArray(value: JsonArray): JsonArray =
        FixtureJson.ordered(value) as JsonArray
}
