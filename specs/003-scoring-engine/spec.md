# Feature Specification: Scoring Engine

**Feature Branch**: `003-scoring-engine`
**Created**: 2026-03-15
**Status**: Draft
**Input**: User description: "scoring engine — §6 of original_spec/tv_couchraoke_spec.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Score a note after it is sung (Priority: P1)

As a singer, I need each note I sing to be evaluated and scored immediately after the note window closes so that my running score reflects my performance in real time.

**Why this priority**: This is the core scoring loop — every other feature depends on per-note score being correctly calculated and accumulated. Nothing else can be validated without it.

**Independent Test**: Can be fully tested by supplying a parsed song with known notes, a set of pitch frames with known hit/miss values, and verifying that each finalized note produces the correct `note_score` and accumulates into the correct score field (`Score` or `ScoreGolden`).

**Acceptance Scenarios**:

1. **Given** a note with `N > 0` qualifying pitch frames and all frames matching the pitch, **When** the note is finalized, **Then** `note_score = max_note_score` (perfect score for that note).
2. **Given** a note with `N > 0` qualifying frames and some frames missing, **When** the note is finalized, **Then** `note_score = max_note_score × (hits / N)` using IEEE 754 double-precision division.
3. **Given** a note with `N = 0` qualifying frames, **When** the note is finalized, **Then** `note_score = 0`.
4. **Given** a Normal or Rap note, **When** its score is accumulated, **Then** `note_score` is added to `Player.Score`.
5. **Given** a Golden or RapGolden note, **When** its score is accumulated, **Then** `note_score` is added to `Player.ScoreGolden`.
6. **Given** a Freestyle note, **When** it is evaluated, **Then** `note_score = 0` regardless of frames received.

---

### User Story 2 - Determine whether a pitch frame is a hit (Priority: P1)

As a singer, I need the system to correctly judge whether each pitch I sing matches the target note so that my score reflects my actual pitch accuracy.

**Why this priority**: Hit detection is the gate for all scoring — wrong hit logic corrupts every downstream calculation.

**Independent Test**: Can be tested independently by supplying pitch frames with known `midiNote` values and target notes with known `toneSemitone` and `noteType`, then verifying hit/miss outcomes per note type and difficulty level.

**Acceptance Scenarios**:

1. **Given** a Normal or Golden note and a pitch frame within the difficulty tolerance range after octave normalization, **When** `isPitchMatch` is evaluated, **Then** the frame is a hit.
2. **Given** a Normal or Golden note and a pitch frame outside the tolerance range, **When** `isPitchMatch` is evaluated, **Then** the frame is a miss.
3. **Given** a Rap or RapGolden note and a frame with `toneValid = true`, **When** `isPitchMatch` is evaluated, **Then** the frame is a hit regardless of pitch distance.
4. **Given** a Rap or RapGolden note and a frame with `toneValid = false`, **When** `isPitchMatch` is evaluated, **Then** the frame is a miss.
5. **Given** a detected pitch more than one octave away from the target, **When** octave normalization runs, **Then** the pitch is shifted by 12 semitones until within 6 semitones of the target before comparison.

---

### User Story 3 - Earn a line bonus for singing a phrase well (Priority: P2)

As a singer, I want to earn bonus points for singing each phrase accurately so that clean, consistent singing is rewarded beyond just individual note hits.

**Why this priority**: Line bonus adds 1000 points to the 10000-point total and affects normalization of note scores. It must be correct for total score to be meaningful, but the per-note loop can be verified first.

**Independent Test**: Can be tested by supplying a song with multiple lines, simulating hit rates per line, and verifying that `Player.ScoreLine` accumulates correct per-line bonus values based on `LinePerfection`.

**Acceptance Scenarios**:

1. **Given** a singer hits every note in a line, **When** the line is finalized, **Then** `LinePerfection = 1.0` and the full `LineBonusPerLine` is awarded.
2. **Given** a line where `MaxLineScore <= 2`, **When** the line is finalized, **Then** `LinePerfection = 1.0` (forgiveness rule).
3. **Given** a line where `LineScoreValue = 0` (empty line), **When** the line is finalized, **Then** no line bonus is awarded for that line and the pool is divided among non-empty lines only.
4. **Given** `LineBonusEnabled = OFF`, **When** scoring runs, **Then** `MaxSongPoints = 10000` and no line bonus is computed.

---

### User Story 4 - See a correctly rounded final score (Priority: P2)

As a singer, I want my final score displayed as a clean rounded number so that it is readable and consistent with the original game.

**Why this priority**: Rounding is a normative parity requirement. Wrong rounding produces a score that diverges from expected values even when underlying math is correct.

**Independent Test**: Can be tested by supplying known floating-point score accumulators and verifying that `ScoreInt`, `ScoreGoldenInt`, `ScoreLineInt`, and `ScoreTotalInt` are produced by the exact rounding formulas, including the intentional asymmetry between line score and note score rounding.

**Acceptance Scenarios**:

1. **Given** known `Player.Score` and `Player.ScoreGolden` values, **When** display rounding runs, **Then** `ScoreInt = round(Score/10) × 10`.
2. **Given** `ScoreInt < Player.Score`, **When** golden rounding runs, **Then** `ScoreGoldenInt = ceil(ScoreGolden/10) × 10` (opposite direction).
3. **Given** `ScoreInt >= Player.Score`, **When** golden rounding runs, **Then** `ScoreGoldenInt = floor(ScoreGolden/10) × 10`.
4. **Given** any valid performance, **When** `ScoreTotalInt` is computed, **Then** it never exceeds 10000.
5. **Given** known `Player.ScoreLine`, **When** line score rounding runs, **Then** `ScoreLineInt = floor(round(ScoreLine) / 10) × 10` (intentionally different from note score rounding).

---

### Edge Cases

- A note receives qualifying frames but all frames have `toneValid = false` — `hits = 0`, `note_score = 0`.
- A song has only Freestyle notes — `ScoreTotalInt = 0`.
- A song has only Rap/RapGolden notes — pitch distance is never evaluated.
- A detected pitch is exactly 6 semitones above or below the target — boundary behavior of the octave normalization loop.
- `LineBonusPerLine` is a non-integer float — must not be integer-divided before multiplying by `LinePerfection`.
- A medley song — `TrackScoreValue` must only sum notes within `[medleyStartBeat, medleyEndBeat)`.
- Rounding asymmetry: `.5` values handled differently for line score vs note score.
- A perfect performance with `LineBonusEnabled = ON` produces exactly `ScoreTotalInt = 10000`.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST evaluate each note independently when it is finalized, computing `note_score` from the ratio of hits to qualifying frames using IEEE 754 double-precision division.
- **FR-002**: The system MUST use `N = 0` handling: if no qualifying frames were received for a note, `note_score = 0`.
- **FR-003**: The system MUST accumulate Normal and Rap note scores into `Player.Score` and Golden and RapGolden scores into `Player.ScoreGolden`.
- **FR-004**: The system MUST treat Freestyle notes as non-scoring (`ScoreFactor = 0`) regardless of frames received.
- **FR-005**: The system MUST apply hit detection per note type: Normal/Golden require pitch within tolerance after octave normalization; Rap/RapGolden require only `toneValid = true`.
- **FR-006**: The system MUST apply octave normalization by shifting the detected semitone value by ±12 until within 6 semitones of the target, without reducing to pitch class first.
- **FR-007**: The system MUST derive the USDX semitone value as `Tone = midiNote − 36` before octave normalization.
- **FR-008**: The system MUST support three difficulty levels — Easy (±2 semitones), Medium (±1 semitone), Hard (±0 semitones) — applied per player.
- **FR-009**: The system MUST default each newly assigned singer to Medium difficulty.
- **FR-010**: The system MUST compute `max_note_score = (MaxSongPoints / TrackScoreValue) × ScoreFactor[noteType] × durationBeats` for each note. When `TrackScoreValue = 0` (e.g., a track containing only Freestyle notes), `max_note_score = 0.0` for all notes.
- **FR-011**: The system MUST evaluate line bonus at sentence completion (when the last scorable note of the sentence has been finalized).
- **FR-012**: The system MUST compute `LinePerfection = clamp(LineScore / (MaxLineScore − 2), 0, 1)` except when `MaxLineScore <= 2`, in which case `LinePerfection = 1`.
- **FR-013**: The system MUST distribute `MaxLineBonusPool` (1000 when `LineBonusEnabled = ON`) evenly across non-empty lines using float division. When `nonEmptyLineCount = 0`, `lineBonusPerLine = 0.0` and no line bonus is awarded.
- **FR-014**: The system MUST skip line bonus for empty lines (`LineScoreValue = 0`) and exclude them from the non-empty line count.
- **FR-015**: The system MUST support `LineBonusEnabled` toggling, with `MaxSongPoints = 9000` when ON and `MaxSongPoints = 10000` when OFF.
- **FR-016**: The system MUST compute `TrackScoreValue` by summing `durationBeats × ScoreFactor` over all notes in the track, filtering to `[medleyStartBeat, medleyEndBeat)` for medley songs. A song is treated as a medley when both `SongHeader.medleyStartBeat` and `SongHeader.medleyEndBeat` are non-null. `SongHeader.calcMedleyEnabled` is a legacy field that has no effect on scoring and MUST be ignored.
- **FR-017**: The system MUST round scores using the normative formulas: `ScoreInt = round(Score/10) × 10`, `ScoreGoldenInt` in the opposite direction to `ScoreInt`, and `ScoreLineInt = floor(round(ScoreLine) / 10) × 10`.
- **FR-018**: The system MUST ensure `ScoreTotalInt = ScoreInt + ScoreGoldenInt + ScoreLineInt` and that this value never exceeds 10000 for a perfect performance.

### Assumptions

- This feature consumes valid note-window outputs from the beat timing engine and `toneValid` + `midiNote` values from pitch frames already filtered for lateness.
- `midiNote` from the phone is an integer MIDI note number; the TV derives `Tone = midiNote − 36`.
- One `Player` per track; duet songs have two independent score accumulators.
- `ScoreFactor` constants are normative: Freestyle=0, Normal=1, Golden=2, Rap=1, RapGolden=2.
- `NOTE_FINALIZATION_DELAY_MS = 450` (from beat timing engine feature).
- Scoring runs independently of UI rendering — score state is exposed reactively.

### Key Entities

- **PlayerScore**: Per-player score accumulator holding `Score`, `ScoreGolden`, `ScoreLine`, `ScoreLast`, and their rounded display integers.
- **NoteResult**: The outcome of evaluating one finalized note — `hits`, `N`, `note_score`, and the note type it accumulated into.
- **LineBonusResult**: The outcome of evaluating one finalized line — `LinePerfection`, `LineBonusPerLine`, and contribution to `Player.ScoreLine`.
- **ScoringConfig**: Per-session configuration including `LineBonusEnabled`, per-player `Difficulty`, and derived `MaxSongPoints`.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of mandatory per-note scoring acceptance fixtures (T6.1.1–T6.1.6) produce results matching expected values, including perfect, partial, and zero-frame cases.
- **SC-002**: 100% of mandatory pitch hit-detection fixtures (T6.2.1–T6.2.3, T6.4.1–T6.4.7) produce correct hit/miss outcomes for all note types and difficulty levels.
- **SC-003**: 100% of mandatory line bonus acceptance fixtures (T6.5.1–T6.5.5) produce `ScoreLineInt` values matching expected outputs, including the forgiveness rule and empty-line exclusion.
- **SC-004**: 100% of mandatory rounding acceptance fixtures (T6.6.1–T6.6.5) produce `ScoreInt`, `ScoreGoldenInt`, `ScoreLineInt`, and `ScoreTotalInt` values matching expected outputs, with the intentional asymmetry preserved.
- **SC-005**: A perfect performance on any fixture song with `LineBonusEnabled = ON` produces exactly `ScoreTotalInt = 10000`.
- **SC-006**: Repeated scoring calculations for the same inputs produce identical results across runs (deterministic).

---

## Clarifications

### Session 2026-03-15

- Q: When `nonEmptyLineCount = 0`, what should `lineBonusPerLine` be to avoid division by zero? → A: `lineBonusPerLine = 0.0` — Freestyle notes are effectively nonexistent for scoring; a track with no scorable lines awards no line bonus.
- Q: When `TrackScoreValue = 0` (all-Freestyle track), how should `max_note_score` be computed to avoid division by zero? → A: `max_note_score = 0.0` for all notes — Freestyle sections are not scored, so the guard returns zero without dividing.
- Q: For medley filtering in `TrackScoreValue` (FR-016), which field gates the filter — `medleyStartBeat/End` bounds, `calcMedleyEnabled`, or both? → A: Filter when both `medleyStartBeat` and `medleyEndBeat` are non-null. `calcMedleyEnabled` is a legacy field from an old spec version — it has no role in scoring and must be ignored entirely.
