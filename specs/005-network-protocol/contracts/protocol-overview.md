# Protocol Contracts: Network Protocol (005)

This document summarizes the TV-side protocol contracts after the playback-state clarification update.

## Transport Channels

| Channel | Direction | Format | Notes |
|---|---|---|---|
| WebSocket | Bidirectional control | JSON | Session control, assignment, playback state, clock sync |
| UDP | Phone → TV | Fixed 16-byte binary | Real-time pitch frames |
| HTTP | TV → Phone | JSON/media | Manifest fetch and direct asset retrieval |

## Control Message Contract

- Every JSON control message includes `type` and `protocolVersion`.
- `protocolVersion` remains `1` for this feature scope.
- Unknown message types are ignored with a warning after handshake; unexpected handshake message types remain fatal.
- The TV remains authoritative for session state, singer assignment, and playback state.

## Handshake Sequence

```text
Phone → TV: hello
TV validates: token, protocolVersion, httpPort, session capacity, lock state
TV → Phone: sessionState (includes new sender/connection ID)
TV → Phone (later/as needed): ping / clockAck, assignSinger, playbackState
TV → Phone: error (only for rejection cases)
TV → Phone's HTTP server: manifest fetch from /manifest.json
```

## `assignSinger` Contract

Purpose: static assignment/configuration for one singer and one `songInstanceSeq`.

### Required fields
- `type = "assignSinger"`
- `protocolVersion = 1`
- `sessionId`
- `songInstanceSeq`
- `playerId`
- `difficulty`
- `effectiveMicDelayMs`
- `expectedPitchFps`
- `stopAtLyricsTimeMs`
- `udpPort`

### Optional fields
- `songTitle`
- `songArtist`
- `tsTvMs`

### Explicit exclusions
- No `thresholdIndex`
- No `startMode`
- No `countdownMs`
- No live playback progression fields
- No `endTimeTvMs`

## `playbackState` Contract

Purpose: authoritative playback snapshot for countdown, play, pause, stop, reconnect sync, and paused-aware song timing.

### Required fields
- `type = "playbackState"`
- `protocolVersion = 1`
- `sessionId`
- `songInstanceSeq`
- `revision`
- `state`
- `lyricsTimeMs`
- `stopAtLyricsTimeMs`
- `tsTvMs`

### Conditional field
- `countdownRemainingMs` is required only when `state = "countdown"`

### Optional fields
- `reason`
- `songTitle`
- `songArtist`

### Semantics
- `state ∈ {countdown, playing, paused, stopped}`
- `lyricsTimeMs` is the authoritative paused-aware song clock
- `stopAtLyricsTimeMs` is the phone-facing logical assignment end point in song time
- `revision` increments on playback contract changes such as pause, resume, seek, and stop
- reconnect re-sends the latest `playbackState` without incrementing `revision`
- `reason` is an optional open-ended hint and must not be treated as a closed enum

## Reconnect Contract

On reconnect during an active song assignment:
1. TV assigns a new sender/connection ID.
2. TV re-sends `assignSinger` for the active assignment.
3. TV re-sends the latest `playbackState` snapshot with the existing revision.
4. Phone resumes from the latest authoritative playback snapshot.

## Pitch Frame Contract

```text
Offset  Size  Type     Field
0       4     uint32   seq
4       4     int32    tvTimeMs
8       4     uint32   songInstanceSeq
12      1     uint8    playerId
13      1     uint8    midiNote
14      2     uint16   connectionId
```

Drop the frame silently if any of the following is true:
- packet size is not exactly 16 bytes
- sender/connection ID is unknown
- sender/connection ID does not match the assigned player slot
- `songInstanceSeq` does not match the active assignment
- assignment is already stopped

## Error Codes

| Code | Meaning |
|---|---|
| `invalid_token` | Missing or wrong join token |
| `protocol_mismatch` | Unsupported protocol version |
| `session_full` | Session capacity reached |
| `session_locked` | Session locked by external session owner |
