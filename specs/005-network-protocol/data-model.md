# Data Model: Network Protocol (005)

## Package Layout

```
com.couchraoke.tv.
  domain/
    network/
      protocol/
        ProtocolMessages.kt       # All control message data classes
        ControlMessageCodec.kt    # JSON encode/decode via kotlinx.serialization
      pitch/
        PitchFrame.kt             # 16-byte frame model
        PitchFrameCodec.kt        # encode/decode (little-endian ByteArray ↔ PitchFrame)
      clock/
        ClockSyncEngine.kt        # NTP-lite: ping schedule, clockAck dispatch
    session/
      ISessionGate.kt             # Interface — implemented by feature 006
      ConnectionRegistry.kt       # connectionId counter + active connection map
      SessionToken.kt             # join code generation, formatting, normalization
  data/
    network/
      WebSocketServer.kt          # Ktor CIO: handshake, error dispatch, message routing
      UdpPitchReceiver.kt         # DatagramSocket: bind, receive loop, validate, route
      ManifestFetcher.kt          # OkHttp: GET /manifest.json → List<ManifestEntry>
      MdnsAdvertiser.kt           # JmDNS: multicast lock, service register/unregister
  res/xml/
    network_security_config.xml   # Cleartext traffic for RFC-1918 addresses
```

---

## Domain Entities

### Control Messages (`domain/network/protocol/ProtocolMessages.kt`)

All messages carry a common envelope. `type` and `protocolVersion` are required on every message.

```kotlin
// Common envelope fields (included on every message)
// type: String, protocolVersion: Int = 1, tsTvMs: Long? = null  (JSON "number"; Long serializes as integer — valid JSON number per B.2.x schemas)

@Serializable data class HelloMessage(
    val type: String = "hello",
    val protocolVersion: Int,
    val clientId: String,           // stable UUID, minLength 8
    val deviceName: String,
    val appVersion: String,
    val httpPort: Int,              // 1024..65535
    val capabilities: Capabilities,
    val tsTvMs: Long? = null,
)

@Serializable data class Capabilities(val pitchFps: Int? = null)

@Serializable data class SessionStateMessage(
    val type: String = "sessionState",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val slots: SlotMap,
    val inSong: Boolean,
    val songTimeSec: Double? = null,
    val connectionId: Int? = null,  // uint16; present only in initial response to hello
    val tsTvMs: Long? = null,
)

@Serializable data class SlotMap(val P1: SlotInfo, val P2: SlotInfo)
@Serializable data class SlotInfo(val connected: Boolean, val deviceName: String)

@Serializable data class AssignSingerMessage(
    val type: String = "assignSinger",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val songInstanceSeq: Long,      // uint32
    val playerId: String,           // "P1" | "P2"
    val difficulty: String,         // "Easy" | "Medium" | "Hard"
    val thresholdIndex: Int,        // 0..7
    val effectiveMicDelayMs: Int,
    val expectedPitchFps: Int,
    val startMode: String,          // "countdown" | "live"
    val countdownMs: Int? = null,   // required if startMode == "countdown"
    val endTimeTvMs: Long,
    val udpPort: Int,
    val songTitle: String? = null,
    val songArtist: String? = null,
    val tsTvMs: Long? = null,
)

@Serializable data class ErrorMessage(
    val type: String = "error",
    val protocolVersion: Int = 1,
    val code: String,
    val message: String,
    val tsTvMs: Long? = null,
)

@Serializable data class PingMessage(
    val type: String = "ping",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvSendMs: Long,
    val tsTvMs: Long? = null,
)

@Serializable data class PongMessage(
    val type: String = "pong",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvSendMs: Long,
    val tPhoneRecvMs: Long,
    val tPhoneSendMs: Long,
    val tsTvMs: Long? = null,
)

@Serializable data class ClockAckMessage(
    val type: String = "clockAck",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvRecvMs: Long,
    val tsTvMs: Long? = null,
)
```

**Error codes (normative):**
- `invalid_token` — missing or wrong join code
- `protocol_mismatch` — `protocolVersion != 1`
- `session_full` — roster full
- `session_locked` — session is locked (flag owned by feature 006)

---

### Pitch Frame (`domain/network/pitch/`)

```kotlin
data class PitchFrame(
    val seq: Long,              // uint32 — frame counter
    val tvTimeMs: Int,          // int32 — phone's estimate of TV monotonic ms
    val songInstanceSeq: Long,  // uint32 — matches assignSinger.songInstanceSeq
    val playerId: Int,          // uint8 — 0=P1, 1=P2
    val midiNote: Int,          // uint8 — 0..127 voiced; 255 = unvoiced
    val connectionId: Int,      // uint16 — sender ID assigned at handshake
) {
    val toneValid: Boolean get() = midiNote != 255
    val tone: Int get() = midiNote - 36  // USDX semitone scale (C2=36 → tone=0)
}
```

**Wire format** (16 bytes, little-endian):
```
Offset  Size  Type     Field
  0      4    uint32   seq
  4      4    int32    tvTimeMs
  8      4    uint32   songInstanceSeq
 12      1    uint8    playerId
 13      1    uint8    midiNote
 14      2    uint16   connectionId
```

**Drop conditions (any one → silent drop):**
- Length ≠ 16 bytes
- `connectionId` not in active registry
- `connectionId` does not match the expected sender for the declared `playerId`
- `songInstanceSeq` does not match the active song
- `tvTimeMs` regresses > 200 ms from prior accepted frame for that player

---

### Session Token (`domain/session/SessionToken.kt`)

```kotlin
data class SessionToken(
    val raw: String,           // normalized: uppercase, no hyphens/spaces (e.g. "ABCDEFGH")
    val display: String,       // grouped for display (e.g. "ABCD-EFGH")
)
// Generation: SecureRandom, ≥ 32 bits entropy, RECOMMENDED 64 bits
// Normalization: uppercase, strip spaces and hyphens
// Comparison: case-insensitive after stripping spaces and hyphens
```

---

### Connection Registry (`domain/session/ConnectionRegistry.kt`)

```kotlin
// Maps clientId → Connection
data class Connection(
    val clientId: String,
    val deviceName: String,
    val connectionId: UShort,   // uint16; assigned at handshake, new on reconnect
    val httpPort: Int,
    val phoneIp: String,        // inferred from WebSocket remote address
    val playerSlot: String?,    // "P1" | "P2" | null if not assigned as singer
)
// connectionId counter: increments from 1, wraps at 65535
// Active connection table: Map<String, Connection> (keyed by clientId)
```

---

### Session Gate Interface (`domain/session/ISessionGate.kt`)

Minimal interface that feature 006 implements. Feature 005 reads from it but never writes session lifecycle state.

```kotlin
interface ISessionGate {
    val isLocked: Boolean
    val sessionId: String
    val maxConnections: Int      // = 10 for MVP (T8.3.7: session_full fires when >10 are connected; only P1/P2 can sing)
}
```

---

### Clock Sync (`domain/network/clock/ClockSyncEngine.kt`)

**State machine:**
- `IDLE` → `SYNCING` (on phone connect: 5 exchanges at 100ms intervals)
- `SYNCING` → `SYNCED` (after 5 exchanges complete)
- `SYNCED` → `SUSPENDED` (when song starts)
- `SUSPENDED` → `SYNCED` (single exchange on song end or reconnect)

**Per-exchange data (not persisted):**
```
pingId: String   — random UUID per exchange
t1 = tTvSendMs   — TV monotonic ms at ping send
t2 = tPhoneRecvMs — phone monotonic ms at pong receipt
t3 = tPhoneSendMs — phone monotonic ms at pong send
t4 = tTvRecvMs   — TV monotonic ms at clockAck send
```
The phone derives: `clockOffsetMs = ((t2 - t1) + (t3 - t4)) / 2` — TV does not compute this; TV just sends clockAck.

---

## State Transitions Summary

```
Phone lifecycle (per clientId):
  ABSENT
    → HANDSHAKING   (WebSocket opens; token checked)
    → CONNECTED     (hello validated; connectionId assigned; manifest fetched)
    → SINGING       (assignSinger sent)
    → CONNECTED     (song ends)
    → ABSENT        (WebSocket closes; songs removed from library)
    → HANDSHAKING   (reconnect; new connectionId assigned)
```

---

## New Files Summary

| File | Type | Owner |
|---|---|---|
| `domain/network/protocol/ProtocolMessages.kt` | Kotlin data classes | 005 |
| `domain/network/protocol/ControlMessageCodec.kt` | Serialization helper | 005 |
| `domain/network/pitch/PitchFrame.kt` | Data class | 005 |
| `domain/network/pitch/PitchFrameCodec.kt` | Binary codec | 005 |
| `domain/network/clock/ClockSyncEngine.kt` | Domain logic | 005 |
| `domain/session/ISessionGate.kt` | Interface | 005 (006 implements) |
| `domain/session/ConnectionRegistry.kt` | Domain logic | 005 |
| `domain/session/SessionToken.kt` | Domain logic | 005 |
| `data/network/WebSocketServer.kt` | Infrastructure | 005 |
| `data/network/UdpPitchReceiver.kt` | Infrastructure | 005 |
| `data/network/ManifestFetcher.kt` | Infrastructure | 005 |
| `data/network/MdnsAdvertiser.kt` | Infrastructure | 005 |
| `res/xml/network_security_config.xml` | Config | 005 |
