# Parser Contract — USDX TXT to ParsedSong

## Purpose
Define the contract exposed by the USDX parser to the rest of the application.

## Input Contract
The parser accepts a single song package context containing:
- A song identifier
- The raw TXT content for exactly one song
- Access to file existence checks for assets referenced by that TXT file

## Output Contract
The parser returns exactly one `ParsedSong` result containing:
- Validity state for the song
- Normalized header data
- One or two parsed tracks
- Ordered lines and note events
- Derived downstream-ready song summary fields
- Ordered diagnostics captured during parsing and cleanup

## Behavioral Contract
1. The parser processes one song at a time.
2. The parser is authoritative for single-song parsing, validation, diagnostics, and derived metadata.
3. The parser does not aggregate multiple songs or build the library index.
4. The parser preserves unknown header information instead of discarding it.
5. The parser accumulates diagnostics during parsing rather than stopping at the first problem.
6. The parser rejects songs only when fatal validity rules are violated.
7. The parser performs required asset existence checks needed to determine single-song validity and optional-asset presence.

## Validation Contract
The parser must reject songs when any of these occur:
- Missing required title, artist, BPM, or required audio reference
- Required audio asset missing
- Malformed recognized numeric fields
- Unsupported variable BPM body lines
- Unsupported relative sentence format
- Invalid duet marker
- No remaining non-empty line after cleanup
- Malformed or unsupported song version

## Non-Fatal Behavior Contract
The parser must continue and record diagnostics when any of these occur:
- Unknown header tags
- Unknown body tokens
- Empty-value header tags
- Header-like lines without a separator
- Malformed optional metadata
- Missing optional assets
- Zero-duration notes converted to freestyle

## Derived Metadata Contract
The parser must produce downstream-ready fields for:
- Duet detection
- Rap detection
- Preview start value and fallback behavior
- Medley inputs and medley source
- Optional asset presence flags

## Fixture Contract
Acceptance fixtures for this feature must be able to assert:
- Valid vs invalid outcomes
- Stable invalidation code for invalid songs
- Line number when a failure is line-specific
- Parsed structure of tracks, lines, and notes
- Derived summary values
- Preserved custom tags and diagnostics
