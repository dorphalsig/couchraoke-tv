# Beat Timing Contract — ParsedSong to Timing Outputs

## Purpose
Define the internal domain contract used by later singing-screen and scoring features to resolve deterministic beat positions, playback bounds, note timing windows, and late-frame eligibility from valid parsed fixed-BPM songs.

## Input Contract
The timing engine consumes:
- exactly one valid `ParsedSong`
- header timing fields from `SongHeader`, including `bpm`, `gapMs`, `startSec`, and `endMs`
- parsed track structure from `Track`, `Line`, and `NoteEvent`
- runtime playback inputs including `lyricsTimeSec`, `songStartTvMs`, `micDelayMs`, and optional media duration

Out-of-scope inputs:
- raw song TXT parsing
- variable-BPM songs rejected earlier by the parser
- adaptive mic-delay inference
- UI state or Android framework types

## Output Contract
The timing engine returns deterministic timing results containing:
- a beat cursor for requested playback positions
- playback bounds derived from start/end metadata plus media-duration fallback
- one note timing window per parsed note
- note finalization timing for late-frame collection
- per-frame eligibility decisions based on timestamp membership and lateness

## Beat Cursor Contract
For any valid timing request, the engine must:
- subtract `gapMs` from playback time before calculating the visible beat cursor
- preserve negative chart-relative time during pre-roll
- use the fixed-BPM internal-beat conversion rules for beat math
- derive the visible current beat by flooring the internal mid-beat value

## Note Window Contract
For each parsed note, the engine must:
- derive `noteStartTvMs` and `noteEndTvMs` from `songStartTvMs`, authored beats, `bpm`, `gapMs`, and `micDelayMs`
- treat membership as start-inclusive and end-exclusive
- derive `finalizationTvMs` as `noteEndTvMs + 450`
- treat zero-duration notes as zero-width timing windows rather than mutating authored beat data

## Calibration and Bounds Contract
The engine must:
- accept `micDelayMs` only in the inclusive range `0..400`
- default `micDelayMs` to `0` when unset
- initialize playback from `startSec` when present, otherwise from `0.0`
- use positive `endMs` as the song-end boundary and otherwise fall back to media duration

## Fixture Contract
Acceptance fixtures for this feature must be able to assert:
- named playback positions mapping to expected beat cursors
- note-window start, end, and finalization boundaries
- boundary membership at note start and note end
- mic-delay shifts without changing authored beat definitions
- explicit end-boundary handling versus media-duration fallback
- late-frame rejection when `latenessMs > 450`
