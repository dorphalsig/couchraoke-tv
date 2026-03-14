# Feature Specification: USDX Parser

**Feature Branch**: `001-usdx-parser`
**Created**: 2026-03-14
**Status**: Draft
**Input**: User description: "usdx-parser"

## Clarifications

### Session 2026-03-14

- Q: What should the scope of `usdx-parser` be? → A: Parsing plus single-song validation, diagnostics, and derived song metadata, but not multi-file library aggregation.
- Q: Where should asset existence and resolved song metadata be decided? → A: In `usdx-parser` during single-song parsing, not later in `song-library`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Accept valid USDX songs (Priority: P1)

As a TV host, I need valid UltraStar Deluxe song text files to be parsed into a consistent song model so that the song library, timing engine, and scoring engine can use the same authoritative song data.

**Why this priority**: Without reliable parsing of valid songs, no later feature can load songs, display metadata, calculate timing, or score singing.

**Independent Test**: Can be fully tested by supplying valid single-track and duet song text files and confirming that the parser returns a complete parsed song record with the expected metadata, tracks, lines, notes, and derived flags.

**Acceptance Scenarios**:

1. **Given** a valid single-track song file with required headers and valid note lines, **When** the file is parsed, **Then** the system returns a valid parsed song with one track, at least one line, and all note events in file order.
2. **Given** a valid duet song file that uses `P1` and `P2` markers, **When** the file is parsed, **Then** the system returns a valid parsed song with two tracks and assigns notes and line breaks to the currently active track.
3. **Given** a valid song file with optional metadata such as preview, video, cover, background, medley tags, and duet singer names, **When** the file is parsed, **Then** the parsed song includes those values when present and leaves them absent when not provided.

---

### User Story 2 - Reject invalid songs with actionable diagnostics (Priority: P2)

As a library builder, I need malformed or unsupported song files to be rejected with stable, actionable diagnostics so that invalid songs do not appear playable and users can understand why they were excluded.

**Why this priority**: The library must remain trustworthy. Accepting corrupt songs would break later playback and scoring, while silent rejection would make troubleshooting difficult.

**Independent Test**: Can be fully tested by supplying malformed files that are missing required headers, reference missing required audio, contain invalid numeric fields, unsupported variable BPM changes, invalid duet markers, or no usable notes, then confirming the parser rejects them with the expected reason and line information when applicable.

**Acceptance Scenarios**:

1. **Given** a song file missing a required field such as title, artist, BPM, or required audio reference, **When** the file is parsed, **Then** the system marks the song invalid and records the missing-required-field reason.
2. **Given** a song file with a recognized line whose required numeric fields cannot be parsed, **When** the file is parsed, **Then** the system marks the song invalid and records the failure with the line number.
3. **Given** a song file containing unsupported body constructs such as variable BPM changes or unsupported relative sentence format, **When** the file is parsed, **Then** the system marks the song invalid with the corresponding unsupported-feature reason.

---

### User Story 3 - Preserve derived metadata for downstream features (Priority: P3)

As a downstream feature owner, I need parsing to produce normalized metadata and derived song flags so that later features can build the song library, determine medley eligibility, show song attributes, and compute timing without reinterpreting the raw file.

**Why this priority**: This keeps parsing as the single source of truth and prevents later features from duplicating song-format logic.

**Independent Test**: Can be fully tested by supplying songs with different combinations of media references, rap notes, duet markers, preview cues, and medley tags, then confirming the parsed output includes the correct derived values and defaults.

**Acceptance Scenarios**:

1. **Given** a song with rap notes, duet markers, and explicit medley tags, **When** the file is parsed, **Then** the parsed output correctly identifies rap presence, duet status, and medley eligibility inputs.
2. **Given** a song with no preview start tag but valid medley tags, **When** the file is parsed, **Then** the parsed output provides the medley-based preview start fallback.
3. **Given** a song with unknown header tags, empty-value tags, and malformed optional tags, **When** the file is parsed, **Then** the parser preserves unknown tags in encounter order and continues unless a fatal validity rule is violated.

---

### Edge Cases

- A song omits line break markers but still contains note events.
- A song contains duplicate known header tags; the later valid value must win.
- A song uses optional media tags that point to missing files.
- A song contains an invalid duet marker such as `P3`.
- A song contains a note with zero duration.
- A song contains unknown body tokens that should be ignored without invalidating the song.
- A song has no remaining non-empty lines after cleanup.
- A song declares a version that is malformed or outside the supported range.
- A song contains external video references that should not be treated as local playable video assets.
- A song includes medley tags that are incomplete or logically invalid.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST parse UltraStar Deluxe song text files into a structured parsed-song result that distinguishes song metadata, tracks, lines, notes, and diagnostics.
- **FR-002**: The system MUST require non-empty title, artist, and BPM fields for song validity.
- **FR-003**: The system MUST require a valid primary audio reference according to song format version rules, including preferring `AUDIO` over `MP3` for modern format files and requiring `MP3` for legacy files.
- **FR-004**: The system MUST reject a song when its required audio reference cannot be resolved to an available file.
- **FR-005**: The system MUST support the defined note-body tokens for normal, golden, freestyle, rap, rap-golden, line breaks, end-of-song, and duet track switching.
- **FR-006**: The system MUST route notes and line breaks to the currently active duet track when duet markers are present, and MUST reject any duet marker other than `P1` or `P2`.
- **FR-007**: The system MUST treat unsupported variable-BPM body lines and unsupported relative sentence format as fatal validation failures.
- **FR-008**: The system MUST continue parsing past unknown header tags, unknown body tokens, and malformed optional fields while recording diagnostics, unless a fatal validity rule is violated.
- **FR-009**: The system MUST preserve unknown header tags in encounter order, including tags with empty values and tag-like lines without a separator.
- **FR-010**: The system MUST remove empty lines after parsing and MUST reject any track that has no remaining non-empty line.
- **FR-011**: The system MUST create at least one line container for songs that contain note events but omit explicit line break markers.
- **FR-012**: The system MUST convert zero-duration notes into freestyle notes while preserving a warning diagnostic.
- **FR-013**: The system MUST treat duplicate known header tags using last-valid-value-wins behavior.
- **FR-014**: The system MUST interpret supported timing and media metadata, including gap, start, end, preview start, video, cover, background, instrumental, vocals, medley tags, and duet singer labels.
- **FR-015**: The system MUST derive downstream-ready metadata from the parsed song during single-song parsing, including duet status, rap presence, preview start value, medley eligibility inputs, and presence of optional assets.
- **FR-016**: The system MUST validate the declared song format version, treating absent version as legacy format, rejecting malformed versions, and rejecting unsupported major versions.
- **FR-017**: The system MUST produce a structured diagnostics list for every parse attempt, where each diagnostic includes severity, stable reason, human-readable message, song identifier, and line number when a specific line caused the issue.
- **FR-018**: The system MUST mark the final parse result as valid only when all required header, body, and post-parse cleanup rules succeed.

### Assumptions

- This feature covers single-song parsing, validation, diagnostics, and derived song metadata only; it does not cover multi-song library aggregation, beat-to-time conversion, score computation, networking, UI behavior, or media playback.
- Asset existence checks needed for validity are evaluated using the song package available to the parser at parse time.
- The parsed-song result is the authoritative source for later library, timing, and scoring features.
- Unsupported legacy relative-body timing remains out of scope for this feature and is rejected rather than approximated.

### Key Entities *(include if feature involves data)*

- **Parsed Song**: The complete result of processing one song text file, including validity, structured song data, derived fields, and diagnostics.
- **Song Header**: Normalized song metadata and tag values such as title, artist, version, BPM, timing fields, media references, medley tags, duet labels, and preserved unknown tags.
- **Track**: One singable lane of a song; a normal song has one track and a duet song has two tracks.
- **Line**: An ordered grouping of note events within a track used to preserve song phrasing and later scoring boundaries.
- **Note Event**: A single timed singing event with note type, start beat, duration, tone, and lyric text.
- **Diagnostic Entry**: A structured parse message describing a warning or invalidation reason, with optional line number.
- **Derived Song Summary**: Downstream-ready attributes inferred from parsing, such as duet status, rap presence, preview start value, medley eligibility inputs, and optional-asset presence.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of mandatory parser acceptance fixtures for valid songs produce a valid parsed-song result whose metadata, track structure, and note structure match the expected output.
- **SC-002**: 100% of mandatory parser acceptance fixtures for invalid songs are rejected with the expected stable invalidation reason, and line-specific failures include the expected line number.
- **SC-003**: 100% of mandatory parser acceptance fixtures that exercise derived values produce the expected duet status, rap presence, preview start behavior, medley inputs, and optional-asset presence flags.
- **SC-004**: For duplicate tags, unknown tags, zero-duration notes, missing line breaks, and optional missing assets, the parser behaves deterministically across repeated runs on the same input.
- **SC-005**: A downstream consumer can build the library-facing song summary for a parsed valid song without reparsing raw text or reinterpreting header/body rules.
