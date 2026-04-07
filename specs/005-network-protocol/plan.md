# Implementation Plan: Network Protocol

**Branch**: `005-network-protocol` | **Date**: 2026-04-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-network-protocol/spec.md`

## Summary

Update the existing TV-side network protocol implementation so `assignSinger` becomes a static assignment/configuration contract and a new authoritative `playbackState` message carries countdown, play/pause/stop state, paused-aware playback time, reconnect replay, and early-stop semantics. Preserve the current LAN-only TV-host-authoritative architecture, sender-ID reassignment on reconnect, direct phone asset streaming, fixed UDP pitch transport, and existing network/session test coverage while extending protocol models, control-message routing, and reconnect behavior.

## Technical Context

**Language/Version**: Kotlin 2.3.10 on Java 11  
**Primary Dependencies**: Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, kotlinx-serialization-json 1.10.0, kotlinx-coroutines-core 1.9.0, JmDNS 3.6.3, OkHttp via existing Media3/Coil stack  
**Storage**: In-memory session/network state only; no persistence  
**Testing**: JUnit4 JVM unit + acceptance tests, kotlinx-coroutines-test, Ktor test host, MockWebServer, JaCoCo coverage gates  
**Target Platform**: Android TV app (minSdk 28, target/compileSdk 36)  
**Project Type**: Mobile app (Android TV host)  
**Performance Goals**: Keep LAN control-message propagation under spec targets, including playbackState transition delivery within 250 ms and reconnect replay within 1 second  
**Constraints**: LAN-only operation, TV remains authoritative, no remote asset persistence, fixed 16-byte UDP pitch transport, direct HTTP asset retrieval from phones, JUnit4-only Android testing  
**Scale/Scope**: One active TV host session, up to 10 connected phones, 2 active singer slots, one authoritative playbackState stream per active assignment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Pass**: Plan preserves Couchraoke's LAN-only operating model and keeps the TV Host as the authoritative game engine.
- **Pass**: No new libraries or forbidden alternatives are introduced; work stays within the approved Android TV stack.
- **Pass**: Changes remain within Domain/Data boundaries: protocol models and session logic in domain, WebSocket/UDP/network wiring in data.
- **Pass**: Networking, streaming, and scoring contracts remain intact: direct asset streaming, fixed UDP pitch frames, and no transport fallback changes.
- **Pass**: Plan includes protocol, reconnect, and acceptance-test updates using existing JUnit4-based Android test tooling.
- **Pass**: Post-merge branch hygiene remains governed by constitution branch renaming rules; no worktree changes are needed for this feature branch.

## Project Structure

### Documentation (this feature)

```text
specs/005-network-protocol/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── protocol-overview.md
└── tasks.md
```

### Source Code (repository root)

```text
app/
├── src/main/kotlin/com/couchraoke/tv/
│   ├── data/network/
│   │   ├── ManifestFetcher.kt
│   │   ├── MdnsAdvertiser.kt
│   │   ├── UdpPitchReceiver.kt
│   │   └── WebSocketServer.kt
│   ├── domain/network/
│   │   ├── clock/
│   │   │   └── ClockSyncEngine.kt
│   │   ├── pitch/
│   │   │   ├── PitchFrame.kt
│   │   │   └── PitchFrameCodec.kt
│   │   └── protocol/
│   │       ├── ControlMessageCodec.kt
│   │       └── ProtocolMessages.kt
│   ├── domain/session/
│   │   ├── ConnectionRegistry.kt
│   │   ├── ISessionGate.kt
│   │   └── SessionToken.kt
│   └── domain/library/
│       ├── ManifestModels.kt
│       └── SongLibrary.kt
├── src/main/res/xml/
│   └── network_security_config.xml
└── src/test/kotlin/com/couchraoke/tv/
    ├── data/network/
    │   ├── ManifestFetcherTest.kt
    │   ├── UdpPitchReceiverTest.kt
    │   ├── WebSocketServerAcceptanceTest.kt
    │   └── WebSocketServerTest.kt
    ├── domain/network/
    │   ├── clock/
    │   │   ├── ClockSyncEngineAcceptanceTest.kt
    │   │   └── ClockSyncEngineTest.kt
    │   ├── pitch/
    │   │   ├── PitchFrameCodecAcceptanceTest.kt
    │   │   └── PitchFrameCodecTest.kt
    │   └── protocol/
    │       └── ControlMessageCodecTest.kt
    └── domain/session/
        ├── ConnectionRegistryTest.kt
        └── FakeSessionGate.kt
```

**Structure Decision**: Keep the existing single Android app module and extend the already-established `domain/network`, `data/network`, and `domain/session` areas instead of introducing new modules or architectural layers.

## Phase 0: Research Summary

Resolved in `research.md`:
- `playbackState` is the single authoritative live playback contract.
- `assignSinger` is static assignment/config only.
- Reconnect replays the latest `playbackState` without incrementing `revision`.
- `playbackState.reason` remains an optional open-ended hint.
- `thresholdIndex` is removed from the TV→phone assignment contract.
- `stopAtLyricsTimeMs` replaces phone-facing `endTimeTvMs` semantics.

## Phase 1: Design & Contracts

### Data model implications

1. **Protocol message updates**
   - Add `PlaybackStateMessage` to `ProtocolMessages.kt`.
   - Remove obsolete `AssignSingerMessage` fields: `thresholdIndex`, `startMode`, `countdownMs`, `endTimeTvMs`.
   - Rename/replace assignment stop-point semantics with `stopAtLyricsTimeMs`.

2. **Runtime playback snapshot source**
   - Introduce or extend a domain-level playback snapshot representation that WebSocket control flow can serialize into `playbackState`.
   - Ensure revision accounting is tied only to playback contract changes, not reconnect transport retries.

3. **Reconnect behavior**
   - Reuse current sender-ID regeneration path in `ConnectionRegistry`.
   - Extend reconnect replay to emit both `assignSinger` and the latest `playbackState` snapshot.

4. **Pitch validation compatibility**
   - Keep current UDP frame shape unchanged.
   - Ensure frames after stopped state are dropped consistently with the new playback-state contract.

### Contract updates

- `contracts/protocol-overview.md` now documents:
  - slimmed `assignSinger`
  - new `playbackState`
  - reconnect replay ordering
  - open-ended `reason` semantics

### Quickstart updates

- `quickstart.md` now guides implementation order around protocol model updates, reconnect replay behavior, and focused network/session test execution.

## Phase 2: Implementation Strategy

### Workstreams

1. **Protocol schema and codec alignment**
   - Update message data classes and JSON codec expectations.
   - Preserve compatibility for unknown message handling and protocolVersion enforcement.

2. **WebSocket control-flow changes**
   - Refactor assignment sending to use static-only `assignSinger` payloads.
   - Add `playbackState` emission points for countdown, play, pause, seek correction, stop, and reconnect replay.
   - Ensure reconnect sends latest snapshot with existing revision.

3. **Session/playback integration**
   - Identify where active playback state is sourced for WebSocket emission.
   - Keep ownership of broader session lifecycle outside feature 005.
   - Ensure stopped-state knowledge is available for UDP drop behavior.

4. **Test updates**
   - Update `ControlMessageCodecTest` for new/removed message fields.
   - Update `WebSocketServerTest` and `WebSocketServerAcceptanceTest` for playbackState emission and reconnect replay semantics.
   - Update any fixture-driven expectations impacted by `stopAtLyricsTimeMs` and removed assignment fields.
   - Preserve or extend acceptance coverage for reconnect, pause/resume, countdown, and stopped-state behavior.

## Post-Design Constitution Check

- **Pass**: Design keeps TV authoritative and LAN-only.
- **Pass**: No forbidden stack changes or architectural boundary violations are introduced by the new playback contract.
- **Pass**: Direct asset streaming and UDP pitch transport remain unchanged.
- **Pass**: Planned test coverage addresses changed control contracts and reconnect behavior using existing JUnit4 tooling.
- **Pass**: No constitution violations require complexity-tracking exemptions.

## Complexity Tracking

No constitution violations or exception cases identified.
