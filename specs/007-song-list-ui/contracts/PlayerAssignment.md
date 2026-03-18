# Contract: PlayerAssignment

**Owner**: Feature 007 (Song List UI) — produced by Select Players modal
**Consumer**: Feature 008 (Singing Screen) — receives this as navigation argument

---

## Purpose

`PlayerAssignment` is the handoff payload from the Song List screen to the Singing screen. It encodes the complete singer configuration chosen in the Select Players modal.

---

## Shape

```kotlin
data class PlayerAssignment(
    val mode: SelectPlayersMode,           // SingleSong | Medley
    val song: SongEntry?,                  // non-null iff mode=SingleSong
    val medleyPlaylist: List<SongEntry>?,  // non-null iff mode=Medley (immutable snapshot)
    val player1: PhoneOption,              // required
    val player1Difficulty: Difficulty,
    val player2: PhoneOption?,             // null = no second singer
    val player2Difficulty: Difficulty?,    // null iff player2 == null
    val player1Part: DuetPart,             // always P1 unless solo duet part = P2
    val player2Part: DuetPart?,            // null iff player2 == null
)
```

---

## Invariants

1. `mode == SingleSong` → `song != null` and `medleyPlaylist == null`
2. `mode == Medley` → `song == null` and `medleyPlaylist != null` and `medleyPlaylist.isNotEmpty()`
3. `player2 != null` → `player2Difficulty != null` and `player2Part != null`
4. `player2 == null` → `player2Difficulty == null` and `player2Part == null`
5. All `SongEntry` objects in `medleyPlaylist` are an immutable snapshot taken at the moment Start is pressed; the Singing screen MUST NOT read from `SongLibrary` again for this run.
6. For medley play: no duet songs (all entries have `isDuet=false` by `canMedley` definition); therefore `player1Part=P1`, `player2Part=null`.

---

## Protocol side effects triggered by consumer (feature 008)

On receiving `PlayerAssignment`, the Singing screen MUST:
- Call `Session.lockForSong(player1.clientId, player2?.clientId)` to transition session to Locked.
- Send `assignSinger` WebSocket messages to each selected phone (P1, P2 if present) with `udpPort`.
- Fetch `txtUrl` synchronously and parse before starting playback.

---

## Navigation

`PlayerAssignment` is passed as a Compose Navigation argument (Parcelable or serialized) when navigating from the Song List route to the Singing route.
