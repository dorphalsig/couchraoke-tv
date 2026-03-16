# Tasks: Song Library (Data Layer)

**Input**: Design documents from `/specs/004-song-library/`
**Prerequisites**: plan.md ✓, spec.md ✓, data-model.md ✓, contracts/SongLibrary.md ✓, quickstart.md ✓

**Tests**: TDD mandatory (constitution). Test tasks appear before their corresponding implementation tasks. Tests MUST be written to fail before implementation begins.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1, US2, US3)
- Exact file paths included in all descriptions

---

## Phase 1: Setup

**Purpose**: Gradle task registration and fixture staging

- [ ] T001 Register `libraryUnitTest`, `libraryAcceptanceTest`, and `libraryTest` Gradle tasks in `app/build.gradle.kts` following the existing `scoringUnitTest`/`scoringAcceptanceTest`/`scoringTest` pattern (include `**/domain/library/**/*Test.class` / exclude `*AcceptanceTest.class`)
- [ ] T002 Copy F01 fixture to classpath: copy `original_spec/fixtures/F01_song_discovery_validation_acceptance/songs_root/` and `expected.discovery.json` into `app/src/test/resources/fixtures/library/song_discovery/`

**Checkpoint**: Gradle library tasks registered; fixture files on the test classpath

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Parser extension and core data classes that every user story depends on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 [P] Add `MALFORMED_BODY_FIELD` to `DiagnosticCode` enum in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/DiagnosticEntry.kt` (backward-compatible; existing `MALFORMED_NUMERIC_FIELD` values in `HeaderParser` are unchanged)
- [ ] T004 [P] Update `BodyParser.parse()` to emit `DiagnosticCode.MALFORMED_BODY_FIELD` instead of `MALFORMED_NUMERIC_FIELD` for body-parse fatal errors in `app/src/main/kotlin/com/couchraoke/tv/domain/parser/BodyParser.kt`
- [ ] T005 [P] Create `ManifestEntry` plain data class (fields: `relativeTxtPath`, `modifiedTimeMs`, `txtUrl`, `audioUrl?`, `videoUrl?`, `coverUrl?`, `backgroundUrl?`, `instrumentalUrl?`, `vocalsUrl?`) in `app/src/main/kotlin/com/couchraoke/tv/domain/library/ManifestModels.kt`
- [ ] T006 [P] Create `SongEntry` data class with all fields from data-model.md (identity, validation, display, derived flags, preview/seek metadata, asset URLs; reuse `MedleySource` from `domain.parser`) in `app/src/main/kotlin/com/couchraoke/tv/domain/library/SongEntry.kt`

**Checkpoint**: Foundation ready — all user story phases can now begin in parallel

---

## Phase 3: User Story 1 — Library Lifecycle (Priority: P1) 🎯 MVP

**Goal**: `DefaultSongLibrary` accurately maintains per-phone song collections across add/remove/replace operations and returns a correctly sorted aggregate view.

**Independent Test**: Create an in-memory `DefaultSongLibrary`, call `addPhone` / `removePhone`, and assert size, content, `songId` format, and sort order — no parser or network code required.

### Tests for User Story 1 *(write first — must FAIL before T008)*

- [ ] T007 [US1] Write `SongLibraryTest` covering all 5 lifecycle scenarios T3.1.1–T3.1.5: two phones add 3 songs each → 6 total; phone disconnect removes all 4 songs; re-fetch same phone replaces entries (no duplicates); `songId == phoneClientId + \"::\" + relativeTxtPath`; `getSortedSongs()` returns Artist→Album→Title case-insensitive order — `app/src/test/kotlin/com/couchraoke/tv/domain/library/SongLibraryTest.kt`. Also cover: `getSongById(entry.songId)` returns that entry; `getSongById(\"nonexistent\")` returns null; `addPhone(clientId, emptyList())` removes all prior entries for that phone without error. Also cover: `getSongById(entry.songId)` returns that entry; `getSongById("nonexistent")` returns null; `addPhone(clientId, emptyList())` removes all prior entries for that phone without error. Use backtick `given <context>, when <action>, then <expected outcome>` naming for all test functions.

### Implementation for User Story 1

- [ ] T008 [US1] Create `SongLibrary` interface and `DefaultSongLibrary` implementation (`LinkedHashMap<String, List<SongEntry>>` keyed by `phoneClientId`; `getSortedSongs()` uses `compareBy { Triple(it.artist.orEmpty().lowercase(), it.album.orEmpty().lowercase(), it.title.orEmpty().lowercase()) }`) in `app/src/main/kotlin/com/couchraoke/tv/domain/library/SongLibrary.kt`
- [ ] T009 [US1] Run `./gradlew :app:libraryUnitTest` — confirm `SongLibraryTest` passes; report test count and 0 failures

**Checkpoint**: User Story 1 fully functional and independently testable

---

## Phase 4: User Story 2 — Song Entry Validation (Priority: P2)

**Goal**: `SongIndexer.fromParseResult()` maps `DiagnosticCode` errors to stable `invalidReasonCode` strings; `SongDiscovery.discoverFromDirectory()` discovers and indexes all `.txt` files in a directory tree; F01 acceptance fixture passes.

**Independent Test**: Call `SongIndexer.fromParseResult()` with fixture-backed `ParseResult` objects and assert `isValid`, `invalidReasonCode`, and `invalidLineNumber`. Then run `SongDiscoveryAcceptanceTest` against F01 fixture.

### Tests for User Story 2 *(write first — must FAIL before T012)*

- [ ] T010 [P] [US2] Write `SongValidationTest` covering unit validation scenarios T3.2.1–T3.2.8: missing `#ARTIST` → `ERROR_CORRUPT_SONG_MISSING_REQUIRED_HEADER`; absent audio file → `ERROR_CORRUPT_SONG_FILE_NOT_FOUND`; v1.0.0 `#AUDIO`+`#MP3` → resolvedAudio from `#AUDIO`; legacy no-`#VERSION` → resolvedAudio from `#MP3`; legacy `#AUDIO` without `#MP3` → missing header error; absent `#VIDEO` → `isValid=true` + `hasVideo=false`; `#BPM:0` → `ERROR_CORRUPT_SONG_MALFORMED_HEADER`; non-numeric `#BPM` → malformed header + correct `invalidLineNumber` — `app/src/test/kotlin/com/couchraoke/tv/domain/library/SongValidationTest.kt`. Also cover: `relativeTxtPath` containing `..` → `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_INVALID_PATH`; `relativeTxtPath` starting with `/` → same result. Also cover: `relativeTxtPath` containing `..` → `isValid=false`, `invalidReasonCode=ERROR_CORRUPT_SONG_INVALID_PATH`; `relativeTxtPath` starting with `/` → same result. Use backtick `given <context>, when <action>, then <expected outcome>` naming for all test functions.
- [ ] T011 [P] [US2] Write `SongDiscoveryAcceptanceTest` (T3.2.9): load F01 fixture via `SongDiscovery.discoverFromDirectory()` using classpath resource `fixtures/library/song_discovery/songs_root`; parse `expected.discovery.json` with `kotlinx.serialization.json`; assert `isValid`, `invalidReasonCode`, and `invalidLineNumber` for every entry — `app/src/test/kotlin/com/couchraoke/tv/domain/library/SongDiscoveryAcceptanceTest.kt`. Use backtick \`given <context>, when <action>, then <expected outcome>\` naming for all test functions.

### Implementation for User Story 2

- [ ] T012 [US2] Implement `SongIndexer` object with `fromParseResult()`: set `songId = phoneClientId + "::" + relativeTxtPath`; copy `isValid` from `parsedSong.isValid`; map first ERROR `DiagnosticCode` to `invalidReasonCode` per the table in data-model.md; set `invalidLineNumber`; copy all asset URLs from parameters — `app/src/main/kotlin/com/couchraoke/tv/domain/library/SongIndexer.kt`
- [ ] T013 [US2] Implement `SongDiscovery` class with `discoverFromDirectory()`: walk `rootDir` recursively for `.txt` files; compute `relativeTxtPath` relative to `rootDir` with `/` separators and no leading `/`; call `readFile(path)` then `UsdxParser.parse()`; pass `ParseResult` to `SongIndexer.fromParseResult()` with stub URLs derived from `rootDir.relativize(txtPath)` — `app/src/main/kotlin/com/couchraoke/tv/domain/library/SongDiscovery.kt`
- [ ] T014 [US2] Run `./gradlew :app:libraryTest` — confirm `SongValidationTest` (unit) and `SongDiscoveryAcceptanceTest` pass; report test count and 0 failures

**Checkpoint**: User Stories 1 and 2 both independently functional and tested

---

## Phase 5: User Story 3 — Derived Index Fields (Priority: P3)

**Goal**: `SongIndexer.fromParseResult()` computes all derived flags (`isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`, `medleySource`, medley beats, `calcMedleyEnabled`) and `previewStartSec` at ingest time.

**Independent Test**: Pass inline songs with known properties to `SongIndexer.fromParseResult()` and assert individual derived field values.

### Tests for User Story 3 *(write first — must FAIL before T016)*

- [ ] T015 [US3] Write `SongIndexerTest` covering derived field scenarios T3.3.1–T3.3.8: P1/P2 tracks → `isDuet=true`; R/G notes → `hasRap=true`; duet → `canMedley=false`; non-duet with `#MEDLEYSTARTBEAT`+`#MEDLEYENDBEAT` → `canMedley=true`, `medleySource=EXPLICIT`, beats stored; non-duet no medley tags → `canMedley=false`, `medleySource=NONE`; `#PREVIEWSTART:45.0` → `previewStartSec=45.0`; no `#PREVIEWSTART` with medley tags → `previewStartSec = beat * 60.0 / (bpm * 4.0) + gapMs / 1000.0`; no `#PREVIEWSTART` no medley → `previewStartSec=0.0` — `app/src/test/kotlin/com/couchraoke/tv/domain/library/SongIndexerTest.kt`. Also cover: `#PREVIEWSTART:0` and `#PREVIEWSTART:-1.0` → treated as absent, fall back to medley beat formula or `0.0`. Also cover: `#PREVIEWSTART:0` and `#PREVIEWSTART:-1.0` → treated as absent, fall back to medley beat formula or `0.0`. Use backtick `given <context>, when <action>, then <expected outcome>` naming for all test functions.

### Implementation for User Story 3

- [ ] T016 [US3] Extend `SongIndexer.fromParseResult()` with derived field computation: copy `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `medleySource`, `medleyStartBeat`, `medleyEndBeat`, `calcMedleyEnabled`, and `previewStartSec` directly from `parsedSong.derivedSummary.*` (trust the parser — do not re-derive from raw tracks); set `canMedley = !derivedSummary.isDuet && derivedSummary.medleySource == MedleySource.EXPLICIT`; use `derivedSummary.previewStartSec ?: 0.0` — `app/src/main/kotlin/com/couchraoke/tv/domain/library/SongIndexer.kt`
- [ ] T017 [US3] Run `./gradlew :app:libraryTest` — confirm all four test classes pass (`SongLibraryTest` + `SongValidationTest` + `SongDiscoveryAcceptanceTest` + `SongIndexerTest`); report total test count and 0 failures

**Checkpoint**: All three user stories fully functional and independently tested

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T018 Run `./gradlew :app:test` — confirm full test suite passes with no regressions across parser, timing, scoring, and library packages; report test count
- [ ] T019 [P] Run `./gradlew :app:jacocoTestReport` and verify library domain coverage ≥ 80% overall and ≥ 60% per file; report per-file coverage for all files under `domain/library/`
- [ ] T020 Run `./gradlew :app:lint :app:detekt` — fix any warnings or error-level findings in `domain/library/` source files
- [ ] T021 After merge to master, rename branch to `[✓] 004-song-library`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Stories (Phases 3–5)**: All depend on Phase 2 completion; can proceed in priority order (P1 → P2 → P3) or in parallel if staffed
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Needs only `SongEntry` (T006) and `SongLibrary` (T008) — no parser wiring required
- **US2 (P2)**: Needs `SongIndexer` (T012) for validation mapping; `SongDiscovery` (T013) for F01 acceptance; `MALFORMED_BODY_FIELD` (T003) for correct diagnostic mapping
- **US3 (P3)**: Extends `SongIndexer` (T016) — requires T012 to exist first; tests (T015) can be written in parallel with T012 implementation

### Within Each User Story

- Tests written first (must compile but fail) before implementation
- Models (`SongEntry`, `ManifestEntry`) before services (`SongIndexer`, `SongDiscovery`, `SongLibrary`)
- Verify Gradle task passes before marking story complete

### Parallel Opportunities

- T003, T004, T005, T006 can all run in parallel (different files)
- T010, T011 can be written in parallel (different test files, same phase)
- T007 test writing can begin as soon as T006 (`SongEntry`) exists

---

## Parallel Example: Phase 2

```bash
# All four foundational tasks in parallel:
Agent 1: T003 — Add MALFORMED_BODY_FIELD to DiagnosticEntry.kt
Agent 2: T004 — Update BodyParser.kt
Agent 3: T005 — Create ManifestModels.kt
Agent 4: T006 — Create SongEntry.kt
```

## Parallel Example: User Story 2 Test Writing

```bash
# Both test files in parallel (different files, no code dependency):
Agent 1: T010 — SongValidationTest.kt (unit scenarios)
Agent 2: T011 — SongDiscoveryAcceptanceTest.kt (F01 fixture)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (library lifecycle)
4. **STOP and VALIDATE**: Run `./gradlew :app:libraryUnitTest` independently
5. Continue to US2 if lifecycle is stable

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Lifecycle working → `./gradlew :app:libraryUnitTest` green
3. US2 → Validation + F01 fixture passing → `./gradlew :app:libraryTest` green
4. US3 → Derived fields all passing → full `./gradlew :app:libraryTest` green
5. Polish → `./gradlew :app:test` no regressions → merge → rename branch

---

## Notes\n\n- `org.json` is Android-stubbed and MUST NOT be used in JVM unit tests — use `kotlinx.serialization.json` (already on classpath) for JSON parsing in `SongDiscoveryAcceptanceTest`
- `SongEntry` reuses `MedleySource` from `domain.parser` — do not redefine it
- `SongIndexer` is an `object` (stateless singleton) per contract
- F01 fixture must be loaded via `javaClass.classLoader.getResourceAsStream(...)` for Android classpath compatibility, not via `Paths.get()`
- Parser extension (T003, T004) is backward-compatible: `MALFORMED_NUMERIC_FIELD` in `HeaderParser` is unchanged; only `BodyParser` usages are updated
- `SongDiscoveryAcceptanceTest` is an acceptance `[A]` test (fixture-driven, classpath resource) — it runs in the unit CI job via `libraryAcceptanceTest` Gradle task, NOT in the instrumented job
