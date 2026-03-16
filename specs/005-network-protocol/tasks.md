# Tasks: Network Protocol (005)

**Input**: Design documents from `specs/005-network-protocol/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Coverage requirement**: JaCoCo ≥ 80% overall / ≥ 60% per file (`./gradlew ciUnitTests`)
**Acceptance tests**: All F12v2, F14v2, F15 test cases must pass (T8.3.x, T8.5.x, T8.6.x)

---

## Phase 1: Setup

**Purpose**: Platform config and package scaffolding. No logic — unblocks all later tasks.

- [ ] T001 Create `app/src/main/res/xml/network_security_config.xml` with RFC-1918 cleartext domain-config per §8.7.3.1 exact XML (7 domain entries)
- [ ] T002 Update `app/src/main/AndroidManifest.xml` — add `CHANGE_WIFI_MULTICAST_STATE` permission and `android:networkSecurityConfig="@xml/network_security_config"` on `<application>`
- [ ] T003 [P] Create empty package directories: `domain/network/protocol/`, `domain/network/pitch/`, `domain/network/clock/`, `domain/session/`, `data/network/` — add a `.gitkeep` file in each so the directories are tracked by git before source files land

---

## Phase 2: Foundation (Blocking Prerequisites)

**Purpose**: Pure-Kotlin domain models that all user story phases depend on. No Android imports.

**⚠️ CRITICAL**: All Phase 3+ tasks depend on this phase being complete.

- [ ] T004 [P] Create `app/src/main/kotlin/com/couchraoke/tv/domain/network/protocol/ProtocolMessages.kt` — `@Serializable` data classes for `HelloMessage`, `Capabilities`, `SessionStateMessage`, `SlotMap`, `SlotInfo`, `AssignSingerMessage`, `ErrorMessage`, `PingMessage`, `PongMessage`, `ClockAckMessage` per data-model.md
- [ ] T005 [P] Create `app/src/main/kotlin/com/couchraoke/tv/domain/session/ISessionGate.kt` — interface with `isLocked: Boolean`, `sessionId: String`, `maxConnections: Int` (= 10 for MVP per T8.3.7; only P1/P2 can sing, but up to 10 phones may be connected); create `app/src/test/kotlin/com/couchraoke/tv/domain/session/FakeSessionGate.kt` — test stub implementing `ISessionGate`
- [ ] T006 Create `app/src/main/kotlin/com/couchraoke/tv/domain/network/protocol/ControlMessageCodec.kt` — `Json { ignoreUnknownKeys = true }` instance; `decodeType(json): String?` to peek the `type` field; typed decode per message class; unknown type → log warning + return null (depends on T004)
- [ ] T007 [P] Create `app/src/main/kotlin/com/couchraoke/tv/domain/session/SessionToken.kt` — `generate()`: `SecureRandom` 64-bit → 10-char uppercase alphanumeric (≈46 bits entropy; exceeds spec's required 32-bit minimum and is sufficient for LAN join-code collision prevention); `normalize(input)`: uppercase + strip spaces/hyphens; `matches(input, token)`: normalize both then compare; `display`: insert hyphen at position 5 → `XXXXX-XXXXX`; create `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionTokenTest.kt` covering generate entropy (≥8 chars), normalize strips hyphens+spaces, matches is case-insensitive, display format
- [ ] T008 Create `app/src/main/kotlin/com/couchraoke/tv/domain/session/ConnectionRegistry.kt` — `data class Connection(clientId, deviceName, connectionId: UShort, httpPort, phoneIp, playerSlot)`; `register(clientId,...): UShort` increments counter from 1; `deregister(clientId)`; `getByClientId(clientId): Connection?`; `getByConnectionId(id): Connection?`; re-register assigns new id; create `app/src/test/kotlin/com/couchraoke/tv/domain/session/ConnectionRegistryTest.kt` covering first connection=1, second=2, reconnect gets new id, deregistered id not found, getByConnectionId lookup

**Checkpoint**: Foundation complete — user story phases can begin.

---

## Phase 3: User Stories 1 & 2 — Phone Join + Rejection (Priority: P1) 🎯 MVP

**Goal US1**: Phone sends valid hello → TV responds with sessionState + connectionId → manifest fetched → songs appear in library.
**Goal US2**: Invalid connections rejected immediately with normative error codes.

**Independent Test**: Simulate hello handshake with valid/invalid inputs; verify sessionState connectionId, error codes, and SongLibrary state against F15 fixture.

### Implementation for US1 + US2

- [ ] T009 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/data/network/ManifestFetcher.kt` — `fetch(phoneIp: String, httpPort: Int, clientId: String, library: SongLibrary): Result<Unit>`; OkHttp synchronous `GET http://<phoneIp>:<httpPort>/manifest.json`; on 200: parse JSON array via kotlinx.serialization into `List<ManifestEntry>`; call `library.addPhone(clientId, entries)`; on failure: retain prior catalog + return `Result.failure`; create `app/src/test/kotlin/com/couchraoke/tv/data/network/ManifestFetcherTest.kt` covering successful fetch + addPhone called, HTTP 500 → prior catalog retained, timeout → prior catalog retained
- [ ] T010 [P] [US1] Create `app/src/main/kotlin/com/couchraoke/tv/data/network/MdnsAdvertiser.kt` — `start(joinCode: String, wsPort: Int)`: acquire `WifiManager.MulticastLock("jmdns_lock")`; create `JmDNS`; register `_karaoke._tcp` service with instance name `KaraokeTV-<last4ofNormalizedCode>` and TXT records `code=<normalizedCode>` + `v=1`; `stop()`: unregister service + close JmDNS + release lock; create `app/src/test/kotlin/com/couchraoke/tv/data/network/MdnsAdvertiserTest.kt` covering instance name format `KaraokeTV-EFGH` for code `ABCDEFGH`, TXT record fields present
- [ ] T011 [US1] [US2] Create `app/src/main/kotlin/com/couchraoke/tv/data/network/WebSocketServer.kt` — Ktor CIO on configurable port; single route `ws://?token=`; on connect: (1) validate token via `SessionToken.matches` → reject `invalid_token` if wrong, (2) receive and decode hello → reject `protocol_mismatch` if version≠1, reject if `httpPort` missing, reject `session_full` if `registry.size >= gate.maxConnections` (maxConnections=10), reject `session_locked` if `gate.isLocked`; on valid hello: `registry.register(clientId,...)` → send `sessionState` with `connectionId`; **proactor pattern**: launch `ManifestFetcher.fetch` and `ClockSyncEngine.startInitialSync` each in a separate `launch { }` coroutine so the WebSocket receive loop never blocks; on close: `registry.deregister(clientId)` + `library.removePhone(clientId)`; message loop: pong → forward to `ClockSyncEngine`; unexpected message type **during handshake** (before hello received) → close connection with `error`; unknown type **after** handshake → log+ignore; no server-side validation of `clientId` or `appVersion` fields (trusted from phone) (depends on T004–T008, T009)
- [ ] T012 [US1] Create `app/src/test/kotlin/com/couchraoke/tv/data/network/WebSocketServerTest.kt` — unit tests: valid hello → sessionState contains connectionId; second connection → connectionId=2; manifest fetch triggered after hello; disconnect → songs removed; unknown message type ignored
- [ ] T013 [US1] [US2] Create `app/src/test/kotlin/com/couchraoke/tv/data/network/WebSocketServerAcceptanceTest.kt` — fixture F15; cover all 13 handshake cases T8.3.1–T8.3.13 (valid hello, missing httpPort, manifest fetch, wrong version, wrong token, session locked, session full when >10 connections, manifest update, disconnect removes songs, re-fetch replaces, assignSinger fields, optional fields, connectionId absent from assignSinger) and 5 sender-ID cases T8.5.1–T8.5.5; also cover edge case: unexpected non-hello message type during handshake → fatal error + close

**Checkpoint**: US1 + US2 complete — a phone can join, its songs appear, invalid joins are rejected.

---

## Phase 4: User Story 3 — Singing Instructions & Pitch Stream (Priority: P2)

**Goal**: TV sends singer assignment to phone; phone streams pitch frames; TV decodes, validates, and routes them correctly.

**Independent Test**: Decode F12v2 fixture frames; verify all drop rules; verify singer assignment fields against F15 T8.3.11–T8.3.13 and sender-ID tests T8.5.1–T8.5.5.

### Implementation for US3

- [ ] T014 [P] [US3] Create `app/src/main/kotlin/com/couchraoke/tv/domain/network/pitch/PitchFrame.kt` — data class with fields `seq: Long`, `tvTimeMs: Int`, `songInstanceSeq: Long`, `playerId: Int`, `midiNote: Int`, `connectionId: Int`; computed `val toneValid = midiNote != 255`; computed `val tone = midiNote - 36`
- [ ] T015 [P] [US3] Create `app/src/main/kotlin/com/couchraoke/tv/domain/network/pitch/PitchFrameCodec.kt` — `decode(bytes: ByteArray): PitchFrame?` returns null if `bytes.size != 16`; little-endian parsing per wire format in data-model.md; `encode(frame: PitchFrame): ByteArray`; create `app/src/test/kotlin/com/couchraoke/tv/domain/network/pitch/PitchFrameCodecTest.kt` covering all field values, midiNote=255 → toneValid=false, midiNote=0 → toneValid=true, encode→decode round-trip, length≠16 → null
- [ ] T016 [P] [US3] Create `app/src/test/kotlin/com/couchraoke/tv/domain/network/pitch/PitchFrameCodecAcceptanceTest.kt` — fixture F12v2/`expected.json`; T8.6.1 (row 0 all fields), T8.6.2 (midiNote=255→toneValid=false), T8.6.3 (midiNote=0→toneValid=true), T8.6.4 (round-trip), T8.6.5 (≠16 bytes→null) (depends on T015)
- [ ] T017 [US3] Create `app/src/main/kotlin/com/couchraoke/tv/data/network/UdpPitchReceiver.kt` — `bind(port: Int)`: `DatagramSocket(port)` bound once; `setActiveSong(instanceSeq: Long)`; `setPlayerConnection(playerId: Int, connectionId: Int)`; receive loop on dedicated thread: decode 16 bytes → apply drop rules (unknown connectionId, player/connection mismatch, songInstanceSeq mismatch, tvTimeMs regression >200ms) → invoke `onFrame(PitchFrame)` callback; `stop()` closes socket; create `app/src/test/kotlin/com/couchraoke/tv/data/network/UdpPitchReceiverTest.kt` covering T8.6.6 (connectionId mismatch→dropped), T8.6.7 (songInstanceSeq mismatch→dropped), T8.6.8 (unknown playerId→dropped), timestamp regression→dropped, valid frame→callback invoked (depends on T014, T015)

**Checkpoint**: US3 complete — pitch frames are received, validated, and routed correctly.

---

## Phase 5: User Story 4 — Song File Delivery (Priority: P2)

**Goal**: Song asset URLs from the manifest are passed directly to the media player; cleartext HTTP is allowed to phone IPs; manifest failures are handled gracefully.

**Independent Test**: Verify network_security_config.xml is present and correct; verify ManifestFetcher does not download or store audio files, only passes URLs; verify failure behavior with F18 fixture if applicable.

*Note: Core implementation is already complete (T001 network config, T009 ManifestFetcher). This phase adds the failure + range test coverage.*

- [ ] T018 [P] [US4] Extend `app/src/test/kotlin/com/couchraoke/tv/data/network/ManifestFetcherTest.kt` — add F18 fixture test cases: phone HTTP server returns 206 Partial Content for range request (verifies URL pass-through only — TV does not re-request); asset URLs stored as-received with no local path transformation; empty manifest (zero songs) → `addPhone` called with empty list

**Checkpoint**: US4 complete — song file delivery verified, HTTP config confirmed.

---

## Phase 6: User Story 5 — Clock Sync (Priority: P2)

**Goal**: TV runs 5-exchange clock sync burst on phone connect; suspends during song; resumes after song; phone can compute clock offset from all 4 timestamps.

**Independent Test**: Simulate ping/pong exchange sequence; verify 5-burst schedule; verify clockAck carries correct tTvRecvMs; verify suspend/resume; verify F14v2 fixture timestamps.

### Implementation for US5

- [ ] T019 [P] [US5] Create `app/src/main/kotlin/com/couchraoke/tv/domain/network/clock/ClockSyncEngine.kt` — constructor takes `clock: () -> Long` (injectable monotonic ms), `pingSender: PingSender` interface, and `coroutineScope: CoroutineScope` + `dispatcher: CoroutineDispatcher = Dispatchers.Default` (injected for testability per constitution §V); `startInitialSync(clientId)`: launches a coroutine that schedules 5 pings 100ms apart using `delay(100)`; `onPong(pong: PongMessage): ClockAckMessage`: builds `clockAck` with `tTvRecvMs = clock()`; `suspendSync()` / `resumeSingle(clientId)`: song-state management; define `fun interface PingSender { fun send(clientId: String, ping: PingMessage) }`; tests MUST use `StandardTestDispatcher` + `runTest { }` (never `UnconfinedTestDispatcher`)
- [ ] T020 [P] [US5] Create `app/src/test/kotlin/com/couchraoke/tv/domain/network/clock/ClockSyncEngineTest.kt` — unit tests: onPong returns clockAck with correct pingId and tTvRecvMs; startInitialSync dispatches exactly 5 pings; suspendSync prevents further pings; resumeSingle dispatches exactly 1 ping
- [ ] T021 [US5] Create `app/src/test/kotlin/com/couchraoke/tv/domain/network/clock/ClockSyncEngineAcceptanceTest.kt` — fixture F14v2: verify all 4 timestamps (tTvSendMs, tPhoneRecvMs, tPhoneSendMs, tTvRecvMs) present in a complete exchange; verify clockAck echoes correct pingId (depends on T019)

**Checkpoint**: US5 complete — clock sync produces all timestamps needed for accurate pitch frame timing.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T022 [P] Add `networkUnitTest` and `networkAcceptanceTest` Gradle tasks to `app/build.gradle.kts` following the same pattern as `libraryUnitTest` / `libraryAcceptanceTest`; include `**/network/**` and `**/session/**` paths
- [ ] T023 Run `./gradlew lint detekt` — fix all error-level Detekt and Android Lint findings (CI gate per constitution §V); then run `./gradlew test` — all tests must pass; run `./gradlew ciUnitTests` — JaCoCo ≥ 80% overall / ≥ 60% per file must pass; fix any coverage gaps before proceeding
- [ ] T024 Commit all changes to branch `005-network-protocol` with message `feat: 005-network-protocol complete`
- [ ] T025 After merge to master: rename branch to `[✓] 005-network-protocol`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately; T001/T002/T003 fully parallel
- **Phase 2 (Foundation)**: Depends on Phase 1; T004/T005/T007 fully parallel; T006 after T004; T008 after T005
- **Phase 3 (US1+US2)**: Depends on Phase 2; T009/T010 parallel; T011 after T004–T009; T012/T013 after T011
- **Phase 4 (US3)**: Depends on Phase 2; T014/T015/T016 parallel; T017 after T014+T015
- **Phase 5 (US4)**: Depends on T009 (ManifestFetcher exists)
- **Phase 6 (US5)**: Depends on Phase 2 only (ClockSyncEngine is pure domain)
- **Phase 7 (Polish)**: Depends on all story phases complete

### User Story Dependencies

- **US1 + US2 (P1)**: Depend on Foundation — no cross-story deps
- **US3 (P2)**: Depends on Foundation only — can run in parallel with US1+US2
- **US4 (P2)**: Depends on T009 from Phase 3 — thin phase
- **US5 (P2)**: Depends on Foundation only — can run in parallel with US1+US2 and US3

### Parallel Opportunities

```bash
# Phase 2 — run together:
T004  ProtocolMessages.kt
T005  ISessionGate.kt + FakeSessionGate.kt
T007  SessionToken.kt + SessionTokenTest.kt

# Phase 3+4 — after Phase 2, run all together:
T009  ManifestFetcher.kt          [US1]
T010  MdnsAdvertiser.kt           [US1]
T014  PitchFrame.kt               [US3]
T015  PitchFrameCodec.kt + test   [US3]
T016  PitchFrameCodecAcceptanceTest [US3]
T019  ClockSyncEngine.kt          [US5]
T020  ClockSyncEngineTest.kt      [US5]
```

---

## Implementation Strategy

### MVP (US1 + US2 only — Phases 1–3)

1. Phase 1: Setup (config files)
2. Phase 2: Foundation (message models, token, registry)
3. Phase 3: WebSocket server + ManifestFetcher + mDNS
4. **Validate**: Phone can join; songs appear; invalid joins rejected
5. Run `./gradlew test` — F15 T8.3.1–T8.3.13 must pass

### Full Delivery (all stories — Phases 1–7)

After MVP: add pitch stream (US3) → verify song file delivery (US4) → add clock sync (US5) → Polish

---

## Notes

- `[P]` = different files, no shared dependencies — safe to run in parallel
- `[US1]`/`[US2]` etc. = traceability to spec.md user stories
- `FakeSessionGate` (T005) is required for WebSocketServer tests in T012/T013
- `ISessionGate` interface is owned by this feature; feature 006 provides the real implementation
- Do NOT modify files outside this feature's scope; report out-of-scope bugs and stop
- Commit after each phase or logical group
