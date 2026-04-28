# Foundation Boundary Contracts

## Purpose

This file defines the material producer/consumer boundaries for the Phase 0 TV host foundation slice. Each contract is specified as FQCN + method + signature, with the required payload rules that later implementation and validation must honor.

## Contract 1: Parser entry point

- **Producer**: `com.couchraoke.tv.domain.usdx.UsdxParser`
- **Consumers**: fixture harness, library indexing adapter, scoring chart loader
- **Callable interface**:

```kotlin
package com.couchraoke.tv.domain.usdx

interface UsdxParser {
    fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong>
}
```

### Payload contract
- `songId` is caller-supplied and canonical; parser does not derive it from filesystem paths or URLs.
- `txtBytes` are raw TXT bytes; parser applies deterministic internal decode rules.
- Success returns `ParsedSong` with info/warn diagnostics only.
- Hard-invalid songs return `Result.failure(ParseException)` carrying the structured diagnostics accumulated up to termination, including at least one `Invalid` diagnostic.

### Required rules
- Parser diagnostics are machine/test-facing: fixture assertions compare severity, stable code, TXT identifier, and deterministic line number when present.
- Human-readable diagnostic text is produced by logging or presentation mapping and is not required in the core parse result.
- Dev/debug builds SHOULD log detailed parser diagnostics at the configured diagnostic log level; release builds MAY log only warning/error summaries.
- UI error modals may display generic invalid-song messages and do not need direct parser diagnostic text.
- Fixture validation MUST NOT depend on log scraping.
- Header parsing stops at the first non-`#` line.
- Known duplicate header tags keep the last successfully parsed value.
- Unknown header tags are preserved in `customTags` in encounter order.
- `#ENCODING`, `#RESOLUTION`, `#NOTESGAP`, `#DUETSINGERP1`, `#DUETSINGERP2`, `#CALCMEDLEY`, `#INSTRUMENTAL`, and `#VOCALS` remain custom tags with no TV-side semantic processing.
- RELATIVE header tags are preserved, but RELATIVE body syntax invalidates the song.

## Contract 2: Parsed song result model

- **Producer**: `com.couchraoke.tv.domain.usdx.UsdxParser`
- **Consumers**: `com.couchraoke.tv.domain.library.*`, `com.couchraoke.tv.domain.scoring.*`, fixture assertions
- **Data contract**:

```kotlin
package com.couchraoke.tv.domain.usdx.model

data class ParsedSong(
    val songId: String,
    val header: SongHeader,
    val timing: SongTiming,
    val tracks: List<Track>,
    val diagnostics: List<DiagnosticEntry>
)

data class SongHeader(
    val title: String,
    val artist: String,
    val bpmFile: Float,
    val gapMs: Float,
    val audio: String,
    val startSec: Float?,
    val endMs: Int?,
    val videoGapSec: Float?,
    val previewStartSec: Float?,
    val video: String?,
    val cover: String?,
    val background: String?,
    val version: String,
    val year: Int?,
    val genre: String?,
    val album: String?,
    val isDuet: Boolean,
    val p1Name: String?,
    val p2Name: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val customTags: List<CustomHeaderTag>
)

data class Track(
    val playerId: PlayerId,
    val lines: List<Line>,
    val trackScoreValue: Long
)

data class Line(
    val lineIndex: Int,
    val notes: List<NoteEvent>,
    val lineScoreValue: Long
)

data class NoteEvent(
    val noteType: NoteType,
    val startBeatFile: Int,
    val durationBeats: Int,
    val toneSemitone: Int,
    val lyric: String
)

data class DiagnosticEntry(
    val severity: Severity,
    val code: String,
    val txtUri: String,
    val lineNumber: Int? = null
)
```

### Required rules
- Stored beats are authored file beats with no scaling.
- `durationBeats == 0` converts to `Freestyle`, retains duration `0`, emits a warning diagnostic with a line number when deterministic, and scores zero.
- `lineScoreValue` and `trackScoreValue` are canonical parser outputs and must not be recomputed differently downstream.
- Note/beat interval convention is `startBeatFile <= beat < startBeatFile + durationBeats`.
- Parsed `SongHeader.previewStartSec` preserves source tag presence and remains nullable. Fallback/default materialization is applied only by the library/index projection.

## Contract 3: Library-facing indexed-song seam

- **Producer**: future library/indexing layer in `com.couchraoke.tv.domain.library`
- **Consumers**: selection flow, manifest/discovery fixtures
- **Callable interface**:

```kotlin
package com.couchraoke.tv.domain.library

import kotlinx.coroutines.flow.StateFlow

interface LibraryManager {
    /**
     * Observable catalog seam used by selection flow and later runtime library refresh.
     *
     * Phase 0 may back this with fixture/static data only. Live manifest refresh,
     * disconnect removal, and multi-phone replacement behavior are outside the
     * Phase 0 implementation gate unless explicitly added later.
     */
    val songs: StateFlow<List<IndexedSong>>

    fun getSong(songId: String): IndexedSong?
}
```

- **Data contract**:

```kotlin
package com.couchraoke.tv.domain.library

data class IndexedSong(
    val songId: String,
    val phoneClientId: String,
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val genre: String?,
    val txtUrl: String,
    val audioUrl: String,
    val videoUrl: String?,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val isDuet: Boolean,
    val hasRap: Boolean,
    val hasVideo: Boolean,
    val canMedley: Boolean,
    val medleySource: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val startSec: Float,
    val previewStartSec: Float
)
```

### Required rules
- Phase 0 defines the observable catalog seam because downstream TV consumers depend on it, but Phase 0 only validates fixture/static indexed-song projection. Runtime manifest refresh, disconnect removal, and multi-phone replacement remain later-phase behavior.
- `songId == phoneClientId + "::" + relativeTxtPath`.
- `relativeTxtPath` uses `/`, has no leading `/`, contains no `.` or `..` segments, and preserves case.
- `txtUrl` and `audioUrl` are non-null for valid manifest-eligible entries.
- `hasVideo == (videoUrl != null)`.
- `canMedley` is true only when the song is not a duet and valid medley tags exist. Duet songs must produce `canMedley = false` and `medleySource = null` even if source medley beat tags are present.
- Parsed `SongHeader.previewStartSec` preserves source tag presence and remains nullable. `IndexedSong.previewStartSec` materializes the fallback/default value and is always non-null. Medley fallback uses `BeatCalculator.beatInternalToTimeSec(medleyStartBeat.toDouble(), bpmFile * 4)`.
- `previewStartSec` derives from valid `#PREVIEWSTART` when present and greater than `0`, otherwise valid solo medley tags converted with static-BPM beat-time math, otherwise `0.0`.
- `startSec` derives only from `#START`, else `0.0`.
- The TV always receives and plays one phone-provided premixed audio resource through `IndexedSong.audioUrl`. The phone owns any source-stem handling and premixing. The TV does not expose, request, mix, schedule, or buffer separate instrumental/vocal assets.

## Contract 4: Beat-time conversion seam

- **Producer**: `com.couchraoke.tv.domain.scoring.BeatCalculator`
- **Consumers**: lyric highlighting, scoring-window math, preview derivation from medley beats
- **Callable interface**:

```kotlin
package com.couchraoke.tv.domain.scoring

object BeatCalculator {
    fun timeSecToMidBeatInternal(tSec: Double, bpmInternal: Float): Double
    fun beatInternalToTimeSec(beatInternal: Double, bpmInternal: Float): Double
}
```

### Required rules
- `bpmInternal = bpmFile * 4`.
- File beats are authoritative and stored/processed unchanged.
- Lyrics consumers use `micDelayMs = 0`.
- Scoring-window consumers use the configured `micDelayMs` in `0..400`.
- Passing the wrong delay for a consumer is a conformance error.
- Time/beat round trips for deterministic tests must match within `1e-9s`.

## Contract 5: Scoring engine seam

- **Producer**: `com.couchraoke.tv.domain.scoring.ScoringEngine`
- **Consumers**: future playback/game coordinator; Phase 0 fixture harness via final score assertions
- **Callable interface**:

```kotlin
package com.couchraoke.tv.domain.scoring

interface ScoringEngine {
    fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig)
    fun setSongStart(songStartTvMs: Long)
    suspend fun finalizeAll(): Map<PlayerId, PlayerScore>
    fun reset()
}
```

- **Supporting data contract**:

```kotlin
package com.couchraoke.tv.domain.scoring.model

data class BeatRange(
    val startBeat: Int,
    val endBeat: Int
)

data class ScoringConfig(
    val playerDifficulties: Map<PlayerId, Difficulty>,
    val lineBonusEnabled: Boolean
)

data class PlayerScore(
    val score: Double,
    val scoreGolden: Double,
    val scoreLine: Double,
    val scoreLast: Double,
    val scoreInt: Int,
    val scoreGoldenInt: Int,
    val scoreLineInt: Int,
    val scoreTotalInt: Int
)

data class PitchSample(
    val playerId: PlayerId,
    val midiNote: Int,
    val tvTimeMs: Long?
)
```

### Required rules
- The public Phase 0 `ScoringEngine` interface does not accept pitch samples directly. Fixture pitch input is injected into the implementation/test harness as a stand-in for the future §2.2 runtime pitch-frame source.
- `ScoringConfig` is the only source of player difficulty and line-bonus settings for the current song.
- `toneValid` is derived as `midiNote != 255`.
- Sample tone is `midiNote - 36`.
- Normal/Rap accumulate in `score`; Golden/RapGolden accumulate in `scoreGolden`; Freestyle never accumulates score.
- Easy/Medium/Hard tolerances are `±2`, `±1`, and `±0` respectively, with Medium as the default.
- Octave normalization uses the full-semitone while-loop and never pitch-class modulo 12.
- Line bonus uses parser-computed line/track score values, skips empty lines, applies the forgiveness rule for `MaxLineScore <= 2`, and updates `scoreLast` after each sentence bonus.
- Display rounding keeps the intentional asymmetry and must never yield totals above `10000`.

## Contract 6: Future runtime seam documented now, deferred from Phase 0 gate

- **Producer**: `com.couchraoke.tv.data.network.NetworkController`
- **Consumer**: `com.couchraoke.tv.domain.scoring.ScoringEngine`
- **Callable shape**:

```kotlin
package com.couchraoke.tv.data.network

interface NetworkPitchSource {
    val pitchFrames: kotlinx.coroutines.flow.SharedFlow<PitchFrame>
}
```

### Required rules
- Runtime pitch transport is out of Phase 0 scope.
- Later runtime layers may feed scoring through a live pitch source, but Phase 0 validates only the pure math contract via `PitchSample` fixtures.
