# Data Model: Song List UI (007)

## Existing entities consumed (read-only)

### `SongEntry` *(domain/library — feature 004)*
All fields are used by this feature. Key fields for the Song List screen:

| Field | Type | UI use |
|---|---|---|
| `songId` | `String` | Stable key for Compose `LazyGrid` items and medley list |
| `title` | `String` | Tile label; search match field |
| `artist` | `String` | Tile label; sort key; search match field |
| `album` | `String?` | Sort key; search match field |
| `coverUrl` | `String?` | Coil `AsyncImage` source; null → placeholder |
| `audioUrl` | `String?` | ExoPlayer preview source; null → suppress preview |
| `previewStartSec` | `Double` | Preview seek position (0.0 → seek to 0) |
| `isDuet` | `Boolean` | `D` chip; Random Duet filter; duet gating in Select Players |
| `hasRap` | `Boolean` | `R` chip |
| `hasVideo` | `Boolean` | `V` chip |
| `hasInstrumental` | `Boolean` | `I` chip |
| `canMedley` | `Boolean` | `M` chip; long-press Add-to-Medley gate; Auto Medley eligibility |

### `Session` *(domain/session — feature 006)*

| Member | Type | UI use |
|---|---|---|
| `state` | `SessionState` (Open/Locked/Ended) | Observed via `events` Flow |
| `token` | `String` | QR code payload and join code text |
| `displayNames` | `Map<String, String>` | `clientId → displayName` for Select Players dropdown |
| `connectedClientIds` | `Set<String>` | Determines which phones appear in Select Players |
| `events` | `SharedFlow<SessionEvent>` | Triggers library/roster refresh in ViewModel |

### `SessionToken` *(domain/session — feature 006)*

| Method | Purpose |
|---|---|
| `SessionToken.display(token)` | Formats token as `ABCD-EFGH` for header and QR payload |

---

## New entities owned by this feature

### `SongListUiState` *(presentation/songlist)*

The single immutable UI state snapshot vended by `SongListViewModel`. Compose observes this as a `StateFlow`.

```
SongListUiState {
    allSongs: List<SongEntry>             // full library snapshot, sorted
    searchQuery: String                    // current search text (empty = no filter)
    filteredSongs: List<SongEntry>         // derived: allSongs filtered by searchQuery
    medleyPlaylist: List<SongEntry>        // transient medley list, order matters
    isReorderingMedleyIndex: Int?          // non-null when a row is in Reorder mode
    selectPlayersDialog: SelectPlayersDialogState?  // null = modal closed
    errorModal: ErrorModalState?           // null = no error shown
    previewingSongId: String?              // song currently being previewed (or null)
    joinToken: String                      // displayed as join code + QR payload
    sessionState: SessionState             // Open / Locked / Ended
}
```

**Invariants**:
- `filteredSongs` is always derived from `allSongs` + `searchQuery`; never set independently.
- `medleyPlaylist` is always a subset of `allSongs` (by reference equality on `songId`). After a library refresh, any entry whose `songId` is no longer in `allSongs` is retained in the playlist (stale — surfaces as error at playback time per spec).
- `selectPlayersDialog` and `errorModal` are mutually exclusive (at most one shown at a time).

---

### `SelectPlayersDialogState` *(presentation/songlist)*

Holds the current state of the Select Players modal.

```
SelectPlayersDialogState {
    mode: SelectPlayersMode               // SingleSong | Medley(count: Int)
    song: SongEntry?                      // null for medley mode
    availablePhones: List<PhoneOption>    // connected phones in displayName order
    player1Selection: PhoneOption?        // required; null = not yet chosen
    player1Difficulty: Difficulty         // Easy | Medium | Hard (default: Medium)
    player2Selection: PhoneOption?        // optional
    player2Difficulty: Difficulty         // only relevant when player2Selection != null
    soloPartSelection: DuetPart?          // P1 | P2; only relevant for solo duet play
    isLoading: Boolean                    // true while fetching txt (single-song only)
}
```

**`PhoneOption`**:
```
PhoneOption {
    clientId: String
    displayName: String
}
```

**`Difficulty`**: enum — `Easy`, `Medium`, `Hard`

**`DuetPart`**: enum — `P1`, `P2`

**`SelectPlayersMode`**: sealed class — `SingleSong(song: SongEntry)`, `Medley(count: Int)`

---

### `ErrorModalState` *(presentation/songlist)*

```
ErrorModalState {
    title: String          // e.g. "ERROR"
    bodyLine1: String      // e.g. "This song can't be played."
    bodyLine2: String?     // optional second line
}
```

---

### `PlayerAssignment` *(presentation/songlist — produced, consumed by Singing screen)*

Produced by Select Players on Start. Passed to the Singing screen feature (008) as the navigation argument.

```
PlayerAssignment {
    mode: SelectPlayersMode                  // SingleSong | Medley
    song: SongEntry?                         // null for medley
    medleyPlaylist: List<SongEntry>?         // null for single song
    player1: PhoneOption
    player1Difficulty: Difficulty
    player2: PhoneOption?
    player2Difficulty: Difficulty?
    player1Part: DuetPart                    // P1 (or P2 if solo duet part selected)
    player2Part: DuetPart?                   // P2 (or null if no Player 2)
}
```

---

## State transitions: Medley playlist lifecycle

```
Song List Screen enters
  → MedleyPlaylist = []

Long-press canMedley=true tile
  → MedleyPlaylist += song (appended)

Long-press canMedley=false tile
  → show blocking modal (no state change)

OK on playlist row
  → enter Reorder mode for that row

OK / Back in Reorder mode
  → confirm / cancel reorder

Long-press on playlist row
  → delete row immediately

Auto Medley (Random 5)
  → MedleyPlaylist = sample(filteredSongs where canMedley=true, max=5)

Song List Screen exits to Settings / Singing / Results
  → MedleyPlaylist = [] (cleared)

Modal overlay opened/closed (Select Players, error)
  → MedleyPlaylist unchanged
```

---

## State transitions: SelectPlayersDialog

```
OK on song tile or Random action
  → SelectPlayersDialogState(mode=SingleSong, song=it, availablePhones=connectedPhones)

Play Medley (non-empty playlist)
  → SelectPlayersDialogState(mode=Medley(n), song=null, ...)

Start pressed (validation passes)
  → emit PlayerAssignment → navigate to Singing screen

Cancel / Back
  → selectPlayersDialog = null (modal closed, return to Song List)

Start pressed, audioUrl=null
  → selectPlayersDialog = null, errorModal = ErrorModalState(song-unavailable)
```

---

## SongLibrary refresh trigger

`SongListViewModel` observes `Session.events: SharedFlow<SessionEvent>`. On `PhoneConnected`, `PhoneDisconnected`, or `PhoneReconnected`, it calls `SongLibrary.getSortedSongs()` and updates `allSongs` in the UI state (which re-derives `filteredSongs`).

---

## Revision 2 additions (2026-03-31 — §3.4 wireframe update)

### New fields in `SongListUiState`

| Field | Type | Purpose |
|---|---|---|
| `focusedSong` | `SongEntry?` | Song whose info is displayed in the preview pane. Sticky: set on grid focus, never cleared on focus-leave. Reset to null on screen re-entry. |
| `isPairingOverlayOpen` | `Boolean` | Whether the Join pairing overlay (QR + join code) is shown. Default `false`. |
| `currentHint` | `HintMode?` | Drives the contextual hints bar at screen bottom. `null` = hidden. |
| `duplicateMedleyFeedback` | `Boolean` | `true` briefly when the user long-presses a tile already in the medley for the second consecutive time on the same focused tile. Resets on focus change. |

### New enum: `HintMode`

```
enum HintMode {
    SongGridFocused,          // "OK = Sing   Long-Press OK = Add to Medley"
    MedleyPlaylistFocused,    // "OK = Reorder   Long-Press OK = Delete"
    ReorderActive,            // "Up/Down = Move   OK = Accept   Back = Cancel"
}
```

### New enum: `BackResult`

```
enum BackResult {
    ClosedModal,     // a modal/overlay was open and has been closed
    MovedToSearch,   // focus was in grid/left panel, moved to search field
    ClearedFilter,   // search filter was active, now cleared
    ExitApp,         // no filter active and focus already in header → exit
}
```

### Updated derivation rules in `SongListUiState`

| Derived field | Rule |
|---|---|
| `isRandomMedleyEnabled` (UI-computed) | `filteredSongs.count { it.canMedley } >= 2` |

### State transitions: Pairing overlay

```
Join button pressed (OK)
  → isPairingOverlayOpen = true

Back / Cancel in overlay
  → isPairingOverlayOpen = false (focus returns to Song List)
```

### State transitions: Preview pane

```
Grid tile gains focus
  → focusedSong = the focused SongEntry (preview pane updates)
  → after 500 ms debounce: audio preview starts (existing behavior)

Focus leaves grid (to Search, buttons, playlist, etc.)
  → focusedSong UNCHANGED (sticky — pane retains last song info)
  → audio preview stops (existing FR-027 behavior)

Screen re-entry (from Settings, Singing, Results)
  → focusedSong = null (pane shows placeholder: app logo on dimmed bg)
```

### Updated state transitions: Medley playlist (duplicate handling)

```
Long-press canMedley=true tile, song NOT in playlist
  → MedleyPlaylist += song (appended)

Long-press canMedley=true tile, song IS in playlist (1st attempt on this tile)
  → silently ignored; lastDuplicateAttemptTileId = song.songId

Long-press canMedley=true tile, song IS in playlist (2nd+ attempt on same tile)
  → duplicateMedleyFeedback = true (shown briefly, e.g., "Already in medley")

Focus moves to a different tile
  → lastDuplicateAttemptTileId = null; duplicateMedleyFeedback = false

Random Medley pressed
  → MedleyPlaylist = sample(filteredSongs where canMedley=true, min(5, size))
  → Open Select Players with mode=Medley(n)
```
