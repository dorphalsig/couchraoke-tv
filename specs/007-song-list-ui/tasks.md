# Tasks: Song List UI (Landing Screen)

**Input**: Design documents from `specs/007-song-list-ui/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**TDD mandate**: Test tasks appear before their corresponding implementation tasks within each phase. Tests MUST fail before implementation begins.

**Organization**: Phases 1–2 are blocking setup. Phases 3–8 map to User Stories US1–US6 in priority order. Phase 9 is polish.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no shared state)
- **[Story]**: Maps to user story (US1–US6)

---

## Phase 1: Setup — Gradle Bootstrapping (first Compose screen)

**Purpose**: Add Compose for TV, Hilt, KSP, and ViewModel to the project. These are one-time additions required for all future UI features. No user story work can begin until the project builds with Compose enabled.

- [x] T001 Add Compose BOM (`2025.05.01`), `tv-material` 1.0.0, `tv-foundation` 1.0.0, Hilt 2.56.1, `hilt-navigation-compose` 1.2.0, KSP `2.3.10-1.0.31` versions and library aliases to `gradle/libs.versions.toml` (see research.md §R6/R7 for full entries)
- [x] T002 Add `kotlin.compose`, `hilt`, and `ksp` plugin aliases to `gradle/libs.versions.toml` [plugins] section; apply them in root `build.gradle.kts` (`apply false`) and in `app/build.gradle.kts` (applied); add all Compose/TV/Hilt/ViewModel `implementation` and `ksp` dependencies to `app/build.gradle.kts`; run `./gradlew assembleDebug` and confirm BUILD SUCCESSFUL
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

**Goal**: Display the full song library in a TV-navigable grid. Host can navigate to a song, press OK, and see the Select Players modal.

**Independent Test**: Populate `SongLibrary` with 6 songs from 2 phones → Song List screen shows all 6 in Artist→Album→Title order with correct tile content → press OK on any tile → Select Players modal opens with correct subtitle.

### Tests for US1 *(write first — must FAIL before T016)*

- [x] T012 [P] [US1] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SelectPlayersGatingTest.kt` — unit tests on `SongListViewModel`: `given non-duet song selected, when onSongSelected, then dialog.player2SelectorEnabled=false and player2Difficulty hidden`; `given duet song, player1+player2 assigned, when checked, then swapParts available`; `given no phones connected, when onSongSelected, then dialog shows noPhones=true`; `given song with audioUrl=null, when onSelectPlayersStart, then selectPlayersDialog=null and errorModal set with correct text`

### Implementation for US1

- [x] T013 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SongTile.kt` — TV `Card` composable; `AsyncImage` for cover (Coil, placeholder from resources); `Text` for title + artist; tag chip row in lower-right (`D`/`R`/`V`/`I`/`M` — only show chips where flag=true); `combinedClickable(onClick=…, onLongClick=…)`
- [x] T014 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/JoinWidget.kt` — `Box` with `focusable = false`; generate QR `Bitmap` via `BarcodeEncoder` (ZXing) from WebSocket URL in a `LaunchedEffect`; `Image` composable; join code `Text` below; size constraints: `fillMaxHeight(0.16f)` minimum, `widthIn(min = 280.dp)` at 1080p
- [x] T015 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SelectPlayersModal.kt` — `AlertDialog`-style TV overlay; subtitle from `SelectPlayersMode`; Player 1 phone dropdown (required) + difficulty dropdown; Player 2 section: hidden entirely for Medley mode, disabled selector for non-duet, full for duet; "Swap Parts" button when both players assigned; solo duet-part radio for single player; blocking state matching §9.3 exactly: ⚠ "No phones connected." / "Connect phones in Settings to sing." / `[Open Settings > Connect Phones]` (Not Implemented stub) / `[Cancel]`; Start / Cancel buttons; wires all events to ViewModel
- [x] T016 [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` — top-level Composable; two-column `Row` layout; left panel contains `JoinWidget` + medley placeholder (`Spacer` for now); right panel contains search placeholder + `SongGrid`; collect `SongListUiState` from `SongListViewModel`; render `SelectPlayersModal` when `selectPlayersDialog != null`; render error `AlertDialog` when `errorModal != null`; empty-state messages per FR-010/FR-011
- [x] T017 [US1] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SongGrid.kt` — `TvLazyVerticalGrid(columns = TvGridCells.Fixed(if 4K then 4 else 3))`; items keyed by `songId`; each item is `SongTile`; expose `onSongClick` and `onSongLongClick` and `onSongFocused` callbacks; assign `FocusRequester` to first item (top-left) for initial focus; assign `Modifier.focusProperties { left = medleyFocusRequester }` on leftmost-column tiles
- [x] T018 [US1] Implement DPAD focus routing in `SongListScreen.kt` — create `FocusRequester` instances for: firstTile, searchField, medleyPlaylist top-row, playMedleyButton; wire `focusProperties` on left-panel edge and right-panel edge composables per spec navigation map (Auto Medley button is out of scope; Play Medley button is the bottom-most focusable in the left panel — DPAD Down from Play Medley = no action); `LaunchedEffect(Unit)` requests focus on firstTile (or searchField if `filteredSongs.isEmpty()`)
- [x] T019 [US1] Implement song selection state in `SongListViewModel` — `onSongSelected(song)`: builds `SelectPlayersDialogState` with connected phones from `Session`; `onSelectPlayersStart()`: validates audioUrl, emits `PlayerAssignment` or sets `errorModal`; `onSelectPlayersCancel()`: clears dialog; `onErrorModalDismissed()`: clears error; run `./gradlew test` to confirm T012 passes

**Checkpoint**: Song List screen renders, grid shows all songs, OK on tile opens Select Players. `./gradlew test` green.

---

## Phase 4: User Story 2 — Search and Filter Songs (Priority: P1)

**Goal**: Host can type in the Search field to filter the grid instantly; Back clears filter.

**Independent Test**: Load 20 songs, type artist name → only matching songs visible; press Back → all songs restored; no filter + Back → app exits.

### Tests for US2 *(write first — must FAIL before T022)*

- [x] T020 [P] [US2] Extend `SongFilterTest.kt` with edge-case tests: `given query matches title only, when filtered, then song included`; `given query matches album only, when filtered, then song included`; `given searchQuery non-empty, when Back pressed (onBackPressed), then searchQuery cleared and filteredSongs = allSongs`; `given searchQuery empty, when Back pressed, then appExitEvent emitted`

### Implementation for US2

- [x] T021 [P] [US2] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/SearchField.kt` — TV-styled `BasicTextField` (or `OutlinedTextField`); on OK/confirm key: trigger system `TextInputService` dialog (or `SoftwareKeyboardController.show()`); on text change: call `onSearchQueryChanged`; show clear indicator when query non-empty
- [x] T022 [US2] Implement Back-key handling in `SongListScreen.kt` — intercept `BackHandler`: if `searchQuery.isNotEmpty()` call `onSearchQueryChanged("")`; else allow default back (app exit); pass `filteredSongs` (not `allSongs`) to `SongGrid`; run `./gradlew test` to confirm T020 passes

**Checkpoint**: Search filters the grid live; Back clears filter. `./gradlew test` green.

---

## Phase 5: User Story 3 — Join Widget Always Visible (Priority: P1)

**Goal**: QR code and join code are displayed at all times in the left panel; non-focusable; correctly sized; QR encodes full WebSocket URL with session token.

**Independent Test**: Render `SongListScreen` with a live session → left panel shows QR image and join code text → neither element receives TV focus → QR bitmap encodes `ws://<ip>:<port>/?token=<token>`.

### Tests for US3 *(write first — must FAIL before T025)*

- [x] T023 [P] [US3] Add test to `SongListViewModelTest.kt`: `given Session with token "ABCD1234", when UiState observed, then joinToken = SessionToken.display("ABCD1234")`; `given Session emits RosterChanged, when observed, then joinToken unchanged`

### Implementation for US3

- [x] T024 [US3] Implement `joinToken` population in `SongListViewModel` — on init, set `joinToken = SessionToken.display(session.token)`; ensure it updates if session re-creates (observe `RosterChanged` or `SessionState` change); run `./gradlew test` to confirm T023 passes
- [x] T025 [US3] Implement `JoinWidget.kt` fully — wire `joinToken` from `SongListUiState`; QR payload = `ws://<tvIp>:<port>/?token=<rawToken>` (TV IP sourced from `WebSocketServer` or a DI-provided `TvNetworkInfo`); enforce size constraints (at least `(screenHeight * 0.16f).dp` square, minimum `280.dp`); add join code `Text` with font size `(screenHeight * 0.035f).dp` minimum; integrate into left panel of `SongListScreen.kt`

**Checkpoint**: QR and join code always visible; QR is non-focusable; correct payload. `./gradlew test` green.

---

## Phase 6: User Story 4 — Song Preview Audio While Browsing (Priority: P2)

**Goal**: When a tile holds focus for 500 ms and Preview Volume > 0, audio starts at `previewStartSec`. Stops on any focus change or modal open.

**Independent Test**: Focus a tile for 600 ms → ExoPlayer starts at `previewStartSec`; move focus → ExoPlayer stops; focus tile with `audioUrl=null` → no audio, no error; `previewStartSec=0` → seek to position 0.

### Tests for US4 *(write first — must FAIL before T028)*

- [x] T026 [P] [US4] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongPreviewControllerTest.kt` — unit tests: `given song with audioUrl=null, when startPreview, then ExoPlayer.setMediaItem never called`; `given song with previewStartSec=0.0, when startPreview, then ExoPlayer seeks to 0`; `given song with previewStartSec=45.5, when startPreview, then ExoPlayer seeks to 45500ms`; `given preview playing, when stopPreview called, then ExoPlayer.stop called`
- [x] T027 [P] [US4] Add preview debounce tests to `SongListViewModelTest.kt`: `given song focused, when 500ms advance with TestCoroutineScheduler, then previewController.startPreview called`; `given song focused then different song focused before 500ms, when scheduler advanced, then only second song preview starts`; `given onScreenVisible(false), when called, then stopPreview called`

### Implementation for US4

- [x] T028 [US4] Implement `SongPreviewController.kt` fully (from T011 stub) — `startPreview`: guards `audioUrl == null` (return silently); calls `player.setMediaItem(MediaItem.fromUri(audioUrl))`; `player.seekTo((previewStartSec * 1000).toLong())`; `player.prepare()`; `player.play()`; `stopPreview`: `player.stop()`; `release`: `player.release()`; run `./gradlew test` to confirm T026 passes
- [x] T029 [US4] Implement preview wiring in `SongListViewModel` — `focusedSongIdFlow.debounce(500).collect { id -> id?.let { startPreviewForSong(it) } ?: previewController.stopPreview() }`; `onScreenVisible(false)` → `previewController.stopPreview()`; `onSongSelected` / `onPlayMedley` → `previewController.stopPreview()`; run `./gradlew test` to confirm T027 passes
- [x] T030 [US4] Wire `onSongFocused` in `SongGrid.kt` — track focused tile via `onFocusChanged` modifier; call `onSongFocused(song.songId)` when focused, `onSongFocused(null)` when unfocused; propagate from `SongListScreen.kt` to ViewModel

**Checkpoint**: Preview plays after 500 ms focus hold; stops on focus leave/modal. `./gradlew test` green.

---

## Phase 7: User Story 5 — Build and Play a Medley (Priority: P2)

**Goal**: Long-press adds songs to a transient playlist; Play Medley opens Select Players; Auto Medley fills with 5 random eligible songs; Reorder and delete in playlist.

**Independent Test**: Long-press 3 `canMedley=true` songs → playlist shows all 3 → press Play Medley → Select Players opens with "Medley — 3 songs" subtitle and no Player 2 section.

### Tests for US5 *(write first — must FAIL before T033)*

- [x] T031 [P] [US5] Write `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/MedleyPlaylistTest.kt` — unit tests on ViewModel: `given canMedley=true song, when onSongLongPressed, then medleyPlaylist contains song`; `given canMedley=false song, when onSongLongPressed, then errorModal set with exact blocking text`; `given playlist has 3 songs, when onPlaylistRowLongPressed(1), then playlist has 2 songs`; `given reorder started at index 2, when onReorderConfirm(2,0), then song moved to top`; `given reorder started, when onReorderCancel, then playlist unchanged`; `given playlist non-empty, when clearMedleyPlaylist called, then playlist empty`

### Implementation for US5

- [x] T032 [P] [US5] Implement medley logic in `SongListViewModel` — `onSongLongPressed`: check `canMedley` → append or set error modal; `onPlaylistRowLongPressed(index)`: remove at index; `onPlaylistRowSelected(index)`: set `isReorderingMedleyIndex`; `onReorderConfirm/Cancel`; `onPlayMedley`: build `SelectPlayersDialogState(mode=Medley(n))`; `clearMedleyPlaylist()`; run `./gradlew test` to confirm T031 passes
- [x] T033 [P] [US5] Create `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/MedleyPlaylist.kt` — `LazyColumn` with fixed-height constraint (`min(7 * rowHeight, screenHeight * 0.25f)`, minimum 3 rows always visible); each row shows `"<Artist>  <Title>"`; OK = `onRowSelected`, long-press = `onRowLongPress`; Reorder mode visual (highlight + Up/Down DPAD only, Left/Right do nothing); `Play Medley` button below list (Auto Medley is out of scope for this feature)
- [x] T034 [US5] Integrate `MedleyPlaylist.kt` into `SongListScreen.kt` left panel below `JoinWidget`; wire all medley events to ViewModel; connect Play Medley → Select Players (already handled by T019); wire DPAD routing from medley playlist top-row ↔ Play Medley button ↔ last playlist row per navigation map; wire `onNavigateAway` → `clearMedleyPlaylist()`

**Checkpoint**: Medley playlist builds correctly; Play Medley opens Select Players with correct mode. `./gradlew test` green.

---

## Phase 8: User Story 6 — Random Song Selection (Priority: P3)

**Goal**: "Sing Random Song" and "Sing Random Duet" buttons select from the currently visible filtered set and open Select Players. Buttons disabled when set is empty.

**Independent Test**: Load 10 songs (3 duets), type a filter leaving 4 songs (1 duet) → Sing Random Song selects from the 4; Sing Random Duet selects the 1 duet; clear filter, empty result set → both buttons disabled.

### Tests for US6 *(write first — must FAIL before T036)*

- [x] T035 [P] [US6] Add random-action tests to `SongListViewModelTest.kt`: `given filteredSongs has 5 songs, when onRandomSong, then selectPlayersDialog.song is one of the 5`; `given filteredSongs has 3 duets and 2 non-duets, when onRandomDuet, then dialog.song.isDuet=true`; `given filteredSongs has no duets, when onRandomDuet, then errorModal set`; `given filteredSongs empty, when onRandomSong or onRandomDuet, then neither dialog nor errorModal set (buttons should be disabled, event should not reach ViewModel)`

### Implementation for US6

- [x] T036 [US6] Implement `onRandomSong` / `onRandomDuet` in `SongListViewModel` — `onRandomSong`: `filteredSongs.filter { it.isValid }.randomOrNull() ?: → errorModal`; `onRandomDuet`: `filteredSongs.filter { it.isDuet }.randomOrNull() ?: → errorModal`; both open `selectPlayersDialog` with `SelectPlayersMode.SingleSong` on success; run `./gradlew test` to confirm T035 passes
- [x] T037 [US6] Create / complete `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/ActionButtons.kt` — "Sing Random Song" and "Sing Random Duet" buttons; `enabled = filteredSongs.isNotEmpty()` (random song) and `filteredSongs.any { it.isDuet }` (random duet); wire `onClick` events to ViewModel; integrate into right panel of `SongListScreen.kt` below search field above grid

**Checkpoint**: Random actions work; buttons correctly disabled. `./gradlew test` green.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [x] T038 [P] Add JaCoCo exclusion patterns in `app/build.gradle.kts` for Compose-generated classes (`**/*ComposableSingletons*`, `**/*_Factory*`, `**/*_MembersInjector*`, `**/Hilt_*`) to prevent generated code from lowering coverage below thresholds
- [x] T039 [P] Add `@Suppress("LongMethod")` with rationale comment on Composable functions that genuinely exceed Detekt's 40-line threshold (e.g., `SongListScreen`, `SelectPlayersModal`); do NOT suppress logic functions — keep them short
- [x] T040 [P] Write mandated spec acceptance tests per §3.4: add `T3.4.1`–`T3.4.6` as `@Test` methods in `SongListViewModelTest.kt` matching IDs and descriptions from the original spec (empty states, canMedley=false modal text, Play Medley disabled when empty, Back with/without filter)
- [x] T041 Run `./gradlew ciUnitTests` — confirm BUILD SUCCESSFUL; report test count and coverage; fix any failing tests or coverage gaps before marking done
- [x] T042 Run `./gradlew detekt :app:lintDebug` — confirm both pass clean; fix any new rule violations introduced by this feature
- [ ] T043 After feature is merged to `master`, rename branch: `git branch -m 007-song-list-ui "[✓] 007-song-list-ui"`

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
| US3 (join widget) | None | Phase 2 only |
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
T012 (SelectPlayersGatingTest), T013 (SongTile), T014 (JoinWidget) ← parallel

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
5. Complete Phase 5: US3 (join widget fully wired)
6. **STOP AND VALIDATE**: T041 + T042 pass; screen usable end-to-end

### Full Delivery

7. Phase 6: US4 (preview)
8. Phase 7: US5 (medley)
9. Phase 8: US6 (random)
10. Phase 9: Polish → T041/T042 final pass → T043 branch closure

---

## Notes

- [P] tasks modify different files — safe to parallelize
- Each phase ends with `./gradlew test` — never leave a phase with red tests
- `SongListViewModel` is the single state owner — all composables are stateless
- `SongPreviewController` is released in `ViewModel.onCleared()` — no manual lifecycle management needed in Compose
- `PlayerAssignment` must be `@Parcelize` for Compose Navigation
- 4K column detection threshold (`screenWidthDp >= 3840`) should be marked `TODO(SPEC: verify on real 4K hardware)`
- Tiny-file exemption (≤30 non-comment lines) applies to simple data classes — add to JaCoCo excludes if needed
