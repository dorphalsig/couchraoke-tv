# Implementation Plan: Song List UI (Landing Screen) — Revision 2

**Branch**: `007-song-list-ui` | **Date**: 2026-03-31 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/007-song-list-ui/spec.md` (revised 2026-03-31)

**Note**: This is a **revision** of the original plan. The spec was updated to reflect §3.4 wireframe changes: Join button + modal (replacing always-visible QR), preview pane, Random Medley in scope, header restructure, and expanded DPAD navigation.

## Summary

Implement the Song List landing screen for Couchraoke TV. The screen is the app's entry point and consists of: a header row (join code, search, Join button, Settings stub), a secondary action bar (Random Song / Random Duet / Random Medley), a two-column body (left: 16:9 preview pane + medley playlist; right: song grid), a contextual hints bar, and modal overlays (Select Players, pairing overlay, error dialogs). The screen reads from SongLibrary (feature 004) and Session/SessionToken/ConnectionRegistry (feature 006), but does not modify them. All new components use Jetpack Compose for TV with Hilt DI, and tests use JUnit4 + Roborazzi on Robolectric.

## Technical Context

**Language/Version**: Kotlin 2.3.10 / Java 11 (Android minSdk 28, compileSdk 36)
**Primary Dependencies**: Jetpack Compose for TV (tv-material 1.0.0), Compose BOM 2025.05.01, lifecycle-viewmodel-compose, Hilt 2.59.1, hilt-navigation-compose 1.2.0, Media3 ExoPlayer 1.9.2, Coil 3.4.0, ZXing 4.3.0, kotlinx-coroutines 1.9.0
**Storage**: In-memory only — SongLibrary (session-scoped), Session (session-scoped), MedleyPlaylist (screen-visit-scoped)
**Testing**: JUnit4 + kotlinx-coroutines-test + Roborazzi 1.59.0 + Robolectric 4.14 + Compose UI Test JUnit4
**Target Platform**: Android TV (API 28+, 1080p primary, 4K secondary)
**Project Type**: Android TV app (single module)
**Performance Goals**: Grid filter ≤ 300 ms; preview audio start ≤ 600 ms after 500 ms focus-hold; QR scannable from 3 m on 55" TV
**Constraints**: LAN-only; no cloud services; composition must not ANR on TV remote input latency
**Scale/Scope**: Typical library 50–500 songs across 1–4 phones; medley playlist ≤ ~20 entries

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. LAN-only operation, TV as authoritative host | ✅ Pass | Song assets stream from phones via LAN HTTP. QR encodes WS URL on LAN. No cloud. |
| II. Approved technology stack | ✅ Pass | All deps (Compose TV, Hilt, Media3, Coil, ZXing, Ktor) are already on the classpath. No new libraries introduced. |
| III. Clean architecture boundaries | ✅ Pass | ViewModel is single source of UI state. Domain interfaces (SongLibrary, Session, SessionToken) stay in domain/. Presentation code in presentation/songlist/. |
| IV. Streaming & performance contracts | ✅ Pass | Preview uses Media3 ExoPlayer for LAN HTTP streaming. No local persistence of remote assets. |
| V. Testing & quality gates | ✅ Pass | JUnit4-only. Roborazzi for all screen states (VR-001, 14 baselines). Coverage ≥ 80%/60%. StandardTestDispatcher. |
| V. Roborazzi screen state + nav path + interaction coverage | ✅ Pass | VR-001 covers all 14 screen states. Pairing overlay open/dismiss, Select Players open/dismiss, error modal open/dismiss captured. |
| VI. Inter-task contracts published before dependent tasks | ✅ Pass | Contracts for SongListUiState, PlayerAssignment, and SongListNavigation already exist. Will be updated in Phase 1 for new deltas. |
| VII. Branch hygiene | ✅ Pass | Branch renamed to `[✓] 007-song-list-ui` after merge. |

## Project Structure

### Documentation (this feature)

```text
specs/007-song-list-ui/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   ├── SongListUiState.md     # Updated for preview pane, Join overlay, Random Medley
│   ├── PlayerAssignment.md    # Unchanged
│   └── SongListNavigation.md  # Updated for Join button overlay
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/couchraoke/tv/
├── presentation/songlist/
│   ├── SongListScreen.kt              # Main screen composable (header, body, hints bar)
│   ├── SongListViewModel.kt           # UI state management, business logic
│   ├── SongListUiState.kt             # State data class
│   ├── SongFilter.kt                  # Search filtering logic
│   ├── PlayerAssignment.kt            # Handoff to Singing screen
│   ├── components/
│   │   ├── HeaderBar.kt               # NEW: join code + search + Join + Settings
│   │   ├── ActionButtons.kt           # Updated: + Random Medley button
│   │   ├── PreviewPane.kt             # NEW: 16:9 song preview pane
│   │   ├── JoinWidget.kt              # RENAMED → PairingOverlay.kt (or repurposed)
│   │   ├── SongGrid.kt                # Song grid with tiles
│   │   ├── SongTile.kt                # Individual tile
│   │   ├── MedleyPlaylist.kt          # Playlist with reorder/delete
│   │   ├── SelectPlayersModal.kt      # Player assignment modal
│   │   ├── SearchField.kt             # Inline search (may merge into HeaderBar)
│   │   └── ContextualHintsBar.kt      # NEW: bottom hints bar
│   └── preview/
│       ├── ISongPreviewController.kt  # Preview interface
│       └── SongPreviewController.kt   # ExoPlayer-based audio preview
├── domain/library/
│   ├── SongLibrary.kt                 # Interface (read-only from this feature)
│   └── SongEntry.kt                   # Data class (read-only from this feature)
├── domain/session/
│   ├── Session.kt                     # Session interface (read-only from this feature)
│   └── SessionToken.kt                # Token generation (read-only from this feature)
└── di/
    └── SongListModule.kt              # Hilt module for this feature's bindings

app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/
├── SongListViewModelTest.kt           # Updated for new behaviors
├── SongFilterTest.kt                  # Existing
├── MedleyPlaylistTest.kt             # Updated: duplicate handling
├── SelectPlayersGatingTest.kt        # Existing
├── SongPreviewControllerTest.kt      # Existing
├── SongListScreenStateTest.kt        # Roborazzi: Updated for new baselines
├── FakeSession.kt                    # Test doubles
├── FakeSongLibrary.kt                # Test doubles
└── fixtures/
    └── SongListScreenFixtures.kt     # Updated for new screen states
```

**Structure Decision**: Single Android module (existing). All new code goes under `presentation/songlist/` following the established pattern. No new modules needed.

## Complexity Tracking

No constitution violations to justify.

## Changes Required (Delta from Existing Implementation)

The following summarizes the implementation work needed to bring the existing codebase in line with the revised spec:

### New Components
1. **PreviewPane.kt** — 16:9 composable in left panel. Shows cover (centered + blurred bg), title, artist, tag chips. Sticky behavior. Placeholder state (app logo on dimmed bg).
2. **PairingOverlay.kt** — Modal dialog with QR code (320dp+ min) + join code. Replaces always-visible JoinWidget in left panel.
3. **HeaderBar.kt** — Full-width header: join code text (left), Search field, `[ JOIN ]` button, `[ ⚙ SETTINGS ]` button (right).
4. **ContextualHintsBar.kt** — Bottom bar with dynamic hints based on focused element.

### Modified Components
5. **SongListScreen.kt** — Restructure layout: header → action bar → two-column body → hints bar.
6. **SongListViewModel.kt** — Add: Random Medley logic, duplicate medley prevention (per-tile tracking with escalating feedback), Join overlay state, preview pane state (focused song sticky), Back key cascade (grid/left → search → filter → exit).
7. **SongListUiState.kt** — Add: `focusedSong: SongEntry?`, `isPairingOverlayOpen: Boolean`, `isRandomMedleyEnabled: Boolean`, `duplicateMedleyFeedback: Boolean`.
8. **ActionButtons.kt** — Add Random Medley button, disabled+greyed-out styling for all three.
9. **MedleyPlaylist.kt** — Duplicate prevention logic.
10. **JoinWidget.kt** → Repurpose or replace with PairingOverlay.

### Navigation Changes
11. **FR-009 DPAD map** — Add Join button + Settings button nav. Left-panel entry target priority.
12. **FR-015 Back cascade** — 4-level: close modal → move to search → clear filter → exit.

### Test Updates
13. **Roborazzi baselines** — 2 new states: pairing overlay, preview pane with song info.
14. **SongListViewModelTest** — Random Medley, duplicate medley, Back cascade, Join overlay toggle, preview pane state.
15. **SongListScreenFixtures** — New fixtures for preview pane, pairing overlay, Random Medley disabled/enabled states.
