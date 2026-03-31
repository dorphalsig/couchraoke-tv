# Research: Song List UI (007)

## R1 — Compose for TV: Focus management and DPAD navigation

**Decision**: Use `androidx.tv:tv-material` + `androidx.tv:tv-foundation` for TV-optimised composables (e.g., `TvLazyVerticalGrid`, `Card`, `Surface`). Override focus traversal with `Modifier.focusProperties { }` where the spec requires non-default directional routing (e.g., leftmost grid tile → medley playlist column).

**Rationale**: Compose for TV ships TV-aware focus traversal out of the box. `focusProperties { left = … }` lets you attach a `FocusRequester` to the target composable and wire the custom jump without fighting the default algorithm. This is the documented pattern for non-grid cross-region navigation.

**Alternatives considered**:
- Plain `androidx.compose.foundation` with no TV extension: rejected — lacks TV-specific surface state (focused/pressed/selected), requires manually re-implementing all visual feedback that TV Material provides.
- View-based XML (`RecyclerView` + `GridLayoutManager`): rejected — constitution mandates Compose for TV.

---

## R2 — Media3 preview playback lifecycle in Compose

**Decision**: Manage a single `ExoPlayer` instance inside `SongListViewModel` as a `StateFlow`-backed `SongPreviewController`. The controller is created lazily on first preview attempt and released in `ViewModel.onCleared()`. Preview start/stop is driven by UI state changes (focused song ID), not by Compose composition events directly, to avoid lifecycle race conditions.

**Rationale**: Keeping the player in ViewModel decouples it from Compose recomposition cycles. The ViewModel survives config changes; the player is not recreated on every recompose. The 500 ms debounce before starting preview is implemented as a `debounce` operator on the `focusedSongId` Flow.

**Alternatives considered**:
- `DisposableEffect` in a Composable: rejected — player leaked on back-stack navigation because the composable may leave composition before `onDispose` is called in certain TV navigation patterns.
- A separate `Service`: rejected — over-engineering for a short in-screen preview.

---

## R3 — Long-press detection in Compose for TV

**Decision**: Use `Modifier.combinedClickable(onLongClick = { … })` for both grid tiles and playlist rows. `combinedClickable` respects the 500 ms threshold from `ViewConfiguration.getLongPressTimeout()` which satisfies the spec's ≥ 500 ms requirement on Android.

**Rationale**: `combinedClickable` is the standard Compose API for long-press and is TV-remote-compatible (works with DPAD OK button). No custom pointer-input logic needed.

**Alternatives considered**:
- `pointerInput(Unit) { detectTapGestures(onLongPress = …) }`: rejected — over-specified, doesn't compose cleanly with `clickable` focus semantics on TV.

---

## R4 — Coil async image loading in Compose for TV

**Decision**: Use Coil 3 (`coil-compose` + `coil-network-okhttp`, already in the catalog) with `AsyncImage`. Configure an `OkHttpClient` that sets `connectTimeout` / `readTimeout` to 2 s for cover image fetches. Use a drawable `placeholder` and `error` fallback from the app resources.

**Rationale**: Coil + OkHttp is already the approved and configured stack (in `build.gradle.kts`). No additional library needed.

**Alternatives considered**:
- Glide: rejected — not in the approved stack (constitution §II).
- Picasso: rejected — same reason.

---

## R5 — ViewModel + Hilt wiring for first Compose screen

**Decision**: Add `hilt-android` + `hilt-compiler` + `hilt-navigation-compose` to the project. Annotate `SongListViewModel` with `@HiltViewModel`. The ViewModel receives `SongLibrary` and `Session` via Hilt constructor injection. A `@Module` in `di/` provides both as singletons scoped to the application.

**Rationale**: Hilt is the constitution-approved DI framework. Adding it now (on the first screen) establishes the DI graph for all future UI features. `hilt-navigation-compose` is required to use `hiltViewModel()` in Compose.

**Alternatives considered**:
- Manual `ViewModelProvider.Factory`: rejected — violates Hilt constitution requirement; creates factory boilerplate that Hilt removes.
- Koin: rejected — not in the approved stack.

---

## R6 — Compose BOM and TV library versions

**Decision**:

| Artifact | Version | Notes |
|---|---|---|
| Compose BOM | `2025.05.01` | Latest stable BOM as of spec date; pins all `androidx.compose.*` versions consistently |
| `androidx.tv:tv-material` | `1.0.0` | Stable GA |
| `androidx.tv:tv-foundation` | `1.0.0` | Stable GA |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | BOM-managed (`2.9.x`) | |
| `com.google.dagger:hilt-android` | `2.56.1` | Latest stable |
| `androidx.hilt:hilt-navigation-compose` | `1.2.0` | Latest stable |
| Kotlin Compose compiler plugin | via `kotlin.plugin.compose` (`2.3.10`) | Kotlin 2.x ships its own Compose compiler plugin |

**Rationale**: BOM-managed Compose avoids version skew. Kotlin 2.x ships the Compose compiler as a first-party plugin (`org.jetbrains.kotlin.plugin.compose`) — no separate `composeOptions.kotlinCompilerExtensionVersion` needed.

---

## R7 — New Gradle additions required (first UI feature)

These additions are one-time bootstrapping required because this is the first Compose screen:

**`gradle/libs.versions.toml` — new entries**:
```toml
[versions]
composeBom = "2025.05.01"
tvCompose = "1.0.0"
hilt = "2.56.1"
hiltNavigationCompose = "1.2.0"

[libraries]
# Compose BOM
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-runtime = { group = "androidx.compose.runtime", name = "runtime" }
# Compose for TV
tv-material = { group = "androidx.tv", name = "tv-material", version.ref = "tvCompose" }
tv-foundation = { group = "androidx.tv", name = "tv-foundation", version.ref = "tvCompose" }
# ViewModel
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose" }
# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.3.10-1.0.31" }
```

**`app/build.gradle.kts` — plugin additions**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.compose)  // add
    alias(libs.plugins.hilt)            // add
    alias(libs.plugins.ksp)             // add (required for Hilt annotation processing)
}
```

**`app/build.gradle.kts` — dependency additions**:
```kotlin
// Compose BOM
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.foundation)
implementation(libs.androidx.compose.runtime)
implementation(libs.tv.material)
implementation(libs.tv.foundation)
implementation(libs.androidx.lifecycle.viewmodel.compose)

// Hilt
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.hilt.navigation.compose)

// Debug (tooling)
debugImplementation(libs.androidx.compose.ui.tooling.preview)
```

**`root/build.gradle.kts` — add plugin classpaths**:
```kotlin
alias(libs.plugins.kotlin.compose) apply false
alias(libs.plugins.hilt) apply false
alias(libs.plugins.ksp) apply false
```

---

## Revision 2 Research (2026-03-31 — §3.4 wireframe update)

The following research items address the new/changed requirements from the spec revision.

---

## R8 — Preview Pane: Blurred Background Cover Image

**Decision**: Use `Modifier.blur()` (RenderEffect-based, API 31+) with a Coil `AsyncImage` for the blurred background fill. On API 28–30, degrade gracefully to a dimmed solid-color background (no blur).

**Rationale**: `Modifier.blur()` is the native Compose API for Gaussian blur and works on all Android TV devices running API 31+ (which is the vast majority). The minSdk 28 fallback is a simple alpha-dimmed background. No third-party blur library is needed.

**Alternatives considered**:
- RenderScript blur: Deprecated since API 31; adds complexity.
- Third-party blur library: Not in the approved stack.
- Custom shader: Over-engineered for a static background fill.

**Implementation pattern**:
```kotlin
Box(modifier = Modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))) {
    // Blurred background (full-bleed cover)
    AsyncImage(
        model = song.coverUrl,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
            .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(24.dp) else Modifier)
            .alpha(0.6f)
    )
    // Centered sharp cover (square, fitted)
    AsyncImage(
        model = song.coverUrl,
        contentScale = ContentScale.Fit,
        modifier = Modifier.align(Alignment.Center).fillMaxHeight(0.8f).aspectRatio(1f)
    )
    // Song info overlay below the pane (title, artist, tag chips)
}
```

---

## R9 — Join Button + Pairing Overlay (Replacing Always-Visible QR)

**Decision**: The `[ JOIN ]` button is a standard TV-material `Button` composable in the header row. Pressing OK opens a Compose `Dialog` containing the QR code (ZXing) and join code text. The dialog dismisses on Back.

**Rationale**: A `Dialog` is the correct overlay primitive — it blocks input to the underlying screen and dismisses on Back natively. The spec says the overlay does not affect the navigation stack (modal behavior), which `Dialog` satisfies.

**Alternatives considered**:
- Full-screen route push: Wrong — modals must not push onto the nav stack per spec FR-033.
- `AlertDialog`: Too restrictive for custom QR layout; can't easily center a large bitmap with text below.

**Implementation**:
- Reuse existing ZXing QR generation logic from old `JoinWidget.kt`.
- QR bitmap generated in ViewModel (deterministic for Roborazzi tests).
- State: `SongListUiState.isPairingOverlayOpen: Boolean`.
- ViewModel: `onJoinPressed()` sets `isPairingOverlayOpen = true`. `onPairingOverlayDismissed()` sets it to `false`.

---

## R10 — Random Medley Selection Algorithm

**Decision**: Filter `filteredSongs` to `canMedley=true`, shuffle (with injectable `Random` for test determinism), take `min(5, size)`. Requires `size >= 2` to be enabled. The selected songs replace any existing medley playlist and Select Players opens immediately.

**Rationale**: Simple and spec-compliant. Seeded `Random` makes the selection deterministic in tests.

**Alternatives considered**:
- Weighted random (less-recently-played): No playback history exists. Unnecessary complexity.
- User confirmation before overwriting playlist: Spec doesn't require it.

**Implementation**:
- `SongListViewModel.onRandomMedley()` validates ≥ 2 eligible, selects, replaces medley playlist, opens Select Players.
- Button enabled: `filteredSongs.count { it.canMedley } >= 2`.

---

## R11 — Preview Pane Sticky State

**Decision**: `SongListUiState` gains `focusedSong: SongEntry?` updated on grid tile focus. When focus leaves the grid, `focusedSong` is NOT cleared (sticky). It resets to null only on screen re-entry.

**Rationale**: Sticky behavior means the visual pane always shows something after first focus. Audio preview still stops on focus-leave (FR-027/FR-049). The ViewModel distinguishes visual state (sticky) from audio state (stop on leave).

---

## R12 — Duplicate Medley Prevention (Per-Tile Tracking)

**Decision**: ViewModel tracks `lastDuplicateAttemptTileId: String?`. On long-press of a song already in the playlist: if `lastDuplicateAttemptTileId != song.songId` → silently ignore and set the ID; if already set → show brief feedback ("Already in medley"). Clear on focus change.

**Rationale**: Matches the user's clarification: first attempt silent, repeated on same tile shows feedback. Per-tile tracking (not per-song-name) means re-focusing the same tile resets the counter.

---

## R13 — Back Key Cascade

**Decision**: Implement as `SongListViewModel.onBackPressed(): BackResult` returning an enum. The composable intercepts Back via `onPreviewKeyEvent` and acts on the result.

**Rationale**: Centralizing in ViewModel makes the 4-level cascade fully unit-testable without UI rendering.

**Levels**: (1) Close modal → (2) Move focus to Search → (3) Clear filter → (4) Exit app.

---

## R14 — Contextual Hints Bar

**Decision**: A `ContextualHintsBar` composable at screen bottom. Driven by `SongListUiState.currentHint: HintMode?` enum set by the ViewModel based on which element has focus.

**Enum values**:
- `SongGridFocused` → `OK = Sing   Long-Press OK = Add to Medley`
- `MedleyPlaylistFocused` → `OK = Reorder   Long-Press OK = Delete`
- `ReorderActive` → `Up/Down = Move   OK = Accept   Back = Cancel`
- `null` → hints bar hidden

---

## R15 — Header Layout with Search Field

**Decision**: The header row is a single `Row` composable spanning full width, containing (left-to-right): join code `Text`, `SearchField` (expanding weight), `Button("JOIN")`, `IconButton(gear)`. Below it, a secondary `Row` holds the three Random action buttons.

**Rationale**: Matches the updated wireframe. The search field takes remaining horizontal space between the join code text and the action buttons.

**DPAD focus**: Search left → Join button, Search right → Settings button. Wired via `focusProperties`.
