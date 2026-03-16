# Quickstart: Network Protocol (005)

## What this feature adds

TV-side network layer: WebSocket control channel, UDP pitch receiver, manifest HTTP fetch, mDNS advertisement, and NTP-lite clock sync. Phones join via a join code; the TV advertises via JmDNS, handles the handshake, fetches song catalogs, and routes pitch frames to the scoring engine.

## Key entry points

| Class | Package | Role |
|---|---|---|
| `WebSocketServer` | `data.network` | Start/stop the Ktor CIO WebSocket server; validates token and dispatches control messages |
| `UdpPitchReceiver` | `data.network` | Binds UDP port; receive loop; validates and routes `PitchFrame` objects |
| `ManifestFetcher` | `data.network` | `fetch(phoneIp, httpPort, clientId): Result<List<ManifestEntry>>` |
| `MdnsAdvertiser` | `data.network` | `start(joinCode, wsPort)` / `stop()` — JmDNS + multicast lock |
| `ClockSyncEngine` | `domain.network.clock` | `startSync(clientId)` — triggers 5-exchange rapid sync then manages schedule |
| `ConnectionRegistry` | `domain.session` | Assigns and tracks `connectionId` per phone; validates pitch frame routing |
| `SessionToken` | `domain.session` | `generate(): SessionToken`, `normalize(input): String`, `matches(input, token): Boolean` |

## Running tests

```bash
# All network-protocol tests
./gradlew test --tests "*.network.*" --tests "*.session.*"

# Full suite
./gradlew test
```

## Fixtures used (do not recreate)

| Fixture | Used by |
|---|---|
| `original_spec/fixtures/F12v2_pitch_stream_validation_semantics/` | `PitchFrameCodec` acceptance tests |
| `original_spec/fixtures/F13_jitter_buffer_selection_staleness/` | `UdpPitchReceiver` staleness tests |
| `original_spec/fixtures/F14v2_clock_sync_phone_side/` | `ClockSyncEngine` acceptance tests |
| `original_spec/fixtures/F15_session_lifecycle_disconnect_reconnect/` | `WebSocketServer` acceptance tests |
| `original_spec/fixtures/F18_http_server_range_coordination/` | `ManifestFetcher` tests |

## New resource files

- `app/src/main/res/xml/network_security_config.xml` — cleartext HTTP allowed for RFC-1918 LAN addresses (required for manifest fetch and asset streaming on API 28+)
- `AndroidManifest.xml` — add `CHANGE_WIFI_MULTICAST_STATE` permission and reference `network_security_config`

## Architecture boundaries

- `domain/` — pure Kotlin, no Android imports, fully unit-testable
- `data/` — Android/Ktor/JmDNS/OkHttp wiring; Android framework types stay here
- `ISessionGate` — interface defined here, **implemented by feature 006**; stub/fake implementation needed for 005 tests
