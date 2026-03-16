# Data Model: Song Library (004)

**Branch**: `004-song-library` | **Date**: 2026-03-16

---

## Entities

### SongEntry

The TV's per-song record in the in-memory library. Immutable once created by `SongIndexer`.

**Package**: `com.couchraoke.tv.domain.library`

```
SongEntry
├── Identity
│   ├── songId: String               // phoneClientId + "::" + relativeTxtPath
│   ├── phoneClientId: String        // which phone provided this
│   ├── relativeTxtPath: String      // normalized: forward slashes, no leading /, no ..
│   └── modifiedTimeMs: Long         // last-modified of .txt at scan time (from phone)
├── Validation
│   ├── isValid: Boolean
│   ├── invalidReasonCode: String?   // see error codes below; null if isValid=true
│   └── invalidLineNumber: Int?      // 1-based; null if not line-specific
├── Display
│   ├── artist: String?              // null only if parsing failed before extracting it
│   ├── title: String?
│   └── album: String?               // null if absent; sorts as "" in Artist→Album→Title order
├── Derived Flags
│   ├── isDuet: Boolean
│   ├── hasRap: Boolean
│   ├── hasVideo: Boolean
│   ├── hasInstrumental: Boolean
│   ├── canMedley: Boolean           // false for duets; true only when medleySource=EXPLICIT
│   ├── medleySource: MedleySource   // NONE | EXPLICIT (reuse domain.parser.MedleySource)
│   ├── medleyStartBeat: Int?        // non-null when medleySource=EXPLICIT
│   ├── medleyEndBeat: Int?          // non-null when medleySource=EXPLICIT
│   └── calcMedleyEnabled: Boolean   // false iff #CALCMEDLEY:OFF
├── Preview/Seek Metadata
│   ├── startSec: Double             // from #START, default 0.0
│   └── previewStartSec: Double      // see computation below
└── Asset URLs (as received from manifest; null if absent)
    ├── txtUrl: String               // required; non-null for any entry (valid or not)
    ├── audioUrl: String?
    ├── videoUrl: String?
    ├── coverUrl: String?
    ├── backgroundUrl: String?
    ├── instrumentalUrl: String?
    └── vocalsUrl: String?
```

**previewStartSec computation** (in priority order):
1. `#PREVIEWSTART` value if present and `> 0.0`
2. `beat * 60.0 / (bpmFile * 4.0) + gapMs / 1000.0` where `beat = medleyStartBeat` (if `medleySource = EXPLICIT`)
3. `0.0`

**invalidReasonCode values** (stable string constants; §4.3):

| Code | When |
|------|------|
| `ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER` | `#TITLE`, `#ARTIST`, `#BPM`, or required audio tag absent/empty |
| `ERROR_CORRUPT_SONG_FILE_NOT_FOUND` | Required audio file absent on disk/phone |
| `ERROR_CORRUPT_SONG_MALFORMED_HEADER` | `#BPM` or other header field non-parseable |
| `ERROR_CORRUPT_SONG_MALFORMED_BODY` | Note token with non-parseable numeric field in body |
| `ERROR_CORRUPT_SONG_NO_NOTES` | No singable notes remain after empty-line cleanup |
| `ERROR_CORRUPT_SONG_INVALID_PATH` | `relativeTxtPath` contains `..` or starts with `/` |

**DiagnosticCode → invalidReasonCode mapping** (applied by `SongIndexer`):

| DiagnosticCode(s) | invalidReasonCode |
|---|---|
| `MISSING_REQUIRED_ARTIST`, `MISSING_REQUIRED_TITLE`, `MISSING_REQUIRED_BPM`, `MISSING_REQUIRED_AUDIO`, `MISSING_REQUIRED_FIELD` | `ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER` |
| `REQUIRED_AUDIO_NOT_FOUND` | `ERROR_CORRUPT_SONG_FILE_NOT_FOUND` |
| `MALFORMED_NUMERIC_FIELD` (header origin) | `ERROR_CORRUPT_SONG_MALFORMED_HEADER` |
| `MALFORMED_BODY_FIELD` *(new code added to DiagnosticCode)* | `ERROR_CORRUPT_SONG_MALFORMED_BODY` |
| `EMPTY_TRACK_AFTER_CLEANUP`, `NO_USABLE_NOTES` | `ERROR_CORRUPT_SONG_NO_NOTES` |

> **Note**: A new `DiagnosticCode.MALFORMED_BODY_FIELD` value is added to the existing enum (backward-compatible) and used by `BodyParser` instead of `MALFORMED_NUMERIC_FIELD` for body-parse fatal errors. This disambiguates header vs body malformed fields without requiring a `source` field on `DiagnosticEntry`.

---

### SongLibrary

In-memory aggregate index. Lives for the duration of one session.

**Package**: `com.couchraoke.tv.domain.library`

**Interface**:
```
SongLibrary
├── addPhone(clientId: String, entries: List<SongEntry>)  // replaces existing
├── removePhone(clientId: String)
├── getSortedSongs(): List<SongEntry>                     // sorted by Artist→Album→Title (case-insensitive)
├── getSongById(songId: String): SongEntry?
└── getSongsByPhone(clientId: String): List<SongEntry>
```

**DefaultSongLibrary implementation**: `LinkedHashMap<String, List<SongEntry>>` keyed by `phoneClientId`. Not thread-safe (single-threaded session context; concurrency managed by caller in feature 005).

**Sort key**: `Triple(artist.orEmpty().lowercase(), album.orEmpty().lowercase(), title.orEmpty().lowercase())`

---

### ManifestEntry

Raw per-song entry as received from the phone's `/manifest.json`. Plain data class; JSON annotations added in feature 005.

**Package**: `com.couchraoke.tv.domain.library`

```
ManifestEntry
├── relativeTxtPath: String
├── modifiedTimeMs: Long
├── txtUrl: String
├── audioUrl: String?
├── videoUrl: String?
├── coverUrl: String?
├── backgroundUrl: String?
├── instrumentalUrl: String?
└── vocalsUrl: String?
```

> The phone provides asset URLs as-computed; it does NOT pre-parse song content into the manifest fields. The TV fetches `.txt` content via `txtUrl` at validation/indexing time (see `SongDiscovery`).

---

### SongIndexer

Stateless converter: `ParseResult` + asset URLs + identity fields → `SongEntry`.

**Package**: `com.couchraoke.tv.domain.library`

**Key method**:
```
SongIndexer.fromParseResult(
    parseResult: ParseResult,
    phoneClientId: String,
    relativeTxtPath: String,
    modifiedTimeMs: Long,
    txtUrl: String,
    audioUrl: String?, videoUrl: String?, coverUrl: String?,
    backgroundUrl: String?, instrumentalUrl: String?, vocalsUrl: String?,
): SongEntry
```

Applies the `DiagnosticCode → invalidReasonCode` mapping. Computes `previewStartSec` from header fields. Derives `canMedley` from `isDuet` + `medleySource`.

---

### SongDiscovery

Orchestrates local `.txt` discovery + parsing → `List<SongEntry>`. Used by acceptance tests (F01 fixture) and by the phone-side parity test.

**Package**: `com.couchraoke.tv.domain.library`

**Key method**:
```
SongDiscovery(
    parser: UsdxParser,
    indexer: SongIndexer,
).discoverFromDirectory(
    rootDir: Path,          // directory to scan recursively
    phoneClientId: String,  // used to form songId
    fileResolver: FileResolver,  // checks asset file existence
    readFile: (Path) -> String,  // reads .txt content; injectable for tests
): List<SongEntry>
```

Scans recursively for `.txt` files, reads each, calls `UsdxParser.parse()`, passes `ParseResult` + manifest-like URL stubs to `SongIndexer.fromParseResult()`.

---

## Relationships

```
SongLibrary
  └── holds Map<phoneClientId, List<SongEntry>>

SongDiscovery
  ├── uses UsdxParser (domain.parser)
  ├── uses FileResolver (domain.parser)
  └── uses SongIndexer

SongIndexer
  ├── reads ParseResult / ParsedSong / DerivedSongSummary (domain.parser)
  └── produces SongEntry

SongEntry
  └── reuses MedleySource (domain.parser.MedleySource)
```

---

## Parser Extension

**File**: `app/src/main/kotlin/com/couchraoke/tv/domain/parser/DiagnosticEntry.kt`

Add `MALFORMED_BODY_FIELD` to the `DiagnosticCode` enum.

**File**: `app/src/main/kotlin/com/couchraoke/tv/domain/parser/BodyParser.kt`

Replace `DiagnosticCode.MALFORMED_NUMERIC_FIELD` with `DiagnosticCode.MALFORMED_BODY_FIELD` in `fatalResult` calls within `BodyParser.parse()`.

This is a backward-compatible change (new enum value, existing usages of `MALFORMED_NUMERIC_FIELD` in `HeaderParser` are unchanged).
