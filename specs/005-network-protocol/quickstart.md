# Quickstart: Network Protocol (005)

## What this feature adds

This feature now exposes two distinct TV→phone contracts during singing:
- `assignSinger` for static assignment/configuration
- `playbackState` for authoritative live playback state and paused-aware timing

The rest of feature 005 continues to provide TV-side networking: WebSocket control, UDP pitch reception, manifest fetch, mDNS advertising, and clock sync.

## Key implementation goals

1. Preserve the LAN-only TV-host-authoritative model.
2. Keep `assignSinger` static and remove phone sensitivity/countdown ownership from it.
3. Emit `playbackState` whenever playback meaningfully changes or must be replayed on reconnect.
4. Re-send the latest `playbackState` on reconnect without incrementing `revision`.
5. Continue dropping invalid UDP pitch frames silently.

## Main code areas

| Path | Responsibility |
|---|---|
| `app/src/main/kotlin/com/couchraoke/tv/domain/network/protocol/ProtocolMessages.kt` | Control-message schemas including `assignSinger` and `playbackState` |
| `app/src/main/kotlin/com/couchraoke/tv/domain/network/protocol/ControlMessageCodec.kt` | JSON encoding/decoding |
| `app/src/main/kotlin/com/couchraoke/tv/data/network/WebSocketServer.kt` | Handshake, control-message dispatch, reconnect replay |
| `app/src/main/kotlin/com/couchraoke/tv/domain/session/ConnectionRegistry.kt` | Sender-ID allocation and reconnect identity handling |
| `app/src/main/kotlin/com/couchraoke/tv/data/network/UdpPitchReceiver.kt` | UDP pitch validation and routing |
| `app/src/main/kotlin/com/couchraoke/tv/domain/network/clock/ClockSyncEngine.kt` | Clock sync exchanges |

## Suggested implementation sequence

1. Update protocol message models and codecs.
2. Update WebSocket control flow to send `playbackState` snapshots and replay them on reconnect.
3. Remove obsolete `assignSinger` fields and replace phone-facing time semantics with `stopAtLyricsTimeMs`.
4. Update tests for codec, WebSocket behavior, and reconnect semantics.
5. Run focused network/session tests, then the full JVM test suite.

## Test commands

```bash
./gradlew networkTest
./gradlew testDebugUnitTest
./gradlew ciUnitTests
```

## Expected reconnect behavior

- reconnect gets a new sender ID
- current assignment is replayed via `assignSinger`
- latest playback snapshot is replayed via `playbackState`
- replay uses the existing `revision`

## Notes

- `playbackState.reason` is informational and open-ended.
- `thresholdIndex` no longer belongs to the TV→phone assignment contract.
- countdown ownership now lives entirely in `playbackState`.
