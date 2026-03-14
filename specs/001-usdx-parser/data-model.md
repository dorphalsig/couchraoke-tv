# Data Model — USDX Parser

## ParsedSong
- **Purpose**: Authoritative result of parsing one USDX TXT song file.
- **Fields**:
  - `songIdentifier`
  - `isValid`
  - `header`
  - `tracks`
  - `derivedSummary`
  - `diagnostics`
- **Relationships**:
  - Owns one `SongHeader`
  - Owns one or two `Track` records
  - Owns one `DerivedSongSummary`
  - Owns zero or more `DiagnosticEntry` records
- **Validation rules**:
  - Valid only when required header, body, and post-parse cleanup rules all succeed
  - Must always include diagnostics gathered during parsing

## SongHeader
- **Purpose**: Normalized header metadata parsed from TXT header lines.
- **Fields**:
  - `version`
  - `title`
  - `artist`
  - `bpm`
  - `gapMs`
  - `startSec`
  - `endMs`
  - `previewStartSec`
  - `audioReference`
  - `videoReference`
  - `videoGapSec`
  - `coverReference`
  - `backgroundReference`
  - `instrumentalReference`
  - `vocalsReference`
  - `medleyStartBeat`
  - `medleyEndBeat`
  - `calcMedleyEnabled`
  - `p1Name`
  - `p2Name`
  - `customTags`
- **Validation rules**:
  - `title`, `artist`, and `bpm` are required and non-empty/non-zero
  - `version` absent means legacy format
  - malformed or unsupported versions invalidate the song
  - duplicate known tags use last-valid-value-wins behavior
  - unknown tags are preserved in encounter order

## Track
- **Purpose**: One singable lane in the parsed song.
- **Fields**:
  - `trackId`
  - `lines`
- **Relationships**:
  - Owns zero or more `Line` records during parsing
  - Must contain at least one non-empty line after cleanup for the song to remain valid

## Line
- **Purpose**: Ordered phrase container that groups note events for later timing and scoring.
- **Fields**:
  - `startBeat`
  - `notes`
- **Validation rules**:
  - Empty lines are removed during cleanup
  - At least one line must remain in every parsed track after cleanup
  - If note events exist without explicit line breaks, at least one line is still created

## NoteEvent
- **Purpose**: One parsed singing event from the song body.
- **Fields**:
  - `noteType` (`Normal`, `Golden`, `Freestyle`, `Rap`, `RapGolden`)
  - `startBeat`
  - `durationBeats`
  - `tone`
  - `lyricText`
- **Validation rules**:
  - Numeric fields must parse successfully for recognized note tokens
  - Zero-duration notes are converted to freestyle and produce a warning

## CustomTag
- **Purpose**: Preserved unknown or non-standard header content.
- **Fields**:
  - `tagName`
  - `content`
  - `lineNumber`
- **Validation rules**:
  - Encounter order is preserved
  - Empty tag values and no-separator header lines are stored rather than discarded

## DiagnosticEntry
- **Purpose**: Structured parse-time message describing warnings, info, or invalidation.
- **Fields**:
  - `severity`
  - `code`
  - `message`
  - `songIdentifier`
  - `lineNumber`
- **Validation rules**:
  - Invalid songs must include at least one `invalid` diagnostic
  - The song's top-level invalid reason must match one invalid diagnostic code

## DerivedSongSummary
- **Purpose**: Downstream-ready derived metadata produced during single-song parsing.
- **Fields**:
  - `isDuet`
  - `hasRap`
  - `hasVideo`
  - `hasInstrumental`
  - `previewStartSec`
  - `medleySource`
  - `medleyStartBeat`
  - `medleyEndBeat`
  - `calcMedleyEnabled`
- **Validation rules**:
  - Derived during parsing, not deferred to later features
  - Medley eligibility inputs depend on explicit medley tags and non-duet structure
  - Optional asset presence depends on parse-time file existence checks

## State Transitions
1. **Unparsed input** → raw TXT content available
2. **Header parsed** → normalized header values and header diagnostics collected
3. **Body parsed** → note/line/track structures created and body diagnostics collected
4. **Cleanup complete** → empty lines removed, track validity decided
5. **Derived summary computed** → duet, rap, preview, medley, and optional asset metadata finalized
6. **Final result emitted** → `ParsedSong` marked valid or invalid with diagnostics
