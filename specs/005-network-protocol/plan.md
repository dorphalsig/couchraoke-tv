# Implementation Plan: Network Protocol

**Branch**: `005-network-protocol` | **Date**: 2026-03-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/005-network-protocol/spec.md`

## Summary

Implement the TV-side network layer for the Couchraoke karaoke system. A Ktor CIO WebSocket server accepts phone connections authenticated by a session join code. On successful handshake, the TV assigns a `connectionId`, fetches the phone's song catalog via HTTP, and starts NTP-lite clock sync. A UDP `DatagramSocket` receives 16-byte pitch frames routed by `connectionId`. JmDNS advertises the session on the LAN. All business logic lives in pure Kotlin domain classes; Android/Ktor/JmDNS wiring stays in the data layer.

## Technical Context

**Language/Version**: Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36)
**Primary Dependencies**: Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, JmDNS 3.6.3 (override: original spec §8.2.1 lists 3.5.9; 3.6.3 chosen per research.md for better stability on Android TV), kotlinx-serialization-json, OkHttp (transitive via media3/coil), `java.net.DatagramSocket` (stdlib)
**Storage**: In-memory only (`ConnectionRegistry`, `SongLibrary`) — no persistence
**Testing**: JUnit4, `./gradlew test` (JVM unit tests), JaCoCo ≥80% overall / ≥60% per file
**Target Platform**: Android TV (API 28+)
**Project Type**: Android application module
**Performance Goals**: Handshake + manifest fetch < 3s on LAN; clock sync offset < 10ms; pitch frame validation < 1ms per frame
**Constraints**: LAN-only; no cloud services; cleartext HTTP restricted to RFC-1918 addresses; `protocolVersion = 1` only
**Scale/Scope**: up to 10 concurrent phone connections (`maxConnections=10` per T8.3.7); 2 singer slots (P1 + P2); ~50 pitch frames/sec per active singer; 5-exchange clock sync burst on connect

## Constitution Check

- **LAN-only**: ✅ All communication is WebSocket/UDP/HTTP over LAN. No external endpoints.
- **Approved stack**: ✅ Ktor CIO + WebSockets, JmDNS, kotlinx-serialization, OkHttp (transitive). No Gson/Moshi/Retrofit/NSD Manager.
- **Architecture boundaries**: ✅ Business logic in `domain/network/` and `domain/session/` (pure Kotlin). Android wiring in `data/network/`. `ISessionGate` interface keeps 005 decoupled from 006's session lifecycle.
- **Networking contracts**: ✅ Pitch transport uses fixed 16-byte UDP frames; no batching. Direct HTTP streaming from phone (no TV-side asset storage). WebSocket for control only.
- **Testing**: ✅ JUnit4 only. Acceptance tests against F12v2, F13, F14v2, F15, F18 fixtures. JaCoCo thresholds enforced.
- **Branch hygiene**: Branch `005-network-protocol` will be renamed `[✓] 005-network-protocol` after merge to master.

## Project Structure

### Documentation (this feature)

```text
specs/005-network-protocol/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   └── protocol-overview.md
└── tasks.md             ← Phase 2 output (/speckit.tasks)
```

### Source Code

```text
app/src/main/kotlin/com/couchraoke/tv/
  domain/
    network/
      protocol/
        ProtocolMessages.kt       # @Serializable data classes: hello, sessionState, assignSinger,
                                  #   error, ping, pong, clockAck
        ControlMessageCodec.kt    # Json decode helper; unknown type → ignore+warn; version check
      pitch/
        PitchFrame.kt             # 16-byte frame model + toneValid + tone (midiNote-36)
        PitchFrameCodec.kt        # ByteArray(16) ↔ PitchFrame; null on invalid length
      clock/
        ClockSyncEngine.kt        # Ping dispatch, pong → clockAck, 5-burst + suspend schedule
    session/
      ISessionGate.kt             # interface: isLocked, sessionId, maxConnections
      ConnectionRegistry.kt       # connectionId counter (uint16, +1 from 1), active Map<clientId,Connection>
      SessionToken.kt             # SecureRandom 64-bit, normalize(), matches(), display grouping
  data/
    network/
      WebSocketServer.kt          # Ktor CIO; token + version + httpPort validation; sessionState with connectionId
      UdpPitchReceiver.kt         # DatagramSocket.bind(); receive loop; drop rules; route callback
      ManifestFetcher.kt          # OkHttp GET /manifest.json; parse ManifestEntry list; retain+toast on error
      MdnsAdvertiser.kt           # JmDNS; multicast lock acquire/release; _karaoke._tcp; TXT code=+v=1
app/src/main/res/xml/
  network_security_config.xml     # cleartext for RFC-1918 subnets per §8.7.3.1
app/src/main/AndroidManifest.xml  # add CHANGE_WIFI_MULTICAST_STATE; add networkSecurityConfig ref
app/src/test/kotlin/com/couchraoke/tv/
  domain/
    network/
      protocol/
        ProtocolMessagesTest.kt       # encode/decode round-trip; unknown type ignored
      pitch/
        PitchFrameCodecTest.kt        # unit: all fields; midiNote=255; length≠16
        PitchFrameCodecAcceptanceTest.kt  # F12v2 fixture
      clock/
        ClockSyncEngineTest.kt        # unit: ping→pong→clockAck flow; suspend/resume
    session/
      SessionTokenTest.kt             # generate entropy; normalize; match; display format
      ConnectionRegistryTest.kt       # connectionId increment; new id on reconnect; lookup
  data/
    network/
      WebSocketServerTest.kt          # unit: valid hello → sessionState with connectionId
      WebSocketServerAcceptanceTest.kt  # F15 fixture: T8.3.1–T8.3.13, T8.5.1–T8.5.5
      UdpPitchReceiverTest.kt         # unit + F12v2: drop rules T8.6.1–T8.6.8
      ManifestFetcherTest.kt          # unit: success, HTTP error → retain, F18
      MdnsAdvertiserTest.kt           # unit: service name format; TXT record fields
```

## Implementation Waves

### Wave 1 — Protocol message models + codec
**Files**: `ProtocolMessages.kt`, `ControlMessageCodec.kt`
**Tests**: `ProtocolMessagesTest.kt`
- All `@Serializable` data classes (hello, sessionState, assignSinger, error, ping, pong, clockAck)
- `Json { ignoreUnknownKeys = true; coerceInputValues = true }` instance in codec
- `decodeMessage(json: String): Any?` — returns typed message or null with log on unknown type
- **Acceptance**: encode→decode round-trip for each message type; unknown `type` → null+warn; missing required field → exception

### Wave 2 — Pitch frame codec
**Files**: `PitchFrame.kt`, `PitchFrameCodec.kt`
**Tests**: `PitchFrameCodecTest.kt`, `PitchFrameCodecAcceptanceTest.kt`
- `decode(bytes: ByteArray): PitchFrame?` — null if length ≠ 16; little-endian parsing
- `encode(frame: PitchFrame): ByteArray`
- `toneValid = (midiNote != 255)`, `tone = midiNote - 36`
- **Acceptance**: F12v2 `expected.json` rows — all field values, midiNote=255 → toneValid=false, round-trip

### Wave 3 — Session token + connection registry
**Files**: `SessionToken.kt`, `ConnectionRegistry.kt`, `ISessionGate.kt`
**Tests**: `SessionTokenTest.kt`, `ConnectionRegistryTest.kt`
- `SessionToken.generate()`: `SecureRandom` 64-bit → 10-char uppercase alphanumeric; display as `XXXXX-XXXXX`
- `normalize(input)`: uppercase, strip spaces+hyphens
- `matches(input, token)`: normalize both then compare
- `ConnectionRegistry`: incrementing `UShort` counter from 1; `register(clientId, ...) → UShort`; `deregister(clientId)`; `getByClientId(clientId): Connection?`; new id on re-register
- `ISessionGate` stub (fake) for test use in later waves

### Wave 4 — Clock sync engine
**Files**: `ClockSyncEngine.kt`
**Tests**: `ClockSyncEngineTest.kt` (+ F14v2 if applicable)
- `startInitialSync(clientId)`: dispatches 5 pings 100ms apart
- `onPong(pong: PongMessage): ClockAckMessage`: builds clockAck immediately
- `suspend()` / `resumeSingle(clientId)`: song-active state management
- No internal coroutine scheduler — takes a `Clock` and a `PingSender` interface (injected)

### Wave 5 — WebSocket server
**Files**: `WebSocketServer.kt`
**Tests**: `WebSocketServerTest.kt`, `WebSocketServerAcceptanceTest.kt`
- Ktor CIO server on configurable port; single route `ws://<host>:<port>/` with `?token=` query param
- On connect: validate token → version → httpPort presence → session capacity → isLocked
- On valid hello: assign connectionId via `ConnectionRegistry`; send `sessionState` with connectionId; trigger manifest fetch; start clock sync
- **Proactor pattern (normative)**: `ManifestFetcher.fetch` and clock sync MUST be launched in separate coroutines (`launch { }`) so the WebSocket receive loop never blocks. This ensures minimal latency for control messages (ping/pong) which must be processed promptly even while a manifest fetch is in flight.
- Message loop: route pong → clock engine; route unknown → ignore+warn; unexpected message type **during handshake** = fatal, close connection
- On close: `ConnectionRegistry.deregister`; `SongLibrary.removePhone`
- **Acceptance**: F15 cases T8.3.1–T8.3.13, T8.5.1–T8.5.5

### Wave 6 — UDP pitch receiver
**Files**: `UdpPitchReceiver.kt`
**Tests**: `UdpPitchReceiverTest.kt`
- `bind(port: Int)`: `DatagramSocket(port)` — called once at session start
- Receive loop: decode 16 bytes → apply drop rules → invoke `onFrame(frame: PitchFrame)` callback
- Drop rules (all silent): length≠16; unknown connectionId; connectionId/playerId mismatch; songInstanceSeq mismatch; tvTimeMs regression >200ms
- Runs on a dedicated background thread; `stop()` closes socket and exits loop
- **Acceptance**: F12v2 T8.6.1–T8.6.8

### Wave 7 — Manifest fetcher + library integration
**Files**: `ManifestFetcher.kt`
**Tests**: `ManifestFetcherTest.kt`
- `fetch(phoneIp: String, httpPort: Int, clientId: String): Result<List<ManifestEntry>>`
- OkHttp synchronous call; `GET http://<phoneIp>:<httpPort>/manifest.json`
- On 200: parse JSON array → `List<ManifestEntry>` via kotlinx.serialization; call `SongLibrary.addPhone(clientId, entries)`
- On failure (non-200, timeout, IOE): retain prior catalog; emit error toast event; return `Result.failure`
- **Acceptance**: success path; HTTP error → retain; F18 (range headers if tested here)

### Wave 8 — mDNS + platform config
**Files**: `MdnsAdvertiser.kt`, `network_security_config.xml`, `AndroidManifest.xml`
**Tests**: `MdnsAdvertiserTest.kt`
- `start(joinCode: String, wsPort: Int)`: acquire `WifiManager.MulticastLock("jmdns_lock")`; create `JmDNS`; register `_karaoke._tcp` service; instance name = `KaraokeTV-<last4>`; TXT: `code=<normalizedCode>` + `v=1`
- `stop()`: unregister service; close JmDNS; release multicast lock
- `network_security_config.xml`: RFC-1918 domain-config per §8.7.3.1 exact XML
- `AndroidManifest.xml`: add `CHANGE_WIFI_MULTICAST_STATE`; add `android:networkSecurityConfig="@xml/network_security_config"`

## Complexity Tracking

No constitution violations. No exceptions required.
