# Data Model: Network Protocol (005)

## Package Layout

```text
app/src/main/kotlin/com/couchraoke/tv/
  domain/
    network/
      protocol/
        ProtocolMessages.kt
        ControlMessageCodec.kt
      pitch/
        PitchFrame.kt
        PitchFrameCodec.kt
      clock/
        ClockSyncEngine.kt
    session/
      ISessionGate.kt
      ConnectionRegistry.kt
      SessionToken.kt
  data/
    network/
      WebSocketServer.kt
      UdpPitchReceiver.kt
      ManifestFetcher.kt
      MdnsAdvertiser.kt
app/src/main/res/xml/
  network_security_config.xml
```

## Domain Entities

### SessionStateMessage

Represents the authoritative response to a successful hello handshake.

| Field | Type | Notes |
|---|---|---|
| `type` | string | Always `sessionState` |
| `protocolVersion` | int | Always `1` |
| `sessionId` | string | Current active session |
| `slots` | object | Current slot occupancy and device names |
| `inSong` | boolean | Whether the TV is currently in a song |
| `songTimeSec` | number? | Current song time when applicable |
| `connectionId` | int? | Unique 16-bit sender ID assigned for this connection |
| `tsTvMs` | long? | TV monotonic timestamp snapshot |

### AssignSingerMessage

Static assignment/configuration message for one singer assignment. It never carries live playback progression.

| Field | Type | Notes |
|---|---|---|
| `type` | string | Always `assignSinger` |
| `protocolVersion` | int | Always `1` |
| `sessionId` | string | Active session identifier |
| `songInstanceSeq` | long | Current song or medley run |
| `playerId` | string | `P1` or `P2` |
| `difficulty` | string | TV-selected singing difficulty |
| `effectiveMicDelayMs` | int | Delay compensation to apply on phone |
| `expectedPitchFps` | int | Expected pitch capture cadence |
| `stopAtLyricsTimeMs` | long | Logical end of the assignment in song time |
| `udpPort` | int | Stable TV UDP pitch listener port |
| `songTitle` | string? | Optional song metadata |
| `songArtist` | string? | Optional song metadata |
| `tsTvMs` | long? | Optional TV monotonic timestamp snapshot |

**Validation rules**
- No `thresholdIndex`, `startMode`, `countdownMs`, or `endTimeTvMs` fields remain in this contract.
- `stopAtLyricsTimeMs` is required for every assignment.
- The same `songInstanceSeq` may be re-sent on reconnect with a new sender ID.

### PlaybackStateMessage

Authoritative playback snapshot for one assigned singer/song instance.

| Field | Type | Notes |
|---|---|---|
| `type` | string | Always `playbackState` |
| `protocolVersion` | int | Always `1` |
| `sessionId` | string | Active session identifier |
| `songInstanceSeq` | long | Current song or medley run |
| `revision` | long | Monotonic within a song instance; changes only on playback contract changes |
| `state` | string | `countdown`, `playing`, `paused`, or `stopped` |
| `lyricsTimeMs` | long | Authoritative paused-aware lyrics clock |
| `stopAtLyricsTimeMs` | long | Logical end of the assignment in song time |
| `countdownRemainingMs` | long? | Required only when `state == countdown` |
| `reason` | string? | Optional open-ended reason hint |
| `songTitle` | string? | Optional song metadata |
| `songArtist` | string? | Optional song metadata |
| `tsTvMs` | long | TV monotonic timestamp for this snapshot |

**Validation rules**
- Reconnect re-sends the latest `revision`; reconnect alone must not increment it.
- `countdownRemainingMs` is present only in countdown state.
- `lyricsTimeMs = 0` means start of the audio file.
- `reason` is informational and must be safely ignorable by clients.

### PitchFrame

Binary pitch sample datagram from phone to TV.

| Field | Type | Notes |
|---|---|---|
| `seq` | uint32 | Frame counter |
| `tvTimeMs` | int32 | Phone’s estimate of TV monotonic ms |
| `songInstanceSeq` | uint32 | Active assignment instance |
| `playerId` | uint8 | `0 = P1`, `1 = P2` |
| `midiNote` | uint8 | `255` means unvoiced |
| `connectionId` | uint16 | Sender ID assigned during handshake |

**Drop rules**
- Drop frames with wrong size.
- Drop frames with unknown sender IDs.
- Drop frames whose sender ID does not match the assigned player slot.
- Drop frames whose `songInstanceSeq` does not match the active assignment.
- Drop frames received after the assignment has entered stopped state.

### Connection

Represents a currently connected phone.

| Field | Type | Notes |
|---|---|---|
| `clientId` | string | Stable phone identity |
| `deviceName` | string | User-visible device label |
| `connectionId` | ushort | New value assigned on every reconnect |
| `httpPort` | int | Phone manifest/song server port |
| `phoneIp` | string | Remote IP inferred from the connection |
| `playerSlot` | string? | Optional current singer slot |

### SessionToken

Human-enterable join token for a single active session.

| Field | Type | Notes |
|---|---|---|
| `raw` | string | Normalized uppercase token |
| `display` | string | Grouped display form |

### ClockSyncExchange

Transient timing exchange used to map phone monotonic time to TV monotonic time.

| Field | Type | Notes |
|---|---|---|
| `pingId` | string | Unique exchange ID |
| `tTvSendMs` | long | TV send timestamp |
| `tPhoneRecvMs` | long | Phone receive timestamp |
| `tPhoneSendMs` | long | Phone send timestamp |
| `tTvRecvMs` | long | TV receive timestamp |

## Relationships

- One **Connection** may own zero or one current singer slot.
- One **AssignSingerMessage** establishes the static configuration for one connection and one `songInstanceSeq`.
- Many **PlaybackStateMessage** snapshots may be sent for one `AssignSingerMessage` as playback evolves.
- Many **PitchFrame** datagrams may arrive for one active `songInstanceSeq` until that assignment is stopped.
- One **SessionToken** gates all connections in the active session.

## State Transitions

### Connection lifecycle

```text
ABSENT
  → HANDSHAKING
  → CONNECTED
  → SINGING
  → CONNECTED
  → ABSENT
  → HANDSHAKING (reconnect assigns new connectionId)
```

### PlaybackState lifecycle

```text
countdown
  → playing
  → paused
  → playing
  → stopped
```

Allowed notes:
- Countdown may transition directly to `stopped` if playback is cancelled before start.
- Reconnect does not create a new playback state transition by itself; it replays the latest snapshot.
- Seek preserves the current high-level state (`playing` or `paused`) while incrementing `revision` and updating `lyricsTimeMs`.
