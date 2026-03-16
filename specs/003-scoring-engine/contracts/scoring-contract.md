# Scoring Contract — Pitch Frames to Score Outputs

## Purpose
Define the internal domain contract used by later singing-screen and session-summary features to evaluate note hits, compute running scores, award line bonuses, and produce rounded display integers from valid finalized note windows and eligible pitch frames.

## Input Contract
The scoring engine consumes:
- a valid `ParsedSong` and `ScoringConfig` to build a `TrackScoringProfile` once per song
- finalized `NoteTimingWindow` instances from the beat timing engine (one per note, emitted after `finalizationTvMs`)
- the corresponding `NoteEvent` from the parsed track (for `noteType`, `tone`, `durationBeats`)
- a list of `PitchFrame` values already filtered for timing eligibility (lateness ≤ 450 ms, within note window) by the beat timing engine
- a `PlayerScore` accumulator to update after each note and line

Out-of-scope inputs:
- Raw USDX song parsing (handled by the parser feature)
- Beat timing and note window derivation (handled by the timing engine)
- Lateness filtering of pitch frames (handled by the timing engine before scoring)
- UI rendering or reactive state management (deferred to singing-screen feature)
- Adaptive difficulty adjustment

## Output Contract
The scoring engine returns:
- a `TrackScoringProfile` (built once per song) containing `trackScoreValue`, `nonEmptyLineCount`, and `lineBonusPerLine`
- a `NoteResult` per evaluated note containing `hits`, `n`, `noteScore`, `maxNoteScore`, and `accumulator`
- an updated `PlayerScore` after each `accumulateNote` call
- a `LineBonusResult` per finalized sentence containing `linePerfection` and `lineBonusAwarded`
- a `DisplayScore` containing the four rounded integer display values

## buildProfile Contract
For any valid `ParsedSong` and `ScoringConfig`, the engine must:
- sum `durationBeats × ScoreFactor[noteType]` over all notes in the specified track
- filter notes to `[medleyStartBeat, medleyEndBeat)` when the song header includes medley bounds
- count non-empty lines (those where `lineScoreValue > 0`) for line bonus distribution
- compute `lineBonusPerLine` using float division of `maxLineBonusPool / nonEmptyLineCount`; yield `0.0` when `nonEmptyLineCount = 0` or `lineBonusEnabled = false`

## isPitchMatch Contract
For any pitch frame and note, the engine must:
- derive `tone = midiNote - 36`
- apply octave normalization: while `abs(tone - targetTone) > 6`, shift `tone` by ±12 toward `targetTone`
- for NORMAL and GOLDEN notes: return `abs(normalizedTone - targetTone) <= toleranceForDifficulty`
- for RAP and RAP_GOLDEN notes: return `toneValid == true` (pitch distance is not evaluated)
- for FREESTYLE notes: always return `false` (score factor is 0; frame evaluation is skipped by the caller)
- when `toneValid = false` for any note type: return `false`

## evaluateNote Contract
For any finalized `NoteTimingWindow`, `NoteEvent`, and frame list, the engine must:
- treat all frames in the provided list as pre-filtered eligible frames (no lateness re-check)
- set `n` to the count of frames in the list
- set `hits` to the count of frames for which `isPitchMatch` returns `true`
- set `noteScore = maxNoteScore × (hits / n)` using IEEE 754 double-precision division when `n > 0`
- set `noteScore = 0.0` when `n = 0`
- set `noteScore = 0.0` for FREESTYLE notes regardless of frame content
- set `accumulator = SCORE` for NORMAL and RAP; `SCORE_GOLDEN` for GOLDEN and RAP_GOLDEN; `NONE` for FREESTYLE

## accumulateNote Contract
For any `PlayerScore` and `NoteResult`, the engine must:
- add `noteScore` to `playerScore.score` when `accumulator = SCORE`
- add `noteScore` to `playerScore.scoreGolden` when `accumulator = SCORE_GOLDEN`
- set `playerScore.scoreLast = noteScore` unconditionally
- leave `playerScore.scoreLine` unchanged

## evaluateLine Contract
For any line finalization, the engine must:
- compute `linePerfection = clamp(lineScore / (maxLineScore - 2), 0.0, 1.0)` when `maxLineScore > 2`
- set `linePerfection = 1.0` when `maxLineScore ≤ 2` (forgiveness rule)
- compute `lineBonusAwarded = lineBonusPerLine × linePerfection`
- return `lineBonusAwarded = 0.0` for empty lines (caller must pass `lineScore = 0` for empty lines)

## computeDisplayScores Contract
For any `PlayerScore`, the engine must:
- compute `scoreInt = round(score / 10) × 10`
- compute `scoreGoldenInt = ceil(scoreGolden / 10) × 10` if `scoreInt < score`, else `floor(scoreGolden / 10) × 10`
- compute `scoreLineInt = floor(round(scoreLine) / 10) × 10`
- compute `scoreTotalInt = scoreInt + scoreGoldenInt + scoreLineInt`
- guarantee `scoreTotalInt ≤ 10000` for a perfect performance with any valid `ScoringConfig`

## Fixture Contract
Acceptance fixtures for this feature must be able to assert:
- per-note score from perfect, partial, and zero-frame inputs for each note type
- hit and miss outcomes for all difficulty levels and all note types
- octave normalization across single- and multi-octave distance scenarios
- the boundary case where `abs(tone - target) = 6` (within tolerance)
- line perfection values including the forgiveness rule and the `lineScore = 0` empty-line case
- all four rounding formula outputs including the intentional `ScoreLineInt` asymmetry
- that a perfect performance with `LineBonusEnabled = ON` produces exactly `ScoreTotalInt = 10000`
