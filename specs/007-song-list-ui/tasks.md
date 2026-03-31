# Tasks: Song List UI (Landing Screen)

**Input**: Design documents from `specs/007-song-list-ui/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**TDD mandate**: Test tasks appear before their corresponding implementation tasks within each phase. Tests MUST fail before implementation begins.

**Organization**: Phases 1–2 are blocking setup. Phases 3–8 map to User Stories US1–US6 in priority order. Phase 9 is polish.

**Update note (2026-03-31)**: Existing tasks were preserved. Only tasks affected by the revision-2 spec/plan deltas were reset or rewritten: T014, T016, T018, T020–T025, T026–T027, T029–T037, and T040–T046.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no shared state)
- **[Story]**: Maps to user story (US1–US6)

---

## Phase 1: Setup — Gradle Bootstrapping (first Compose screen)

**Purpose**: Add Compose for TV, Hilt, KSP, and ViewModel to the project. These are one-time additions required for all future UI features. No user story work can begin until the project builds with Compose enabled.

- [x] T001 Add Compose BOM (`2025.05.01`), `tv-material` 1.0.0, `tv-foundation` 1.0.0, Hilt 2.56.1, `hilt-navigation-compose` 1.2.0, KSP `2.3.10-1.0.31`, Roborazzi 1.7.0, and Robolectric 4.13 versions plus library/plugin aliases to `gradle/libs.versions.toml` (see research.md §R6/R7 and plan.md Roborazzi section for full entries)
- [x] T002 Add `kotlin.compose`, `hilt`, `ksp`, and `roborazzi` plugin aliases to `gradle/libs.versions.toml` [plugins] section; apply them in root `build.gradle.kts` (`apply false`) and in `app/build.gradle.kts`; add Compose/TV/Hilt/ViewModel `implementation`, Hilt `ksp`, and Roborazzi/Robolectric `testImplementation` dependencies to `app/build.gradle.kts`; configure Roborazzi output dir and create `app/src/test/resources/robolectric.properties`; run `./gradlew app:testDebugUnitTest` and confirm BUILD SUCCESSFUL
- [x] T003 [P] Create `app/src/main/kotlin/com/couchraoke/tv/CouchraokeApp.kt` — annotate with `@HiltAndroidApp`, extend `Application`; update `app/src/main/AndroidManifest.xml` to reference `CouchraokeApp` as `android:name`
- [x] T004 Create `app/src/main/kotlin/com/couchraoke/tv/MainActivity.kt` — annotate with `@AndroidEntryPoint`; call `setContent { }` with a stub `Text("Song List coming soon")`; verify `./gradlew assembleDebug` still passes

**Checkpoint**: Project compiles with Compose + Hilt enabled. `./gradlew assembleDebug` passes.

---

## Phase 2: Foundational — State Types, DI, ViewModel Skeleton

**Purpose**: Create all shared types, the DI graph, and the ViewModel skeleton that every user story phase depends on. No user story can be implemented until this phase is complete.

**⚠️ CRITICAL**: All Phase 3–8 work is blocked until this phase passes `./gradlew test`.

### Foundational Tests *(write first — must FAIL before T009/T010)*

- [x] T005 [P] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt` — test class skeleton + initial-state tests: `given empty library, when ViewModel created, then allSongs=[] filteredSongs=[] joinToken="" sessionState=Open`; `given Session emits PhoneConnected event, when observed, then allSongs updated from SongLibrary.getSortedSongs()`; use `FakeSongLibrary`, `FakeSession` test doubles and `StandardTestDispatcher` / `runTest`
- [x] T006 [P] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongFilterTest.kt` — pure function tests for `filteredSongs` derivation: empty query returns all songs in Artist→Album→Title order; case-insensitive substring matches artist/album/title; non-matching query returns empty list; filter result preserves full sort order

### Foundational Implementation

- [x] T007 [P] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt` — define: `SongListUiState` data class (all fields per data-model.md), `SelectPlayersDialogState`, `ErrorModalState`, `PhoneOption`, `Difficulty` enum, `DuetPart` enum, `SelectPlayersMode` sealed class
- [x] T008 [P] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/PlayerAssignment.kt` — data class per `contracts/PlayerAssignment.md`; mark `@Parcelize` (needed for Compose Navigation argument)
- [x] T009 Create `app/src/main/kotlin/com/couchraoke/tv/di/AppModule.kt` — `@Module @InstallIn(SingletonComponent)`; `@Provides @Singleton` for `SongLibrary` (returns `DefaultSongLibrary`), `Session` (constructs with a stub `IConnectionCloser { }` — **Not Implemented**: `WebSocketServer` is not yet in the DI graph; kick/forget will be wired when the Settings feature is implemented); `@Provides` for `ExoPlayer` instance (used by preview controller); run `./gradlew assembleDebug` and confirm Hilt graph compiles
- [x] T010 Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` — `@HiltViewModel`; inject `SongLibrary`, `Session`, `ExoPlayer`; expose `StateFlow<SongListUiState>`; observe `Session.events` to refresh `allSongs` (`PhoneConnected`/`PhoneDisconnected`/`PhoneReconnected` → call `library.getSortedSongs()`); derive `filteredSongs` from `allSongs + searchQuery`; implement `onSearchQueryChanged` with 150 ms `debounce`; stub all other event handlers as no-ops; run `./gradlew test` and confirm T005/T006 pass
- [x] T011 Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/preview/SongPreviewController.kt` — wraps injected `ExoPlayer`; `startPreview(song: SongEntry)`: if `audioUrl == null` return silently; otherwise set media item with `audioUrl` and seek to `previewStartSec` (seek to 0 if `previewStartSec == 0.0`); `stopPreview()`: stop and clear; `release()`: called from `ViewModel.onCleared()`; integrate into `SongListViewModel` (`focusedSongIdFlow.debounce(500ms).collect { startPreview }`)

**Checkpoint**: `./gradlew test` passes (T005 + T006 green). Project builds. All state types exist.

---

## Phase 3: User Story 1 — Browse Song Grid and Select a Song (Priority: P1) 🎯 MVP

**Goal**: Display the revised Song List layout: header, preview pane, and sorted song grid. Host can navigate to a song tile, press OK, and open the Select Players modal.

**Independent Test**: Populate `SongLibrary` with 6 songs from 2 phones → Song List shows the header (`Code`, Search, Join, Settings), a preview placeholder, and all 6 songs in Artist→Album→Title order → press OK on any tile → Select Players opens with the correct subtitle.

### Tests for US1 *(write first — must FAIL before T016)*

- [x] T012 [P] [US1] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SelectPlayersGatingTest.kt` — unit tests on `SongListViewModel`: `given non-duet song selected, when onSongSelected, then dialog.player2SelectorEnabled=false and player2Difficulty hidden`; `given duet song, player1+player2 assigned, when checked, then swapParts available`; `given no phones connected, when onSongSelected, then dialog shows noPhones=true`; `given song with audioUrl=null, when onSelectPlayersStart, then selectPlayersDialog=null and errorModal set with correct text`

### Implementation for US1

- [x] T013 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SongTile.kt` — TV `Card` composable; `AsyncImage` for cover (Coil, placeholder from resources); `Text` for title + artist; tag chip row in lower-right (`D`/`R`/`V`/`I`/`M` — only show chips where flag=true); `combinedClickable(onClick=…, onLongClick=…)`
- [x] T014 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/PreviewPane.kt` — display-only 16:9 preview pane; placeholder state (app logo on dimmed background) before any tile has been focused; focused state shows centered cover with blurred/dimmed fill, title, artist, and tag chips; keep it non-focusable per FR-046
- [x] T015 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SelectPlayersModal.kt` — `AlertDialog`-style TV overlay; subtitle from `SelectPlayersMode`; Player 1 phone dropdown (required) + difficulty dropdown; Player 2 section: hidden entirely for Medley mode, disabled selector for non-duet, full for duet; "Swap Parts" button when both players assigned; solo duet-part radio for single player; blocking state matching §9.3 exactly: ⚠ "No phones connected." / "Connect phones in Settings to sing." / `[Open Settings > Connect Phones]` (Not Implemented stub) / `[Cancel]`; Start / Cancel buttons; wires all events to ViewModel
- [x] T016 [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/HeaderBar.kt` and restructure `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — full-width header with `Code: XXXX-XXXX`, Search slot, `[ JOIN ]`, and `[ ⚙ SETTINGS ]`; a secondary action-row placeholder; and a two-column body with `PreviewPane` above a medley placeholder on the left and `SongGrid` on the right; keep `SelectPlayersModal`, error modal, and empty-state messages per FR-010/FR-011
- [x] T017 [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SongGrid.kt` — `TvLazyVerticalGrid(columns = TvGridCells.Fixed(if 4K then 4 else 3))`; items keyed by `songId`; each item is `SongTile`; expose `onSongClick` and `onSongLongClick` and `onSongFocused` callbacks; assign `FocusRequester` to first item (top-left) for initial focus; assign `Modifier.focusProperties { left = medleyFocusRequester }` on leftmost-column tiles
- [x] T018 [US1] Update DPAD focus routing in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — add `FocusRequester`s for Search, Join, Settings, the random-action row, first tile, first medley row, and Play Medley; implement the FR-009 map including left-panel entry priority and initial focus on the first tile or Search when the grid is empty
- [x] T019 [US1] Implement song selection state in `SongListViewModel` — `onSongSelected(song)`: builds `SelectPlayersDialogState` with connected phones from `Session`; `onSelectPlayersStart()`: validates audioUrl, emits `PlayerAssignment` or sets `errorModal`; `onSelectPlayersCancel()`: clears dialog; `onErrorModalDismissed()`: clears error; run `./gradlew test` to confirm T012 passes

**Checkpoint**: Song List screen renders in the revised layout, the grid shows all songs, and OK on a tile opens Select Players. `./gradlew test` green.

---

## Phase 4: User Story 2 — Search and Filter Songs (Priority: P1)

**Goal**: Host can type in the header Search field to filter the grid instantly, and Back follows the 4-step Song List cascade.

**Independent Test**: Load 20 songs, type an artist name in header Search → only matching songs are visible; press Back from the grid → focus returns to Search; press Back again with an active filter → all songs are restored; press Back again with no active filter → app exits.

### Tests for US2 *(write first — must FAIL before T022)*

- [x] T020 [P] [US2] Extend `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongFilterTest.kt` and `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt` — cover title/album substring matches plus the `BackResult` cascade: overlay open → `ClosedModal`, grid/left-panel focus → `MovedToSearch`, header focus with active filter → `ClearedFilter`, header focus with empty filter → `ExitApp`

### Implementation for US2

- [x] T021 [P] [US2] Create or update `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SearchField.kt` — TV-styled Search field for the header; OK opens the system text input dialog; confirming text returns focus to the field and applies the filter immediately; expose a clear affordance when the query is non-empty
- [x] T022 [US2] Implement header-search wiring and the Back cascade in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/HeaderBar.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — move focus to Search from the grid/left panel, clear an active filter from the header/top controls, and exit only when no filter is active and focus is already in the top controls

**Checkpoint**: Search filters the grid live from the header, and Back follows the full overlay → search → clear-filter → exit cascade. `./gradlew test` green.

---

## Phase 5: User Story 3 — Join Button and Pairing Overlay (Priority: P1)

**Goal**: The header always shows the text join code, and a focusable `[ JOIN ]` button opens a pairing overlay with the QR code and join code.

**Independent Test**: Render `SongListScreen` with a live session → header shows `Code: XXXX-XXXX` → press Join → pairing overlay opens with QR image and join code text → press Back → overlay closes and focus returns to Song List.

### Tests for US3 *(write first — must FAIL before T025)*

- [x] T023 [P] [US3] Extend `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt` — verify `joinToken = SessionToken.display(session.token)`, `onJoinPressed()` / `onPairingOverlayDismissed()` toggle `isPairingOverlayOpen`, and `onBackPressed()` closes the pairing overlay before the rest of the cascade runs

### Implementation for US3

- [x] T024 [US3] Implement join-code and overlay state in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` — populate the header join code from `SessionToken`, keep it stable across roster updates, and consume `IJoinEndpointProvider` as the read-only source of the canonical join URL for QR payload generation
- [x] T025 [US3] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/PairingOverlay.kt` and wire it from `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/HeaderBar.kt` / `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — `[ JOIN ]` opens a modal pairing overlay with a centered high-contrast QR, minimum `320.dp` square sizing, quiet zone, and join code text ≥ `24.sp`; Back or Cancel dismisses and restores focus to Song List

**Checkpoint**: The header join code is always visible, the Join button opens the pairing overlay, and the QR payload is correct. `./gradlew test` green.

---

## Phase 6: User Story 4 — Song Preview Audio While Browsing (Priority: P2)

**Goal**: A focused song updates the visual preview pane immediately and starts audio after 500 ms; leaving the grid stops audio but keeps the visual preview sticky.

**Independent Test**: Focus a tile for 600 ms → the preview pane shows that song and ExoPlayer starts at `previewStartSec`; move focus out of the grid → ExoPlayer stops but the preview pane still shows the last focused song; re-enter the screen → preview pane resets to the placeholder.

### Tests for US4 *(write first — must FAIL before T028)*

- [x] T026 [P] [US4] Update `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongPreviewControllerTest.kt` — cover silent suppression for `audioUrl=null`, seek-to-0 for `previewStartSec=0.0`, seek to `previewStartSec`, and `stopPreview()` stopping the player without surfacing UI errors
- [x] T027 [P] [US4] Extend `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt` — `onSongFocused(songId)` updates `focusedSong` immediately, preview starts only after the 500 ms debounce, leaving the grid stops audio without clearing `focusedSong`, and `onScreenVisible(true)` resets `focusedSong` on screen re-entry

### Implementation for US4

- [x] T028 [US4] Implement `SongPreviewController.kt` fully (from T011 stub) — `startPreview`: guards `audioUrl == null` (return silently); calls `player.setMediaItem(MediaItem.fromUri(audioUrl))`; `player.seekTo((previewStartSec * 1000).toLong())`; `player.prepare()`; `player.play()`; `stopPreview`: `player.stop()`; `release`: `player.release()`; run `./gradlew test` to confirm T026 passes
- [x] T029 [US4] Update preview wiring in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` — maintain sticky `focusedSong`, debounce `previewingSongId`, stop preview on grid exit/modal open/pairing overlay open/screen hide, and reset `focusedSong` only on screen re-entry
- [x] T030 [US4] Update `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SongGrid.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/PreviewPane.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — tile focus drives both preview-pane content and audio-preview events; leaving the grid preserves the last `focusedSong` visually while notifying the ViewModel that non-grid focus is active

**Checkpoint**: The preview pane updates immediately, audio starts after the 500 ms hold, and audio stops on focus leave/modal while the visual preview stays sticky. `./gradlew test` green.

---

## Phase 7: User Story 5 — Build and Play a Medley (Priority: P2)

**Goal**: Long-press builds a duplicate-free transient playlist; host can reorder or delete entries; Play Medley opens Select Players.

**Independent Test**: Long-press 3 `canMedley=true` songs → playlist shows all 3 → long-press the same focused tile again → first repeat is ignored, repeated presses on that same focused tile show feedback → press Play Medley → Select Players opens with `Medley — 3 songs` and no Player 2 section.

### Tests for US5 *(write first — must FAIL before T033)*

- [x] T031 [P] [US5] Extend `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/MedleyPlaylistTest.kt` — cover unique append, duplicate suppression on the first repeated long-press, `duplicateMedleyFeedback=true` on repeated long-presses of the same focused tile, feedback reset on focus change, reorder cancel restoring the original order, and playlist clearing on non-modal navigation away

### Implementation for US5

- [x] T032 [P] [US5] Implement revised medley logic in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` — unique append only, per-focused-tile duplicate tracking + escalating feedback, reorder/delete, Play Medley dialog state, and clear-on-navigation/start-song/start-medley behavior per FR-041
- [x] T033 [P] [US5] Update `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/MedleyPlaylist.kt` — fixed-height scrollable playlist, OK enters reorder mode, long-press deletes immediately, reorder visuals, and `Play Medley` disabled + non-focusable when the playlist is empty
- [x] T034 [US5] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/ContextualHintsBar.kt` and integrate `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/MedleyPlaylist.kt` into `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — render grid / playlist / reorder hints per FR-044, place the medley list below `PreviewPane`, and wire `onFocusZoneChanged` / `Play Medley` events

**Checkpoint**: The medley playlist stays duplicate-free, reorder/delete work, and Play Medley opens Select Players with the correct mode. `./gradlew test` green.

---

## Phase 8: User Story 6 — Pick a Random Song, Duet, or Medley (Priority: P3)

**Goal**: Random actions choose from the currently visible filtered set; Random Medley selects up to 5 eligible medley songs and opens Select Players.

**Independent Test**: Load 10 songs (3 duets, 6 medley-eligible), type a filter leaving 4 songs (1 duet, 3 medley-eligible) → Sing Random Song selects from the 4; Sing Random Duet selects the 1 duet; Sing Random Medley selects up to 3 eligible songs; clear to an empty filtered result → all three random buttons are disabled, greyed out, and non-focusable.

### Tests for US6 *(write first — must FAIL before T036)*

- [x] T035 [P] [US6] Extend `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt` — `onRandomSong` selects from visible valid songs, `onRandomDuet` selects from visible duet songs, `onRandomMedley` selects `min(5, eligible)` medley songs and replaces the playlist, and impossible presses surface the blocking single-OK modal instead of changing selection

### Implementation for US6

- [x] T036 [US6] Implement `onRandomSong()`, `onRandomDuet()`, and `onRandomMedley()` in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` — use an injectable `Random` for deterministic tests; Random Medley requires at least 2 eligible songs, replaces the current medley playlist, and opens `SelectPlayersDialogState(mode = Medley(n))`
- [x] T037 [US6] Update `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/ActionButtons.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — render "Sing Random Song", "Sing Random Duet", and "Sing Random Medley"; keep ineligible actions greyed out, disabled, and non-focusable; place the row beneath the header and wire all three actions to the ViewModel

**Checkpoint**: Random song, duet, and medley actions work from the filtered set, and disabled actions are visibly unavailable. `./gradlew test` green.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [x] T038 [P] Add JaCoCo exclusion patterns in `app/build.gradle.kts` for Compose-generated classes (`**/*ComposableSingletons*`, `**/*_Factory*`, `**/*_MembersInjector*`, `**/Hilt_*`) to prevent generated code from lowering coverage below thresholds
- [x] T039 [P] Add `@Suppress("LongMethod")` with rationale comment on Composable functions that genuinely exceed Detekt's 40-line threshold (e.g., `SongListScreen`, `SelectPlayersModal`); do NOT suppress logic functions — keep them short
- [x] T040 [P] Update `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/fixtures/SongListScreenFixtures.kt` — deterministic VR-001 fixtures with fixed `SessionToken = "TEST-1234"`, fixed `IJoinEndpointProvider` join URL, local cover-art placeholders, pairing-overlay state, preview-pane focused-song state, and Random Medley enabled/disabled scenarios
- [x] T041 [P] Update `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreenStateTest.kt` — 14 Roborazzi screen-state tests for VR-001: empty, no-songs, populated, filtered-hit, filtered-empty, select-players non-duet, select-players duet-both, select-players duet-solo, no-phones blocking modal, error modal, medley visible, medley reorder, pairing overlay open, and preview-pane focused-song state
- [x] T042 [P] Create `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListNavigationTest.kt` — Compose/Robolectric tests for initial focus, the FR-009 DPAD routing across Search / Join / Settings / action row / grid / left panel, and the FR-015 Back cascade
- [x] T043 [P] Create `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListInteractionTest.kt` — Compose/Robolectric tests for Join overlay open/dismiss, Random Medley enablement semantics, duplicate-medley feedback visibility, and contextual hints-bar transitions
- [x] T044 Record updated Roborazzi baselines in `app/src/test/snapshots/images/com.couchraoke.tv.presentation.songlist/` by running `./gradlew recordRoborazziDebug`; verify 14 baseline PNGs are written for the 14 VR-001 states
- [x] T045 Run the focused Song List suite with `./gradlew testDebugUnitTest --tests "com.couchraoke.tv.presentation.songlist.*"`; fix any failing unit, Compose, or Roborazzi tests before the full verification pass
- [x] T046 Run `./gradlew verifyRoborazziDebug testDebugUnitTest` — confirm updated unit tests, Compose/Robolectric tests, and Roborazzi verification all pass before marking the revision complete
- [x] T047 Run `./gradlew detekt :app:lintDebug` — confirm both pass clean; fix any new rule violations introduced by this feature
- [ ] T048 After feature is merged to `master`, rename branch: `git branch -m 007-song-list-ui "[✓] 007-song-list-ui"`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — **BLOCKS Phases 3–8**
- **Phase 3 (US1)**: Depends on Phase 2 ✅
- **Phase 4 (US2)**: Depends on Phase 2 ✅ — can run in parallel with Phase 3
- **Phase 5 (US3)**: Depends on Phase 2 ✅ — can run in parallel with Phases 3–4
- **Phase 6 (US4)**: Depends on Phase 2 ✅ — can run in parallel with Phases 3–5
- **Phase 7 (US5)**: Depends on Phase 2 ✅ — `SongListScreen` skeleton from T016 recommended first
- **Phase 8 (US6)**: Depends on Phase 2 ✅ — `ActionButtons.kt` stub from T017 recommended first
- **Phase 9 (Polish)**: Depends on all desired stories complete

### User Story Dependencies

| Story | Blocks | Blocked by |
|---|---|---|
| US1 (grid + select) | US4 (needs SongGrid for focus events), US5/US6 (need screen skeleton) | Phase 2 only |
| US2 (search) | None | Phase 2 only |
| US3 (join button + pairing overlay) | None | Phase 2 only |
| US4 (preview) | None | Phase 2 only (SongPreviewController stub in T011) |
| US5 (medley) | None | Phase 2 only (SongListScreen skeleton for integration) |
| US6 (random) | None | Phase 2 only |

### Within Each Phase

1. Test tasks first (must fail before implementation)
2. Independent component tasks (marked [P]) in parallel
3. Integration tasks after components exist
4. `./gradlew test` verification after each phase

---

## Parallel Opportunities

```
# Phase 1 — after T001:
T002, T003 can run in parallel

# Phase 2 — after T002+T003+T004:
T005 (ViewModel tests) and T006 (SongFilterTest)   ← parallel test authoring
T007 (UiState types), T008 (PlayerAssignment)       ← parallel type creation

# Phase 3 — after Phase 2:
T012 (SelectPlayersGatingTest), T013 (SongTile), T014 (PreviewPane) ← parallel

# Phase 6 — after Phase 2:
T026 (SongPreviewControllerTest), T027 (preview debounce tests) ← parallel

# Phase 7 — after Phase 2:
T031 (MedleyPlaylistTest), T032 (medley ViewModel logic), T033 (MedleyPlaylist composable) ← parallel
```

---

## Implementation Strategy

### MVP (User Stories 1–3, all P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational ← critical gate
3. Complete Phase 3: US1 (song grid + select players) → **first runnable screen**
4. Complete Phase 4: US2 (search)
5. Complete Phase 5: US3 (join button + pairing overlay fully wired)
6. **STOP AND VALIDATE**: T046 + T047 pass; screen usable end-to-end with Roborazzi baselines recorded and verified

### Full Delivery

7. Phase 6: US4 (preview)
8. Phase 7: US5 (medley)
9. Phase 8: US6 (random)
10. Phase 9: Polish → T045/T046/T047 final pass → T048 branch closure

---

## Notes

- [P] tasks modify different files — safe to parallelize
- Each phase ends with `./gradlew test` — never leave a phase with red tests
- `SongListViewModel` is the single state owner — all composables are stateless
- `SongPreviewController` is released in `ViewModel.onCleared()` — no manual lifecycle management needed in Compose
- `PlayerAssignment` must be `@Parcelize` for Compose Navigation
- 4K column detection threshold (`screenWidthDp >= 3840`) should be marked `TODO(SPEC: verify on real 4K hardware)`
- Tiny-file exemption (≤30 non-comment lines) applies to simple data classes — add to JaCoCo excludes if needed
