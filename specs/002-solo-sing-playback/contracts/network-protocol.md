# Contract: Network Protocol and Library Fetch

## NetworkController

Planned FQCN: `com.couchraoke.tv.data.network.NetworkController`

```kotlin
interface NetworkController {
    val connectedPhones: StateFlow<List<ConnectedPhone>>
    val phoneEvents: SharedFlow<PhoneEvent>

    fun start(udpPort: Int, wsPort: Int)
    fun stop()
    fun kickPhone(clientId: String)

    fun sendSessionState(phoneId: String)
    fun broadcastPlaybackState(message: PlaybackStateMessage)
    fun sendAssignSinger(phoneId: String, message: AssignSingerMessage)
    fun sendError(phoneId: String, code: String, message: String)

    suspend fun sendPing(phoneId: String): PongResponse
    fun sendClockAck(phoneId: String, ack: ClockAckMessage)

    suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>>
    suspend fun fetchTxt(url: String): Result<ByteArray>
}
```

Iteration 1 excludes `pitchFrames` exposure and UDP frame processing, even though the fixed-size UDP transport contract is preserved for later iterations.

## ConnectedPhone

```kotlin
data class ConnectedPhone(
    val clientId: String,
    val connectionId: UShort,
    val deviceName: String,
    val httpPort: Int,
    val ipAddress: String
)
```

Rules:
- `connectionId` is assigned by the TV after a valid `hello` and changes on reconnect.
- `deviceName` is the display label used in Select Players.

## PhoneEvent

```kotlin
sealed class PhoneEvent {
    data class Connected(val phone: ConnectedPhone) : PhoneEvent()
    data class Disconnected(val clientId: String, val wasAssignedSinger: Boolean) : PhoneEvent()
    data class Reconnected(val clientId: String, val newConnectionId: UShort) : PhoneEvent()
}
```

Rules:
- Required singer disconnect during countdown returns to Select Players with the `DISCONNECTED` modal.
- Required singer disconnect during playback enters DisconnectPaused behavior.
- Spectator disconnect MUST NOT auto-pause.

## WebSocket Join Contract

URL:

```text
ws://<tv-ip>:<wsPort>/?token=<sessionToken>
```

Rules:
- Missing/incorrect token sends `error(code="invalid_token")` and closes.
- Successful `hello` assigns `connectionId` and returns `sessionState`.
- While session is Locked, new joins receive `error(code="session_locked")`.

## mDNS Advertisement

| Field | Value |
|---|---|
| Service type | `_karaoke._tcp` |
| Instance name | `KaraokeTV-<last4>` |
| Port | WebSocket server port |
| TXT `code` | Full join code, uppercase, no hyphens |
| TXT `v` | `1` |

Rules:
- Use jmDNS.
- Acquire multicast lock before advertisement.
- Request local-network permission on Android versions that require it before mDNS/WebSocket/peer HTTP work.

## SongEntry HTTP Manifest

```kotlin
data class SongEntry(
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val genre: String?,
    val isDuet: Boolean,
    val hasRap: Boolean,
    val hasVideo: Boolean,
    val hasInstrumental: Boolean,
    val canMedley: Boolean,
    val medleySource: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val startSec: Float,
    val previewStartSec: Float,
    val txtUrl: String,
    val audioUrl: String,
    val videoUrl: String?,
    val coverUrl: String?,
    val backgroundUrl: String?
)
```

Rules:
- Required fields follow Appendix B `SongEntry` schema.
- Invalid entries are rejected before entering `LibraryManager.songs`.
- TV streams all URLs directly; no remote asset persistence.

## AssignSingerMessage

```kotlin
data class AssignSingerMessage(
    val sessionId: String,
    val songInstanceSeq: UInt,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val startMode: StartMode,
    val countdownMs: Int?,
    val stopAtLyricsTimeMs: Long,
    val udpPort: Int,
    val songTitle: String,
    val songArtist: String
)

enum class StartMode {
    Countdown, // wire value: "countdown"
    Live, // wire value: "live"
}
```

Rules:
- Serialized JSON MUST include common envelope fields `type="assignSinger"` and `protocolVersion=1`; implementations may model these as constants rather than caller-supplied constructor fields.
- Iteration 1 sends only `P1` to the selected singer phone.
- TV MUST NOT send `assignSinger` to non-selected devices.
- TV obtains at least one valid clock-sync sample before sending/start.
- `stopAtLyricsTimeMs` MUST be the finalized value computed after `PlaybackEvent.Prepared`: parsed `#END` when present and positive, otherwise prepared audio duration.
- `assignSinger` MUST be sent after `Prepared` and before `PlaybackIntent.Play`.
- `udpPort` remains required by protocol, but UDP pitch-frame processing is later scope.

## PlaybackStateMessage

```kotlin
data class PlaybackStateMessage(
    val sessionId: String,
    val songInstanceSeq: UInt,
    val revision: Int,
    val state: PlaybackStateValue,
    val lyricsTimeMs: Long,
    val stopAtLyricsTimeMs: Long,
    val countdownRemainingMs: Int?,
    val reason: PlaybackStateReason,
    val tsTvMs: Long?
)
```

Rules:
- Constructed only by PlaybackCoordinator.
- Emitted on countdown, playing, paused, stopped, resume, seek, and reconnect paths.
- `stopAtLyricsTimeMs` MUST be the finalized value computed after `PlaybackEvent.Prepared`: parsed `#END` when present and positive, otherwise prepared audio duration.
- Countdown/playing playback-state emission for song start MUST occur after `Prepared` and before `PlaybackIntent.Play`.
- Reasons in Iteration 1: `""`, `user_pause`, `singer_disconnected`, `song_end`, `user_quit`, `restart`.

## Clock Sync Gate

```kotlin
suspend fun NetworkController.sendPing(phoneId: String): PongResponse
fun NetworkController.sendClockAck(phoneId: String, ack: ClockAckMessage): Unit
```

Iteration 1 rule:
- Before countdown/live playback, each assigned singer must have one valid clock-sync sample.
- Full F21 fixture coverage remains out of Iteration 1 scope.

## Explicit Non-Scope

- UDP `pitchFrame` listener, validation, jitter buffer, live pitch cursor, and score updates are Iteration 2.
- Fixed-size UDP frame format remains unchanged for later work.
