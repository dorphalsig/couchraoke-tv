# Implementation Plan: Song List UI (Landing Screen)

**Branch**: `007-song-list-ui` | **Date**: 2026-03-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/007-song-list-ui/spec.md`

## Summary

Implement the Song List landing screen — the TV host's primary browsing and song-selection surface. Builds on the in-memory `SongLibrary` (004) and `Session` (006) as read-only sources. Introduces the first Compose for TV screen in the project: a two-column layout with a DPAD-navigable song grid, inline search, QR join widget, medley playlist, audio preview, and the Select Players modal. Establishes the Compose + Hilt + ViewModel bootstrapping used by all future UI features.

---

## Technical Context

**Language/Version**: Kotlin 2.3.10 / Java 11 (Android minSdk 28, compileSdk 36)
**Primary Dependencies**: Jetpack Compose for TV (`androidx.tv:tv-material` 1.0.0, `tv-foundation` 1.0.0), Compose BOM `2025.05.01`, `lifecycle-viewmodel-compose`, Hilt 2.56.1, `hilt-navigation-compose` 1.2.0, Media3 ExoPlayer 1.9.2 (already on classpath), Coil 3.4.0 (already on classpath), ZXing `zxing-android-embedded` 4.3.0 (already on classpath)
**Storage**: In-memory only — `SongLibrary` (session-scoped), `Session` (session-scoped)
**Testing**: JUnit4 (`junit:junit:4.13.2`), `kotlinx-coroutines-test`, `StandardTestDispatcher`
**Target Platform**: Android TV, minSdk 28
**Project Type**: Android TV application (first UI screen)
**Performance Goals**: Grid update ≤ 300 ms after search keystroke; phone join → grid update ≤ 2 s; preview starts ≤ 600 ms after 500 ms focus-hold; QR scan at 3 m on 55″ TV
**Constraints**: LAN-only; no cloud calls; JUnit4 only (no JUnit5); ≥ 80% overall / ≥ 60% per-file coverage; Detekt + Lint pass; in-memory state only
**Scale/Scope**: Up to 10 phones × N songs per phone; single active session; one screen

---

## Constitution Check

| Principle | Status | Notes |
|---|---|---|
| LAN-only operation | ✅ Pass | Preview uses `audioUrl` from existing phone manifest; QR payload encodes local WS URL; no new outbound cloud traffic |
| Approved tech stack | ✅ Pass | Compose for TV, Media3, Coil, Hilt, ZXing — all constitution-approved; new Compose/Hilt Gradle additions required (R6/R7 in research.md) |
| Clean Architecture boundaries | ✅ Pass | New code lives entirely in `presentation/songlist/` and `di/`; no Android framework types in `domain/`; `SongListViewModel` is the only VM |
| Networking / streaming contracts | ✅ Pass | No new transport; preview uses same OkHttp-backed `audioUrl` as full playback |
| Tests and quality gates | ✅ Pass | JUnit4-only; `StandardTestDispatcher`; `runTest`; ≥ 80%/60% coverage gates; Detekt/Lint CI gates |
| Branch hygiene | ✅ Acknowledged | After merge to master: rename branch to `[✓] 007-song-list-ui` |

---

## Project Structure

### Documentation (this feature)

```text
specs/007-song-list-ui/
├── plan.md              ← this file
├── research.md          ← Phase 0
├── data-model.md        ← Phase 1
├── contracts/
│   ├── PlayerAssignment.md
│   └── SongListUiState.md
└── tasks.md             ← Phase 2 (/speckit.tasks)
```

### Source Code

**New files — Gradle bootstrapping** (one-time; required for all future UI features):
```text
gradle/libs.versions.toml           ← add Compose BOM, TV, Hilt, KSP versions + libraries
build.gradle.kts                    ← add kotlin.compose / hilt / ksp plugin classpaths
app/build.gradle.kts                ← add kotlin.compose / hilt / ksp plugins + dependencies
```

**New files — Application entry point**:
```text
app/src/main/kotlin/com/couchraoke/tv/
├── CouchraokeApp.kt                ← @HiltAndroidApp Application class
├── MainActivity.kt                 ← @AndroidEntryPoint; hosts Compose NavHost
└── di/
    └── AppModule.kt                ← @InstallIn(SingletonComponent) — provides SongLibrary, Session, ExoPlayer
```

**New files — Presentation layer**:
```text
app/src/main/kotlin/com/couchraoke/tv/presentation/
└── songlist/
    ├── SongListUiState.kt          ← SongListUiState + SelectPlayersDialogState + ErrorModalState
    ├── SongListViewModel.kt        ← @HiltViewModel; owns all screen logic + preview controller
    ├── SongListScreen.kt           ← top-level @Composable; observes ViewModel state
    ├── components/
    │   ├── SongGrid.kt             ← TvLazyVerticalGrid + SongTile; tag chips
    │   ├── JoinWidget.kt           ← QR code (ZXing) + join code text; non-focusable
    │   ├── MedleyPlaylist.kt       ← scrollable playlist column; Reorder mode
    │   ├── SearchField.kt          ← TV text field + system input dialog bridge
    │   ├── ActionButtons.kt        ← Random Song, Random Duet, Play Medley, Auto Medley
    │   └── SelectPlayersModal.kt   ← full modal: phone dropdowns, difficulty, duet gating
    └── preview/
        └── SongPreviewController.kt ← ExoPlayer wrapper; debounce; lifecycle in ViewModel
```

**New test files**:
```text
app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/
├── SongListViewModelTest.kt        ← [U] core state transitions; search; preview debounce
├── SongFilterTest.kt               ← [U] filteredSongs derivation; edge cases
├── MedleyPlaylistTest.kt           ← [U] add / delete / reorder / clear / auto-medley
└── SelectPlayersGatingTest.kt      ← [U] duet gating rules; start validation; error states
```

---

## Complexity Tracking

*No constitution violations.*

---

## Design Notes

### Two-column layout + DPAD routing

The spec's navigation map requires cross-panel jumps that `TvLazyVerticalGrid` does not produce by default. Implementation approach:

1. Assign `FocusRequester` objects to the leftmost grid tile in each row, the medley playlist column, the search field, and the playlist's edge rows.
2. On each grid tile at the leftmost column, set `Modifier.focusProperties { left = medleyPlaylistFocusRequester }`.
3. On each medley playlist row, set `right = searchFieldFocusRequester`.
4. On Play Medley / Auto Medley buttons, set `right = searchFieldFocusRequester`.
5. Initial focus on screen entry: `LaunchedEffect(Unit) { firstTileFocusRequester.requestFocus() }` (or search field if grid empty).

### Preview debounce

```
SongListViewModel:
  focusedSongIdFlow: MutableStateFlow<String?> = MutableStateFlow(null)

  init {
      focusedSongIdFlow
          .debounce(500)
          .onEach { songId -> previewController.startPreview(songId) }
          .launchIn(viewModelScope)
  }

  fun onSongFocused(songId: String?) { focusedSongIdFlow.value = songId }
```

### Medley playlist clear on navigation

The `SongListViewModel` exposes a `clearMedleyPlaylist()` method. The NavController's `onDestinationChanged` listener (or a `DisposableEffect` on the screen's route) clears the playlist when navigating away to a non-modal destination.

### Select Players phone list

Built by zipping `Session.connectedClientIds` with `Session.displayNames`:
```kotlin
val phones = session.connectedClientIds
    .map { id -> PhoneOption(clientId = id, displayName = session.displayNames[id] ?: id) }
    .sortedBy { it.displayName }
```

### QR code generation

Use `BarcodeEncoder` from `zxing-android-embedded` (already on classpath). Generate a `Bitmap` from the WebSocket URL string in a `LaunchedEffect`. The URL format: `ws://<tv-ip>:<port>/?token=<SessionToken.display(token)>`. The TV's IP is obtained from `NetworkInterface` at session start (data layer concern — `WebSocketServer` already knows the port).

### Grid column count

```kotlin
val columns = if (LocalConfiguration.current.screenWidthDp >= 3840) 4 else 3
TvLazyVerticalGrid(columns = TvGridCells.Fixed(columns), ...)
```

4K detection: `screenWidthDp >= 3840` (4K TV logical pixels at ~10 dpi density assumed in TV emulators; actual threshold may need empirical tuning — mark with `TODO(SPEC: verify 4K threshold on real hardware)`).

### Song start failure handling

When the user presses Start in Select Players:
1. ViewModel attempts to confirm `audioUrl != null`.
2. If null → immediately set `errorModal = ErrorModalState(...)` without navigation.
3. If non-null → emit `PlayerAssignment` navigation event → navigate to Singing screen.
4. Actual unreachable URL errors during playback are handled by the Singing screen (feature 008).

---

## New Dependencies Summary

| Artifact | Version | Already in catalog? |
|---|---|---|
| `androidx.tv:tv-material` | 1.0.0 | No — add |
| `androidx.tv:tv-foundation` | 1.0.0 | No — add |
| Compose BOM `androidx.compose:compose-bom` | `2025.05.01` | No — add |
| `androidx.compose.ui:ui` (BOM-managed) | — | No — add |
| `androidx.compose.foundation:foundation` (BOM) | — | No — add |
| `androidx.lifecycle:lifecycle-viewmodel-compose` (BOM) | — | No — add |
| `com.google.dagger:hilt-android` | 2.56.1 | No — add |
| `com.google.dagger:hilt-android-compiler` (KSP) | 2.56.1 | No — add |
| `androidx.hilt:hilt-navigation-compose` | 1.2.0 | No — add |
| Kotlin Compose plugin (`kotlin.plugin.compose`) | 2.3.10 | No — add |
| KSP plugin | 2.3.10-1.0.31 | No — add |
| Hilt Gradle plugin | 2.56.1 | No — add |
| `media3-exoplayer` | 1.9.2 | ✅ Yes |
| `media3-datasource-okhttp` | 1.9.2 | ✅ Yes |
| `coil-compose` | 3.4.0 | ✅ Yes |
| `coil-network-okhttp` | 3.4.0 | ✅ Yes |
| `zxing-android-embedded` | 4.3.0 | ✅ Yes |
