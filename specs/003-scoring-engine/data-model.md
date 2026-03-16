# Data Model — Scoring Engine

## Difficulty
Represents the per-player pitch tolerance setting.

**Values**:
- `EASY` — tolerance 2 semitones (`|delta| <= 2`)
- `MEDIUM` — tolerance 1 semitone (`|delta| <= 1`) — **default for newly assigned singers**
- `HARD` — tolerance 0 semitones (exact match only)

**Relationships**:
- One `ScoringConfig` holds one `Difficulty` per player index
- `Difficulty` is consumed by `isPitchMatch` to gate hit detection

---

## ScoringConfig
Per-session configuration for the scoring engine.

**Fields**:
- `lineBonusEnabled: Boolean` — whether the 1000-point line bonus pool is active
- `maxSongPoints: Int` — derived: `9000` when `lineBonusEnabled = true`, `10000` otherwise
- `maxLineBonusPool: Int` — derived: `1000` when `lineBonusEnabled = true`, `0` otherwise
- `difficulties: Map<Int, Difficulty>` — per-player difficulty, keyed by player index (0-based); defaults to `MEDIUM` for any missing key

**Validation rules**:
- `maxSongPoints` and `maxLineBonusPool` are derived from `lineBonusEnabled` and must not be set independently
- Absent player difficulty keys default to `MEDIUM` (FR-009)

**Relationships**:
- One `ScoringConfig` per game session
- Consumed by `buildProfile` and `isPitchMatch`

---

## TrackScoringProfile
Pre-computed per-track scoring constants derived from the parsed song and session config. Built once before scoring begins.

**Fields**:
- `trackScoreValue: Double` — sum of `durationBeats × ScoreFactor[noteType]` over all scorable notes in the track, filtered to medley bounds if applicable
- `nonEmptyLineCount: Int` — count of lines where `lineScoreValue > 0` (empty lines are excluded from line bonus distribution)
- `lineBonusPerLine: Double` — `maxLineBonusPool / nonEmptyLineCount` using float division; `0.0` when `lineBonusEnabled = false` or `nonEmptyLineCount = 0`
- `medleyStartBeat: Int?` — inclusive lower bound for medley note filtering; null when song is not a medley
- `medleyEndBeat: Int?` — exclusive upper bound for medley note filtering; null when song is not a medley

**Validation rules**:
- `trackScoreValue` must be non-negative; a song with only Freestyle notes yields `trackScoreValue = 0`
- `lineBonusPerLine` must use float division (FR-013: must not be integer-divided before multiplying by `LinePerfection`)

**Relationships**:
- One `TrackScoringProfile` belongs to one track of one parsed song
- Consumed by `evaluateNote` and `evaluateLine`

---

## PitchFrame
A single pitch observation from a phone companion, pre-filtered for lateness by the timing engine.

**Fields**:
- `midiNote: Int` — MIDI note number as transmitted by the phone (0–127)
- `toneValid: Boolean` — whether the companion's pitch detector successfully detected a pitch

**Validation rules**:
- Frames passed to the scoring engine have already passed timing eligibility (lateness ≤ 450 ms and within the note window) — scoring does not re-check timing
- When `toneValid = false`, the frame is always a miss regardless of `midiNote`

**Relationships**:
- Many `PitchFrame` instances may be collected for one `NoteTimingWindow` during the collection window

---

## NoteResult
The outcome of evaluating one finalized note.

**Fields**:
- `noteTimingWindow: NoteTimingWindow` — the timing window that was finalized (from beat timing engine)
- `hits: Int` — number of qualifying frames that were hits
- `n: Int` — number of qualifying frames received (denominator of the hit ratio)
- `noteScore: Double` — computed score for this note; `maxNoteScore × (hits / n)` when `n > 0`, else `0.0`
- `maxNoteScore: Double` — `(maxSongPoints / trackScoreValue) × scoreFactor × durationBeats`; `0.0` when `trackScoreValue = 0`
- `accumulator: ScoreAccumulator` — which player accumulator receives this score

**Relationships**:
- One `NoteResult` is produced per finalized `NoteTimingWindow`
- Consumed by `accumulateNote`

---

## ScoreAccumulator
Enum indicating which player score field a note's score is added to.

**Values**:
- `SCORE` — Normal and Rap notes add to `Player.Score`
- `SCORE_GOLDEN` — Golden and RapGolden notes add to `Player.ScoreGolden`
- `NONE` — Freestyle notes do not accumulate (score is always 0)

---

## PlayerScore
Per-player score accumulator holding running totals.

**Fields**:
- `score: Double` — accumulates Normal and Rap note scores
- `scoreGolden: Double` — accumulates Golden and RapGolden note scores
- `scoreLine: Double` — accumulates line bonus awards
- `scoreLast: Double` — the most recent `noteScore` value (for real-time UI feedback)

**Validation rules**:
- All fields start at `0.0`
- `scoreLast` is overwritten by each note result, not accumulated
- `scoreGolden` never exceeds the golden portion of `maxSongPoints` for a perfect performance

**Relationships**:
- One `PlayerScore` per player per track (duet songs have two independent accumulators)
- Updated by `accumulateNote` after each `NoteResult`
- Used as input to `computeDisplayScores`

---

## DisplayScore
Rounded, display-ready integer scores derived from a `PlayerScore`.

**Fields**:
- `scoreInt: Int` — `round(score / 10) × 10`
- `scoreGoldenInt: Int` — `ceil(scoreGolden / 10) × 10` if `scoreInt < score`, else `floor(scoreGolden / 10) × 10`
- `scoreLineInt: Int` — `floor(round(scoreLine) / 10) × 10`
- `scoreTotalInt: Int` — `scoreInt + scoreGoldenInt + scoreLineInt`; guaranteed ≤ 10000 for a perfect performance

**Validation rules**:
- `scoreTotalInt` must never exceed 10000 (FR-018)
- `scoreLineInt` rounding is intentionally asymmetric from `scoreInt` rounding (FR-017)

**Relationships**:
- Derived from one `PlayerScore` on demand
- Consumed by the UI rendering layer (future singing-screen feature)

---

## LineBonusResult
The outcome of evaluating line bonus at sentence completion.

**Fields**:
- `linePerfection: Double` — `clamp(lineScore / (maxLineScore - 2), 0.0, 1.0)`, or `1.0` when `maxLineScore ≤ 2`
- `lineBonusAwarded: Double` — `lineBonusPerLine × linePerfection`
- `lineScore: Double` — the raw accumulated note score for this line (input echo for traceability)
- `maxLineScore: Double` — the maximum possible note score for this line (input echo)

**Validation rules**:
- When `lineScore = 0` (empty line), no bonus is awarded and the line is excluded from the non-empty line count in `TrackScoringProfile` (FR-014)
- `lineBonusPerLine` must use float division, not integer division, before multiplying by `linePerfection` (FR-013)

**Relationships**:
- One `LineBonusResult` per finalized sentence
- Consumed by the accumulator update for `Player.ScoreLine`

---

## State Transitions

### PlayerScore lifecycle
- `Initial` — all accumulators at `0.0`; assigned when a new player/track begins
- `Accumulating` — updated after each `NoteResult` and each `LineBonusResult`
- `Complete` — song ends; `computeDisplayScores` produces the final `DisplayScore`

### TrackScoringProfile lifecycle
- `Unbuilt` — before `buildProfile` is called
- `Ready` — after `buildProfile` completes; immutable for the duration of the song
