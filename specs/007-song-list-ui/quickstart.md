# Quickstart: Song List UI (007)

## Build & Run

```bash
# Build the debug APK
./gradlew assembleDebug

# Run all unit tests (includes Roborazzi snapshot verification)
./gradlew testDebugUnitTest

# Generate JaCoCo coverage report + verify thresholds
./gradlew ciUnitTests

# Record new Roborazzi baselines (after adding/changing screenshots)
./gradlew recordRoborazziDebug

# Verify Roborazzi baselines (fails on diff)
./gradlew verifyRoborazziDebug

# Run detekt static analysis
./gradlew detekt
```

## Key Source Locations

| Component | Path |
|-----------|------|
| Main screen | `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt` |
| ViewModel | `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt` |
| UI state | `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListUiState.kt` |
| Components | `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/components/` |
| Preview controller | `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/preview/` |
| Tests | `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/` |
| Screenshot baselines | `app/src/test/snapshots/images/` |
| Roborazzi fixtures | `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/fixtures/` |

## Dependencies (read-only, owned by other features)

| Interface | Location | Feature |
|-----------|----------|---------|
| `SongLibrary` | `domain/library/SongLibrary.kt` | 004 |
| `SongEntry` | `domain/library/SongEntry.kt` | 004 |
| `Session` / `ISession` | `domain/session/Session.kt` | 006 |
| `SessionToken` | `domain/session/SessionToken.kt` | 006 |
| `ConnectionRegistry` | `domain/session/ConnectionRegistry.kt` | 006 |

## Testing Commands

```bash
# Full test suite
./gradlew testDebugUnitTest

# Song list tests only
./gradlew testDebugUnitTest --tests "com.couchraoke.tv.presentation.songlist.*"

# ViewModel tests only
./gradlew testDebugUnitTest --tests "com.couchraoke.tv.presentation.songlist.SongListViewModelTest"

# Screenshot tests only
./gradlew testDebugUnitTest --tests "com.couchraoke.tv.presentation.songlist.SongListScreenStateTest"
```

## Architecture Notes

- **ViewModel**: `SongListViewModel` is the single source of UI state (`StateFlow<SongListUiState>`). All user actions flow through ViewModel functions. No mutable state in Composables.
- **Preview**: Audio preview managed by `SongPreviewController` (ExoPlayer) inside ViewModel. Visual preview pane is driven by `focusedSong` in UI state (sticky behavior).
- **Focus management**: Custom DPAD navigation wired via `Modifier.focusProperties { }`. Back key intercepted via `onPreviewKeyEvent`.
- **DI**: Hilt `@HiltViewModel` + `@Module` in `di/`.
