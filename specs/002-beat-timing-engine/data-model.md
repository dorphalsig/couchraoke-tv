# Data Model — Beat Timing Engine

## TimingContext
Represents the per-song timing inputs needed to resolve beat positions and note windows.

**Fields**:
- `songIdentifier` — stable song identity from `ParsedSong`
- `bpmFile` — authoritative file BPM from `SongHeader`
- `bpmInternal` — derived internal BPM used for beat math (`bpmFile × 4`)
- `gapMs` — chart gap offset in milliseconds
- `startSec` — optional playback start offset in seconds
- `endMs` — optional playback end boundary in milliseconds
- `songStartTvMs` — TV monotonic timestamp recorded when `lyricsTimeSec = 0` begins
- `micDelayMs` — manual microphone delay setting
- `mediaDurationSec` — runtime media duration fallback when `endMs` is absent

**Validation rules**:
- `bpmFile` must be present and positive
- `micDelayMs` must be between `0` and `400` milliseconds inclusive
- `songStartTvMs` must be set before note windows are resolved
- `mediaDurationSec` is only used when `endMs` is missing or non-positive

**Relationships**:
- One `TimingContext` belongs to one valid `ParsedSong`
- One `TimingContext` can produce many `BeatCursor` samples and many `NoteTimingWindow` instances

## PlaybackBounds
Represents the song-level timing bounds applied during playback.

**Fields**:
- `initialSongTimeSec` — effective playback start time (`startSec` or `0.0`)
- `effectiveSongEndSec` — song end time from positive `endMs`, or media-duration fallback
- `endsFromHeader` — whether the effective end came from song metadata rather than media duration

**Validation rules**:
- `initialSongTimeSec` defaults to `0.0` when `startSec` is absent
- `effectiveSongEndSec` comes from `endMs / 1000.0` only when `endMs > 0`

**Relationships**:
- One `PlaybackBounds` belongs to one `TimingContext`

## BeatCursor
Represents the UI-facing beat position for a specific playback moment.

**Fields**:
- `lyricsTimeSec` — playback clock relative to audio position `0`
- `highlightTimeSec` — chart-relative time after subtracting the gap offset
- `midBeat` — floating-point internal beat position
- `currentBeat` — visible beat cursor derived by flooring `midBeat`

**Validation rules**:
- Negative `highlightTimeSec` is valid during pre-roll
- `currentBeat` must always be derived deterministically from the same inputs

**Relationships**:
- Many `BeatCursor` samples can be produced from one `TimingContext`

## NoteTimingWindow
Represents the derived TV-time window for one parsed note.

**Fields**:
- `trackId` — owning track identifier
- `lineStartBeat` — start beat of the containing line
- `noteType` — note classification from the parsed note
- `startBeat` — authored note start beat
- `durationBeats` — authored note duration in beats
- `noteStartTvMs` — TV monotonic start boundary for pitch collection
- `noteEndTvMs` — TV monotonic end boundary for pitch collection
- `finalizationTvMs` — TV monotonic time when late-arriving frames are no longer considered

**Validation rules**:
- `noteEndTvMs` must be greater than or equal to `noteStartTvMs`
- Membership uses `noteStartTvMs <= frameTimestampTvMs < noteEndTvMs`
- `finalizationTvMs` must equal `noteEndTvMs + 450`
- Zero-duration notes are allowed and produce zero-width windows

**Relationships**:
- Many `NoteTimingWindow` instances belong to one `Track`
- Each `NoteTimingWindow` is derived from exactly one parsed `NoteEvent`

## PitchFrameTiming
Represents the timing data used to decide whether a pitch frame is eligible for a note window.

**Fields**:
- `frameTimestampTvMs` — TV-time timestamp embedded in the incoming frame
- `arrivalTimeTvMs` — TV monotonic timestamp when the frame was received
- `latenessMs` — derived `arrivalTimeTvMs - frameTimestampTvMs`
- `eligibleForCollection` — whether the frame satisfies note-window and lateness rules

**Validation rules**:
- Frames with `latenessMs > 450` are not eligible
- Eligibility depends on both note-window membership and lateness

**Relationships**:
- Many `PitchFrameTiming` samples may be evaluated against one `NoteTimingWindow`

## State Transitions

### Song playback timing
- `PreRoll` → before the chart origin after applying `gapMs`
- `Active` → playback is within the effective song bounds
- `Ended` → playback reached the effective end boundary from `endMs` or media duration

### Note window lifecycle
- `Pending` → current TV time is before `noteStartTvMs`
- `Collecting` → `noteStartTvMs <= now < noteEndTvMs`
- `WaitingForFinalization` → `noteEndTvMs <= now < finalizationTvMs`
- `Finalized` → `now >= finalizationTvMs`
