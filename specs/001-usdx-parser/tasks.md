# Tasks: USDX Parser

**Input**: Design documents from `/specs/001-usdx-parser/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Per the TDD mandate in the constitution, test tasks MUST be included and MUST appear before their corresponding implementation tasks. Tests MUST be written to fail before implementation begins.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the Kotlin source/test layout and test/build scaffolding needed for parser work.

- [ ] T001 Create parser source and test directory structure under `app/src/main/kotlin/com/couchraoke/tv/domain/parser/` and `app/src/test/kotlin/com/couchraoke/tv/domain/parser/`
- [ ] T002 Create fixture directory structure under `app/src/test/resources/fixtures/parser/valid/`, `app/src/test/resources/fixtures/parser/invalid/`, `app/src/test/resources/fixtures/parser/duet/`, `app/src/test/resources/fixtures/parser/derived/`, and `app/src/test/resources/fixtures/parser/edge/`, sourcing canonical fixture inputs from `original_spec/fixtures/song_txt_variants/`
- [ ] T003 Update JVM test configuration in `app/build.gradle.kts` for parser-focused unit and acceptance tests
- [ ] T004 Configure JaCoCo coverage reporting and verification in `app/build.gradle.kts`, document how constitution coverage thresholds and exemptions are enforced for the parser feature, and add a `jacocoTestCoverageVerification` task
- [ ] T005 Define branch/worktree cleanup note for feature completion in `specs/001-usdx-parser/quickstart.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared parser models and boundaries that every story depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Write failing shared model tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/ParsedSongModelTest.kt`
- [ ] T007 [P] Implement `DiagnosticEntry` and supporting severity/code types in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/DiagnosticEntry.kt`
- [ ] T008 [P] Implement `CustomTag`, `SongHeader`, and version model in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/SongHeader.kt`
- [ ] T009 [P] Implement `NoteEvent` variants in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/NoteEvent.kt`
- [ ] T010 [P] Implement `Line` and `Track` models in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/Track.kt`
- [ ] T011 [P] Implement `DerivedSongSummary`, `ParsedSong`, `ParseResult`, and `FileResolver` in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/DerivedSongSummary.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/parser/ParsedSong.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/parser/ParseResult.kt`, and `app/src/main/kotlin/com/couchraoke/tv/domain/parser/FileResolver.kt`
- [ ] T012 Implement the production file resolver adapter in `app/src/main/kotlin/com/couchraoke/tv/data/files/LocalFileResolver.kt`
- [ ] T013 Run `./gradlew test` to verify the foundational parser model tests pass

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Accept valid USDX songs (Priority: P1) 🎯 MVP

**Goal**: Parse valid single-track and duet USDX song files into a complete structured song model.

**Independent Test**: Supply valid minimal, metadata-rich, and duet TXT fixtures and verify the parser returns a valid `ParsedSong` with the expected header, tracks, lines, note order, and optional metadata fields.

### Tests for User Story 1 *(write first — must FAIL before implementation)*

- [ ] T014 [P] [US1] Write failing header parsing tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/HeaderParserTest.kt`
- [ ] T015 [P] [US1] Write failing body parsing and duet-routing tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/BodyParserTest.kt`
- [ ] T016 [P] [US1] Add valid single-track and duet acceptance fixtures under `app/src/test/resources/fixtures/parser/valid/` and `app/src/test/resources/fixtures/parser/duet/`
- [ ] T017 [P] [US1] Write failing valid-song acceptance tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/UsdxParserAcceptanceTest.kt`

### Implementation for User Story 1

- [ ] T018 [US1] Implement known header tag parsing in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/HeaderParser.kt`
- [ ] T019 [US1] Implement note, sentence, and duet body parsing in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/BodyParser.kt`
- [ ] T020 [US1] Implement parser orchestration for valid songs in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/UsdxParser.kt`
- [ ] T021 [US1] Implement post-parse line creation and cleanup rules for valid songs in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/PostParseValidator.kt`
- [ ] T022 [US1] Run `./gradlew test --tests "com.couchraoke.tv.domain.parser.HeaderParserTest" --tests "com.couchraoke.tv.domain.parser.BodyParserTest" --tests "com.couchraoke.tv.domain.parser.UsdxParserAcceptanceTest"` to verify User Story 1 passes

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Reject invalid songs with actionable diagnostics (Priority: P2)

**Goal**: Reject malformed or unsupported songs with stable invalidation reasons and line-aware diagnostics.

**Independent Test**: Supply invalid fixtures for missing required headers, missing required audio files, malformed body numbers, unsupported variable BPM, unsupported relative format, invalid duet markers, and empty-track cleanup failure, then verify invalid result state and expected diagnostics.

### Tests for User Story 2 *(write first — must FAIL before implementation)*

- [ ] T023 [P] [US2] Add invalid-song acceptance fixtures under `app/src/test/resources/fixtures/parser/invalid/`, including missing required headers, missing required audio files, malformed body numbers, unsupported variable BPM, unsupported relative format, invalid duet markers, and empty-track cleanup failure
- [ ] T024 [P] [US2] Write failing invalid-song acceptance tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/UsdxParserAcceptanceTest.kt`
- [ ] T025 [P] [US2] Write failing post-parse validation tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/PostParseValidatorTest.kt`

### Implementation for User Story 2

- [ ] T026 [US2] Implement required-header and required-audio validation in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/HeaderParser.kt`
- [ ] T027 [US2] Implement fatal body error handling and invalid duet marker handling in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/BodyParser.kt`
- [ ] T028 [US2] Implement invalidation code mapping and diagnostic accumulation in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/DiagnosticEntry.kt` and `app/src/main/kotlin/com/couchraoke/tv/domain/parser/UsdxParser.kt`
- [ ] T029 [US2] Implement empty-line cleanup, no-notes validation, and final validity decision in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/PostParseValidator.kt`
- [ ] T030 [US2] Run `./gradlew test --tests "com.couchraoke.tv.domain.parser.PostParseValidatorTest" --tests "com.couchraoke.tv.domain.parser.UsdxParserAcceptanceTest"` to verify User Story 2 passes

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Preserve derived metadata for downstream features (Priority: P3)

**Goal**: Produce parser-owned derived metadata and preserved custom-tag information so later features can consume song facts without reinterpreting TXT semantics.

**Independent Test**: Supply fixtures covering rap notes, preview fallback, medley metadata, optional asset presence, duplicate known tags, unknown tags, empty-value tags, no-separator header lines, zero-duration notes, and external video references that must not count as local optional video assets, then verify the derived summary and diagnostics exactly match expectations.

### Tests for User Story 3 *(write first — must FAIL before implementation)*

- [ ] T031 [P] [US3] Add derived-metadata and edge-case fixtures under `app/src/test/resources/fixtures/parser/derived/` and `app/src/test/resources/fixtures/parser/edge/`, including external video reference cases that must not count as local optional video assets
- [ ] T032 [P] [US3] Write failing derived-metadata acceptance tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/UsdxParserAcceptanceTest.kt`
- [ ] T033 [P] [US3] Write failing custom-tag and zero-duration note unit tests in `app/src/test/kotlin/com/couchraoke/tv/domain/parser/HeaderParserTest.kt` and `app/src/test/kotlin/com/couchraoke/tv/domain/parser/BodyParserTest.kt`

### Implementation for User Story 3

- [ ] T034 [US3] Implement unknown-tag preservation, duplicate-tag last-wins behavior, and version handling in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/HeaderParser.kt`
- [ ] T035 [US3] Implement zero-duration freestyle conversion and non-fatal unknown-token handling in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/BodyParser.kt`
- [ ] T036 [US3] Implement derived summary computation for duet, rap, preview, medley, and optional assets in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/PostParseValidator.kt`
- [ ] T037 [US3] Finalize parser output contract assembly in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/UsdxParser.kt`
- [ ] T038 [US3] Run `./gradlew test --tests "com.couchraoke.tv.domain.parser.HeaderParserTest" --tests "com.couchraoke.tv.domain.parser.BodyParserTest" --tests "com.couchraoke.tv.domain.parser.UsdxParserAcceptanceTest"` to verify User Story 3 passes

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finish shared verification, documentation alignment, and cross-story cleanup.

- [ ] T039 [P] Document parser fixture coverage and execution notes in `specs/001-usdx-parser/quickstart.md`
- [ ] T040 Run full verification with `./gradlew test`, `./gradlew lint`, and `./gradlew jacocoTestCoverageVerification`
- [ ] T041 [P] Review parser files for naming, formatting, and constitution compliance in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/` and `app/src/test/kotlin/com/couchraoke/tv/domain/parser/`
- [ ] T042 Delete the merged feature branch/worktree after integration per branch hygiene

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational completion
- **User Story 2 (Phase 4)**: Depends on User Story 1 parser skeleton and foundational models
- **User Story 3 (Phase 5)**: Depends on User Story 1 and User Story 2 core parser flow
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - establishes the valid parsing path and parser structure
- **User Story 2 (P2)**: Depends on User Story 1 - extends the parser to reject invalid files with structured diagnostics
- **User Story 3 (P3)**: Depends on User Stories 1 and 2 - completes parser-owned derived metadata and edge-case behavior

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD — constitution mandate)
- Fixture creation precedes acceptance-test implementation
- Header/body/post-parse implementation follows failing tests
- Story verification commands run before moving to the next story

### Parallel Opportunities

- T002, T003, and T004 can proceed in parallel after T001
- T006, T007, T008, T009, T010, and T011 can proceed in parallel after Phase 1 setup tasks are complete
- In User Story 1, T014, T015, and T016 can proceed in parallel before T017
- In User Story 2, T023, T024, and T025 can proceed in parallel
- In User Story 3, T031, T032, and T033 can proceed in parallel
- T039 and T041 can proceed in parallel after implementation is complete

---

## Parallel Example: User Story 1

```bash
# Launch User Story 1 test preparation together:
Task: "Write failing header parsing tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/HeaderParserTest.kt"
Task: "Write failing body parsing and duet-routing tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/BodyParserTest.kt"
Task: "Add valid single-track and duet acceptance fixtures under app/src/test/resources/fixtures/parser/valid/ and app/src/test/resources/fixtures/parser/duet/"
```

---

## Parallel Example: User Story 2

```bash
# Launch invalid-story test inputs together:
Task: "Add invalid-song acceptance fixtures under app/src/test/resources/fixtures/parser/invalid/"
Task: "Write failing invalid-song acceptance tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/UsdxParserAcceptanceTest.kt"
Task: "Write failing post-parse validation tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/PostParseValidatorTest.kt"
```

---

## Parallel Example: User Story 3

```bash
# Launch derived-metadata test prep together:
Task: "Add derived-metadata and edge-case fixtures under app/src/test/resources/fixtures/parser/derived/ and app/src/test/resources/fixtures/parser/edge/"
Task: "Write failing derived-metadata acceptance tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/UsdxParserAcceptanceTest.kt"
Task: "Write failing custom-tag and zero-duration note unit tests in app/src/test/kotlin/com/couchraoke/tv/domain/parser/HeaderParserTest.kt and app/src/test/kotlin/com/couchraoke/tv/domain/parser/BodyParserTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run the User Story 1 tests independently
5. Demo/use the parser for valid-song consumption before expanding scope

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → MVP parser for valid songs
3. Add User Story 2 → Test independently → invalid-song rejection and diagnostics
4. Add User Story 3 → Test independently → derived metadata and edge-case completion
5. Run full verification and remove the merged feature branch/worktree after integration

### Parallel Team Strategy

With multiple developers:

1. One developer completes Setup + Foundational
2. Once Foundation is done:
   - Developer A: User Story 1 valid parsing flow
   - Developer B: User Story 2 invalidation fixtures/tests (after parser skeleton is available)
   - Developer C: User Story 3 derived-metadata fixtures/tests (after parser skeleton is available)
3. Integrate by story in priority order and verify each story independently
4. Remove the merged feature branch/worktree once the work is integrated

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to a specific user story for traceability
- Each user story remains independently testable at its checkpoint
- All tasks include exact file paths and executable verification points
- TDD ordering is preserved: failing tests before implementation
- Suggested MVP scope is **User Story 1 only**
