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
- Hard-invalid songs return `Result.failure(ParseException)` carrying the populated diagnostics list.

### Required rules
- Header parsing stops at the first non-`#` line.
- Known duplicate header tags keep the last successfully parsed value.
- Unknown header tags are preserved in `customTags` in encounter order.
- `#ENCODING`, `#RESOLUTION`, `#NOTESGAP`, `#DUETSINGERP1`, `#DUETSINGERP2`, and `#CALCMEDLEY` remain unknown/custom tags.
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
    val songPath: String,
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
```

### Required rules
- Stored beats are authored file beats with no scaling.
- `durationBeats == 0` converts to `Freestyle`, retains duration `0`, warns, and scores zero.
- `lineScoreValue` and `trackScoreValue` are canonical parser outputs and must not be recomputed differently downstream.
- Note/beat interval convention is `startBeatFile <= beat < startBeatFile + durationBeats`.

## Contract 3: Library-facing indexed-song seam

- **Producer**: future library/indexing layer in `com.couchraoke.tv.domain.library`
- **Consumers**: selection flow, manifest/discovery fixtures
- **Callable interface**:

```kotlin
package com.couchraoke.tv.domain.library

interface LibraryManager {
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
    val hasInstrumental: Boolean,
    val canMedley: Boolean,
    val medleySource: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val startSec: Float,
    val previewStartSec: Float
)
```

### Required rules
- `songId == phoneClientId + "::" + relativeTxtPath`.
- `relativeTxtPath` uses `/`, has no leading `/`, contains no `.` or `..` segments, and preserves case.
- `txtUrl` and `audioUrl` are non-null for valid manifest-eligible entries.
- `hasVideo == (videoUrl != null)`.
- `hasInstrumental` is source metadata only and must not change playback strategy.
- `previewStartSec` derives from `#PREVIEWSTART`, then valid medley start, then `0.0`.
- `startSec` derives only from `#START`, else `0.0`.

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
- This seam is documented during planning because it materially feeds scoring.
- It is not part of the Phase 0 delivery gate.
- Phase 0 fixtures use `PitchSample` instead of live UDP/network frames.

## Validation contract

The authoritative validation gate for all contracts in this file is the scoped `:app:testBranch` command defined in `plan.md` and used by the eventual implementation tasks.
