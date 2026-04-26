# Feature Specification: Phase 0 TV Host Foundation

**Feature Branch**: `001-tv-host-foundation`  
**Created**: 2026-04-26  
**Status**: Draft  
**Input**: User description: "Create a fully detailed, self-contained Phase 0 foundation specification for the Couchraoke TV host app. Include parser, beat-time conversion, scoring math, fixture harness expectations, and completion gates. Do not depend on any external source specification except test fixtures."

## Clarifications

### Session 2026-04-26

- Q: When source requirements and current fixture files disagree for Phase 0 details, which source should the generated spec treat as authoritative? → A: Case-by-case, with any chosen override made explicit in this spec.
- Q: For F05 legacy RELATIVE mode, should Phase 0 parse it or reject it? → A: Reject RELATIVE.
- Q: For F02 previewStartSec derivation when `#PREVIEWSTART` is absent but `#START` is present, which behavior should Phase 0 specify? → A: Align to the canonical index contract: `previewStartSec` uses `#PREVIEWSTART`, then medley start when medley tags are valid, then `0.0`; `#START` affects `startSec` only.
- Q: For F02 encoding cases, should Phase 0 add charset detection or use a deterministic version/header rule? → A: Use a deterministic parser-internal decode rule; `#ENCODING` is preserved as an unknown tag and never receives semantic processing.
- Q: Should Phase 0 include scoring finalization timing/jitter buffer behavior, or keep it out despite the broader scoring section listing it? → A: Keep runtime jitter/finalization behavior out of the Phase 0 delivery gate and document it only as a future seam.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Validate USDX Songs Deterministically (Priority: P1)

As the TV host app, Couchraoke needs a deterministic foundation for reading USDX song files so later library, playback, scoring, and UI features can trust the same parsed chart and metadata model. Phase 0 validates only host-owned pure logic: parsing, song validation, diagnostics, and score-value precomputation. It does not require phones, LAN discovery, playback, UI, or Android runtime behavior.

**Why this priority**: Every later phase depends on a reliable song model. If parsing or validation is ambiguous, later playback and scoring defects become hard to isolate.

**Independent Test**: Can be fully tested by running the parser and validation fixture suite against `fixtures/F01_song_discovery_validation_acceptance`, `fixtures/F02_header_parsing_edge_cases`, `fixtures/F03_body_grammar_token_recognition`, `fixtures/F04_duet_parsing_track_routing`, and `fixtures/F05_legacy_relative_mode_semantics`, then comparing deterministic outputs and diagnostics to expected fixture files.

**Acceptance Scenarios**:

1. **Given** a recursive fixture song tree containing valid songs, malformed songs, missing-audio songs, legacy songs, and optional missing media, **When** the host validation harness discovers and validates each `song.txt`, **Then** valid songs are accepted, invalid songs are rejected with stable diagnostic codes, and optional missing media does not invalidate a song.
2. **Given** a USDX file with recognized note tokens, line breaks, duet part markers, and end markers, **When** the parser reads the body, **Then** notes are assigned to the correct track and sentence, note fields are preserved, and parser-computed line and track score values become canonical.
3. **Given** a USDX file with malformed required headers, unsupported version, unsupported variable BPM body token, unsupported RELATIVE body format, invalid duet marker, malformed numeric fields, or no valid notes after cleanup, **When** the parser validates the file, **Then** the song is rejected with the required invalidation code and line number when the failing input line is deterministic.
4. **Given** a USDX file with unknown or malformed non-required header tags, empty-value tags, no-separator tags, unknown body tokens, or optional malformed tags, **When** the parser validates the file, **Then** the parser emits non-fatal diagnostics, preserves custom header tags in encounter order, and continues where the rules allow.

---

### User Story 2 - Convert Beat and Time Coordinates Consistently (Priority: P2)

As the TV host app, Couchraoke needs one deterministic beat-time foundation so lyric highlighting and scoring windows agree about song timing. Phase 0 covers static-BPM beat conversion, GAP handling, mic-delay-aware scoring windows, and interval boundary rules. Later pitch-lane rendering may consume the same beat-time contract, but lane coordinate-system rules are not part of this phase.

**Why this priority**: Scoring and rendering are only trustworthy if every consumer uses the same beat grid and boundary convention.

**Independent Test**: Can be fully tested by running the beat-time fixture suite against `fixtures/F06_beat_time_conversion_static_bpm` and inline boundary cases, without network, playback, phones, or UI.

**Acceptance Scenarios**:

1. **Given** a static-BPM chart with `#BPM` and `#GAP`, **When** the host converts lyric timeline samples into beat positions, **Then** highlight beats match the fixture's expected values exactly.
2. **Given** the same static-BPM chart and a nonzero mic delay, **When** the host computes scoring cursor positions and note scoring windows, **Then** scoring windows are shifted later by the configured delay while lyric highlighting remains unshifted.
3. **Given** any note with a start beat and duration, **When** the host checks note activity or scoring sample inclusion, **Then** the note is active for `startBeat <= beat < startBeat + duration` and inactive at the exclusive end beat.
4. **Given** any converted beat value, **When** it is converted to time and back within the supported static-BPM model, **Then** the round trip matches to a precision suitable for fixture expectations (`1e-9s` for the deterministic round-trip case).

---

### User Story 3 - Score Singing Math Without Runtime Dependencies (Priority: P3)

As the TV host app, Couchraoke needs deterministic scoring math before the live pitch pipeline exists. Phase 0 scores fixture-provided pitch samples against parsed charts, including per-note scoring, difficulty tolerance, octave normalization, rap scoring, freestyle behavior, line bonus, medley-window score-value math, and display rounding.

**Why this priority**: Later live pitch transport and UI can be tested independently only if the underlying scoring math is already proven correct.

**Independent Test**: Can be fully tested by running scoring fixtures `fixtures/F08_scoring_beat_stepping_interval_semantics`, `fixtures/F09_pitch_tolerance_octave_normalization`, `fixtures/F10_rap_scoring_tonevalid_gate`, and `fixtures/F11_line_bonus_and_rounding`, plus the `fixtures/F03_body_grammar_token_recognition/scoring/freestyle_only` subcase.

**Acceptance Scenarios**:

1. **Given** a perfect fixture performance where every qualifying sample is a hit, **When** scoring finalizes the pure math result set for the song, **Then** the final displayed total is exactly `10000`.
2. **Given** no qualifying samples for a note, **When** that note is scored, **Then** the note contributes `0`.
3. **Given** a note with some hit samples and some miss samples, **When** that note is scored, **Then** the note score is proportional to `hits / total qualifying samples` using double-precision division.
4. **Given** Normal, Golden, Rap, RapGolden, and Freestyle notes, **When** scoring accumulates results, **Then** Normal and Rap add to the normal score bucket, Golden and RapGolden add to the golden score bucket, and Freestyle adds nothing.
5. **Given** Easy, Medium, and Hard player difficulties, **When** Normal or Golden notes are evaluated, **Then** tolerance is Easy ±2 semitones, Medium ±1 semitone, and Hard ±0 semitones, with Medium as the default for newly assigned singers.
6. **Given** Rap or RapGolden notes, **When** samples are evaluated, **Then** pitch difference is ignored and only voiced/unvoiced status determines hits.
7. **Given** a completed sentence, **When** its last scorable note has finalized within the pure math model, **Then** line bonus is applied according to the line perfection formula, empty lines receive no line bonus, and the player's score-last checkpoint is updated after the bonus.
8. **Given** score buckets with fractional values, **When** display values are produced, **Then** normal, golden, line, and total values use the required rounding rules and the total never exceeds `10000`.

---

### User Story 4 - Establish the Phase 0 Quality Gate (Priority: P4)

As a maintainer, I need Phase 0 to provide a fresh, repeatable quality gate so later phases can build on a stable host foundation rather than inheriting unverified math or parsing behavior.

**Why this priority**: This phase is useful only if the fixture suite and coverage gate prove the foundation is complete and isolated.

**Independent Test**: Can be tested by running this repository's scoped `testBranch` gate for the Phase 0 parser, beat-time, and scoring math modules and verifying that all required fixtures pass fresh.

**Acceptance Scenarios**:

1. **Given** the Phase 0 implementation, **When** the scoped `testBranch` gate runs, **Then** F01-F06 and F08-F11 pass fresh.
2. **Given** the Phase 0 implementation, **When** coverage is measured for parser and scoring math modules, **Then** coverage is at least 80% for those modules.
3. **Given** the Phase 0 implementation, **When** host-independent tests are executed, **Then** they do not require Android framework runtime, LAN services, phones, mock phones, playback backends, UI rendering, or file/network I/O inside the parser/scoring math core.

---

### Edge Cases

- A song with `#TITLE`, `#ARTIST`, `#BPM`, or the version-appropriate required audio tag missing or malformed is invalid.
- A song with `#BPM <= 0`, non-numeric `#BPM`, unsupported `#VERSION`, unsupported `B` body token, unsupported RELATIVE body sentence syntax, invalid duet marker, malformed note numeric fields, missing required audio file, or no valid remaining notes is invalid.
- A song with missing optional media such as `#VIDEO`, `#COVER`, or `#BACKGROUND` remains valid and treats the missing optional asset as absent.
- Duplicate known header tags use the last successfully parsed value.
- Unknown header tags, malformed optional header tags, empty-value header tags, and no-separator header lines are non-fatal; they produce diagnostics and preserve custom tag data when applicable.
- `#ENCODING`, `#RESOLUTION`, `#NOTESGAP`, `#DUETSINGERP1`, `#DUETSINGERP2`, and `#CALCMEDLEY` are always unknown tags with no semantic processing.
- `#TAGS` is parsed as recognized metadata only for version `>= 1.0.0`; otherwise it is preserved as custom-tag data.
- Unknown body tokens are non-fatal; they are skipped and diagnosed without rejecting the song.
- A note with `durationBeats = 0` is converted to Freestyle, keeps duration `0`, produces the warning message `found note with length zero -> converted to FreeStyle` with a line number, and contributes `0` score.
- Empty sentences produced during parsing are removed before validation; each track must retain at least one non-empty sentence or the song is invalid.
- A duet song must produce exactly two tracks; a non-duet song must produce exactly one track.
- File beat values are used as authored; note activity and sample inclusion use start-inclusive/end-exclusive intervals.
- Lyric highlighting uses no mic delay; any later lane-beat consumer and scoring windows use configured mic delay.
- A pitch sample is tone-valid if and only if `midiNote != 255`; fixture shorthands must normalize to that rule before scoring.
- `#PREVIEWSTART` takes precedence for preview start when present and positive; otherwise `previewStartSec` falls back to medley start when valid medley tags exist, otherwise `0.0`. `#START` affects `startSec` only.
- Octave normalization must operate on the full semitone value before comparison, not on pitch class modulo 12.
- Golden score rounding deliberately uses the opposite direction from the rounded normal score bucket to prevent totals above `10000`.
- Empty lines do not receive line bonus; very small line max scores (`<= 2`) are treated as perfect by the forgiveness rule.
- Medley-window score-value math treats notes outside the configured medley beat window as Freestyle for score-value purposes, even though medley sequencing itself is outside Phase 0.
- Medley total aggregation is a later results-screen seam and is not part of the Phase 0 F11 gate.
- If the required validation gate cannot produce a fresh scoped `testBranch` pass, Phase 0 is not complete.

## Requirements *(mandatory)*

### Scope Boundaries

- Phase 0 includes parser, song validation, beat-time conversion, scoring math, line bonus, rounding, and fixture harness support needed to validate those components.
- Phase 0 excludes live phone companion behavior, WebSocket control sessions, UDP pitch transport, LAN discovery, HTTP asset streaming, playback, UI screens, game flow orchestration, Android runtime behavior, pitch-lane coordinate-system rules, jitter buffering, note-finalization scheduling, late-frame filtering, and medley sequencing beyond pure score-window math.
- The TV host remains the authoritative owner of parsed chart state and scoring outcomes. Companion devices must not define or override the parsed chart, score values, display rounding, or final scoring outcomes.
- Remote song assets must not be persisted by the TV host. Phase 0 may reference fixture files for validation, but it must not introduce persistent storage for remote song assets.
- The parser and scoring math core must be host-independent and testable without Android framework types, network sockets, playback engines, UI rendering, or live phone clients.
- In this repository, the scoped `testBranch` gate is the enforcement mechanism for the Phase 0 Definition of Done. It validates the required fixture suite and coverage thresholds; it does not add product scope beyond this specification.

### Functional Requirements

#### Phase 0 Delivery Gate

- **FR-001**: Phase 0 MUST deliver deterministic parsing and validation for USDX song files.
- **FR-002**: Phase 0 MUST deliver deterministic static-BPM beat-time conversion.
- **FR-003**: Phase 0 MUST deliver deterministic per-note scoring math for Normal, Golden, Rap, RapGolden, and Freestyle notes.
- **FR-004**: Phase 0 MUST deliver deterministic line bonus and display rounding math.
- **FR-005**: Phase 0 MUST include a fixture-based validation harness for F01-F06 and F08-F11.
- **FR-006**: Phase 0 MUST NOT require a phone, LAN service, playback backend, UI, Android runtime, or mock phone to validate the required parser and scoring outcomes.
- **FR-007**: Phase 0 MUST NOT include jitter buffering, note-finalization scheduling, late-frame filtering, or live pitch transport behavior in its delivery gate; those belong to later fixture groups outside Phase 0.
- **FR-008**: Parser core MUST have no network, playback, scoring, UI, or filesystem responsibilities; fixture discovery/validation may resolve fixture files only at the harness boundary.
- **FR-009**: Scoring math MUST have no network, playback, parser-discovery, UI, or Android runtime responsibilities.
- **FR-010**: Phase 0 MUST NOT include F07 because no F07 fixture is part of the required Phase 0 gate.
- **FR-011**: Phase 0 MUST NOT claim completion unless F01, F02, F03, F04, F05, F06, F08, F09, F10, and F11 pass fresh through the scoped `testBranch` gate.
- **FR-012**: Phase 0 MUST meet at least 80% coverage on the parser and scoring math modules included in this phase.

#### USDX Parse Input and Result

- **FR-013**: The parser MUST accept a caller-supplied canonical song identifier and raw TXT bytes. The parser MUST NOT derive the song identifier from file paths or network data.
- **FR-014**: A successful parse MUST return `Result.success(ParsedSong)` whose `diagnostics` list may contain info/warn entries only.
- **FR-015**: A hard-invalid song MUST fail as `Result.failure(ParseException)` carrying the populated diagnostics list, including at least one invalid-severity diagnostic.
- **FR-016**: Every parse attempt MUST produce structured diagnostics for warnings and invalidations that are deterministic for fixture assertions.
- **FR-017**: Diagnostics MUST include severity, stable code, human-readable message, TXT identifier, and optional 1-based line number when a specific line caused the issue.

#### Parsed Song Header Semantics

- **FR-018**: The header MUST include required song title, artist, positive file BPM, GAP in milliseconds defaulting to `0`, resolved audio filename, and canonical song-root path or URI.
- **FR-019**: The header MUST support optional playback timing fields: start position in seconds, end position in milliseconds, video gap in seconds, and preview start in seconds.
- **FR-020**: The header MUST support optional media fields: video, cover, background, instrumental, and vocals.
- **FR-021**: Instrumental and vocals tags MUST be parsed and preserved as source metadata; TV playback strategy MUST still treat later manifest playback as a single pre-mixed audio URL, not separate instrumental/vocal assets.
- **FR-022**: The header MUST support optional metadata fields for version, year, genre, album, duet singer names, medley start beat, medley end beat, and custom tags.
- **FR-023**: If `#VERSION` is absent, the parser MUST treat the song as legacy version `0.3.0`.
- **FR-024**: If `#VERSION` is present but does not parse as dotted numeric version data, the song MUST be invalid.
- **FR-025**: If `#VERSION >= 2.0.0`, the song MUST be invalid with `ERROR_CORRUPT_SONG_INVALID_VERSION`.
- **FR-026**: Versions below `2.0.0` are supported for Phase 0 validation, subject to all other rules.
- **FR-027**: For version `>= 1.0.0`, `#AUDIO` MUST take precedence over `#MP3`; at least one of them MUST be present, and if both are present `#AUDIO` is used.
- **FR-028**: For legacy songs where `#VERSION` is absent or `< 1.0.0`, `#MP3` MUST be present and `#AUDIO` MUST be ignored for required-audio resolution.
- **FR-029**: The resolved required audio file MUST exist for validation fixtures; missing required audio MUST invalidate the song with `ERROR_CORRUPT_SONG_FILE_NOT_FOUND`.
- **FR-030**: Missing optional media targets MUST NOT invalidate the song; the missing optional media field MUST be treated as absent.
- **FR-031**: Derived `previewStartSec` for library-facing indexed songs MUST use `#PREVIEWSTART` when present and greater than `0`; otherwise it MUST use medley start time when valid medley tags produce `medleySource = "tag"`; otherwise it MUST use `0.0`.
- **FR-032**: `#START` MUST represent playback start position only and MUST map to `startSec`; it MUST NOT be used as the preview fallback rule.

#### Header Parsing Rules

- **FR-033**: Header parsing MUST read header lines while the first character is `#`; the first non-header line ends header parsing.
- **FR-034**: Header tag matching MUST be case-insensitive after trimming and uppercasing the tag name.
- **FR-035**: Duplicate known header tags MUST use the last successfully parsed value.
- **FR-036**: Malformed required header tags MUST invalidate the song.
- **FR-037**: Malformed optional header tags MUST produce a warning and be treated as absent.
- **FR-038**: Unknown tags MUST be preserved as custom header tags in encounter order and MUST NOT invalidate the song.
- **FR-039**: Empty-value tags and no-separator header lines MUST be diagnosed and preserved as custom tags where representable.
- **FR-040**: `#ENCODING`, `#RESOLUTION`, `#NOTESGAP`, `#DUETSINGERP1`, `#DUETSINGERP2`, and `#CALCMEDLEY` MUST be treated as unknown tags regardless of version; they MUST be preserved and MUST NOT receive semantic processing.
- **FR-041**: `#RELATIVE` as a header tag MUST be treated as an unknown custom tag with no semantic effect.

#### Supported Header Tags

- **FR-042**: The parser MUST recognize `#TITLE` as required string song title.
- **FR-043**: The parser MUST recognize `#ARTIST` as required string song artist.
- **FR-044**: The parser MUST recognize `#BPM` as required positive file BPM.
- **FR-045**: The parser MUST recognize `#GAP` as optional millisecond delay from audio start to first beat, defaulting to `0`.
- **FR-046**: The parser MUST recognize `#MP3` and `#AUDIO` as version-dependent audio source tags.
- **FR-047**: The parser MUST recognize `#VIDEO`, `#VIDEOGAP`, `#COVER`, and `#BACKGROUND` as optional media tags.
- **FR-048**: The parser MUST recognize `#INSTRUMENTAL` as an optional source backing track tag and `#VOCALS` as an optional source vocal stem tag.
- **FR-049**: The parser MUST recognize `#START`, `#END`, and `#PREVIEWSTART` as optional playback offset tags.
- **FR-050**: The parser MUST recognize `#VERSION` as the optional source file version tag.
- **FR-051**: The parser MUST recognize `#MEDLEYSTARTBEAT` and `#MEDLEYENDBEAT` as optional integer beat tags for medley eligibility and scoring-window math.
- **FR-052**: The parser MUST recognize `#P1` and `#P2` as optional duet singer-name tags stored as metadata only.
- **FR-053**: The parser MUST recognize `#YEAR`, `#GENRE`, `#EDITION`, `#CREATOR`, and `#LANGUAGE` as optional metadata tags, and MUST recognize `#TAGS` as optional metadata only for version `>= 1.0.0`.
- **FR-054**: Any tag not listed above as recognized MUST be preserved in custom tags and MUST NOT cause rejection solely because it is unknown.

#### Body Grammar and Track Routing

- **FR-055**: The parser MUST support note tokens `:`, `*`, `F`, `R`, and `G`.
- **FR-056**: The parser MUST support `-` as a line break/new sentence token.
- **FR-057**: The parser MUST support `E` as the end-of-song-data token.
- **FR-058**: The parser MUST support `P1` and `P2` as duet part delimiters.
- **FR-059**: Each note line MUST contain token, start beat, duration in beats, tone semitone, and lyric remainder.
- **FR-060**: The parser MUST store `startBeatFile` and `durationBeats` as authored chart beat values with no multiplication applied to the stored note data.
- **FR-061**: The parser MUST store `toneSemitone` as the USDX semitone index where C2 is tone `0`.
- **FR-062**: The parser MUST preserve the lyric remainder as authored, including empty lyric text.
- **FR-063**: If the first non-empty body line begins with a duet part marker, the song MUST be treated as a duet.
- **FR-064**: In duet songs, `P1` and `P2` markers MUST set the active track, and notes plus line breaks MUST be assigned to the current active track.
- **FR-065**: A single `E` token MUST end the file body for both solo and duet charts.
- **FR-066**: A duet song MUST produce exactly two tracks; a non-duet song MUST produce exactly one track.
- **FR-067**: A `P` marker other than P1 or P2 MUST invalidate the song with `ERROR_CORRUPT_SONG_INVALID_DUET_MARKER`.
- **FR-068**: Unknown body tokens MUST be skipped with a non-fatal diagnostic and MUST NOT invalidate the song.
- **FR-069**: A recognized note or sentence token with malformed numeric fields MUST invalidate the song with `ERROR_CORRUPT_SONG_MALFORMED_BODY`.
- **FR-070**: A `B` body token for variable BPM MUST invalidate the song with `ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM`.
- **FR-071**: RELATIVE body format represented by sentence lines with an extra numeric beat-delta parameter MUST invalidate the song with `ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE`.
- **FR-072**: Phase 0 MUST use hardcoded semantics equivalent to `RapToFreestyle = false` and `OutOfBoundsToFreestyle = false`; Rap and out-of-range notes MUST NOT be converted to Freestyle except for the explicit zero-duration rule.

#### Parser Invariants and Score Values

- **FR-073**: Every parsed note across all tracks MUST have `durationBeats >= 0`.
- **FR-074**: Any note with `durationBeats = 0` MUST be converted to Freestyle, retain duration `0`, produce the warning message `found note with length zero -> converted to FreeStyle` with a line number, and contribute `0` score.
- **FR-075**: Empty sentences with zero note events after parsing MUST be removed.
- **FR-076**: After sentence cleanup, every track MUST contain at least one remaining sentence; otherwise the song MUST be invalid with `ERROR_CORRUPT_SONG_NO_NOTES`.
- **FR-077**: Songs with no valid note lines MUST be invalid with `ERROR_CORRUPT_SONG_NO_NOTES`.
- **FR-078**: A line's score value MUST equal the sum of `durationBeats * ScoreFactor(noteType)` for notes in that line.
- **FR-079**: A track's score value MUST equal the sum of `durationBeats * ScoreFactor(noteType)` for notes in that track.
- **FR-080**: Line and track score values computed by the parser are canonical for Phase 0 scoring and MUST NOT be recomputed differently by scoring consumers.
- **FR-081**: Note active intervals MUST use `startBeatFile <= beat < endBeatFileExclusive`.
- **FR-082**: `endBeatFileExclusive` MUST equal `startBeatFile + durationBeats`.

#### Minimum Invalid Diagnostic Codes

- **FR-083**: The parser/validator MUST support `ERROR_CORRUPT_SONG_FILE_NOT_FOUND` for missing or unresolvable required audio.
- **FR-084**: The parser/validator MUST support `ERROR_CORRUPT_SONG_NO_NOTES` for no remaining valid sentences after cleanup.
- **FR-085**: The parser/validator MUST reserve `ERROR_CORRUPT_SONG_NO_BREAKS` for inability to construct any sentence container.
- **FR-086**: The parser/validator MUST support `ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER` for missing required title, artist, audio, MP3, or BPM data according to version rules.
- **FR-087**: The parser/validator MUST support `ERROR_CORRUPT_SONG_MALFORMED_HEADER` for required headers that are present but malformed or unparseable.
- **FR-088**: The parser/validator MUST support `ERROR_CORRUPT_SONG_MALFORMED_BODY` for recognized body tokens whose numeric fields cannot be parsed.
- **FR-089**: The parser/validator MUST support `ERROR_CORRUPT_SONG_UNSUPPORTED_VARIABLE_BPM` for `B` body tokens.
- **FR-090**: The parser/validator MUST support `ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE` for legacy RELATIVE sentence syntax.
- **FR-091**: The parser/validator MUST support `ERROR_CORRUPT_SONG_INVALID_VERSION` for invalid version syntax or versions `>= 2.0.0`.
- **FR-092**: The parser/validator MUST support `ERROR_CORRUPT_SONG_INVALID_DUET_MARKER` for duet part markers other than P1 or P2.

#### Discovery and Validation Fixture Semantics

- **FR-093**: The Phase 0 validation harness MUST recursively discover `.txt` files under fixture song roots when a fixture requires recursive discovery.
- **FR-094**: Fixture TXT identifiers MUST be portable and deterministic; absolute local paths and dynamic modified-time values MUST NOT be asserted unless a fixture explicitly requires them.
- **FR-095**: Valid discovered songs MUST expose deterministic library-facing `IndexedSong` fields when a fixture exercises indexed-song behavior: `songId`, `phoneClientId`, `relativeTxtPath`, `modifiedTimeMs`, `title`, `artist`, `album`, `year`, `genre`, `txtUrl`, `audioUrl`, `videoUrl`, `coverUrl`, `backgroundUrl`, `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`, `medleySource`, `medleyStartBeat`, `medleyEndBeat`, `startSec`, and `previewStartSec`.
- **FR-096**: Invalid discovered songs MUST expose deterministic validity, invalid reason code, and invalid line number when fixture expectations require them.
- **FR-097**: A valid song's derived identifier format for library-facing fixtures MUST be `phoneClientId + "::" + relativeTxtPath`.
- **FR-098**: Relative TXT paths in fixture-facing indexed-song behavior MUST use forward slashes, MUST NOT start with `/`, MUST NOT contain `.` or `..` segments, and MUST preserve case.
- **FR-099**: A valid manifest-eligible entry MUST have non-null `txtUrl` and `audioUrl` values.
- **FR-100**: `hasVideo` MUST equal whether `videoUrl` is present, and `hasInstrumental` MUST reflect source `#INSTRUMENTAL` presence only; playback strategy MUST still rely on a single pre-mixed `audioUrl`.
- **FR-101**: `medleySource = "tag"` requires non-null `medleyStartBeat` and `medleyEndBeat` with `medleyStartBeat < medleyEndBeat`; `medleySource = null` requires `canMedley = false`.
- **FR-102**: Medley auto-calculation from refrain-finding tags is out of Phase 0 scope; `#CALCMEDLEY` MUST be preserved as an unknown tag with no semantic processing.

#### Beat-Time Conversion

- **FR-103**: USDX beat numbers in `.txt` files MUST be the authoritative beat grid.
- **FR-104**: File beats in note lines and sentence lines MUST be stored and processed as integer chart beat values without scaling the stored beat values.
- **FR-105**: Internal BPM for conversion MUST equal `BPM_file * 4`.
- **FR-106**: `BeatCalculator.timeSecToMidBeatInternal(tSec, bpmInternal)` MUST convert time in seconds relative to chart origin to beat position as `tSec * (bpmInternal / 60.0)`.
- **FR-107**: `BeatCalculator.beatInternalToTimeSec(beatInternal, bpmInternal)` MUST convert beat position to time in seconds relative to chart origin as `beatInternal * (60.0 / bpmInternal)`.
- **FR-108**: Chart origin for lyric highlighting MUST be `lyricsTimeSec - GAPms / 1000.0`.
- **FR-109**: Lyric highlight beat MUST be `floor(timeSecToMidBeatInternal(lyricsTimeSec - GAPms / 1000.0, bpmInternal))` and MUST use mic delay `0`.
- **FR-110**: Any later lane-beat consumer and every scoring-window calculation MUST include the configured mic delay, but the Phase 0 deliverable is limited to the shared beat-time math rather than lane coordinate rendering.
- **FR-111**: Effective mic delay for Phase 0 scoring math MUST be a caller-supplied integer in the range `0..400` milliseconds; `0` means no compensation.
- **FR-112**: The effective mic delay MUST shift scoring windows later by the configured number of milliseconds and MUST be recomputed on every song start, restart, and reconnect.
- **FR-113**: Passing the lyric-highlight delay to scoring consumers, or passing the scoring delay to lyric-highlight consumers, is a conformance error.
- **FR-114**: For a note with start beat and duration, the scoring start time MUST be `songStartTvMs + (startBeat * 15000 / BPM_file) + GAPms + micDelayMs`.
- **FR-115**: For a note with start beat and duration, the scoring end time MUST be `songStartTvMs + ((startBeat + durationBeats) * 15000 / BPM_file) + GAPms + micDelayMs`.
- **FR-116**: A pitch sample is within a note window only if `noteStartTvMs <= sample.tvTimeMs < noteEndTvMs`.
- **FR-117**: F06's static-BPM fixture MUST pass, including the case where `lyricsTimeSec = 5.0`, `GAPms = 2000`, and `BPM_file = 120` produce highlight beat `24`.
- **FR-118**: F06's scoring-cursor case MUST pass, including the corresponding nonzero mic-delay expectation where the scoring cursor differs from the highlight cursor.
- **FR-119**: Beat-time round trips for deterministic inline tests MUST match within `1e-9s`.

#### Scoring Configuration and Budgets

- **FR-120**: Scoring MUST receive all current-song gameplay inputs that affect scoring through explicit scoring configuration.
- **FR-121**: Scoring configuration MUST include player difficulty per assigned singer and whether line bonus is enabled.
- **FR-122**: Scoring MUST NOT read difficulty or line-bonus settings implicitly from UI state, global settings, or hidden mutable state.
- **FR-123**: If line bonus is enabled, maximum song note points MUST be `9000` and maximum line bonus pool MUST be `1000`.
- **FR-124**: If line bonus is disabled, maximum song note points MUST be `10000` and maximum line bonus pool MUST be `0`.
- **FR-125**: Score factor MUST be `0` for Freestyle, `1` for Normal, `2` for Golden, `1` for Rap, and `2` for RapGolden.
- **FR-126**: For medley-window scoring math, only notes within `[medleyStartBeat, medleyEndBeat)` contribute to track score value; notes outside the window are treated as Freestyle for score-value purposes.

#### Per-Note Scoring

- **FR-127**: A note's qualifying sample set MUST contain the samples selected for that note window according to the Phase 0 fixture or inline test case.
- **FR-128**: If a note has zero qualifying samples, its note score MUST be `0`.
- **FR-129**: If a note has qualifying samples, the hit count MUST be the number of qualifying samples that satisfy the note-type hit rule.
- **FR-130**: Maximum note score MUST equal `(MaxSongPoints / TrackScoreValue) * ScoreFactor(noteType) * durationBeats`.
- **FR-131**: Actual note score MUST equal `maximum note score * (hits / qualifying sample count)`.
- **FR-132**: The `hits / qualifying sample count` division MUST use double-precision floating-point division, not integer division.
- **FR-133**: Normal and Rap note scores MUST add to the normal score accumulator.
- **FR-134**: Golden and RapGolden note scores MUST add to the golden score accumulator.
- **FR-135**: Freestyle notes MUST never add score, even when samples are voiced and pitch-matching.
- **FR-136**: A pitch sample's tone in USDX semitone space MUST be derived as `midiNote - 36` when MIDI-note fixture data is used.
- **FR-137**: A sample is tone-valid if and only if `midiNote != 255`; there is no separate wire-level boolean, and any fixture shorthand MUST normalize to that rule before scoring.

#### Hit Detection and Difficulty

- **FR-138**: Freestyle notes MUST never be evaluated for hits.
- **FR-139**: Normal and Golden notes MUST hit only when the sample is tone-valid and the octave-normalized sample tone is within the active player's tolerance range from the target tone.
- **FR-140**: Rap and RapGolden notes MUST hit when the sample is tone-valid; pitch difference MUST be ignored.
- **FR-141**: Easy difficulty MUST use a tolerance range of `2` semitones.
- **FR-142**: Medium difficulty MUST use a tolerance range of `1` semitone.
- **FR-143**: Hard difficulty MUST use a tolerance range of `0` semitones.
- **FR-144**: Medium MUST be the default difficulty for newly assigned singers.
- **FR-145**: Difficulty tolerance MUST apply only to Normal and Golden notes.
- **FR-146**: F09 MUST pass all tolerance and octave-normalization subcases.
- **FR-147**: F10 MUST pass Rap presence-only scoring, including voiced beats scoring and unvoiced beats missing.

#### Octave Normalization

- **FR-148**: Before comparing a detected tone to the target tone, scoring MUST normalize the detected tone to the closest octave of the target.
- **FR-149**: While `Tone - TargetTone > 6`, scoring MUST subtract `12` from `Tone`.
- **FR-150**: While `Tone - TargetTone < -6`, scoring MUST add `12` to `Tone`.
- **FR-151**: Scoring MUST NOT reduce tones to pitch class modulo 12 before applying the octave-normalization loop.
- **FR-152**: The octave-normalization loop MUST operate on the full semitone value and shift by octaves until distance from target is within ±6.

#### Line Bonus

- **FR-153**: A sentence is complete when its last scorable note has been finalized within the scoring-math model.
- **FR-154**: Line bonus evaluation MUST run when a sentence completes.
- **FR-155**: Per-line maximum score MUST equal `MaxSongPoints * (LineScoreValue / TrackScoreValue)`.
- **FR-156**: A player's current line score MUST equal `(normal score + golden score) - scoreLast`.
- **FR-157**: If the maximum line score is `<= 2`, line perfection MUST be `1`.
- **FR-158**: Otherwise, line perfection MUST be clamped to `[0, 1]` after computing `line score / (maximum line score - 2)`.
- **FR-159**: Empty lines with `LineScoreValue = 0` MUST NOT receive line bonus.
- **FR-160**: Non-empty line count MUST equal total lines minus empty lines.
- **FR-161**: Line bonus per line MUST equal `MaxLineBonusPool / NonEmptyLines` using floating-point division.
- **FR-162**: The line score accumulator MUST add `LineBonusPerLine * LinePerfection` for each completed non-empty line.
- **FR-163**: `scoreLast` MUST be updated after each sentence's line bonus is applied.
- **FR-164**: F11 MUST pass the perfect-performance line bonus case where normal notes yield `3000`, golden notes yield `6000`, line bonus yields `1000`, and total displayed score is `10000`.

#### Rounding and Display Scores

- **FR-165**: Normal display score MUST equal `round(normal score / 10) * 10`.
- **FR-166**: Line display score MUST equal `floor(round(line score) / 10) * 10`.
- **FR-167**: If rounded normal display score is less than the raw normal score, golden display score MUST equal `ceil(golden score / 10) * 10`.
- **FR-168**: If rounded normal display score is greater than or equal to the raw normal score, golden display score MUST equal `floor(golden score / 10) * 10`.
- **FR-169**: Total display score MUST equal normal display score plus golden display score plus line display score.
- **FR-170**: The line score rounding asymmetry is intentional and MUST NOT be normalized to the normal/golden rounding formula.
- **FR-171**: Golden score opposite-direction rounding is intentional and MUST prevent final displayed total from exceeding `10000`.
- **FR-172**: F11 MUST pass all rounding fixture expectations, including total score never exceeding `10000`.
- **FR-173**: Medley total aggregation is a later results-screen seam and MUST NOT be required for the Phase 0 F11 gate unless a future scope amendment explicitly adds it.

#### Fixture Harness Requirements

- **FR-174**: The fixture harness MUST support discovery/index fixtures that compare deterministic `expected.discovery.json` outputs and MUST keep Phase 0 validation at the pure unit/fixture boundary rather than a peer-boundary socket harness.
- **FR-175**: The fixture harness MUST support parsed-song fixtures that compare deterministic `expected.parsedSong.json` outputs.
- **FR-176**: The fixture harness MUST support scoring fixtures containing `song.txt`, pitch-frame data where present, and expected score outputs, without depending on live sockets, companion-device emulation, or runtime coroutine scheduling.
- **FR-177**: The fixture harness SHOULD construct very small pitch sample sequences inline when a fixture explicitly permits inline construction instead of separate frame files.
- **FR-178**: Dynamic fields such as absolute local paths, generated song IDs, modified times, and runtime timestamps MUST NOT be asserted unless the fixture defines deterministic expected values.
- **FR-179**: F01 MUST validate recursive discovery, required header rejection, missing required audio rejection, malformed body rejection, v1 audio precedence, legacy MP3 preference, legacy missing MP3 rejection, missing optional video acceptance, and the library-facing `IndexedSong` invariants covered by the fixture.
- **FR-180**: F02 MUST validate header title, artist, version, BPM, resolved audio, preview start, ordered custom tags, malformed BPM rejection, and deterministic validity diagnostics.
- **FR-181**: F03 MUST validate unknown body token handling, malformed body rejection, zero-duration Freestyle conversion, implicit single-sentence behavior when no break lines exist, no-notes rejection, variable BPM rejection, RELATIVE header preservation, RELATIVE body rejection, and Freestyle scoring as zero.
- **FR-182**: F04 MUST validate duet P1/P2 routing and invalid duet marker rejection.
- **FR-183**: F05 MUST validate legacy RELATIVE semantics according to the Phase 0 rule that RELATIVE body syntax is unsupported and invalid.
- **FR-184**: F06 MUST validate static-BPM beat-time conversion.
- **FR-185**: F08 MUST validate scoring interval semantics and start-inclusive/end-exclusive note activity.
- **FR-186**: F09 MUST validate pitch tolerance and octave normalization.
- **FR-187**: F10 MUST validate Rap tone-valid gating.
- **FR-188**: F11 MUST validate line bonus and rounding.

### Key Entities and Normative Contracts *(include if feature involves data)*

The code-style contract blocks in this section are normative for this specification. For Phase 0, these contracts are the authoritative field/type/nullability definitions for parser, library-facing index, beat-time conversion, and scoring seams. This section is intentionally technical; the self-contained slice must preserve exact contracts rather than abstract them into prose-only entities.

#### Parser and Parsed-Song Contracts

```kotlin
interface UsdxParser {
    /**
     * Parse a USDX TXT file into a ParsedSong.
     *
     * @param songId canonical "clientId::relativeTxtPath" supplied by caller;
     *               parser does not derive it.
     * @param txtBytes raw file bytes (encoding handled internally).
     * @return Result.success with ParsedSong whose diagnostics may contain
     *         info/warn entries only. Hard-invalid songs return
     *         Result.failure(ParseException) carrying the populated diagnostics list.
     */
    fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong>
}

class ParseException(
    val diagnostics: List<DiagnosticEntry>
) : RuntimeException(diagnostics.firstOrNull { it.severity == Severity.Invalid }?.message)

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

data class CustomHeaderTag(
    val tag: String,
    val content: String
)

data class SongTiming(
    val bpmFile: Float
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
) {
    val startBeatFile: Int
        get() = notes.firstOrNull()?.startBeatFile ?: 0
    val endBeatFileExclusive: Int
        get() = notes.maxOfOrNull { it.endBeatFileExclusive } ?: 0
    val isEmpty: Boolean
        get() = lineScoreValue == 0L
}

data class NoteEvent(
    val noteType: NoteType,
    val startBeatFile: Int,
    val durationBeats: Int,
    val toneSemitone: Int,
    val lyric: String
) {
    val endBeatFileExclusive: Int
        get() = startBeatFile + durationBeats
}

enum class NoteType { Normal, Golden, Rap, RapGolden, Freestyle }

data class DiagnosticEntry(
    val severity: Severity,
    val code: String,
    val message: String,
    val txtUri: String,
    val lineNumber: Int? = null
)

enum class Severity { Info, Warn, Invalid }
```

**Parser invariants and NFRs**:
- `durationBeats >= 0` for every `NoteEvent`; `durationBeats == 0` is parser-converted to `Freestyle` and scores zero.
- `tracks.size == 2` iff `header.isDuet == true`; otherwise `tracks.size == 1`.
- Variable-BPM songs are rejected at parse time.
- `lineScoreValue` and `trackScoreValue` are computed by the parser and are canonical.
- Parser core is a pure function with no I/O, no network knowledge, no playback knowledge, and no scoring knowledge.
- Parser SLA for this phase: parse time `< 50ms` for a 10KB TXT and no allocation beyond the result model in profiler validation.

#### Supporting Types Used by Phase 0

```kotlin
enum class PlayerId { P1, P2 }

data class BeatRange(
    val startBeat: Int,
    val endBeat: Int
)

enum class Difficulty { Easy, Medium, Hard }
```

#### Library-Facing Index Contract

```kotlin
interface LibraryManager {
    val songs: StateFlow<List<IndexedSong>>
    fun getSong(songId: String): IndexedSong?
}

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

**Indexed-song invariants**:
- `songId == phoneClientId + "::" + relativeTxtPath` byte-exact.
- `relativeTxtPath` uses `/`, does not begin with `/`, contains no `.` or `..` path segments, and preserves case.
- `txtUrl` and `audioUrl` are non-null for manifest-published valid songs.
- `hasVideo == (videoUrl != null)`.
- `hasInstrumental` is source metadata only and MUST NOT change TV playback strategy.
- `medleySource == "tag"` requires non-null beats with `medleyStartBeat < medleyEndBeat`.
- `previewStartSec` derives from `#PREVIEWSTART`, then medley start when medley tags are valid, then `0.0`.
- `startSec` derives from `#START`, else `0.0`.

#### Beat-Time Contract

```kotlin
object BeatCalculator {
    fun timeSecToMidBeatInternal(tSec: Double, bpmInternal: Float): Double {
        return tSec * (bpmInternal / 60.0)
    }

    fun beatInternalToTimeSec(beatInternal: Double, bpmInternal: Float): Double {
        return beatInternal * (60.0 / bpmInternal)
    }
}
```

**Beat-time seam rules**:
- Lyrics-beat consumers use `micDelayMs = 0`.
- Lane-beat and scoring-window consumers use the configured mic delay.
- Passing the wrong delay for a consumer is a conformance error.
- The mic-delay value is recomputed on every song start, restart, and reconnect.
- Phase 0 specifies the shared math only; pitch-lane coordinate-system geometry is a later phase.

#### Scoring Contract and Future Runtime Seam

```kotlin
interface ScoringEngine {
    val playerScores: StateFlow<Map<PlayerId, PlayerScore>>
    val livePitch: SharedFlow<PitchEvent>

    fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig)
    fun setSongStart(songStartTvMs: Long)
    fun start()
    fun suspend()
    fun resume()
    suspend fun finalizeAll(): Map<PlayerId, PlayerScore>
    fun reset()
    fun stop()
}

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

data class PitchEvent(
    val playerId: PlayerId,
    val midiNote: UByte,
    val toneValid: Boolean,
    val tvTimeMs: Long,
    val arrivalTvMs: Long
)

data class PitchSample(
    val playerId: PlayerId,
    val midiNote: Int,
    val tvTimeMs: Long?
)
```

**Phase 0 scoring seam rules**:
- Phase 0 validates only the pure math subset of the scoring contract: beat-window inclusion, per-note scoring, tolerance, octave normalization, line bonus, medley-window score-value math, and display rounding.
- Runtime jitter buffering, coroutine scheduling, frame staleness filtering, live-pitch emission, and note-finalization orchestration are future seams and are explicitly outside the Phase 0 gate.
- `ScoringConfig` is the only allowed source of current-song scoring configuration.
- `PitchSample` is the Phase 0 fixture contract; it MUST normalize to the same semantics as runtime `PitchEvent`, especially `toneValid == (midiNote != 255)` and `tone = midiNote - 36`.
- Scoring math remains fixture-testable without UDP, sockets, Android runtime, or live coroutine execution.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of required Phase 0 fixture groups pass fresh: F01, F02, F03, F04, F05, F06, F08, F09, F10, and F11.
- **SC-002**: Parser and scoring math modules meet at least 80% test coverage during the scoped validation run.
- **SC-003**: A 10KB USDX TXT fixture parses in under 50ms in the parser benchmark or equivalent Phase 0 performance test, and parser profiling shows no allocation beyond the result model during parse.
- **SC-004**: Perfect scoring fixture performances produce `scoreTotalInt == 10000` in 100% of applicable cases.
- **SC-005**: Scoring fixtures covering silence, no qualifying samples, or Freestyle-only notes produce `scoreTotalInt == 0` or zero per-note score deltas as specified by their expected outputs.
- **SC-006**: Static-BPM beat-time conversion fixture F06 matches all expected cursor values, and the round-trip inline test matches within `1e-9s`.
- **SC-007**: Display score totals never exceed `10000` across F11 and any inline rounding edge cases.
- **SC-008**: Required invalid parser cases produce the expected stable invalidation code and deterministic line number in 100% of fixture cases that assert them.
- **SC-009**: The Phase 0 parser and scoring math validation suite completes without requiring a phone, mock phone, LAN discovery, WebSocket, UDP socket, HTTP asset server, playback backend, UI renderer, or Android framework runtime.
- **SC-010**: The scoped `testBranch` gate passes fresh before Phase 0 completion is claimed.

## Assumptions

- Phase 0 corresponds only to the Foundation iteration: parser, beat-time conversion, scoring math, line bonus, rounding, and fixture harness support.
- Later features will add phones, networking, playback, UI, game phase orchestration, live pitch transport, results screens, and medley sequencing. Those later features are intentionally out of scope here.
- Fixture references are allowed as external validation artifacts. This specification is otherwise self-contained and does not depend on another requirements document for parser or scoring behavior.
- The fixture manifest under `fixtures/manifest.json` is the authoritative list of available acceptance fixture groups, while this Phase 0 scope selects only F01-F06 and F08-F11.
- If an existing fixture README or expected output conflicts with this specification, the conflict must be resolved deliberately and called out in this spec before implementation claims completion; do not silently weaken the requirement.
- Manual mic delay exists as a caller-supplied value for Phase 0 scoring math tests. Settings UI for changing it is out of scope.
- Phase 0 score math may use fixture-provided or inline pitch samples. Live UDP jitter buffering and frame validation are out of scope for this phase.
- Medley sequencing is out of scope, but medley-window score-value math is included because parser and scoring fixtures exercise medley-related fields.
- Within this repository, `testBranch` is the scoped validation gate that enforces the Definition of Done for this slice.
- Invalid songs may still be represented in fixture validation outputs for diagnostics, but they are not eligible to appear as valid songs in a manifest-facing output.
