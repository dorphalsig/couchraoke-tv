# Contract: PlaybackCoordinator

## PlaybackCoordinator

Planned FQCN: `com.couchraoke.tv.domain.playback.PlaybackCoordinator`

```kotlin
interface PlaybackCoordinator {
    val phase: StateFlow<GamePhase>
    val playbackIntents: SharedFlow<PlaybackIntent>
    val activeSong: StateFlow<ActiveSongState?>

    suspend fun startSong(selection: SongStartSelection)
    fun onPlaybackEvent(event: PlaybackEvent)
    fun pause()
    fun resume()
    suspend fun restart()
    fun quitToSongList()
}
```

## Producer / Consumer

Producer:
- PlaybackCoordinator owns host game phase, `songInstanceSeq`, selected singer assignment, session lock/open behavior, playback-state messages, and song-end routing.

Consumers:
- SongListViewModel / SelectPlayers modal call `startSong`.
- SingingViewModel calls pause/resume/restart/quit and observes `phase`, `activeSong`, and `playbackIntents`.
- PlaybackController reports `PlaybackEvent` values.
- NetworkController receives `assignSinger`, `sessionState`, and `playbackState` messages from coordinator.

## SongStartSelection

```kotlin
data class SongStartSelection(
    val songId: String,
    val playerPhoneId: String,
    val playerId: PlayerId = PlayerId.P1,
    val difficulty: Difficulty,
    val countdownEnabled: Boolean,
    val countdownSeconds: Int
)
```

Rules:
- Iteration 1 supports exactly one selected singer, P1.
- Selected song must be a normal solo-capable song. Duet and medley execution are disabled.

## GamePhase

```kotlin
sealed class GamePhase {
    data object Idle : GamePhase()
    data class Loading(val songId: String) : GamePhase()
    data class Countdown(val remainingMs: Int) : GamePhase()
    data object Playing : GamePhase()
    data object Paused : GamePhase()
    data class DisconnectPaused(val disconnectedPlayer: PlayerId) : GamePhase()
}
```

Iteration 1 transitions:

| From | To | Trigger |
|---|---|---|
| Idle | Loading | User starts song from Select Players |
| Loading | Countdown | Playback prepared, clock-sync sample valid, assignSinger sent, countdown enabled |
| Loading | Playing | Playback prepared, clock-sync sample valid, assignSinger sent, countdown disabled |
| Loading | Idle | Fetch/parse/audio/playback prep error |
| Countdown | Playing | Countdown reaches 0 |
| Countdown | Idle | Required singer disconnects |
| Playing | Paused | User presses Back |
| Playing | DisconnectPaused | Required singer WebSocket drops |
| Playing | Idle | Playback reaches stop boundary / PlaybackEvent.Ended |
| Paused | Playing | User selects Resume |
| Paused | Loading | User confirms Restart |
| Paused | Idle | User confirms Quit |
| DisconnectPaused | Playing | Reconnect + resume/continue |
| DisconnectPaused | Idle | User confirms Quit |

Note: The future full FSM includes `Stopped` and `Results`; Iteration 1 returns directly to `Idle`/Song List on normal end because Results is out of scope.

## Start Song Sequence

1. Resolve selected `IndexedSong` from `LibraryManager.getSong(songId)`.
2. Resolve selected `ConnectedPhone` from `NetworkController.connectedPhones`.
3. Increment `songInstanceSeq`.
4. Fetch song TXT through `NetworkController.fetchTxt(song.txtUrl)`.
5. Parse with `UsdxParser.parse(song.songId, txtBytes)`.
6. Build `SingingRenderModel` with static note targets and sentence-paged lyrics.
7. Emit `PlaybackIntent.Prepare(audioUrl, videoUrl, videoGapSec, startSec)`.
8. Wait for `PlaybackEvent.Prepared(effectivePlaybackDurationMs)`.
9. Compute `stopAtLyricsTimeMs` from `#END` if present, otherwise prepared effective duration.
10. Obtain one valid clock-sync sample for the selected singer.
11. Send `AssignSingerMessage` to selected phone only.
12. Lock session and emit playback state for countdown or playing.
13. Countdown if enabled; otherwise emit `PlaybackIntent.Play` immediately.
14. On `PlaybackEvent.Ready(songStartTvMs)`, call `ScoringEngine.setSongStart(songStartTvMs)` only as a gate compatibility call; no note finalization/scoring is in scope.

## Pause / Resume / Restart / Quit

- `pause()`: emits playback pause, broadcasts paused playbackState with `reason=user_pause`.
- `resume()`: resumes playback and broadcasts playing playbackState.
- `restart()`: resets per-song state, increments `songInstanceSeq`, re-sends assignSinger, and restarts from `startSec`.
- `quitToSongList()`: stops playback, sends `sessionState.inSong=false`, opens session, and returns to Song List.

## Error Handling

- TXT fetch/parse/start failure: return to Song List and show start-failure modal.
- Playback error: stop playback/scoring placeholders, return to Song List, show blocking error modal, session Open.
- Required singer disconnect during countdown: cancel and return to Select Players with `DISCONNECTED` modal.
- Spectator disconnect: no auto-pause.

## Explicit Non-Scope

- Real scoring, note finalization, Results screen, live pitch, UDP pitch-frame ingestion, duet execution, and medley execution.
