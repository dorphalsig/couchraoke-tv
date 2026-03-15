# Tasks: Beat Timing Engine

**Input**: Design documents from `/specs/002-beat-timing-engine/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/beat-timing-contract.md`, `quickstart.md`

**Tests**: Test tasks are included for every user story. Write each story’s tests first and confirm they fail before implementing the corresponding timing code.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no unresolved dependencies)
- **[Story]**: User story label (`[US1]`, `[US2]`, `[US3]`)
- Every task includes the exact file path it changes

## Path Conventions

- Runtime timing code lives under `app/src/main/kotlin/com/couchraoke/tv/domain/timing/`
- JVM timing tests live under `app/src/test/kotlin/com/couchraoke/tv/domain/timing/`
- Parser-compatible acceptance fixtures live under `app/src/test/resources/fixtures/parser/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the timing-engine scaffolding and targeted verification entry points.

- [ ] T001 Create timing package scaffolding in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/` and `app/src/test/kotlin/com/couchraoke/tv/domain/timing/`
- [ ] T002 [P] Create timing fixture directories in `app/src/test/resources/fixtures/parser/derived/18_beat_timing_basic/`, `app/src/test/resources/fixtures/parser/derived/19_beat_timing_gap_and_start/`, and `app/src/test/resources/fixtures/parser/edge/20_beat_timing_boundary_case/`
- [ ] T003 [P] Configure `timingUnitTest`, `timingAcceptanceTest`, and `timingTest` selectors in `app/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared timing models, engine entry points, and test shells that every story depends on.

**⚠️ CRITICAL**: No user story work should begin until this phase is complete.

- [ ] T004 Create failing timing test shells in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt` and `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt`
- [ ] T005 Implement shared immutable timing data types in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingModels.kt`
- [ ] T006 Implement the base `ParsedSong` timing entry points and fixed-BPM conversion helpers in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngine.kt`

**Checkpoint**: Shared timing package, models, and test entry points exist; story work can proceed.

---

## Phase 3: User Story 1 - Keep the singing cursor in sync (Priority: P1) 🎯 MVP

**Goal**: Convert playback time into a deterministic current beat that stays aligned through baseline, pre-roll, and start-offset playback.

**Independent Test**: Supply a valid parsed song with fixed BPM, gap, and optional start timing, then confirm the same playback positions always produce the same current beat and pre-roll beat values.

### Tests for User Story 1 *(write first — must FAIL before implementation)*

- [ ] T007 [P] [US1] Add failing beat-cursor unit tests for baseline conversion, negative pre-roll, and beat round-trip behavior in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt`
- [ ] T008 [P] [US1] Add failing acceptance tests for baseline and gap/start playback positions in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt`

### Implementation for User Story 1

- [ ] T009 [P] [US1] Add baseline and gap/start fixture inputs plus expected cursor snapshots in `app/src/test/resources/fixtures/parser/derived/18_beat_timing_basic/` and `app/src/test/resources/fixtures/parser/derived/19_beat_timing_gap_and_start/`
- [ ] T010 [US1] Finalize `TimingContext`, `PlaybackBounds`, and `BeatCursor` fields for cursor evaluation in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingModels.kt`
- [ ] T011 [US1] Implement beat-cursor APIs, gap-aware highlight time, floored current-beat output, and start-offset initialization in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngine.kt`

**Checkpoint**: User Story 1 should now return stable beat cursors for baseline, pre-roll, and start-offset playback.

---

## Phase 4: User Story 2 - Define note timing windows for judgement (Priority: P2)

**Goal**: Translate parsed notes into deterministic TV-time windows on the shared playback timeline, including membership boundaries, explicit late-frame rejection, and finalization timing.

**Independent Test**: Supply parsed notes with known beats and durations, then confirm the derived start, end, and finalization boundaries, start-inclusive/end-exclusive membership rules, and rejection of frames when `latenessMs > 450`.

### Tests for User Story 2 *(write first — must FAIL before implementation)*

- [ ] T012 [P] [US2] Add failing unit tests for note-window derivation, zero-duration notes, and start-inclusive/end-exclusive membership in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt`
- [ ] T013 [P] [US2] Add failing acceptance tests for note-boundary membership, explicit late-frame rejection when `latenessMs > 450`, and late-finalization behavior in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt`

### Implementation for User Story 2

- [ ] T014 [P] [US2] Add boundary and late-frame fixture expectations in `app/src/test/resources/fixtures/parser/edge/20_beat_timing_boundary_case/`
- [ ] T015 [US2] Add `NoteTimingWindow` and `PitchFrameTiming` outputs to `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingModels.kt`
- [ ] T016 [US2] Implement note-window derivation from the shared playback timeline, start-inclusive/end-exclusive boundary membership checks, frame-eligibility evaluation that drops frames when `latenessMs > 450`, and `finalizationTvMs = noteEndTvMs + 450` in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngine.kt`

**Checkpoint**: User Story 2 should now derive note windows and eligibility decisions deterministically from parsed notes.

---

## Phase 5: User Story 3 - Apply calibration and playback bounds consistently (Priority: P3)

**Goal**: Apply manual microphone delay and playback bounds without mutating authored beat structure.

**Independent Test**: Vary `micDelayMs`, `startSec`, `endMs`, and media duration inputs, then confirm windows shift or stop exactly as specified while authored beats stay unchanged.

### Tests for User Story 3 *(write first — must FAIL before implementation)*

- [ ] T017 [P] [US3] Add failing unit tests for `micDelayMs` default/range validation and playback-end selection in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt`
- [ ] T018 [P] [US3] Add failing acceptance tests for mic-delay shifts and explicit-end versus media-duration fallback in `app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt`

### Implementation for User Story 3

- [ ] T019 [P] [US3] Add calibration and playback-bound expectation data in `app/src/test/resources/fixtures/parser/derived/19_beat_timing_gap_and_start/` and `app/src/test/resources/fixtures/parser/edge/20_beat_timing_boundary_case/`
- [ ] T020 [US3] Finalize `TimingContext` and `PlaybackBounds` validation rules for `micDelayMs`, `startSec`, `endMs`, and media-duration fallback in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingModels.kt`
- [ ] T021 [US3] Implement `micDelayMs` default/range handling, note-window shifting on the shared playback timeline, explicit `endMs` handling, and media-duration fallback in `app/src/main/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngine.kt`

**Checkpoint**: All user stories should now be independently functional, including calibration and playback bounds.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Run feature-wide verification and close the feature branch/worktree after integration.

- [ ] T022 [P] Verify the quickstart command set from `specs/002-beat-timing-engine/quickstart.md` against the timing tasks configured in `app/build.gradle.kts`
- [ ] T023 [P] Run the timing verification and coverage flow referenced by `app/build.gradle.kts` and record any missing acceptance coverage follow-ups in `specs/002-beat-timing-engine/tasks.md`
- [ ] T024 Record feature closure in `specs/002-beat-timing-engine/tasks.md` after renaming the merged worktree/branch `002-beat-timing-engine` to `[✓] 002-beat-timing-engine`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** — no dependencies
- **Phase 2: Foundational** — depends on Phase 1; blocks all story work
- **Phase 3: US1** — depends on Phase 2 only; delivers the MVP beat cursor
- **Phase 4: US2** — depends on Phase 2 and reuses the shared beat/time helpers from Phase 2
- **Phase 5: US3** — depends on Phases 3 and 4 because calibration and bounds extend both cursor and note-window behavior
- **Phase 6: Polish** — depends on all completed story phases

### User Story Dependency Graph

- **US1 (P1)** → independent MVP after Foundational
- **US2 (P2)** → independent after Foundational, but shares engine/model files with US1
- **US3 (P3)** → builds on US1 cursor behavior and US2 note-window behavior

### Within Each User Story

- Write tests first and confirm they fail
- Add or update fixture data before final acceptance verification
- Finalize models before engine behavior in the same story
- Complete the story before moving to polish tasks

### Parallel Opportunities

- `T002` and `T003` can run in parallel during Setup
- `T007` and `T008` can run in parallel for US1 test authoring
- `T012` and `T013` can run in parallel for US2 test authoring
- `T017` and `T018` can run in parallel for US3 test authoring
- Fixture tasks `T009`, `T014`, and `T019` can be delegated independently from source-code tasks once the corresponding tests are defined

---

## Parallel Example: User Story 1

```text
T007 Add failing beat-cursor unit tests in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt
T008 Add failing acceptance tests in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt
```

## Parallel Example: User Story 2

```text
T012 Add failing note-window unit tests in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt
T013 Add failing acceptance tests for note-boundary membership and latenessMs > 450 rejection in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt
```

## Parallel Example: User Story 3

```text
T017 Add failing calibration/bounds unit tests in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineTest.kt
T018 Add failing calibration/bounds acceptance tests in app/src/test/kotlin/com/couchraoke/tv/domain/timing/BeatTimingEngineAcceptanceTest.kt
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Setup
2. Complete Foundational
3. Complete US1
4. Validate the beat cursor independently before starting note-window work

### Incremental Delivery

1. Setup + Foundational
2. Deliver US1 and verify deterministic beat cursor behavior
3. Deliver US2 and verify note-window boundaries plus late-frame handling
4. Deliver US3 and verify mic-delay plus playback-bound consistency
5. Run polish verification and close the merged branch/worktree

### Parallel Team Strategy

1. One developer handles setup/build tasks (`T001`-`T003`)
2. One developer prepares fixture updates (`T009`, `T014`, `T019`)
3. One developer implements model and engine changes in sequence across `BeatTimingModels.kt` and `BeatTimingEngine.kt`
4. Rejoin for final verification and branch/worktree closure

---

## Notes

- `[P]` means the task can be done independently in a different file or directory
- Story labels map every implementation task back to a single user story
- Acceptance fixtures must stay parser-compatible
- Keep timing logic pure Kotlin in the domain layer; do not introduce Android framework types into `app/src/main/kotlin/com/couchraoke/tv/domain/timing/`
- Preserve fixed-BPM-only assumptions established by the parser feature
