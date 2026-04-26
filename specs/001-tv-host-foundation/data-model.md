# Phase 0 Data Model

## Purpose

Phase 0 defines the host-owned parser, indexing, beat-time, and scoring data contracts that later playback, UI, and networking layers consume. The entities in this file are normative for the Phase 0 slice.

## Entity Overview

```text
ParsedSong
├── SongHeader
├── SongTiming
├── List<Track>
│   └── List<Line>
│       └── List<NoteEvent>
└── List<DiagnosticEntry>

IndexedSong

ScoringConfig
PlayerScore
PitchSample
BeatRange
```

## Parser Entities

### ParsedSong
| Field | Type | Required | Notes |
|---|---|---:|---|
| `songId` | `String` | Yes | Caller-supplied canonical identifier. Parser does not derive it from paths. |
| `header` | `SongHeader` | Yes | Resolved metadata and validation-sensitive header fields. |
| `timing` | `SongTiming` | Yes | File-BPM timing context for beat conversion. |
| `tracks` | `List<Track>` | Yes | Exactly 1 track for solo, exactly 2 for duet. |
| `diagnostics` | `List<DiagnosticEntry>` | Yes | Deterministic warnings/info for valid songs; populated invalids on failure. |

**Validation rules**:
- Success returns `Result.success(ParsedSong)` with info/warn diagnostics only.
- Hard-invalid songs fail as `Result.failure(ParseException)` carrying the populated diagnostic list.
- Parsed songs must preserve authored beat values and parser-computed score values.

### SongHeader
| Field | Type | Required | Notes |
|---|---|---:|---|
| `title` | `String` | Yes | Required string header. |
| `artist` | `String` | Yes | Required string header. |
| `bpmFile` | `Float` | Yes | Must be numeric and `> 0`. |
| `gapMs` | `Float` | Yes | Defaults to `0`. |
| `audio` | `String` | Yes | Resolved required audio filename per version rules. |
| `songPath` | `String` | Yes | Canonical song-root path or URI from caller context. |
| `startSec` | `Float?` | No | Derived from `#START` only. |
| `endMs` | `Int?` | No | Parsed from `#END`. |
| `videoGapSec` | `Float?` | No | Parsed from `#VIDEOGAP`. |
| `previewStartSec` | `Float?` | No | `#PREVIEWSTART` > medley start > `0.0`. |
| `video` | `String?` | No | Optional media tag. Missing file does not invalidate. |
| `cover` | `String?` | No | Optional media tag. |
| `background` | `String?` | No | Optional media tag. |
| `instrumental` | `String?` | No | Source metadata only. |
| `vocals` | `String?` | No | Source metadata only. |
| `version` | `String` | Yes | Defaults to legacy `0.3.0` when absent. |
| `year` | `Int?` | No | Optional metadata. |
| `genre` | `String?` | No | Optional metadata. |
| `album` | `String?` | No | Optional metadata. |
| `isDuet` | `Boolean` | Yes | True only when duet body structure is present. |
| `p1Name` | `String?` | No | Metadata only. |
| `p2Name` | `String?` | No | Metadata only. |
| `medleyStartBeat` | `Int?` | No | Optional medley beat start. |
| `medleyEndBeat` | `Int?` | No | Optional medley beat end. |
| `customTags` | `List<CustomHeaderTag>` | Yes | Unknown/malformed representable tags in encounter order. |

**Validation rules**:
- `#VERSION >= 2.0.0` is invalid.
- For version `>= 1.0.0`, `#AUDIO` takes precedence over `#MP3`; at least one must exist.
- For legacy songs (`#VERSION` absent or `< 1.0.0`), `#MP3` is required and `#AUDIO` is ignored for required-audio resolution.
- `#ENCODING`, `#RESOLUTION`, `#NOTESGAP`, `#DUETSINGERP1`, `#DUETSINGERP2`, and `#CALCMEDLEY` always remain unknown/custom tags.
- Duplicate known tags use the last successfully parsed value.
- Malformed required headers invalidate; malformed optional headers warn and become absent.

### CustomHeaderTag
| Field | Type | Required | Notes |
|---|---|---:|---|
| `tag` | `String` | Yes | Tag name as preserved by parser. |
| `content` | `String` | Yes | Raw representable content. |

### SongTiming
| Field | Type | Required | Notes |
|---|---|---:|---|
| `bpmFile` | `Float` | Yes | File BPM; internal BPM is `bpmFile * 4`. |

### Track
| Field | Type | Required | Notes |
|---|---|---:|---|
| `playerId` | `PlayerId` | Yes | `P1` or `P2`. |
| `lines` | `List<Line>` | Yes | Empty lines removed before final validation. |
| `trackScoreValue` | `Long` | Yes | Canonical parser-computed sum of `durationBeats * ScoreFactor(noteType)`. |

**Validation rules**:
- Solo songs produce exactly 1 track.
- Duet songs produce exactly 2 tracks.
- Every track must retain at least one non-empty sentence after cleanup.

### Line
| Field | Type | Required | Notes |
|---|---|---:|---|
| `lineIndex` | `Int` | Yes | Stable line ordering within track. |
| `notes` | `List<NoteEvent>` | Yes | Ordered note events for the sentence. |
| `lineScoreValue` | `Long` | Yes | Canonical parser-computed sum of scorable note values in the line. |
| `startBeatFile` | computed `Int` | Yes | First note start or `0` when empty. |
| `endBeatFileExclusive` | computed `Int` | Yes | Max note end beat or `0` when empty. |
| `isEmpty` | computed `Boolean` | Yes | `lineScoreValue == 0L`. |

**Validation rules**:
- Empty parsed sentences are removed before final validation.
- Empty lines do not receive line bonus.

### NoteEvent
| Field | Type | Required | Notes |
|---|---|---:|---|
| `noteType` | `NoteType` | Yes | `Normal`, `Golden`, `Rap`, `RapGolden`, `Freestyle`. |
| `startBeatFile` | `Int` | Yes | Authored file beat, stored without scaling. |
| `durationBeats` | `Int` | Yes | Must be `>= 0`. |
| `toneSemitone` | `Int` | Yes | USDX semitone where C2 = 0. |
| `lyric` | `String` | Yes | Preserved lyric remainder, may be empty. |
| `endBeatFileExclusive` | computed `Int` | Yes | `startBeatFile + durationBeats`. |

**Validation rules**:
- `durationBeats = 0` converts to `Freestyle`, warns with line number, and still stores duration `0`.
- Note activity and sample inclusion use `startBeatFile <= beat < endBeatFileExclusive`.

### DiagnosticEntry
| Field | Type | Required | Notes |
|---|---|---:|---|
| `severity` | `Severity` | Yes | `Info`, `Warn`, `Invalid`. |
| `code` | `String` | Yes | Stable fixture-assertable code. |
| `message` | `String` | Yes | Human-readable deterministic message. |
| `txtUri` | `String` | Yes | Deterministic TXT identifier. |
| `lineNumber` | `Int?` | No | 1-based when deterministically attributable. |

**Minimum invalid codes**:
- `ERROR_CORRUPT_SONG_FILE_NOT_FOUND`
- `ERROR_CORRUPT_SONG_NO_NOTES`
- `ERROR_CORRUPT_SONG_NO_BREAKS`
- `ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER`
- `ERROR_CORRUPT_SONG_MALFORMED_HEADER`
- `ERROR_CORRUPT_SONG_MALFORMED_BODY`
- `ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM`
- `ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE`
- `ERROR_CORRUPT_SONG_INVALID_VERSION`
- `ERROR_CORRUPT_SONG_INVALID_DUET_MARKER`

## Library-Facing Index Entity

### IndexedSong
| Field | Type | Required | Notes |
|---|---|---:|---|
| `songId` | `String` | Yes | `phoneClientId + "::" + relativeTxtPath` |
| `phoneClientId` | `String` | Yes | Contributing phone/client id. |
| `relativeTxtPath` | `String` | Yes | Forward slashes, no leading `/`, no `.` or `..`, case preserved. |
| `modifiedTimeMs` | `Long` | Yes | Deterministic when fixture asserts it. |
| `title` | `String` | Yes | Library-facing song title. |
| `artist` | `String` | Yes | Library-facing artist. |
| `album` | `String?` | No | Optional. |
| `year` | `Int?` | No | Optional. |
| `genre` | `String?` | No | Optional. |
| `txtUrl` | `String` | Yes | Non-null for valid manifest-eligible entries. |
| `audioUrl` | `String` | Yes | Non-null for valid manifest-eligible entries. |
| `videoUrl` | `String?` | No | Optional. |
| `coverUrl` | `String?` | No | Optional. |
| `backgroundUrl` | `String?` | No | Optional. |
| `isDuet` | `Boolean` | Yes | Derived from parsed chart. |
| `hasRap` | `Boolean` | Yes | True when `R` or `G` tokens exist. |
| `hasVideo` | `Boolean` | Yes | Must equal `videoUrl != null`. |
| `hasInstrumental` | `Boolean` | Yes | Source metadata only; not playback strategy. |
| `canMedley` | `Boolean` | Yes | True only for valid solo medley-tag songs. |
| `medleySource` | `String?` | No | `"tag"` or `null` in Phase 0. |
| `medleyStartBeat` | `Int?` | No | Required when `medleySource == "tag"`. |
| `medleyEndBeat` | `Int?` | No | Required when `medleySource == "tag"`. |
| `startSec` | `Float` | Yes | `#START` or `0.0`. |
| `previewStartSec` | `Float` | Yes | `#PREVIEWSTART` > medley start > `0.0`. |

**Validation rules**:
- Invalid songs never surface as valid manifest/index entries.
- `medleySource == "tag"` requires both beats and `start < end`.
- `medleySource == null` requires `canMedley == false`.
- `hasInstrumental` is display metadata only.

## Beat-Time and Scoring Entities

### BeatRange
| Field | Type | Required | Notes |
|---|---|---:|---|
| `startBeat` | `Int` | Yes | Inclusive start. |
| `endBeat` | `Int` | Yes | Exclusive end. |

### ScoringConfig
| Field | Type | Required | Notes |
|---|---|---:|---|
| `playerDifficulties` | `Map<PlayerId, Difficulty>` | Yes | One entry per assigned singer. |
| `lineBonusEnabled` | `Boolean` | Yes | Controls score budget split. |

**Validation rules**:
- Scoring must not read difficulty or line-bonus settings from hidden/global state.
- Default difficulty for newly assigned singers is `Medium`.

### PlayerScore
| Field | Type | Required | Notes |
|---|---|---:|---|
| `score` | `Double` | Yes | Normal + Rap accumulator. |
| `scoreGolden` | `Double` | Yes | Golden + RapGolden accumulator. |
| `scoreLine` | `Double` | Yes | Line bonus accumulator. |
| `scoreLast` | `Double` | Yes | Last sentence checkpoint after line bonus application. |
| `scoreInt` | `Int` | Yes | Rounded normal display score. |
| `scoreGoldenInt` | `Int` | Yes | Opposite-direction rounded golden display score. |
| `scoreLineInt` | `Int` | Yes | Rounded line display score. |
| `scoreTotalInt` | `Int` | Yes | Sum of display buckets; must never exceed `10000`. |

### PitchSample
| Field | Type | Required | Notes |
|---|---|---:|---|
| `playerId` | `PlayerId` | Yes | Owning player. |
| `midiNote` | `Int` | Yes | `255` means unvoiced/tone-invalid. |
| `tvTimeMs` | `Long?` | No | Fixture-provided monotonic sample time. |

**Derived rules**:
- `toneValid == (midiNote != 255)`
- `toneSemitone = midiNote - 36` when a voiced MIDI note is present

## Enumerations

### PlayerId
- `P1`
- `P2`

### Difficulty
- `Easy` → tolerance `±2`
- `Medium` → tolerance `±1`
- `Hard` → tolerance `±0`

### NoteType
- `Normal`
- `Golden`
- `Rap`
- `RapGolden`
- `Freestyle`

### Severity
- `Info`
- `Warn`
- `Invalid`

## State and Transition Notes

### Parser outcome transitions
- Raw TXT bytes + caller-supplied `songId`
  - valid parse → `Result.success(ParsedSong)`
  - hard-invalid parse → `Result.failure(ParseException(diagnostics))`

### Scoring lifecycle transitions relevant to Phase 0
- `loadChart(...)` loads static chart/config state
- `setSongStart(...)` establishes scoring time origin
- scoring math evaluates note windows and accumulates buckets
- `finalizeAll()` materializes final `Map<PlayerId, PlayerScore>` for fixture assertions

Runtime jitter buffering, reconnect behavior, and live coroutine scheduling are future seams and are not part of the Phase 0 validation model.
