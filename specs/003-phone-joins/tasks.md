# Tasks: Phone Joins (Slice 1)

**Input**: Design documents from `specs/003-phone-joins/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Mandatory, not optional. FR-038 requires a fresh scoped `testBranch` pass **and** a passing loopback gate driven by the real out-of-process peer, and states neither substitutes for the other. FR-039 bars any in-process fake from satisfying a claim that the transport, the announcement, or the connection lifecycle works, and bars any skipped or conditionally-skipped test from satisfying a gate.

**Organization**: Grouped by user story so each is independently implementable and testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel — different files, no dependency on incomplete work
- **[Story]**: US1, US2, US3 — maps to the priorities in spec.md
- Every task carries an exact file path

## Path Conventions

Single Android module `:app`.

- Production: `app/src/main/kotlin/com/couchraoke/tv/`
- Tests: `app/src/test/kotlin/com/couchraoke/tv/`

Referred to below as **`«main»/`** and **`«test»/`**.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Declare dependencies and permissions before any code needs them. FR-037 forbids adding them ad hoc later.

- [X] T001 Add the three catalogue entries to `gradle/libs.versions.toml`: `zxing = "3.5.3"` under `[versions]`, then `zxing-core = { group = "com.google.zxing", name = "core", version.ref = "zxing" }`, `androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }`, and `kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }` under `[libraries]`. Do not add Hilt, navigation-compose, or bump `kotlin` — plan.md's Dependency Governance table records why each is refused.
- [X] T002 Wire the new entries in `app/build.gradle.kts`: `implementation(libs.zxing.core)`, `implementation(libs.androidx.lifecycle.viewmodel.compose)`, `testImplementation(libs.kotlinx.coroutines.test)`.
- [X] T003 [P] Add `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` to `app/src/main/AndroidManifest.xml` beside the existing `CHANGE_WIFI_MULTICAST_STATE`. Without it `ConnectivityLocalAddressProvider` returns null and every session start fails through the FR-028 modal, which looks like a logic bug.
- [X] T004 [P] Create the empty package directories under `«main»/`: `domain/session/model`, `domain/control/model`, `domain/platform`, `data/control`, `data/discovery`, `data/platform`, `presentation/join`, `presentation/songlist`, `presentation/qr`, `di`.
- [X] T005 Confirm the pre-existing suite is still green before changing behaviour: `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks`. Expect 55 tests, 0 failures, 0 skips. A regression here is from T001–T002, not from this feature's logic.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The session core, the wire codec, the four ports, the real transport, and the loopback harness. None of the three user stories is observable without a real socket, so the transport is foundational rather than owned by any single story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Value types and pure session core

- [X] T006 [P] Create the five value types in `«main»/domain/session/model/` — `SessionId.kt`, `DeviceId.kt`, `ConnectionId.kt`, `JoinCode.kt`, `AssetPort.kt` — each a `value class` with the `init` invariants in data-model.md (`DeviceId` length ≥ 8, `ConnectionId` 1..65535, `AssetPort` 1024..65535, `SessionId` non-empty). `JoinCode` is a `data class(adjective, noun)` exposing `display` as `ADJECTIVE-NOUN` uppercase.
- [X] T007 [P] Create `GamePhase.kt` in `«main»/domain/session/` as an enum of the nine states: `Open`, `Preparing`, `Countdown`, `Live`, `Paused`, `DisconnectPaused`, `Stopped`, `Results`, `Error`.
- [X] T008 [P] Create `SessionLifecycle.kt` in `«main»/domain/session/` as an enum `Open`, `Locked`, `Ended`. Distinct from `GamePhase` — this is what produces the `session_locked` refusal.
- [X] T009 Implement `GamePhaseMachine` in `«main»/domain/session/GamePhaseMachine.kt` per contracts/domain-api.md: a static allow-list, `canTransition`, and `transition` returning `PhaseTransitionResult.Accepted`/`Rejected`. A rejected transition must leave `current` untouched and must not throw (FR-026). Add `PhaseTransitionResult.kt` alongside.
- [X] T010 Write `GamePhaseMachineFixtureTest` in `«test»/domain/session/GamePhaseMachineFixtureTest.kt`, driving **every** entry of `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json` — all 20 valid edges accepted and all 8 invalid edges rejected — by reading the fixture, not by restating the table in Kotlin. Assert explicitly that `Open→Error` is **absent** from the valid list, since FR-028's modal depends on it.
- [X] T011 [P] Implement `ConnectionIdAllocator` in `«main»/domain/session/ConnectionIdAllocator.kt`: starts at 1, increments per call, never repeats within a session, wraps within uint16. Test in `«test»/domain/session/ConnectionIdAllocatorTest.kt`.

### Wire protocol

- [X] T012 [P] Create the serializable DTOs in `«main»/domain/control/model/` — `Hello.kt`, `SessionState.kt` (with `Slots`, `SlotDto`, `ConnectedDeviceDto`), `Refusal.kt` — with the exact property names, types, nullability and defaults pinned in contracts/wire-protocol.md. No `@SerialName` renaming. `Slots.P1`/`P2` need a scoped `@Suppress` for the naming rule with the reason recorded inline.
- [X] T013 [P] Create `RefusalReason` in `«main»/domain/control/RefusalReason.kt` as an enum carrying its wire `code` string, so the payload code and the WebSocket close reason cannot drift apart.
- [X] T014 Implement `ControlMessageCodec` in `«main»/domain/control/ControlMessageCodec.kt` per contracts/domain-api.md. Configure `Json` with `explicitNulls = false` and `ignoreUnknownKeys = false`; serialize outbound `sessionState` with `encodeDefaults = true` so the schema-required `type` and `protocolVersion` are emitted while nullable optionals stay absent. `connectionId` must be **omitted**, not null, outside the reply to a `hello` (FR-014).
- [X] T015 Write `ControlMessageCodecFixtureTest` in `«test»/domain/control/ControlMessageCodecFixtureTest.kt` asserting round-trips against `fixtures/F20_websocket_message_validation/`. Assert the three refusal cases byte-for-byte. Do **not** assert the accept case byte-for-byte — `case_valid_hello/expected.sessionState.json` omits the schema-required `connectedDevices` and fills `slots.P1` from a join, both of which contradict Appendix B.2.2. Validate the emitted payload against B.2.2 instead, and reference spec.md Out-of-Scope Observation 8 in a comment so the deviation is not mistaken for a bug.

### Ports

- [X] T016 [P] Declare `ControlTransport.kt` in `«main»/domain/control/` with `ControlTransport`, `StartedTransport`, `ControlConnectionHandler` and `ControlConnection`, exactly as signed in contracts/ports.md. `refuse(code, message)` must be a single method — splitting send from close is what produces the peer's exit 4.
- [X] T017 [P] Declare `SessionAnnouncer.kt` (with `AnnouncementHandle`), `LocalAddressProvider.kt` and `MulticastLease.kt` in `«main»/domain/platform/`, per contracts/ports.md. These use `java.net.Inet4Address`, never Android types.

### Adapters — thin, and excluded from coverage selection

> These four are never passed to `--src`. They carry no branching beyond null handling; their proof is the loopback gate (research.md R8).

- [X] T018 Implement `KtorControlTransport` in `«main»/data/control/KtorControlTransport.kt` using Ktor server CIO + WebSockets. Bind `0.0.0.0` so all interfaces accept (FR-008). Report the **actual** bound port via `StartedTransport.boundPort`, so port `0` yields an ephemeral port for tests. Read the token from the `token` query parameter without validating it. `refuse` sends the `error` frame, flushes, then closes with `CloseReason(VIOLATED_POLICY, reason = code)` — code 1008, reason equal to the code string (FR-016).
- [X] T019 [P] Implement `JmdnsSessionAnnouncer` in `«main»/data/discovery/JmdnsSessionAnnouncer.kt`: `JmDNS.create` bound to the supplied address, registering `_karaoke._tcp.local.` with TXT `code` and `v`. Read the registered instance name back into `AnnouncementHandle.registeredInstanceName` — jmDNS renames on collision, and a rename must surface rather than silently desynchronise the announcement from the join code (FR-004).
- [X] T020 [P] Implement `ConnectivityLocalAddressProvider` in `«main»/data/platform/ConnectivityLocalAddressProvider.kt` via `ConnectivityManager.activeNetwork` → `linkProperties`, returning the first `Inet4Address` that is neither loopback nor link-local, else null (research.md R3). Never a list.
- [X] T021 [P] Implement `WifiMulticastLease` in `«main»/data/platform/WifiMulticastLease.kt` over `WifiManager.createMulticastLock`. Both `acquire` and `release` must be idempotent so teardown cannot throw.

### Composition and host shell

- [X] T022 Implement `SessionCoordinator` in `«main»/domain/session/SessionCoordinator.kt` with the full surface from contracts/domain-api.md, plus `SessionEvent.kt`. In this phase implement construction, the two `StateFlow`s, `events`, `requestPhase` delegating to the machine, and `end()`. `authorize` and `admit` are stubbed to be filled by US1 and US2.
- [X] T023 Implement `SessionComponent` in `«main»/di/SessionComponent.kt` — manual construction of the coordinator from the four injected ports plus the generator and clock. No DI framework. This is also where the `SessionId` is **generated**: mint a fresh opaque non-empty identifier per session, unique among the sessions this TV creates (FR-001). No other task owns its creation; `SessionCoordinator` only receives it.
- [X] T024 Write `SessionCoordinatorTest` in `«test»/domain/session/SessionCoordinatorTest.kt`, creating the file, and assert session identity: the `SessionId` handed to the coordinator is non-empty, and constructing many sessions through `SessionComponent` yields no repeat (FR-001). Later tasks extend this same file, and it is already named in plan.md's completion gate.
- [X] T025 Move the multicast lock out of activity scope in `«main»/MainActivity.kt`: delete the `onStart`/`onStop` acquire/release and let the session own the `MulticastLease`, acquired at session start and released when the session ends (FR-005, research.md R5). As written, discovery dies whenever the user switches TV inputs while the session is still live.
- [X] T026 Create the song-selection shell in `«main»/presentation/songlist/SongListScreen.kt`: an empty-state surface with a header Join action, since no library exists in this slice (FR-029). Host it from `MainActivity`. Not parallel with T025 — both edit `MainActivity.kt`.

### Loopback harness

- [X] T027 Build the peer-driving harness in `«test»/gate/MockPhonePeer.kt`: launch `uv run mock-phone` as a subprocess with the given flags, capture **stderr — not stdout**, parse the single `JOIN_RESULT {…}` JSON line, and expose the exit status plus the parsed fields. Map the statuses per quickstart.md — **exit 4 and exit 6 must be surfaced as assertion failures with an explanatory message**, not as generic non-zero exits: 4 means a refusal closed without delivering its reason (FR-016/FR-017) and 6 means the 5-second deadline was never enforced. Two verified traps, see spec.md Obs 17: the peer writes **nothing** to stdout (`logging.basicConfig` with no `stream=` defaults to `sys.stderr`), so redirect stderr or merge both; and the line is timestamp-prefixed — `13:40:28.197 [INFO    ] mock_phone_reconnect: JOIN_RESULT {…}` — so match on `indexOf("JOIN_RESULT ")` and parse from there, never `startsWith`. Set `UV_SYSTEM_CERTS=1` in the **subprocess** environment.
- [X] T028 Create `«test»/gate/LoopbackJoinGateTest.kt` with a JUnit rule that starts the real `KtorControlTransport` on port 0 against a real `SessionCoordinator`, exposes `boundPort`, and tears down after each test. No in-process transport fake anywhere in this file (FR-039). Leave the per-story cases to T037, T047 and T055.

**Checkpoint**: Foundation ready. Gate:

```powershell
.\gradlew.bat :app:testBranch --rerun-tasks `
  --src com.couchraoke.tv.domain.session.GamePhaseMachine `
  --src com.couchraoke.tv.domain.session.ConnectionIdAllocator `
  --src com.couchraoke.tv.domain.control.ControlMessageCodec `
  --test com.couchraoke.tv.domain.session.GamePhaseMachineFixtureTest `
  --test com.couchraoke.tv.domain.session.ConnectionIdAllocatorTest `
  --test com.couchraoke.tv.domain.control.ControlMessageCodecFixtureTest
```

---

## Phase 3: User Story 1 — A phone finds the TV and joins the session (Priority: P1) 🎯 MVP

**Goal**: A phone discovers the TV on the LAN or scans its QR code, connects, is admitted, and appears in the TV's connected list. The host sees the join code, the QR code, and a live connected count.

**Independent Test**: Launch the app, run the real peer against it over loopback with discovery enabled, and confirm the peer's device name appears in the connected list and the peer received a session identity. No song, library, or playback capability required.

### Validation for User Story 1 ⚠️

> Write these first and confirm they fail before implementing.

- [X] T029 [P] [US1] Write `JoinCodeGeneratorTest` in `«test»/domain/session/JoinCodeGeneratorTest.kt`: a seeded `Random` yields a deterministic pair, `display` renders `ADJECTIVE-NOUN` uppercase, and no code repeats across many generations (FR-003).
- [X] T030 [P] [US1] Write `SessionRosterTest` in `«test»/domain/session/SessionRosterTest.kt` for the admit path only: a new device is `Admitted`, size grows by one, and `connected` reflects it. Capacity and reclaim belong to US2 and US3.
- [ ] T031 [P] [US1] Write `QrPayloadEncoderTest` in `«test»/presentation/qr/QrPayloadEncoderTest.kt`: the payload is exactly `ws://<ip>:<port>/?token=<CODE>` and carries no discovery-service identifier (FR-007).
- [ ] T032 [P] [US1] Write `JoinViewModelTest` in `«test»/presentation/join/JoinViewModelTest.kt`: `connectedCount` tracks the coordinator's connected list and follows a join with no user action (SC-007), and the view model performs no I/O (FR-035).

### Implementation for User Story 1

- [X] T033 [P] [US1] Implement `JoinCodeGenerator` in `«main»/domain/session/JoinCodeGenerator.kt` with two bundled ~64-word `List<String>` constants, an injected `Random`, and a per-process issued-set so no code repeats (research.md R7). Constants, not an `assets/` file, so the class stays JVM-pure.
- [X] T034 [US1] Implement `SessionRoster` in `«main»/domain/session/SessionRoster.kt` — `RosterEntry` in `«main»/domain/session/model/RosterEntry.kt`, `RosterAdmission` alongside — covering the admit path and the `connected` derivation. Capacity and reclaim land in US2/US3.
- [X] T035 [US1] Complete `SessionCoordinator.admit` for the success path: validate, allocate a `ConnectionId`, admit to the roster, emit `SessionEvent.Connected`, and return a decision carrying the `sessionState` reply with `connectionId` set (FR-012, FR-013).
- [ ] T036 [US1] Wire session start in `«main»/di/SessionComponent.kt` and `«main»/MainActivity.kt`: resolve the active IPv4, acquire the multicast lease, start the transport, publish the announcement with instance name `KaraokeTV-<noun>`, and verify the registered name matches (FR-004, FR-005, FR-008).
- [ ] T037 [US1] Add the happy-path case to `«test»/gate/LoopbackJoinGateTest.kt`: `--join-only` with the correct token exits 0, returns `sessionState`, and yields `connectionId` ≥ 1; a second peer with a different `--client-id` also joins and receives a **distinct** `connectionId` (acceptance scenario 4).
- [ ] T038 [P] [US1] Implement `QrPayloadEncoder` in `«main»/presentation/qr/QrPayloadEncoder.kt` — pure string construction, no ZXing type in the signature so it stays coverage-selected.
- [ ] T039 [P] [US1] Implement `QrBitmapRenderer` and the `QrCode` composable in `«main»/presentation/qr/`: call `QRCodeWriter.encode` with `EncodeHintType.MARGIN = 4` for the four-module quiet zone, and draw the `BitMatrix` into a Compose `ImageBitmap`, dark-on-light and static (FR-030).
- [ ] T040 [US1] Implement `JoinUiState` and `JoinViewModel` in `«main»/presentation/join/`: expose the join code, the QR payload, and `connectedCount` derived from `coordinator.connectedDevices.size` — the live connections, never the roster size (FR-025, SC-007).
- [ ] T041 [US1] Implement `JoinOverlay` in `«main»/presentation/join/JoinOverlay.kt` as a modal over the song-list shell, QR dominant with the join code directly beneath, connected count shown, entrance limited to a single short fade or scale-fade, and nothing intruding into the QR or its quiet zone (FR-030, FR-031, FR-032). Dismissing returns to the shell without ending the session or disconnecting anyone (FR-033). Tag nodes with test tags so T061 can assert their bounds.
- [X] T042 [US1] Add the disconnect path to `SessionCoordinator.onDisconnected` and `SessionRoster.detach`: remove from the connected list as part of handling the close, **before** any subsequent admission decision, while retaining the roster entry and its capacity slot (FR-023, acceptance scenario 5). Cover the ordering in `«test»/domain/session/SessionCoordinatorTest.kt`. *(Implemented as a plain synchronous test rather than the virtual-time one originally specified: `admit` and `onDisconnected` are non-`suspend` per contracts/domain-api.md and contain no suspension point, so there is no scheduling to control and a `runTest` wrapper would add ceremony without adding proof. The ordering is proved by interleaving instead — admit A, disconnect A, assert the projection is already updated, then admit B and assert B's decision saw A gone.)*

**Checkpoint**: US1 is independently functional. Gate — scoped `testBranch` adding `JoinCodeGenerator`, `SessionRoster`, `SessionCoordinator`, `JoinViewModel`, `QrPayloadEncoder`, **plus** `LoopbackJoinGateTest`. Per FR-038 neither alone completes the story.

---

## Phase 4: User Story 2 — Bad or unwelcome connections are refused clearly (Priority: P2)

**Goal**: Every wrong code, unsupported version, incomplete introduction, capacity overflow, and silent connection is turned away with a machine-readable reason delivered before the socket closes.

**Independent Test**: Drive each rejection from the real peer over loopback and assert the reason code, the delivery of the refusal before the close, and the close code. Independently, replay the F20 cases against the validator with no transport involved.

### Validation for User Story 2 ⚠️

- [ ] T043 [P] [US2] Write `HandshakeValidatorFixtureTest` in `«test»/domain/control/HandshakeValidatorFixtureTest.kt` driving all four `fixtures/F20_websocket_message_validation/` cases, asserting the reason code **and** the exact human-readable message (`"Missing required field: clientId"`, `"Unsupported protocolVersion: 2"`). Add unparseable-JSON and unknown-extra-field cases, both `invalid_message`.
- [ ] T044 [P] [US2] Write `JoinCodeMatcherTest` in `«test»/domain/control/JoinCodeMatcherTest.kt`: `SWIFT-PANDA`, `swift-panda` and `swiftpanda` all match; null, blank and a wrong code do not.
- [ ] T045 [P] [US2] Extend `«test»/domain/session/SessionRosterTest.kt` for capacity: the 11th previously-unseen device is `AtCapacity`, and capacity is evaluated only for unseen devices.
- [ ] T046 [P] [US2] Add a virtual-time deadline test to `«test»/domain/session/SessionCoordinatorTest.kt` using `kotlinx-coroutines-test`: a connection that authorizes and then stays silent is refused at 5 s, and never appears in the connected list or consumes a roster slot (FR-017, SC-004).
- [ ] T047 [US2] Add all five refusal cases to `«test»/gate/LoopbackJoinGateTest.kt`: wrong token → exit 3 `invalid_token`; `--protocol-version 2` → exit 3 `protocol_mismatch`; `--malformed-hello clientId` and `--malformed-hello invalid-json` → exit 3 `invalid_message`; `--silent-handshake --join-timeout 10` → exit 3 `invalid_message` within 5 s. Assert `closeCode` is 1008 and `closeReason` equals the error code in every case.

### Implementation for User Story 2

- [ ] T048 [P] [US2] Implement `JoinCodeMatcher` in `«main»/domain/control/JoinCodeMatcher.kt`: `normalize` uppercases and strips hyphens and surrounding whitespace; `matches` returns false for null or blank.
- [ ] T049 [US2] Implement `HandshakeValidator` in `«main»/domain/control/HandshakeValidator.kt` with `AdmissionDecision.kt` alongside. Follow the normative order from contracts/domain-api.md: parse failure, then wrong `type`, then `protocolVersion`, then missing fields in `required`-array order, then range checks. Version precedes field presence because a version-2 phone may legitimately carry a different field set. Decode to `JsonObject` first so the reported missing field is deterministic rather than dependent on kotlinx's batching.
- [ ] T050 [US2] Implement `SessionCoordinator.authorize` to reject a missing or wrong token with `invalid_token` (FR-009), and complete `admit`'s refusal branches for `protocol_mismatch`, `invalid_message` and `session_full`.
- [ ] T051 [US2] Add capacity to `SessionRoster.admit`: return `AtCapacity` when `size == capacity` **and** the `deviceId` is previously unseen (FR-015). A known device must never be refused for capacity.
- [ ] T052 [US2] Enforce the handshake deadline in `KtorControlTransport`: wrap the wait for the first frame in `withTimeoutOrNull(5.seconds)` and on expiry call `refuse("invalid_message", …)` — the `error` frame is delivered, then the close (FR-017, research.md R6). A pending connection must hold no roster slot.
- [ ] T053 [US2] Implement FR-018's asymmetry in `KtorControlTransport` and the coordinator: an unexpected message type **during** the handshake is fatal and closes the connection; an unrecognised type **after** a successful handshake is logged as a warning and ignored, leaving the connection open.

**Checkpoint**: US1 and US2 both work independently. Gate — scoped `testBranch` adding `HandshakeValidator` and `JoinCodeMatcher`, plus the loopback gate with all five refusal cases.

---

## Phase 5: User Story 3 — A phone that drops off reclaims its place (Priority: P3)

**Goal**: A returning device reuses its roster entry, keeps its capacity slot while away, and receives a fresh connection identity.

**Independent Test**: Connect the real peer, force a drop and reconnect with the same device identity, then assert the roster size is unchanged, the entry is the same one, the phone left and re-entered the connected list, and its connection identity is new.

### Validation for User Story 3 ⚠️

- [ ] T054 [P] [US3] Extend `«test»/domain/session/SessionRosterTest.kt` for reclaim: a known `deviceId` returns `Reclaimed` with size unchanged; reclaim succeeds at capacity; reclaim succeeds whether or not a live connection currently exists (FR-020, FR-021); and `detach` with a stale `ConnectionId` returns false and mutates nothing (FR-022).
- [ ] T055 [US3] Add the reclaim cases to `«test»/gate/LoopbackJoinGateTest.kt`: ten sequential `--join-only` peers fill the roster; an 11th unseen device gets exit 3 `session_full`; one of the original ten rejoins and gets exit 0 with a **different** `connectionId` (SC-005, SC-006). Ten sequential peers suffice because a disconnected device keeps its slot in this slice — `--hold` is needed only where the live count matters.
- [ ] T056 [P] [US3] Add a supersession test to `«test»/domain/session/SessionCoordinatorTest.kt`: with a replacement connection admitted for a device, the superseded connection's late close must not remove the roster entry or invalidate the new `connectionId` (FR-022, acceptance scenario 4).

### Implementation for User Story 3

- [ ] T057 [US3] Complete `SessionRoster.admit`'s reclaim branch and `detach`'s identity guard in `«main»/domain/session/SessionRoster.kt`: a known `deviceId` reuses its entry, refreshes `displayName`/`appVersion`/`assetPort` from the new `hello`, takes a fresh `ConnectionId`, and leaves size unchanged. `detach` no-ops unless the supplied `ConnectionId` is still the entry's active one. Also add `release` and `releaseDisconnected` — unreachable in this slice but required so capacity semantics are complete and testable now (FR-024). **Delete `SessionRosterTest.admittingAnAlreadyPresentDeviceIsNotYetImplemented` and replace it with the real reclaim assertions**: T034 left that characterization test asserting the reclaim branch throws `NotImplementedError`, so it is a deliberate tripwire that will fail the moment you implement this task. That failure is expected — do not "repair" it by keeping the branch unimplemented. Also remove `@Suppress("UnusedParameter")` from `detach`, whose `connectionId` becomes load-bearing here.
- [ ] T058 [US3] Emit `SessionEvent.Reconnected` carrying the previous `ConnectionId` from `SessionCoordinator.admit` when the roster reports `Reclaimed` (FR-019).

**Checkpoint**: All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T059 Write the FR-028 validation in `«test»/presentation/join/JoinViewModelTest.kt`: each of the three start failures — bind failure, announcement failure, no usable IPv4 — surfaces the blocking notice in `JoinUiState`; acknowledging it clears the notice and returns to song selection; and the game phase is still `GamePhase.Open` afterwards, with `GamePhaseMachine` never asked for a transition (SC-008). Without this the one requirement whose entire purpose is handling failure ships unproven.
- [ ] T060 Implement the FR-028 blocking notice: `SessionStartFailure` in `«main»/domain/session/SessionStartFailure.kt` (bind failure, announcement failure, no usable address), surfaced by `JoinViewModel` and rendered as a modal over the song-list shell, dismissible with one acknowledgement that returns to song selection. It must **not** change the game phase and must not attempt a transition — F22 has no `Open→Error` edge (SC-008).
- [ ] T061 [P] Write `JoinOverlayBoundsTest` in `«test»/presentation/join/JoinOverlayBoundsTest.kt` using `getUnclippedBoundsInRoot()` under the pinned `w960dp-h540dp-land-television-xhdpi-notouch` qualifier: every expected element present; nothing exceeding 960×540dp; the QR between 30% and 55% of the shorter viewport dimension; the join code directly beneath the QR with nothing between; and no node overlapping the QR's bounds expanded by its quiet zone (research.md R9, FR-030, FR-031).
- [ ] T062 Confirm no screenshot baseline is recorded by this feature — `git status` must show nothing added under a Roborazzi output directory. The gate is in verify mode and Slice 3 owns baseline creation at the corrected viewport (spec.md Assumptions).
- [ ] T063 [P] Add an FR-027 guard test in `«test»/domain/session/SessionCoordinatorTest.kt` asserting no runtime path in this slice leaves `GamePhase.Open`.
- [ ] T064 Run the full feature gate from plan.md's Validation Gate section with `--rerun-tasks`, then `.\gradlew.bat :app:testDebugUnitTest --tests "*LoopbackJoinGateTest*"`. Both must pass fresh. Confirm 0 skips — FR-039 bars a skipped test from satisfying a gate. Then run the **whole** suite once more, `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks`, and confirm the **54** surviving Slice 0 tests still pass unchanged (SC-009). The scoped gate selects only this feature's classes, so it cannot detect a Slice 0 regression; T005's run was taken before any behaviour changed and proves nothing about the end state. **The baseline is 54, not the 55 T005 measured**, because T025 necessarily deleted `MainActivityTest.activityStartAcquiresAndStopReleasesMulticastLock` — see the Notes below. Do not "restore" it to make the arithmetic work.
- [ ] T065 Walk quickstart.md's "Manual run on a real LAN" end to end: `:app` installed on a real Android device on the LAN, and `mock-phone --join-only --discover` run from a **different** machine on that same LAN, to prove FR-004 and SC-001 outside loopback. `--discover` is mandatory — omitting `--tv-host` is an argparse error, not a fallback to mDNS — and it deliberately ignores `--token`, so a successful join proves the TV registered the service and published the right code in its TXT record. Then verify SC-002's payload half: decode the on-screen QR with any stock camera app, confirm it carries the address the TV is actually reachable on, and dial exactly that with `--tv-host`. **SC-002's single-scan gesture is out of scope and stays unproven** — no companion phone app exists in this repository or on any branch, so no task in this slice can prove it. See spec.md Out-of-Scope Observation 14.
- [ ] T066 [P] Update `specs/003-phone-joins/spec.md`, marking Implementation Readiness items 5–9 resolved with the commits that closed them.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on Setup. **Blocks all user stories.**
- **US1 (Phase 3)**: depends on Foundational.
- **US2 (Phase 4)**: depends on Foundational. Shares `SessionRoster` and `SessionCoordinator` with US1, so it is validatable independently but not editable in parallel with US1 without conflict.
- **US3 (Phase 5)**: depends on Foundational, and genuinely on US2's capacity code — acceptance scenario 3 is "reclaim at capacity", which cannot be asserted until capacity exists.
- **Polish (Phase 6)**: depends on all three.

### Honest note on story independence

The template's ideal is fully parallel stories. That does not hold cleanly here, and pretending otherwise would produce merge conflicts. All three stories converge on two files, `SessionRoster.kt` and `SessionCoordinator.kt`, because admission, refusal and reclaim are three branches of one decision. Each story remains independently **testable** and independently **demoable**, which is what the checkpoints assert; they are not independently **editable**. Run them sequentially in priority order.

### Within Each User Story

- Validation tasks first, failing, before implementation.
- Value types → roster → coordinator → transport wiring → UI.
- The loopback case for a story comes after that story's logic exists, since it drives a real socket.

### Parallel Opportunities

- T003, T004 in Setup.
- T006, T007, T008, T011, T012, T013, T016, T017 in Foundational — all distinct files.
- T019, T020, T021 — three independent adapters. **T018 is not parallel** with them: it is the largest adapter and the transport everything else attaches to.
- All four US1 validation tasks (T029–T032).
- T038, T039 — the QR pair, distinct files.
- US2 validation T043, T044, T045, T046.
- T061, T063, T066 in Polish.

---

## Parallel Example: User Story 1

```text
# Launch all US1 validation tasks together:
Task: "Write JoinCodeGeneratorTest in «test»/domain/session/JoinCodeGeneratorTest.kt"
Task: "Write SessionRosterTest in «test»/domain/session/SessionRosterTest.kt"
Task: "Write QrPayloadEncoderTest in «test»/presentation/qr/QrPayloadEncoderTest.kt"
Task: "Write JoinViewModelTest in «test»/presentation/join/JoinViewModelTest.kt"

# Then the two QR implementation tasks together:
Task: "Implement QrPayloadEncoder in «main»/presentation/qr/QrPayloadEncoder.kt"
Task: "Implement QrBitmapRenderer and QrCode in «main»/presentation/qr/"
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1: Setup.
2. Phase 2: Foundational — the long pole, and the slice's one risky new thing.
3. Phase 3: US1.
4. **Stop and validate**: scoped `testBranch` plus the loopback happy path.
5. Demoable: a phone finds the TV and joins.

### Incremental Delivery

1. Setup + Foundational → a real socket and a real announcement.
2. US1 → a phone joins → demo (MVP).
3. US2 → every refusal proved against the real peer → demo.
4. US3 → reclaim and honest capacity → demo.
5. Polish → both gates fresh, plus a real-LAN run.

---

## Notes

- **Both gates or neither.** FR-038 requires a fresh scoped `testBranch` **and** the loopback gate. A green `testBranch` alone does not complete any story that touches the transport.
- **Adapters are never `--src` selected.** `KtorControlTransport`, `JmdnsSessionAnnouncer`, `ConnectivityLocalAddressProvider` and `WifiMulticastLease` cannot reach 80% line coverage from JVM tests without a fake, which FR-039 bars. Their proof is the loopback gate. Keep them free of logic or the coverage gate will fail on correct code.
- **The coverage gate enforces LINE ≥ 80% and BRANCH ≥ 70%, per bundle.** See plan.md §2. BRANCH is skipped automatically on classes with no decisions, so it never penalises straight-line code — but it also means BRANCH alone would prove nothing, which is why both limits exist. Because the rule aggregates over the whole `--src` selection, keep selections tight.
- **Task order does not compile as written.** T022 constructs `SessionCoordinator`, whose binding constructor needs `SessionRoster` (T034), `HandshakeValidator` (T049) and `JoinCodeMatcher` (T048); T023 needs `JoinCodeGenerator` (T033). Resolved during implementation by forward-declaring those classes at T022 with their `contracts/domain-api.md` signatures and `TODO()` bodies naming the owning task, leaving all behaviour to the tasks above. The task numbering was deliberately **not** changed. Implementing the behaviour early would collide with the story that owns the file and leave its tests nothing to prove.
- **Types with no owning task**, all forward-declared at T022 for the same reason: `SessionSnapshot` and `ConnectedDevice` (required by `SessionCoordinator`'s binding signature; specified in data-model.md but created by no task), `AdmissionDecision` (named in `authorize`/`admit` from T022 but scheduled for creation at T049), `ControlEndpoint` (`JoinViewModel`'s third constructor parameter, created by no task — T040 should own it), and `SessionRoster.clear()` (in the contract, in no task; still `TODO()`).
- **Peer exit 4 and exit 6 are failures, not variants.** 4 means a refusal closed without delivering its reason; 6 means the deadline was never enforced.
- **The Slice 0 baseline is 54 tests from T025 onward, not 55.** T025 deletes the activity-scoped multicast lock, and `MainActivityTest` contained exactly one test — `activityStartAcquiresAndStopReleasesMulticastLock` — which reflectively read `MainActivity.multicastLock` and asserted acquire-on-start/release-on-stop. That is precisely the behaviour FR-005 and research.md R5 call wrong and T025 exists to remove, so the test asserted the defect. It was deleted rather than rewritten: post-T025 `MainActivity` holds no multicast state to assert, and `MainActivity` is not `--src` selected, so a replacement would have been decoration. FR-005 is proved instead by T036's wiring and the L gate. Counts after T025: 54 Slice 0 + 18 Slice 1 = 72.
- **`verifyRoborazziDebug` cannot pass in this repository and is not a gate for this slice.** See spec.md Out-of-Scope Observation 16. Layout is proved numerically by `JoinOverlayBoundsTest` (T061). Do not record a baseline to make it green — quickstart.md forbids it, and the viewport scale is wrong until Slice 3.
- **A cached green is not evidence.** Use `--rerun-tasks` when reporting a gate.
- **`-Proborazzi.record=true` breaks in PowerShell** — it splits at the dot. Use `-ProborazziRecord=true`. Not needed in this slice.
- **`mockphone` needs `UV_SYSTEM_CERTS=1`** in its environment, and the loopback harness must set it in the **subprocess** environment or the peer never launches.
- Concurrent peers need distinct `--client-id`, or each reclaims the same entry and the roster never fills.
- Commit after each task or logical group.
