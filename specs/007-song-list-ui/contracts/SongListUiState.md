# Contract: SongListUiState

**Owner**: Feature 007 — `SongListViewModel`
**Consumer**: Feature 007 — `SongListScreen` Composable

---

## Purpose

`SongListUiState` is the single source of UI truth for the Song List screen. The ViewModel exposes it as `StateFlow<SongListUiState>`. The Composable renders it deterministically. No mutable state lives in the Composable.

---

## Shape (canonical)

```kotlin
data class SongListUiState(
    val allSongs: List<SongEntry> = emptyList(),
    val searchQuery: String = "",
    val filteredSongs: List<SongEntry> = emptyList(), // derived, read-only from UI
    val medleyPlaylist: List<SongEntry> = emptyList(),
    val isReorderingMedleyIndex: Int? = null,
    val selectPlayersDialog: SelectPlayersDialogState? = null,
    val errorModal: ErrorModalState? = null,
    val previewingSongId: String? = null,
    val joinToken: String = "",
    val sessionState: SessionState = SessionState.Open,
    // --- Revision 2 additions (§3.4 wireframe update) ---
    val focusedSong: SongEntry? = null,               // preview pane sticky state
    val isPairingOverlayOpen: Boolean = false,         // Join button overlay
    val currentHint: HintMode? = null,                 // contextual hints bar
    val duplicateMedleyFeedback: Boolean = false,      // "Already in medley" feedback
)
```

---

## Derivation rules

| Derived field | Rule |
|---|---|
| `filteredSongs` | `allSongs.filter { matchesQuery(it, searchQuery) }` — case-insensitive substring across artist/album/title; preserves Artist→Album→Title order |
| `isRandomSongEnabled` (UI-computed) | `filteredSongs.any { it.isValid }` |
| `isRandomDuetEnabled` (UI-computed) | `filteredSongs.any { it.isDuet }` |
| `isPlayMedleyEnabled` (UI-computed) | `medleyPlaylist.isNotEmpty()` |
| `isRandomMedleyEnabled` (UI-computed) | `filteredSongs.count { it.canMedley } >= 2` |

---

## ViewModel API surface (events from UI → ViewModel)

```kotlin
fun onSearchQueryChanged(query: String)       // triggers 150 ms debounce
fun onSongSelected(song: SongEntry)           // opens Select Players
fun onSongLongPressed(song: SongEntry)        // add to medley or show error modal
fun onPlaylistRowSelected(index: Int)         // enter Reorder mode
fun onPlaylistRowLongPressed(index: Int)      // delete row
fun onReorderConfirm(fromIndex: Int, toIndex: Int)
fun onReorderCancel()
fun onPlayMedley()
fun onRandomMedley()
fun onRandomSong()
fun onRandomDuet()
fun onPlayer1Selected(phone: PhoneOption)
fun onPlayer2Selected(phone: PhoneOption?)
fun onPlayer1DifficultyChanged(d: Difficulty)
fun onPlayer2DifficultyChanged(d: Difficulty)
fun onSoloDuetPartChanged(part: DuetPart)
fun onSwapParts()
fun onSelectPlayersStart()                    // validates and emits PlayerAssignment
fun onSelectPlayersCancel()
fun onErrorModalDismissed()
fun onSongFocused(songId: String?)            // drives 500 ms preview debounce + preview pane update
fun onScreenVisible(visible: Boolean)         // stops preview when screen hides; resets focusedSong on re-entry
// --- Revision 2 additions ---
fun onJoinPressed()                           // opens pairing overlay
fun onPairingOverlayDismissed()               // closes pairing overlay
fun onBackPressed(): BackResult               // 4-level cascade: modal → search → filter → exit
fun onFocusZoneChanged(zone: FocusZone)       // updates currentHint based on focused element
```
