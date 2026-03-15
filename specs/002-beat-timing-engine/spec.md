# Feature Specification: Beat Timing Engine

**Feature Branch**: `002-beat-timing-engine`
**Created**: 2026-03-14
**Status**: Draft
**Input**: User description: "beat-timing-engine"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Keep the singing cursor in sync (Priority: P1)

As a singer watching the TV, I need the current song position converted into the correct beat position so that lyric highlighting and beat progress stay visibly aligned with the music.

**Why this priority**: The singing experience breaks immediately if the displayed beat drifts from the song. This is the minimum viable outcome that downstream singing and scoring depend on.

**Independent Test**: Can be fully tested by supplying a valid parsed song with fixed BPM, gap, and optional start timing, then checking that known playback positions produce the expected current beat and remain stable across repeated runs. Fixture `18_beat_timing_basic` (`app/src/test/resources/fixtures/parser/derived/18_beat_timing_basic`) provides authoritative expected beat cursors for this story.

**Acceptance Scenarios**:

1. **Given** a valid parsed song with a fixed BPM and gap value, **When** playback reaches a known song position, **Then** the system returns the expected current beat for UI highlighting.
2. **Given** playback time is still before the chart origin because the song gap has not elapsed, **When** the current beat is requested, **Then** the system returns a deterministic pre-roll beat position instead of snapping to the first playable beat.
3. **Given** a song defines a non-zero start offset, **When** singing begins from that offset, **Then** the timing outputs are aligned to the same beat position the singer sees and hears from the first frame.

---

### User Story 2 - Define note timing windows for judgement (Priority: P2)

As a downstream scoring consumer, I need each note translated from authored beat positions into an unambiguous time window so that incoming pitch frames can later be matched to the correct note.

**Why this priority**: Accurate note boundaries are required before any scoring feature can decide which frames belong to which note.

**Independent Test**: Can be fully tested by supplying parsed notes with known start beats and durations, then verifying the produced note windows, timestamp inclusion rules, explicit rejection of frames with `latenessMs > 450`, and note finalization timing against expected outcomes. Fixture `20_beat_timing_boundary_case` (`app/src/test/resources/fixtures/parser/edge/20_beat_timing_boundary_case`) provides authoritative expectations for boundary membership and late-frame dropping.

**Acceptance Scenarios**:

1. **Given** a parsed note with a start beat and duration, **When** its timing window is derived, **Then** the system produces one start boundary, one end boundary, and one finalization point for that note.
2. **Given** a pitch-frame timestamp lands exactly on a note boundary, **When** note membership is evaluated, **Then** the timestamp is included at the start boundary and excluded at the end boundary.
3. **Given** a note has ended but the late-arrival allowance has not yet elapsed, **When** note readiness is checked, **Then** the note is not yet finalized for downstream judgement.\n4. **Given** a pitch frame arrives where `latenessMs > 450`, **When** note membership is evaluated, **Then** the frame is explicitly dropped and rejected from scoring regardless of finalization state.

---

### User Story 3 - Apply calibration and playback bounds consistently (Priority: P3)

As a host calibrating the karaoke session, I need manual microphone delay and song playback bounds applied consistently so that note collection matches real singing latency and songs start and end predictably.

**Why this priority**: Calibration and playback bounds affect timing quality for every song, but they build on the core beat and note-window behavior above.

**Independent Test**: Can be fully tested by varying manual mic delay, optional song start offset, and optional song end boundary, then confirming that timing outputs shift or stop exactly as specified without changing the authored beat structure.

**Acceptance Scenarios**:

1. **Given** a manual mic delay value within the supported range, **When** note windows are derived, **Then** each note window shifts later by that amount while its authored beat positions remain unchanged.
2. **Given** manual mic delay is unset, **When** note windows are derived, **Then** the system uses the default calibration value.
3. **Given** a song defines an explicit end boundary, **When** playback reaches that boundary, **Then** the timing engine reports the song as ended at that point instead of waiting for the full media length.

---

### Edge Cases

- Song playback is still in the pre-roll gap, producing negative chart-relative time before the first playable beat.
- A note has zero duration, producing a zero-width timing window that never remains active past its start boundary.
- Manual microphone delay is set to either boundary value, 0 milliseconds or 400 milliseconds.
- A pitch frame arrives more than the allowed lateness window after its TV timestamp.
- A song omits an explicit end boundary, so timing must continue until the available media duration ends.
- A song starts from a non-zero start offset and still must apply gap and microphone delay consistently.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST treat parsed song timing metadata and parsed note beat positions as the authoritative source for all beat and note-window calculations.
- **FR-002**: The system MUST convert song playback time into a deterministic chart-relative beat position for any valid fixed-BPM song accepted by the parser.
- **FR-003**: The system MUST provide a current beat output for singing-screen highlighting that accounts for the song gap before determining the visible beat.
- **FR-004**: The system MUST preserve repeatable round-trip behavior between beat-to-time and time-to-beat conversion for the same song inputs.
- **FR-005**: The system MUST support negative chart-relative time before the first playable beat without clamping it to zero.
- **FR-006**: The system MUST derive a timing window for every parsed note using the song start time, note start beat, note duration, song gap, and effective microphone delay.
- **FR-007**: The system MUST treat note timing windows as start-inclusive and end-exclusive when deciding whether a timestamp belongs to a note.
- **FR-008**: The system MUST delay note finalization long enough to include eligible late-arriving pitch frames within the allowed lateness window.
- **FR-009**: The system MUST treat pitch frames that arrive more than 450 milliseconds after their TV timestamp as too late for note timing eligibility.
- **FR-010**: The system MUST apply manual microphone delay as a positive offset to note timing windows and use a default value of 0 milliseconds when no calibration is set.
- **FR-011**: The system MUST accept manual microphone delay values only in the inclusive range 0-400 milliseconds.
- **FR-012**: The system MUST initialize song timing from the optional song start offset when one is provided and from the beginning of the song when it is not.
- **FR-013**: The system MUST treat the optional song end boundary as authoritative when provided and otherwise defer song end timing to the available media duration.
- **FR-014**: The system MUST keep authored beat definitions unchanged when applying playback bounds or microphone delay.

### Assumptions

- This feature consumes valid parsed-song data produced by the USDX parser and does not parse raw song files.
- Only songs with a single authoritative BPM are in scope because variable-BPM songs are rejected earlier.
- This feature uses two authoritative timebases: `lyricsTimeSec`, the audio/playback clock in seconds, and `songStartTvMs`, the TV monotonic timestamp captured when audio position 0 begins. All beat cursors, note timing windows, and timing eligibility decisions are derived from those values.
- The highlight beat cursor is a UI projection of playback time onto the beat grid. It derives `highlightTimeSec = lyricsTimeSec - gapMs / 1000.0` and then applies floor semantics to the converted beat position to produce the visible current beat.
- Scoring does not use a separate running beat timer. Instead, each note's authored beat window is projected into absolute TV monotonic time using `songStartTvMs`, `BPM_file`, `gapMs`, and effective `micDelayMs` to derive `noteStartTvMs` and `noteEndTvMs`.
- For duet songs, there is still one underlying playback timeline. Note timing windows are derived against the mixed playback output rather than separate per-track playback clocks.
- This feature defines beat positions, note timing windows, and timing-related eligibility rules only; it does not calculate pitch matches, note scores, line bonuses, or UI rendering.
- Phone-side pitch capture, network transport, and audio/video playback internals are outside this feature except where their timestamps or configured delays affect timing outputs.

### Key Entities *(include if feature involves data)*

- **Timing Context**: The per-song timing inputs needed to evaluate beats and note windows, including BPM, gap, optional start/end bounds, `lyricsTimeSec`, `songStartTvMs`, and manual microphone delay.
- **Beat Cursor**: The UI-facing current chart beat derived from playback time after applying the song gap and floor semantics for visible lyric highlighting.
- **Note Timing Window**: The derived start, end, and finalization boundaries for one note in TV time, projected from authored beats onto the shared playback timeline.
- **Pitch Frame Timestamp**: An incoming pitch-frame time in TV time, paired with arrival time to determine note eligibility and lateness.
- **Parsed Note**: The authored note input identified by start beat, duration, and note type; the source of truth for note timing windows.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of mandatory beat-timing acceptance fixtures for valid fixed-BPM songs produce the expected current beat at named playback positions, including pre-roll and start-offset cases.
- **SC-002**: 100% of mandatory note-window acceptance fixtures apply the same boundary rule on every run: timestamps at note start are included and timestamps at note end are excluded.
- **SC-003**: 100% of mandatory calibration acceptance fixtures shift note windows by the configured manual microphone delay without changing the authored note beat definitions.
- **SC-004**: 100% of mandatory late-arrival acceptance fixtures treat frames arriving more than 450 milliseconds after their TV timestamp as ineligible for note timing.
- **SC-005**: Repeated timing calculations for the same song inputs, playback positions, and pitch-frame timestamps produce identical beat positions, note windows, and note finalization results across runs.
