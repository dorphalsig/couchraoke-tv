# Feature Specification: Song Library (Data Layer)

**Feature Branch**: `004-song-library`
**Created**: 2026-03-16
**Status**: Draft
**Input**: Song library data layer — browse, discover, load songs from connected phones. Spec: §3.1, §3.3–§3.4 (data layer only, no UI).

## Overview

The TV aggregates song metadata received from all currently connected phones into an in-memory library index. The library is never persisted between sessions. Songs live on the phones — the TV fetches song metadata from each phone's `/manifest.json` HTTP endpoint and builds a local index for browsing, filtering, and playback selection. No song files are stored on the TV.

**Scope**: In-memory library model, song entry data structure and validation rules, derived index fields, and library lifecycle (add/remove songs per phone). Does **not** cover the Song List UI screen (feature 006), HTTP transport to phones (feature 005), or network session management (feature 005).

---

## User Scenarios & Testing

### User Story 1 — Library Lifecycle (Priority: P1)

When phones connect and disconnect during a session, the TV library accurately reflects the current set of available songs with no stale entries and no missed additions.

**Why this priority**: The library is the foundation of all song selection. Incorrect lifecycle management breaks every downstream feature.

**Independent Test**: Can be fully tested by creating an in-memory `SongLibrary`, calling `addPhone(clientId, entries)` and `removePhone(clientId)`, and asserting the resulting song list.

**Acceptance Scenarios**:

1. **Given** two phones each provide 3 valid songs, **When** both manifests are ingested, **Then** the library contains exactly 6 songs.
2. **Given** a phone has previously contributed 4 songs, **When** the phone disconnects, **Then** all 4 of its songs are immediately removed; the library contains only songs from remaining phones.
3. **Given** a phone previously provided 3 songs, **When** a new manifest from the same phone is ingested, **Then** the old 3 entries are replaced — not appended — and the library reflects only the new manifest.
4. **Given** any song in the library, **When** its `songId` is read, **Then** `songId` equals `phoneClientId + "::" + relativeTxtPath`.
5. **Given** songs from multiple phones, **When** the sorted song list is retrieved, **Then** songs are ordered by Artist → Album → Title (case-insensitive).

---

### User Story 2 — Song Entry Validation (Priority: P2)

When a phone sends its manifest, the TV validates each song entry. Invalid entries are accepted into the index flagged as unplayable with a diagnostic code. Valid entries are cleared for playback.

**Why this priority**: Validation prevents playing corrupt or incomplete songs and provides diagnostics for library health reporting.

**Independent Test**: Can be fully tested by calling `SongValidator.validate(parsedSong, assetResolver)` with fixture songs from `F01_song_discovery_validation_acceptance` and asserting `isValid`, `invalidReasonCode`, and `invalidLineNumber`.

**Acceptance Scenarios**:

1. **Given** a song missing `#ARTIST`, **When** validated, **Then** `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER`.
2. **Given** a song where `#AUDIO` references a non-existent file, **When** validated, **Then** `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_FILE_NOT_FOUND`, `invalidLineNumber` points to the `#AUDIO` line.
3. **Given** a v1.0.0 song with both `#AUDIO` and `#MP3`, **When** validated, **Then** `isValid=true` and `resolvedAudio` equals the `#AUDIO` value.
4. **Given** a legacy song (no `#VERSION`) with both `#AUDIO` and `#MP3`, **When** validated, **Then** `isValid=true` and `resolvedAudio` equals the `#MP3` value.
5. **Given** a legacy song with `#AUDIO` but no `#MP3`, **When** validated, **Then** `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER`.
6. **Given** a v1.0.0 song whose `#VIDEO` target file is absent, **When** validated, **Then** `isValid=true`, `hasVideo=false`.
7. **Given** a song with `#BPM:0`, **When** validated, **Then** `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_MALFORMED_HEADER`.
8. **Given** a non-numeric `#BPM` value, **When** validated, **Then** `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_MALFORMED_HEADER`, `invalidLineNumber` is correct.
9. **Given** a songs root directory with a mix of valid and invalid entries in subdirectories, **When** the root is scanned recursively, **Then** all `.txt` files are discovered and validity matches `F01/expected.discovery.json`.

---

### User Story 3 — Derived Index Fields (Priority: P3)

When a song entry is added to the library index, all derived fields (flags, medley eligibility, preview metadata) are computed from the parsed song and stored on the entry.

**Why this priority**: Derived fields power filtering, medley playlist building, and preview playback. They must be computed at ingest time, not on every query.

**Independent Test**: Can be fully tested by ingesting inline songs with known properties and asserting individual flag values on the resulting `SongEntry`.

**Acceptance Scenarios**:

1. **Given** a song with P1/P2 tracks in the body, **When** indexed, **Then** `isDuet=true`.
2. **Given** a song with `R` or `G` note events, **When** indexed, **Then** `hasRap=true`.
3. **Given** a duet song, **When** indexed, **Then** `canMedley=false` regardless of medley tags.
4. **Given** a non-duet song with `#MEDLEYSTARTBEAT` and `#MEDLEYENDBEAT` in the header, **When** indexed, **Then** `canMedley=true`, `medleySource="tag"`, and beat values are stored.
5. **Given** a non-duet song with no medley tags, **When** indexed, **Then** `canMedley=false`, `medleySource=null`.
6. **Given** a song with `#PREVIEWSTART:45.0`, **When** indexed, **Then** `previewStartSec=45.0`.
7. **Given** a song without `#PREVIEWSTART` but with medley tags, **When** indexed, **Then** `previewStartSec` equals the time in seconds of `medleyStartBeat`.
8. **Given** a song without `#PREVIEWSTART` and no medley tags, **When** indexed, **Then** `previewStartSec=0.0`.

---

### Edge Cases

- A phone sends an empty manifest (zero songs) — the library removes all prior entries for that phone; no error is raised.
- A manifest entry has a `relativeTxtPath` containing `..` or starting with `/` — the entry is rejected with `ERROR_CORRUPT_SONG_INVALID_PATH`.
- Two phones send songs with the same `relativeTxtPath` — both are accepted; `songId` is unique because it includes `phoneClientId`.
- A manifest fetch fails (network error or timeout) — the previous catalog for that phone is retained unchanged; a diagnostic is emitted.
- A song body contains only freestyle notes — `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_NO_NOTES` after empty-sentence cleanup.
- `#CALCMEDLEY:OFF` in header — `calcMedleyEnabled=false` is stored on the entry.
- `#PREVIEWSTART` present but value is `0` or negative — treated as absent; falls back to medley or `0.0`.

---

## Requirements

### Functional Requirements

- **FR-001**: The library MUST maintain an in-memory index of song entries grouped by `phoneClientId`; it MUST NOT be persisted between sessions.
- **FR-002**: When a phone's manifest is ingested, the library MUST replace (not append to) all existing entries for that `phoneClientId`.
- **FR-003**: When a phone disconnects, the library MUST immediately remove all entries attributed to that phone's `clientId`.
- **FR-004**: Each song entry MUST have a stable `songId` of the form `phoneClientId + "::" + relativeTxtPath`.
- **FR-005**: `relativeTxtPath` MUST use `/` separators, MUST NOT start with `/`, and MUST NOT contain `.` or `..` segments; entries violating this MUST be rejected with `ERROR_CORRUPT_SONG_INVALID_PATH`.
- **FR-006**: The sorted song list MUST be ordered by Artist → Album → Title, case-insensitively.
- **FR-007**: A song entry MUST be validated: `#TITLE`, `#ARTIST`, and a non-zero parseable `#BPM` MUST be present; a required audio file reference MUST be present and its target file MUST exist.
- **FR-008**: For v1.0.0 songs (both `#AUDIO` and `#MP3` present): `#AUDIO` MUST take precedence as the resolved audio reference.
- **FR-009**: For legacy songs (`#VERSION` absent or `< 1.0.0`): `#MP3` MUST be used; `#AUDIO` MUST be ignored even if present.
- **FR-010**: Missing optional assets (`#VIDEO`, `#COVER`, etc.) MUST NOT invalidate the song; the corresponding flag (`hasVideo`, etc.) MUST be `false`.
- **FR-011**: Invalid entries MUST be stored in the index with `isValid=false`, `invalidReasonCode`, and (where applicable) `invalidLineNumber`; they MUST NOT be selectable for playback.
- **FR-012**: Each entry MUST store derived flags: `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`, `medleySource`, `medleyStartBeat`, `medleyEndBeat`, `calcMedleyEnabled`.
- **FR-013**: `canMedley` MUST be `false` for duet songs. For non-duet songs, `canMedley=true` only when both `#MEDLEYSTARTBEAT` and `#MEDLEYENDBEAT` are present.
- **FR-014**: `previewStartSec` MUST be: `#PREVIEWSTART` value (if present and > 0); else `timeFromBeat(medleyStartBeat)` (if `medleySource != null`); else `0.0`.
- **FR-015**: Each entry MUST store asset URLs as received from the manifest (`txtUrl`, `audioUrl`, `videoUrl`, `coverUrl`, `backgroundUrl`, `instrumentalUrl`, `vocalsUrl`); absent values MUST be stored as `null`.
- **FR-016**: The library MUST expose queries to retrieve the sorted song list, look up a song by `songId`, and filter songs by phone.

### Key Entities

- **SongLibrary**: In-memory aggregate index. Manages per-phone song collections and exposes sorted query and lookup. Lives for the duration of one session.
- **SongEntry**: A single song record containing identity fields (`songId`, `phoneClientId`, `relativeTxtPath`, `modifiedTimeMs`), validation state (`isValid`, `invalidReasonCode`, `invalidLineNumber`), display fields (`artist`, `title`, `album`), derived flags, preview metadata (`startSec`, `previewStartSec`), and asset URLs.
- **ManifestEntry**: A raw song entry as received from `/manifest.json` on the phone — source data before validation and field derivation.
- **SongValidator**: Stateless component that takes a parsed song and an asset resolver, applies all validation rules, and returns a validation result.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: All songs from a connecting phone appear in the library immediately after manifest ingest; no manual refresh required.
- **SC-002**: After a phone disconnects, its songs are removed before any subsequent library query can return them (synchronous removal).
- **SC-003**: Re-fetching a phone's manifest produces an identical library state to initial fetch — no duplicates or stale entries remain.
- **SC-004**: All 9 validation acceptance scenarios (`T3.2.1–T3.2.9`) pass against fixture `F01_song_discovery_validation_acceptance`.
- **SC-005**: All 5 index-field acceptance scenarios (`T3.3.1–T3.3.5`) pass with inline test data.
- **SC-006**: All 5 lifecycle acceptance scenarios (`T3.1.1–T3.1.5`) pass with inline test data.
- **SC-007**: Test coverage ≥ 80% overall and ≥ 60% per file across all library domain files.

---

## Assumptions

- The phone's manifest JSON provides all asset URLs pre-computed; the TV stores them as-received without re-resolving paths against a filesystem.
- File existence checks for audio/video assets are performed via a testable `AssetResolver` abstraction (not direct filesystem access), enabling unit test injection.
- `ParsedSong` from feature 001 (usdx-parser) is the intermediate representation used by `SongValidator`; the manifest provides song text which is parsed before validation.
- `timeFromBeat(beat)` is available from the beat-timing engine (feature 002) and is called for `previewStartSec` computation.
- This feature owns the `com.couchraoke.tv.domain.library` package.
- Sorting is case-insensitive using standard Unicode collation; locale-specific sort orders are deferred.

---

## Dependencies

- **Feature 001 (usdx-parser)**: `ParsedSong`, `SongHeader`, `NoteEvent`, `NoteType` — used by `SongValidator`.
- **Feature 002 (beat-timing-engine)**: `BeatTimingEngine.timeFromBeat()` — used for `previewStartSec` computation.
- **Feature 005 (network-protocol-and-session)**: provides manifest fetch HTTP calls and WebSocket disconnect events that drive library lifecycle. This feature defines only the domain model; network wiring is done in feature 005.
