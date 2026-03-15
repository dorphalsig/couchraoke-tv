# Tasks: Scoring Engine

**Input**: Design documents from `/specs/003-scoring-engine/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

**Tests**: Per the TDD mandate in the constitution, test tasks MUST appear before their corresponding implementation tasks. Tests MUST be written to fail before implementation begins.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared state dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact file paths are included in all descriptions

## Path Conventions

- Source: `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/`
- Tests: `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/`
- Fixtures: `app/src/test/resources/fixtures/scoring/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Gradle task wiring and fixture directory scaffolding. No implementation code.

- [X] T001 Add `scoringUnitTest`, `scoringAcceptanceTest`, and `scoringTest` Gradle tasks to `app/build.gradle.kts` — mirror the `timingUnitTest`/`timingAcceptanceTest`/`timingTest` pattern at line 170; use `**/domain/scoring/**/*Test.class` and `**/domain/scoring/**/*AcceptanceTest.class` include filters
- [X] T002 Create fixture subdirectories with empty `README.md` placeholders: `app/src/test/resources/fixtures/scoring/per_note_scoring/README.md`, `pitch_hit_detection/README.md`, `line_bonus/README.md`, `score_rounding/README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: All shared models, constants, interface, and compilation baseline. MUST be complete before any user story work begins.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 Create `ScoringModels.kt` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringModels.kt` — note: `NoteResult` references `NoteTimingWindow` from `com.couchraoke.tv.domain.timing`; add that import. With: `Difficulty` enum (EASY, MEDIUM, HARD); `ScoreAccumulator` enum (SCORE, SCORE_GOLDEN, NONE); `ScoringConfig` data class (lineBonusEnabled: Boolean, maxSongPoints: Int, maxLineBonusPool: Int, difficulties: Map<Int, Difficulty>); `PitchFrame` data class (midiNote: Int, toneValid: Boolean); `TrackScoringProfile` data class (trackScoreValue: Double, nonEmptyLineCount: Int, lineBonusPerLine: Double, medleyStartBeat: Int?, medleyEndBeat: Int?); `NoteResult` data class (noteTimingWindow: NoteTimingWindow, hits: Int, n: Int, noteScore: Double, maxNoteScore: Double, accumulator: ScoreAccumulator); `PlayerScore` data class (score: Double = 0.0, scoreGolden: Double = 0.0, scoreLine: Double = 0.0, scoreLast: Double = 0.0); `DisplayScore` data class (scoreInt: Int, scoreGoldenInt: Int, scoreLineInt: Int, scoreTotalInt: Int); `LineBonusResult` data class (linePerfection: Double, lineBonusAwarded: Double, lineScore: Double, maxLineScore: Double)
- [X] T004 [P] Create `ScoringConstants.kt` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringConstants.kt` with: `val SCORE_FACTOR: Map<NoteType, Int>` mapping FREESTYLE→0, NORMAL→1, GOLDEN→2, RAP→1, RAP_GOLDEN→2; `val DIFFICULTY_TOLERANCE: Map<Difficulty, Int>` mapping EASY→2, MEDIUM→1, HARD→0; `val DEFAULT_DIFFICULTY = Difficulty.MEDIUM` (depends on T003 for Difficulty type)
- [X] T005 [P] Define `ScoringEngine` interface in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt` with six method signatures: `buildProfile(song: ParsedSong, trackIndex: Int, config: ScoringConfig): TrackScoringProfile`; `isPitchMatch(midiNote: Int, toneValid: Boolean, noteType: NoteType, targetTone: Int, difficulty: Difficulty): Boolean`; `evaluateNote(window: NoteTimingWindow, note: NoteEvent, frames: List<PitchFrame>, profile: TrackScoringProfile): NoteResult`; `accumulateNote(current: PlayerScore, result: NoteResult): PlayerScore`; `evaluateLine(lineScore: Double, maxLineScore: Double, profile: TrackScoringProfile): LineBonusResult`; `computeDisplayScores(playerScore: PlayerScore): DisplayScore` (depends on T003)
- [X] T006 Add `DefaultScoringEngine` class to `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt` implementing `ScoringEngine` with all six methods throwing `NotImplementedError("not yet implemented")` (depends on T005)
- [X] T007 Run `./gradlew :app:scoringUnitTest` and confirm the project compiles cleanly with zero test failures (no tests exist yet — empty suite passes)

**Checkpoint**: Foundation ready — user story implementation can now begin.

---

## Phase 3: User Story 2 — Determine whether a pitch frame is a hit (Priority: P1)

**Goal**: Implement and verify `isPitchMatch` — the gate for all scoring. US2 is implemented before US1 because `evaluateNote` calls `isPitchMatch` internally.

**Independent Test**: Supply pitch frames with known `midiNote`/`toneValid` values and target notes with known `tone` and `noteType`; verify hit/miss outcomes for every note type and every difficulty level, including octave normalization scenarios.

### Tests for User Story 2 *(write first — must FAIL before T012)*

- [X] T008 [P] [US2] Write failing unit tests for octave normalization in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: `given pitch 12 semitones above target, when normalizing, then shifts down to within 6`; `given pitch 24 semitones above, when normalizing, then shifts down twice`; `given pitch exactly 6 semitones away (delta=6 after normalization), when normalizing, then no further shift — verify this is a MISS for all difficulties (EASY tolerance=2, so delta=6 > 2 → miss)`; `given pitch 7 semitones away, when normalizing, then shifts by 12 and re-evaluates delta` — tests call `DefaultScoringEngine().isPitchMatch(...)` and must fail until T012
- [X] T009 [P] [US2] Write failing unit tests for `isPitchMatch` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: NORMAL + MEDIUM + within 1 semitone → true; NORMAL + MEDIUM + outside 1 semitone → false; NORMAL + EASY + within 2 semitones → true; NORMAL + EASY + 3 semitones → false; NORMAL + HARD + exact match → true; NORMAL + HARD + 1 semitone off → false; GOLDEN behaves like NORMAL; RAP + toneValid=true → true regardless of pitch; RAP + toneValid=false → false; RAP_GOLDEN + toneValid=true → true; FREESTYLE → always false; any note type + toneValid=false → false; **default difficulty**: construct ScoringConfig with no entry for playerIndex 0 and assert `config.difficulties.getOrDefault(0, DEFAULT_DIFFICULTY) == Difficulty.MEDIUM` — verifies FR-009 default-Medium invariant (caller resolves difficulty before passing to isPitchMatch)
- [X] T010 [P] [US2] Create `app/src/test/resources/fixtures/scoring/pitch_hit_detection/input.json` and `expected.json` covering all US2 acceptance scenarios: each entry has `{noteType, targetTone, midiNote, toneValid, difficulty}`; expected is `{isPitchMatch: boolean}`; must include one case per spec acceptance scenario (S2.1–S2.5) plus the 6-semitone boundary and octave normalization cases
- [X] T011 [US2] Write failing acceptance test `given fixture pitch_hit_detection, when isPitchMatch evaluated for each case, then result matches expected` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineAcceptanceTest.kt` — load input.json and expected.json, assert each result (depends on T010)

### Implementation for User Story 2

- [X] T012 [US2] Implement `isPitchMatch` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: derive `tone = midiNote - 36`; apply iterative octave normalization (`while abs(tone - targetTone) > 6` shift by ±12 toward target); for FREESTYLE return false; for RAP/RAP_GOLDEN return `toneValid`; for NORMAL/GOLDEN check `toneValid && abs(normalizedTone - targetTone) <= DIFFICULTY_TOLERANCE[difficulty]` — note: `isPitchMatch` receives an already-resolved `Difficulty` value; the caller (evaluateNote) resolves `config.difficulties.getOrDefault(playerIndex, DEFAULT_DIFFICULTY)` before calling this method (depends on T004, T008, T009)
- [X] T013 [US2] Run `./gradlew :app:scoringUnitTest` — all US2 unit tests (T008, T009) must pass
- [X] T014 [US2] Run `./gradlew :app:scoringAcceptanceTest` — US2 acceptance test (T011) must pass

**Checkpoint**: `isPitchMatch` fully functional and verified for all note types and difficulty levels.

---

## Phase 4: User Story 1 — Score a note after it is sung (Priority: P1) 🎯 MVP

**Goal**: Implement `buildProfile`, `evaluateNote`, and `accumulateNote` — the core scoring loop. `isPitchMatch` (T012) is already available as a dependency.

**Independent Test**: Supply a `ParsedSong` with known notes, a `ScoringConfig`, and lists of `PitchFrame` values with known hit/miss outcomes; verify `noteScore`, `accumulator`, and `PlayerScore` after accumulation for each spec scenario.

### Tests for User Story 1 *(write first — must FAIL before T020)*

- [X] T015 [P] [US1] Write failing unit tests for `buildProfile` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: single NORMAL note → `trackScoreValue = durationBeats`; mixed note types → correct weighted sum; Freestyle-only song → `trackScoreValue = 0.0`; medley song → only notes within `[medleyStartBeat, medleyEndBeat)` counted; nonEmptyLineCount counts only lines with lineScoreValue > 0; `lineBonusPerLine = maxLineBonusPool / nonEmptyLineCount` (float); nonEmptyLineCount=0 → `lineBonusPerLine = 0.0`; `lineBonusEnabled=false` → `lineBonusPerLine = 0.0`
- [X] T016 [P] [US1] Write failing unit tests for `evaluateNote` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: N>0 all hits → `noteScore = maxNoteScore`; N>0 partial hits → `noteScore = maxNoteScore × (hits/N)` IEEE 754; N=0 → `noteScore = 0.0`; FREESTYLE → `noteScore = 0.0` regardless of frames; NORMAL note → `accumulator = SCORE`; GOLDEN note → `accumulator = SCORE_GOLDEN`; RAP note → `accumulator = SCORE`; RAP_GOLDEN → `accumulator = SCORE_GOLDEN`; trackScoreValue=0 → `maxNoteScore = 0.0`
- [X] T017 [P] [US1] Write failing unit tests for `accumulateNote` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: SCORE accumulator → adds to `score`, leaves `scoreGolden` unchanged; SCORE_GOLDEN → adds to `scoreGolden`, leaves `score` unchanged; NONE → neither field changes; `scoreLast` always updated to `noteScore` — including Freestyle notes where `noteScore=0.0` (overwrites previous value); accumulation is additive (multiple calls accumulate)
- [X] T018 [P] [US1] Create `app/src/test/resources/fixtures/scoring/per_note_scoring/input.json` and `expected.json` covering all US1 acceptance scenarios: each entry has `{noteType, durationBeats, trackScoreValue, maxSongPoints, frames: [{midiNote, toneValid}], targetTone, difficulty}`; expected has `{hits, n, noteScore, accumulator}`; must cover spec scenarios S1.1–S1.6
- [X] T019 [US1] Write failing acceptance test `given fixture per_note_scoring, when evaluateNote called for each scenario, then result matches expected` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineAcceptanceTest.kt` (depends on T018)

### Implementation for User Story 1

- [X] T020 [US1] Implement `buildProfile` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: iterate track notes, apply medley filter when both `SongHeader.medleyStartBeat != null && medleyEndBeat != null` (ignore `calcMedleyEnabled` — legacy field); sum `durationBeats × SCORE_FACTOR[noteType]` for `trackScoreValue`; group notes by line and compute each line's max-possible score (`sum(durationBeats × SCORE_FACTOR)`) — `nonEmptyLineCount` is the count of lines whose max-possible score > 0 (lines containing only Freestyle notes count as empty); compute `lineBonusPerLine = if (nonEmptyLineCount > 0 && config.lineBonusEnabled) config.maxLineBonusPool.toDouble() / nonEmptyLineCount else 0.0`; zero-guard: if `trackScoreValue = 0.0`, leave it at 0.0 (evaluateNote guards against division) (depends on T003, T004, T015)
- [X] T021 [P] [US1] Implement `accumulateNote` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: copy PlayerScore, update `score` or `scoreGolden` per `result.accumulator`, always set `scoreLast = result.noteScore` (depends on T003, T017)
- [X] T022 [US1] Implement `evaluateNote` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: compute `maxNoteScore = (profile.maxSongPoints / profile.trackScoreValue) × SCORE_FACTOR[noteType] × durationBeats` with zero-guard; for FREESTYLE skip isPitchMatch and return 0.0; count `hits` via `isPitchMatch`; compute `noteScore = if (n > 0) maxNoteScore × (hits.toDouble() / n) else 0.0`; set accumulator per NoteType (depends on T012, T020)
- [X] T023 [US1] Run `./gradlew :app:scoringUnitTest` — all US1 unit tests (T015, T016, T017) must pass
- [X] T024 [US1] Run `./gradlew :app:scoringAcceptanceTest` — US1 acceptance test (T019) must pass

**Checkpoint**: Core scoring loop fully functional. MVP deliverable — US1 + US2 independently verified.

---

## Phase 5: User Story 3 — Earn a line bonus for singing a phrase well (Priority: P2)

**Goal**: Implement `evaluateLine` — per-line bonus calculation with perfection formula, forgiveness rule, and empty-line exclusion.

**Independent Test**: Supply known `lineScore` / `maxLineScore` pairs and a `TrackScoringProfile`; verify `linePerfection` and `lineBonusAwarded` for each scenario including the forgiveness rule and the `lineBonusEnabled = OFF` case.

### Tests for User Story 3 *(write first — must FAIL before T028)*

- [X] T025 [P] [US3] Write failing unit tests for `evaluateLine` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: all notes hit → `linePerfection = 1.0`, full `lineBonusPerLine` awarded; `maxLineScore <= 2` → `linePerfection = 1.0` (forgiveness); partial hit rate → `linePerfection = clamp(lineScore / (maxLineScore - 2), 0, 1)`; `lineScore = 0` (empty line) → `lineBonusAwarded = 0.0`; `lineBonusPerLine = 0.0` (lineBonusEnabled=OFF) → `lineBonusAwarded = 0.0`; `lineBonusPerLine` is float-multiplied, not integer-divided before multiply
- [X] T026 [P] [US3] Create `app/src/test/resources/fixtures/scoring/line_bonus/input.json` and `expected.json` covering all US3 acceptance scenarios: each entry has `{lineScore, maxLineScore, lineBonusPerLine, lineBonusEnabled}`; expected has `{linePerfection, lineBonusAwarded}`; must cover spec scenarios S3.1–S3.4 and include one entry with `lineBonusEnabled=true` and one with `lineBonusEnabled=false` to explicitly exercise the FR-015 toggle (when OFF, `lineBonusPerLine=0.0` and `lineBonusAwarded=0.0`)
- [X] T027 [US3] Write failing acceptance test `given fixture line_bonus, when evaluateLine called for each scenario, then result matches expected` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineAcceptanceTest.kt` (depends on T026)

### Implementation for User Story 3

- [X] T028 [US3] Implement `evaluateLine` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: compute `linePerfection = if (maxLineScore <= 2) 1.0 else (lineScore / (maxLineScore - 2)).coerceIn(0.0, 1.0)`; compute `lineBonusAwarded = profile.lineBonusPerLine * linePerfection` (depends on T025)
- [X] T029 [US3] Run `./gradlew :app:scoringUnitTest` — all US3 unit tests (T025) must pass
- [X] T030 [US3] Run `./gradlew :app:scoringAcceptanceTest` — US3 acceptance test (T027) must pass

**Checkpoint**: Line bonus fully functional. US1 + US2 + US3 independently verified.

---

## Phase 6: User Story 4 — See a correctly rounded final score (Priority: P2)

**Goal**: Implement `computeDisplayScores` with all four normative rounding formulas, including the intentional asymmetry between `ScoreLineInt` and `ScoreInt`.

**Independent Test**: Supply known floating-point `PlayerScore` accumulators; verify `ScoreInt`, `ScoreGoldenInt`, `ScoreLineInt`, `ScoreTotalInt` match normative formulas including `.5` boundary cases and the `ScoreGoldenInt` direction switch.

### Tests for User Story 4 *(write first — must FAIL before T034)*

- [X] T031 [P] [US4] Write failing unit tests for `computeDisplayScores` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineTest.kt`: `ScoreInt = round(score/10) × 10` (verify .5 rounds away from zero); `ScoreGoldenInt = ceil(scoreGolden/10) × 10` when `scoreInt < score`; `ScoreGoldenInt = floor(scoreGolden/10) × 10` when `scoreInt >= score`; `ScoreLineInt = floor(round(scoreLine) / 10) × 10` (intentionally different from ScoreInt); `ScoreTotalInt = ScoreInt + ScoreGoldenInt + ScoreLineInt`; perfect performance with `LineBonusEnabled = ON` → `ScoreTotalInt = 10000`; `ScoreTotalInt` never exceeds 10000
- [X] T032 [P] [US4] Create `app/src/test/resources/fixtures/scoring/score_rounding/input.json` and `expected.json` covering all US4 acceptance scenarios: each entry has `{score, scoreGolden, scoreLine}`; expected has `{scoreInt, scoreGoldenInt, scoreLineInt, scoreTotalInt}`; must cover spec scenarios S4.1–S4.5 plus the `.5` asymmetry boundary case
- [X] T033 [US4] Write failing acceptance test `given fixture score_rounding, when computeDisplayScores called for each scenario, then result matches expected` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineAcceptanceTest.kt` (depends on T032)

### Implementation for User Story 4

- [X] T034 [US4] Implement `computeDisplayScores` in `DefaultScoringEngine` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`: `scoreInt = (kotlin.math.round(score / 10) * 10).toInt()`; `scoreGoldenInt = if (scoreInt < score) (kotlin.math.ceil(scoreGolden / 10) * 10).toInt() else (kotlin.math.floor(scoreGolden / 10) * 10).toInt()`; `scoreLineInt = (kotlin.math.floor(kotlin.math.round(scoreLine) / 10.0) * 10).toInt()`; `scoreTotalInt = scoreInt + scoreGoldenInt + scoreLineInt` (depends on T031)
- [X] T035 [US4] Run `./gradlew :app:scoringUnitTest` — all US4 unit tests (T031) must pass
- [X] T036 [US4] Run `./gradlew :app:scoringAcceptanceTest` — US4 acceptance test (T033) must pass

**Checkpoint**: All four user stories fully functional and independently verified.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Full-suite verification, coverage gates, lint, regression check, and branch closure.

- [X] T037 Run `./gradlew :app:scoringTest` — full scoring suite (unit + acceptance) must pass with zero failures
- [X] T038 Run `./gradlew jacocoTestCoverageVerification` — bundle-level line coverage ≥ 80%; per-file line coverage ≥ 60% for all scoring domain files; fix any gaps
- [X] T039 [P] Run `./gradlew lint` — resolve any Detekt/ktlint findings in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/` and `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/`
- [X] T040 Run `./gradlew test` — no regressions in parser or timing tests; full test suite passes
- [ ] T041 After merge to master, rename feature branch from `003-scoring-engine` to `[✓] 003-scoring-engine` per constitution branch hygiene

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **US2 (Phase 3)**: Depends on Phase 2 completion
- **US1 (Phase 4)**: Depends on Phase 2 + US2 (T012: `isPitchMatch` must exist before `evaluateNote`)
- **US3 (Phase 5)**: Depends on Phase 2 completion — independently testable from US1/US2
- **US4 (Phase 6)**: Depends on Phase 2 completion — independently testable from US1/US2/US3
- **Polish (Phase 7)**: Depends on all user story phases

### User Story Dependencies

- **US2 (P1)**: After Foundational — no other story dependencies
- **US1 (P1)**: After Foundational + US2 (T012 is an internal dependency of T022)
- **US3 (P2)**: After Foundational — independent of US1 and US2 at the test level
- **US4 (P2)**: After Foundational — independent of US1, US2, US3 at the test level

### Within Each User Story

- Tests are written first and MUST fail before implementation begins (TDD — constitution mandate)
- Test writing tasks [P] within a phase can run in parallel (different test methods, different fixture files)
- `buildProfile` (T020) before `evaluateNote` (T022) — evaluateNote takes a TrackScoringProfile parameter
- `accumulateNote` (T021) can be implemented in parallel with `buildProfile` (T020)

### Parallel Opportunities

- Within Phase 2: T004 and T005 in parallel after T003
- Within Phase 3: T008, T009, T010 in parallel (test writing)
- Within Phase 4: T015, T016, T017, T018 in parallel (test writing); T020 and T021 in parallel (implementation)
- Within Phase 5: T025 and T026 in parallel (test writing + fixture)
- Within Phase 6: T031 and T032 in parallel (test writing + fixture)
- After Phase 2 complete: US3 and US4 phases can run in parallel with each other

---

## Parallel Example: User Story 1 (Phase 4)

```bash
# Write all US1 tests in parallel:
Task T015: "Unit tests for buildProfile in ScoringEngineTest.kt"
Task T016: "Unit tests for evaluateNote in ScoringEngineTest.kt"
Task T017: "Unit tests for accumulateNote in ScoringEngineTest.kt"
Task T018: "Per-note scoring fixture files in fixtures/scoring/per_note_scoring/"

# Then implement in order:
Task T020: "Implement buildProfile"
Task T021: "Implement accumulateNote" (parallel with T020)
Task T022: "Implement evaluateNote" (after T020 and T012)
```

---

## Implementation Strategy

### MVP First (US2 → US1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US2 (isPitchMatch — gate for all scoring)
4. Complete Phase 4: US1 (note scoring — core loop)
5. **STOP and VALIDATE**: `./gradlew :app:scoringTest` — US1+US2 independently verified
6. Demo: scoring loop works end-to-end for Normal, Golden, Rap, Freestyle notes

### Incremental Delivery

1. Setup + Foundational → compilation baseline
2. US2 → pitch hit detection verified
3. US1 → core scoring loop verified (**MVP**)
4. US3 → line bonus verified
5. US4 → display rounding verified
6. Polish → coverage gates and lint pass
7. After merge: rename branch to `[✓] 003-scoring-engine`

### Parallel Team Strategy

After Phase 2 (Foundational) is complete:

- Thread A: US2 (T008–T014) → US1 (T015–T024)
- Thread B: US3 (T025–T030) independently
- Thread C: US4 (T031–T036) independently
- All threads converge at Phase 7 (Polish)

---

## Notes

- [P] tasks = different files or independent methods, no shared state conflicts
- [Story] label maps each task to its user story for traceability
- Each user story is independently completable and testable
- TDD is mandatory per constitution §V — tests must fail before implementation
- Commit after each logical group or at each Checkpoint
- `calcMedleyEnabled` in `SongHeader` is a legacy field — ignore it entirely (spec clarification 2026-03-15)
- `trackScoreValue = 0` guard: return `maxNoteScore = 0.0` (spec clarification 2026-03-15)
- `nonEmptyLineCount = 0` guard: return `lineBonusPerLine = 0.0` (spec clarification 2026-03-15)
- Mark merged branch closed with `[✓]` prefix as part of done criteria (T041)
