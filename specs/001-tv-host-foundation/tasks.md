# Tasks: Phase 0 TV Host Foundation

**Input**: Design documents from `/specs/001-tv-host-foundation/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/foundation-boundaries.md`, `quickstart.md`

**Tests**: This feature requires fixture-based validation and a fresh scoped `:app:testBranch` pass before completion is claimed.

**Self-containment rule**: All implementation details needed for these tasks are already captured in `spec.md`, `research.md`, `data-model.md`, `contracts/foundation-boundaries.md`, and `quickstart.md`. Do not reference `original_spec/tv_app.md` during implementation.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently once its prerequisites are complete.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story label for story-phase tasks only
- Every task below includes the exact file path or file set it changes

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the shared Phase 0 test harness plumbing used by all later stories.

- [X] T001 Create fixture path and manifest helpers in `app/src/test/kotlin/com/couchraoke/tv/fixtures/FixturePaths.kt`
- [X] T002 [P] Create deterministic JSON decode/encode helpers for expected discovery, parsed-song, and score snapshots in `app/src/test/kotlin/com/couchraoke/tv/fixtures/FixtureJson.kt`
- [X] T003 [P] Create shared assertions and builders for diagnostics, parsed songs, and pitch samples in `app/src/test/kotlin/com/couchraoke/tv/fixtures/Phase0Assertions.kt`

**Checkpoint**: Shared fixture infrastructure exists and later tests can read fixture inputs without leaking I/O into domain code.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define the shared pure-domain types and snapshot DTOs that multiple stories depend on.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [X] T004 Create the shared `PlayerId` enum in `app/src/main/kotlin/com/couchraoke/tv/domain/model/PlayerId.kt`
- [X] T005 [P] Create scoring model files in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/model/` (`Difficulty.kt`, `BeatRange.kt`, `ScoringConfig.kt`, `PlayerScore.kt`, `PitchSample.kt`)
- [X] T006 [P] Create parser enum files in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/model/` (`NoteType.kt`, `Severity.kt`)
- [X] T007 [P] Create parser diagnostic/support model files in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/model/` (`CustomHeaderTag.kt`, `DiagnosticEntry.kt`, `SongTiming.kt`)
- [X] T008 Create deterministic fixture snapshot DTOs for discovery, parsed-song, and score outputs in `app/src/test/kotlin/com/couchraoke/tv/fixtures/FixtureSnapshots.kt`

**Checkpoint**: Shared contracts exist for parser, beat-time, scoring, and snapshot assertions.

---

## Phase 3: User Story 1 - Validate USDX Songs Deterministically (Priority: P1) 🎯 MVP

**Goal**: Deliver deterministic USDX parsing, validation, diagnostics, score-value precomputation, and library-facing indexed-song projection for F01-F05.

**Independent Test**: Run the parser and discovery fixture suite against `fixtures/F01_song_discovery_validation_acceptance`, `fixtures/F02_header_parsing_edge_cases`, `fixtures/F03_body_grammar_token_recognition`, `fixtures/F04_duet_parsing_track_routing`, and `fixtures/F05_legacy_relative_mode_semantics`, then compare deterministic outputs and diagnostics to the expected snapshot files.

### Validation for User Story 1

- [X] T009 [P] [US1] Create the recursive discovery fixture test for F01 in `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxDiscoveryFixtureTest.kt`
- [X] T010 [P] [US1] Create the parser fixture test for F02-F05 in `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserFixtureTest.kt`

### Implementation for User Story 1

- [X] T011 [P] [US1] Define the parser entry contract in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/UsdxParser.kt`
- [X] T012 [P] [US1] Define the parse failure contract in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/ParseException.kt`
- [X] T013 [P] [US1] Define the library-facing contracts in `app/src/main/kotlin/com/couchraoke/tv/domain/library/LibraryManager.kt` and `app/src/main/kotlin/com/couchraoke/tv/domain/library/IndexedSong.kt`. `LibraryManager` MUST expose both `val songs: StateFlow<List<IndexedSong>>` and `fun getSong(songId: String): IndexedSong?`. Phase 0 may use a fixture/static `StateFlow`; live refresh is out of scope.
- [X] T014 [P] [US1] Create parser result model files in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/model/` (`ParsedSong.kt`, `SongHeader.kt`, `Track.kt`, `Line.kt`, `NoteEvent.kt`)
- [X] T015 [US1] Implement stable diagnostic code creation and structured warning/invalid diagnostic emission for required invalidations and warnings in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/internal/DiagnosticFactory.kt`
- [X] T016 [US1] Implement deterministic header parsing, version validation, audio precedence, preview-start fallback, duplicate-tag last-write-wins, and custom-tag preservation in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/internal/HeaderParser.kt`
- [X] T017 [US1] Implement body grammar, duet routing, invalid duet marker rejection, unknown-token diagnostics, variable-BPM rejection, RELATIVE body rejection, and zero-duration Freestyle conversion in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/internal/BodyParser.kt`
- [X] T018 [US1] Implement canonical `lineScoreValue` and `trackScoreValue` precomputation in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/internal/ScoreValueCalculator.kt`
- [X] T019 [US1] Implement pure parser orchestration and `Result`/`ParseException` mapping in `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/internal/DefaultUsdxParser.kt`
- [X] T020 [US1] Implement fixture-only recursive discovery and indexed-song projection for F01 in `app/src/test/kotlin/com/couchraoke/tv/fixtures/UsdxDiscoveryHarness.kt`, including the rule that `canMedley` is true only for non-duet songs with valid medley tags.
- [X] T021 [US1] Wire deterministic JSON snapshot comparison and invalid line-number assertions into `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxDiscoveryFixtureTest.kt` and `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserFixtureTest.kt`, including the assertion that a duet song with valid medley tags still yields `isDuet=true`, `canMedley=false`, and `medleySource=null`.

**Checkpoint**: User Story 1 yields deterministic parser, diagnostics, and indexed-song outputs for F01-F05 with no Android/runtime dependencies in the parser core.

---

## Phase 4: User Story 2 - Convert Beat and Time Coordinates Consistently (Priority: P2)

**Goal**: Deliver shared static-BPM beat/time conversion and note-window timing math that keeps lyric highlighting and scoring windows on the same deterministic beat grid.

**Independent Test**: Run the beat-time fixture suite against `fixtures/F06_beat_time_conversion_static_bpm` plus inline boundary and round-trip cases, without phones, UI, playback, or network services.

### Validation for User Story 2

- [X] T022 [P] [US2] Create the F06 beat-time fixture test in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculatorFixtureTest.kt`

### Implementation for User Story 2

- [X] T023 [P] [US2] Implement `timeSecToMidBeatInternal` and `beatInternalToTimeSec` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculator.kt`
- [X] T024 [US2] Implement note-window time helpers that derive start/end TV milliseconds from BPM, GAP, and mic delay in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/BeatWindowCalculator.kt`
- [X] T025 [US2] Add lyric-highlight and scoring-cursor fixture assertions, including the `lyricsTimeSec=5.0`, `GAPms=2000`, `BPM_file=120`, and nonzero `micDelayMs` cases, in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculatorFixtureTest.kt`
- [X] T026 [US2] Add start-inclusive/end-exclusive boundary coverage and `1e-9s` beat↔time round-trip assertions in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculatorFixtureTest.kt`

**Checkpoint**: User Story 2 yields deterministic beat/time math for lyric highlighting, scoring windows, and boundary semantics.

---

## Phase 5: User Story 3 - Score Singing Math Without Runtime Dependencies (Priority: P3)

**Goal**: Deliver deterministic pure-math scoring for F08-F11 and the F03 freestyle-only subcase using fixture-provided pitch samples and parsed charts.

**Independent Test**: Run scoring fixtures `fixtures/F08_scoring_beat_stepping_interval_semantics`, `fixtures/F09_pitch_tolerance_octave_normalization`, `fixtures/F10_rap_scoring_tonevalid_gate`, `fixtures/F11_line_bonus_and_rounding`, plus `fixtures/F03_body_grammar_token_recognition/scoring/freestyle_only`, and compare final score outputs to expected snapshots.

### Validation for User Story 3

- [X] T027 [P] [US3] Create the scoring fixture test for F08-F11 plus F03 `scoring/freestyle_only` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineFixtureTest.kt`; use inline pitch samples for small deterministic cases and fixture files only for timing-sensitive replay data shared across tests
- [X] T028 [P] [US3] Create the inline scoring edge-case test for tolerance thresholds, octave loops, rap gating, and display rounding in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringMathUnitTest.kt`; construct pitch samples inline—no fixture files needed for these unit-level cases

### Implementation for User Story 3

- [X] T029 [P] [US3] Define the Phase 0 scoring contract in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt`
- [X] T030 [US3] Implement qualifying-sample selection and `N == 0` note handling in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/QualifyingSampleSelector.kt`
- [X] T031 [US3] Implement note hit evaluation for Normal, Golden, Rap, RapGolden, and Freestyle notes plus Easy/Medium/Hard difficulty rules in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/NoteHitEvaluator.kt`
- [X] T032 [US3] Implement full-semitone octave normalization without modulo-12 reduction in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/OctaveNormalizer.kt`
- [X] T033 [US3] Implement note-score accumulation and medley-window score-value filtering in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/NoteScoreCalculator.kt`
- [X] T034 [US3] Implement line bonus, the `MaxLineScore <= 2` forgiveness rule, `scoreLast` checkpoints, and display-rounding asymmetry in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/ScoreBonusAndRounding.kt`
- [X] T035 [US3] Implement pure scoring orchestration for `loadChart`, `setSongStart`, `finalizeAll`, and `reset` in `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/internal/Phase0ScoringEngine.kt`, using an explicit implementation/test-harness pitch sample source rather than adding sample loading to the public `ScoringEngine` interface
- [X] T036 [US3] Wire pitch-sample normalization (`midiNote != 255`, `midiNote - 36`) and expected-score snapshot assertions into `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineFixtureTest.kt`

**Checkpoint**: User Story 3 yields deterministic pure scoring math with perfect-performance totals of `10000`, correct rap gating, octave normalization, line bonus, and rounding behavior.

---

## Phase 6: User Story 4 - Establish the Phase 0 Quality Gate (Priority: P4)

**Goal**: Prove the Phase 0 parser, beat-time, and scoring slice is complete, covered, performant enough for the phase budget, and still pure JVM logic.

**Independent Test**: Run the scoped `:app:testBranch` command from `specs/001-tv-host-foundation/quickstart.md` and verify F01-F06 plus F08-F11 pass fresh, parser/scoring math coverage is at least 80%, and the core does not require Android runtime services.

### Validation for User Story 4

- [X] T037 [P] [US4] Add parser regression coverage for missing required audio, malformed required headers, duplicate-known-tag last-write-wins, and optional-tag warnings in `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserRegressionTest.kt`
- [X] T038 [P] [US4] Add scoring regression coverage for perfect `10000` totals, empty-line bonus skip, medley-window filtering, and total-never-exceeds-`10000` in `app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringRegressionTest.kt`
- [X] T039 [US4] Add a pure-JVM warmed 10KB parser performance regression test for the Phase 0 `<50ms` budget in `app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserPerformanceTest.kt`. This test guards gross regressions only; allocation profiling is not part of the Phase 0 gate.
- [X] T040 [US4] Run the scoped `:app:testBranch` gate from repo root against `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/`, `app/src/main/kotlin/com/couchraoke/tv/domain/library/`, `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/`, and `app/src/test/kotlin/com/couchraoke/tv/domain/`

**Authoritative command**:

```bash
timeout 10m ./gradlew :app:testBranch \
  --src com.couchraoke.tv.domain.usdx.UsdxParser \
  --src com.couchraoke.tv.domain.usdx.ParseException \
  --src com.couchraoke.tv.domain.usdx.model.ParsedSong \
  --src com.couchraoke.tv.domain.usdx.model.SongHeader \
  --src com.couchraoke.tv.domain.usdx.model.Track \
  --src com.couchraoke.tv.domain.usdx.model.Line \
  --src com.couchraoke.tv.domain.usdx.model.NoteEvent \
  --src com.couchraoke.tv.domain.library.IndexedSong \
  --src com.couchraoke.tv.domain.library.LibraryManager \
  --src com.couchraoke.tv.domain.scoring.BeatCalculator \
  --src com.couchraoke.tv.domain.scoring.ScoringEngine \
  --src com.couchraoke.tv.domain.scoring.model.BeatRange \
  --src com.couchraoke.tv.domain.scoring.model.ScoringConfig \
  --src com.couchraoke.tv.domain.scoring.model.PlayerScore \
  --src com.couchraoke.tv.domain.scoring.model.PitchSample \
  --test com.couchraoke.tv.domain.usdx.UsdxParserFixtureTest \
  --test com.couchraoke.tv.domain.scoring.BeatCalculatorFixtureTest \
  --test com.couchraoke.tv.domain.scoring.ScoringEngineFixtureTest \
  --test com.couchraoke.tv.domain.scoring.ScoringMathUnitTest
```

**Checkpoint**: The scoped quality gate passes fresh with coverage and performance requirements satisfied.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Tighten deterministic outputs and guard the pure-domain boundaries after all stories are working.

- [ ] T041 [P] Normalize fixture snapshot serialization and ordering across `app/src/test/kotlin/com/couchraoke/tv/fixtures/FixtureJson.kt` and `app/src/test/kotlin/com/couchraoke/tv/fixtures/FixtureSnapshots.kt`
- [ ] T042 Audit `app/src/main/kotlin/com/couchraoke/tv/domain/usdx/` and `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/` to keep the Phase 0 core free of Android, network, playback, and filesystem dependencies

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** → no dependencies
- **Phase 2: Foundational** → depends on Phase 1 and blocks all story work
- **Phase 3: US1** → depends on Phase 2
- **Phase 4: US2** → depends on Phase 2
- **Phase 5: US3** → depends on Phase 2 plus the parser contracts from US1 and the beat-window math from US2
- **Phase 6: US4** → depends on US1, US2, and US3 being implemented
- **Final Phase** → depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Starts immediately after Foundational and is the MVP slice
- **US2 (P2)**: Starts immediately after Foundational and stays independently testable from the parser implementation
- **US3 (P3)**: Depends on shared types from Foundational, parser result contracts from US1, and beat-window math from US2
- **US4 (P4)**: Validates the combined Phase 0 slice after US1-US3 are complete

### Within Each User Story

- Write the story’s tests first and make them fail for the intended reason
- Define contracts/models before the internal implementation files that consume them
- Finish the deterministic fixture assertions before declaring the story complete
- Re-run the relevant fresh scoped validation before moving to the next checkpoint

---

## Parallel Opportunities

- **Setup**: T002 and T003 can run in parallel after T001
- **Foundational**: T005, T006, and T007 can run in parallel after T004; T008 can follow once the fixture helpers exist
- **US1**: T009, T010, T011, T012, T013, and T014 can run in parallel; T015-T019 then proceed in parser-internal order; T020 can proceed in parallel with late parser work once the contracts are stable
- **US2**: T022 and T023 can run in parallel; T024 depends on T023; T025-T026 complete the story afterward
- **US3**: T027, T028, and T029 can run in parallel; T030-T035 proceed in scoring-internal order; T036 closes fixture assertions afterward
- **US4**: T037 and T038 can run in parallel; T039 can start once the parser exists; T040 is last
- **Polish**: T041 can run before T042, or both can be split across different reviewers after the gate is green

---

## Parallel Example: User Story 1

```bash
# Parallelizable starting point for US1
Task: "T009 Create the recursive discovery fixture test in app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxDiscoveryFixtureTest.kt"
Task: "T010 Create the parser fixture test in app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserFixtureTest.kt"
Task: "T011 Define the parser entry contract in app/src/main/kotlin/com/couchraoke/tv/domain/usdx/UsdxParser.kt"
Task: "T012 Define the parse failure contract in app/src/main/kotlin/com/couchraoke/tv/domain/usdx/ParseException.kt"
Task: "T013 Define the library-facing contracts in app/src/main/kotlin/com/couchraoke/tv/domain/library/LibraryManager.kt and app/src/main/kotlin/com/couchraoke/tv/domain/library/IndexedSong.kt; LibraryManager exposes songs StateFlow and getSong lookup"
Task: "T014 Create parser result model files in app/src/main/kotlin/com/couchraoke/tv/domain/usdx/model/"
```

## Parallel Example: User Story 2

```bash
# Parallelizable starting point for US2
Task: "T022 Create the F06 beat-time fixture test in app/src/test/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculatorFixtureTest.kt"
Task: "T023 Implement BeatCalculator in app/src/main/kotlin/com/couchraoke/tv/domain/scoring/BeatCalculator.kt"
```

## Parallel Example: User Story 3

```bash
# Parallelizable starting point for US3
Task: "T027 Create the scoring fixture test in app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngineFixtureTest.kt"
Task: "T028 Create the scoring edge-case test in app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringMathUnitTest.kt"
Task: "T029 Define the scoring contract in app/src/main/kotlin/com/couchraoke/tv/domain/scoring/ScoringEngine.kt"
```

## Parallel Example: User Story 4

```bash
# Parallelizable starting point for US4
Task: "T037 Add parser regression coverage in app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserRegressionTest.kt"
Task: "T038 Add scoring regression coverage in app/src/test/kotlin/com/couchraoke/tv/domain/scoring/ScoringRegressionTest.kt"
Task: "T039 Add the warmed parser performance regression test in app/src/test/kotlin/com/couchraoke/tv/domain/usdx/UsdxParserPerformanceTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. Run the US1 fixture suite fresh and stop once F01-F05 are green
5. Use that parser/index slice as the base for later beat-time and scoring work

### Incremental Delivery

1. Setup + Foundational → shared contracts and fixture plumbing ready
2. Add US1 → validate parser/discovery fixtures
3. Add US2 → validate beat-time fixtures
4. Add US3 → validate scoring fixtures
5. Add US4 → run the full scoped quality gate
6. Finish Polish → recheck deterministic snapshots and pure-domain boundaries

### Parallel Team Strategy

1. One engineer completes Setup + Foundational
2. After Foundational:
   - Engineer A works on US1 parser/discovery
   - Engineer B works on US2 beat-time math
3. When US1 and US2 are stable:
   - Engineer C works on US3 scoring math
4. After US1-US3 are green:
   - Engineer D handles US4 gate hardening and the final validation pass

---

## Notes

- `[P]` tasks change different files and can be split safely
- Keep all Phase 0 core logic under `app/src/main/kotlin/com/couchraoke/tv/domain/`
- Keep all fixture file access under `app/src/test/kotlin/com/couchraoke/tv/fixtures/`
- Do not add Android framework types, sockets, playback engines, or filesystem discovery logic to parser/scoring core files
- Do not add or change dependencies unless the change is first planned and routed through `gradle/libs.versions.toml`
- The authoritative completion gate remains the scoped `:app:testBranch` command above
