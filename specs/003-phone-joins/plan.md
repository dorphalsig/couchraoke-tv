# Implementation Plan: Phone Joins

**Branch**: `DH1-slice-1-phone-joins` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/003-phone-joins/spec.md`

## Summary

A phone finds the TV on the LAN and joins its session. The TV creates a session at launch with a two-word join code, publishes a `_karaoke._tcp` announcement, serves a WebSocket control endpoint, and shows a join overlay carrying a QR code and a live connected count. Admission validates the token, the protocol version, and the introduction, refuses with a delivered reason and close code `1008`, caps the roster at ten devices, and readmits a returning device to its existing entry with a fresh connection identity.

The approach keeps every decision in pure Kotlin. Admission, roster, join-code matching, phase transitions, and payload construction are framework-free classes with no Android or Ktor types. Ktor, jmDNS, `ConnectivityManager`, `WifiManager` and Compose sit behind four narrow ports and hold no logic. This is what makes the 80% coverage gate reachable, and it is what lets the loopback gate exercise the real transport rather than a fake.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11, minSdk 30, targetSdk 36
**Primary Dependencies**: Ktor server CIO + WebSockets, jmDNS, Kotlinx Serialization JSON, Compose for TV, ZXing core, Lifecycle ViewModel
**Storage**: None — all session state is in-memory
**Testing**: JUnit 4 + Robolectric 4.16.1 via `quality-conventions`, Compose UI test JUnit4 for bounds assertions, and the real out-of-process `mockphone` peer for the loopback gate.
**Target Platform**: Android TV, S905X4 box, 1920×1080 at `w960dp-h540dp-land-television-xhdpi-notouch`
**Project Type**: Single-module Android application
**Performance Goals**: A phone appears in the connected list within 5 s of starting to look (SC-001). The handshake deadline is a hard 5 s (FR-017). No frame-rate or throughput target applies — this slice renders one static overlay.
**Constraints**: LAN-only, no internet path. The control server accepts on all interfaces but advertises exactly one IPv4 address. Coverage ≥ 80% line on selected classes. No skipped or conditionally-skipped test may satisfy a gate.
**Scale/Scope**: 10 devices per session, one session per TV, two screens (song-list shell + join overlay), one WebSocket endpoint, one mDNS service.

Ktor server, jmDNS, coroutines, serialization and the Compose TV libraries are **already** declared in `gradle/libs.versions.toml`. ZXing `core`, `lifecycle-viewmodel-compose` and `kotlinx-coroutines-test` are added by this plan; see Dependency Governance below. The module is `:app`, alongside the included `quality-conventions` build plugin. The constitution forbids persisting remote song assets, and this slice persists nothing at all.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

*Checked against constitution **v2.0.0**. This plan was originally written under v1.0.0 and re-checked when v2.0.0 landed; no section needed rewriting. The three tightened clauses in Principle IV were already satisfied: an L gate is declared below, screenshots verify against a committed baseline rather than recording, and no gate in this feature contains `assumeTrue` or `@Ignore`. Principle I's newly pinned 20-byte `<IqIBBH` frame is not exercised by this slice — the datagram socket is deliberately unbound until Slice 8.*

### Host Authority — PASS

The TV creates the session identity, the join code, every connection identity, and the roster, and no companion-supplied value overrides them (FR-034). `HandshakeValidator` reads the phone's `hello` but derives nothing authoritative from it; `ConnectionIdAllocator` is TV-owned and monotonic. LAN-only holds: no outbound internet call exists in this slice. Streaming-only remote assets is preserved trivially — the asset-server port is recorded and never contacted (spec Assumptions). Fixed-size UDP pitch transport is untouched: the datagram socket is deliberately not bound here (Slice 8 owns it), so no transport decision is pre-empted.

### Architecture Boundaries — PASS

Four ports separate framework from logic. `ControlTransport`, `SessionAnnouncer`, `LocalAddressProvider` and `MulticastLease` are declared in `domain` and implemented in `data`. Ktor, jmDNS, `ConnectivityManager`, `WifiManager.MulticastLock` and Compose appear only in `data` and `presentation` (FR-036). `JoinViewModel` is the single source of UI state and performs no I/O; it observes `SessionCoordinator` state and maps it to `JoinUiState` (FR-035). Admission and phase logic live in `domain` and import nothing from Android.

### Dependency Governance — PASS WITH ADDITIONS

Every change below goes through `gradle/libs.versions.toml`. No ad hoc addition is permitted during task execution (FR-037).

| Change | Coordinate | Why this slice needs it | Alternative rejected |
|---|---|---|---|
| Add | `com.google.zxing:core:3.5.3` | FR-007/FR-030 require a QR code. Nothing in the catalogue can generate one. | `zxing-android-embedded` pulls a camera activity and scanner UI for a job that is encode-only. `core` returns a `BitMatrix` we draw ourselves onto a Compose `ImageBitmap`, keeping Android types out of the encoder. |
| Add | `androidx.lifecycle:lifecycle-viewmodel-compose` (ref `lifecycleRuntimeKtx`, 2.10.0) | FR-035 requires the join surface to read state from a view model. The catalogue has `lifecycle-runtime-ktx` only, which does not provide `viewModel()`. | Hoisting state into a plain composable would put session observation in UI code, which FR-035 forbids. |
| Add | `org.jetbrains.kotlinx:kotlinx-coroutines-test` (ref `coroutines`, 1.10.2) | The 5 s handshake deadline (FR-017) and the ordering guarantee in FR-023 need deterministic virtual time. | Real-time waits would make the suite slow and flaky, and `Thread.sleep` in a gate is the kind of dishonesty Principle IV targets. |

**Deliberately not added, with rationale:**

- **Hilt.** `CLAUDE.md` lists it, but this slice has one activity and one view model. Introducing a DI framework, its Gradle plugin and its kapt/KSP step alongside the first real transport violates `plan.md`'s "one risky new thing per slice". Manual construction at the composition root is sufficient here; Hilt is a planned addition for the slice that first needs graph-scoped lifetimes.
- **navigation-compose.** The join overlay is a modal over one screen, not a navigation destination. Two screens do not justify a navigation graph.
- **Kotlin 2.3.20.** `CLAUDE.md` records it for this feature but the catalogue pins 2.2.10 and the whole toolchain — AGP 9.1.1, Compose compiler, serialization and detekt plugins — is aligned to it. A language bump is an unrelated risk with no requirement behind it. Flagged as an out-of-scope observation rather than bundled here.

### Contracts First — PASS

Every producer/consumer boundary is pinned as FQCN + method + signature in [contracts/](./contracts/), with field-level payload contracts in [data-model.md](./data-model.md). The four ports, the coordinator's public surface, the validator, the roster, the phase machine and both wire payloads are all specified before implementation.

### Workflow Units — PASS

One `spec.md`, one `plan.md`, one `tasks.md` (the latter produced by `/speckit.tasks`, not here). Phases below are dependency buckets: each may only start when the one before it has a passing gate.

### Validation Gate — DEFINED

Scoped `testBranch` per phase, listed in each phase below. Three rules apply throughout. First, adapters are excluded from `--src` selection because they are thin by construction and cannot reach 80% from JVM tests; their behaviour is proved by the loopback gate instead. Second, and per FR-038, a scoped `testBranch` pass alone never completes a phase that touches the transport — the loopback gate against the real peer must also pass, and neither substitutes for the other. Third, per constitution v2.0.0 Principle IV, the loopback gate is this slice's mandatory **L** gate: a slice proved only by U and S is not proved, and a gate satisfied by a skipped test is a failure rather than a pass.

**The coverage gate enforces LINE ≥ 80% and BRANCH ≥ 70%, evaluated per bundle.**
`QualityConventionsPlugin.kt` originally declared `violationRules { rule { limit { minimum = … } } }`
with no `counter`, so JaCoCo silently applied its `INSTRUCTION` default while every document here
described the gate as line coverage. That was corrected during this slice with explicit `counter`
values. Three consequences worth budgeting against:

- **BRANCH cannot be the only limit.** A class with no `if`/`when` has a 0/0 branch ratio, which
  JaCoCo evaluates as `NaN`, and `Limit.check` returns no violation for `NaN`. A branch-only rule
  would pass most of `domain/` without measuring anything. LINE supplies the floor; BRANCH sharpens
  it where decisions exist.
- **BRANCH runs 10–20 points below LINE on the same code**, which is why the thresholds differ. 70
  is not a weaker standard than 80, it is the same rigor in a stricter metric.
- **The rule evaluates `element = BUNDLE`**, JaCoCo's default — the aggregate over the `--src`
  selection, not each class independently. Keep `--src` selections tight, or a well-covered class
  will mask a poorly covered one.

INSTRUCTION is deliberately not enforced: it counts Kotlin's generated `copy`, `componentN` and
`equals`/`hashCode`, and it penalises `TODO()` stubs for instructions no test can reach.

Run `testBranch`, never `testDebugUnitTest --tests "…"`, as a task gate. The latter skips detekt and
JaCoCo entirely, so it reports green having checked strictly less than the gate claims.


**Feature-level completion gate:**

```powershell
.\gradlew.bat :app:testBranch `
  --src com.couchraoke.tv.domain.session.SessionRoster `
  --src com.couchraoke.tv.domain.session.GamePhaseMachine `
  --src com.couchraoke.tv.domain.session.JoinCodeGenerator `
  --src com.couchraoke.tv.domain.session.ConnectionIdAllocator `
  --src com.couchraoke.tv.domain.control.HandshakeValidator `
  --src com.couchraoke.tv.domain.control.JoinCodeMatcher `
  --src com.couchraoke.tv.domain.control.ControlMessageCodec `
  --src com.couchraoke.tv.domain.session.SessionCoordinator `
  --src com.couchraoke.tv.presentation.join.JoinViewModel `
  --src com.couchraoke.tv.presentation.qr.QrPayloadEncoder `
  --test com.couchraoke.tv.domain.session.SessionRosterTest `
  --test com.couchraoke.tv.domain.session.GamePhaseMachineFixtureTest `
  --test com.couchraoke.tv.domain.session.JoinCodeGeneratorTest `
  --test com.couchraoke.tv.domain.session.ConnectionIdAllocatorTest `
  --test com.couchraoke.tv.domain.control.HandshakeValidatorFixtureTest `
  --test com.couchraoke.tv.domain.control.JoinCodeMatcherTest `
  --test com.couchraoke.tv.domain.control.ControlMessageCodecFixtureTest `
  --test com.couchraoke.tv.domain.session.SessionCoordinatorTest `
  --test com.couchraoke.tv.presentation.join.JoinViewModelTest `
  --test com.couchraoke.tv.presentation.join.JoinOverlayBoundsTest `
  --test com.couchraoke.tv.presentation.qr.QrPayloadEncoderTest
```

plus the loopback gate:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LoopbackJoinGateTest*"
```

## Project Structure

### Documentation (this feature)

```text
specs/003-phone-joins/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── ports.md             # domain-declared ports, FQCN + signature
│   ├── domain-api.md        # coordinator, validator, roster, phase machine
│   └── wire-protocol.md     # hello / sessionState / error field contracts
├── checklists/
│   └── requirements.md  # existing
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/couchraoke/tv/
├── domain/
│   ├── session/                     # pure; no Android, no Ktor
│   │   ├── SessionCoordinator.kt        # orchestrates admission + roster + phase
│   │   ├── SessionRoster.kt             # membership, capacity, reclaim
│   │   ├── GamePhase.kt                 # 9-state enum
│   │   ├── GamePhaseMachine.kt          # F22 transition table
│   │   ├── SessionLifecycle.kt          # Open | Locked | Ended
│   │   ├── JoinCodeGenerator.kt         # adjective + noun
│   │   ├── ConnectionIdAllocator.kt     # uint16 counter from 1
│   │   ├── SessionStartFailure.kt       # bind / announce / no-address
│   │   └── model/                       # SessionId, JoinCode, DeviceId,
│   │                                    # ConnectionId, RosterEntry,
│   │                                    # ConnectedDevice, SessionSnapshot
│   ├── control/                     # pure protocol logic
│   │   ├── ControlTransport.kt          # PORT
│   │   ├── HandshakeValidator.kt
│   │   ├── JoinCodeMatcher.kt
│   │   ├── ControlMessageCodec.kt
│   │   ├── AdmissionDecision.kt
│   │   ├── RefusalReason.kt
│   │   └── model/ Hello.kt, SessionState.kt, Refusal.kt
│   └── platform/                    # PORTS only
│       ├── SessionAnnouncer.kt
│       ├── LocalAddressProvider.kt
│       └── MulticastLease.kt
├── data/                            # adapters — thin, excluded from --src
│   ├── control/KtorControlTransport.kt
│   ├── discovery/JmdnsSessionAnnouncer.kt
│   └── platform/
│       ├── ConnectivityLocalAddressProvider.kt
│       └── WifiMulticastLease.kt
├── presentation/
│   ├── join/ JoinViewModel.kt, JoinUiState.kt, JoinOverlay.kt
│   ├── songlist/ SongListScreen.kt          # empty-state shell + Join action
│   └── qr/ QrPayloadEncoder.kt, QrBitmapRenderer.kt, QrCode.kt
├── di/ SessionComponent.kt              # manual composition root
└── MainActivity.kt                      # hosts the shell; session-scoped lease

app/src/test/kotlin/com/couchraoke/tv/
├── domain/…                         # unit tests mirroring the above
├── presentation/…                   # JoinViewModelTest, JoinOverlayBoundsTest
└── gate/LoopbackJoinGateTest.kt     # drives the real mockphone process
```

**Structure Decision**: Single Android module, extending the existing `domain/` package that already holds `usdx` and `scoring`. Two new domain areas (`session`, `control`) plus a `platform` port package; a new `data/` layer for the three adapters; a new `presentation/` layer. This mirrors the layering the constitution names (Domain, Data, Presentation, DI) onto packages rather than Gradle modules, because module splitting would change the coverage and `testBranch` selector mechanics that Slice 0 established, which is out of scope here.

## Phases

Each phase is a dependency bucket. A phase may not start until the previous one's gate passes fresh.

### Phase A — Pure session core

`GamePhase`, `GamePhaseMachine`, `SessionLifecycle`, `SessionRoster`, `JoinCodeGenerator`, `ConnectionIdAllocator`, and the `model/` value types. No transport, no UI. Satisfies FR-001, FR-003, FR-013, FR-015, FR-019 through FR-027.

The phase machine is driven by `fixtures/F22_gamephase_fsm_transitions/`, which has no `Open→Error` edge — this is why FR-028 surfaces a session-start failure as a modal rather than a transition.

*Gate*: `testBranch` over the six classes above. **U** only; no transport exists yet.

### Phase B — Protocol logic

`HandshakeValidator`, `JoinCodeMatcher`, `ControlMessageCodec`, `AdmissionDecision`, `RefusalReason` and the three payload models. Still pure. Satisfies FR-009 through FR-012, FR-014, FR-016, FR-018.

Driven by `fixtures/F20_websocket_message_validation/` (four cases) and `fixtures/F15_session_lifecycle_disconnect_reconnect/`.

*Gate*: `testBranch` over those classes, asserting each F20 case decodes to the expected refusal or acceptance.

### Phase C — Coordinator

`SessionCoordinator` joins Phase A and Phase B behind one host-owned surface and owns the two observable flows. Satisfies FR-023's ordering rule and FR-034.

*Gate*: `testBranch` including `SessionCoordinator`, with virtual-time tests for the 5 s deadline and an explicit test that a drop is removed from the connected list before the next admission decision.

**The gate's two named tests still fall due at the US2 checkpoint, not at T024**, because T042 writes
the drop-ordering test and T052 the 5 s deadline test, and `authorize`/`admit`/`onDisconnected` stay
stubbed until T035, T042 and T050.

`SessionCoordinator` was briefly excluded from `--src` at T024 under the old INSTRUCTION gate, which
measured it at 70.87%. **That exclusion has been withdrawn.** Under the corrected LINE/BRANCH gate the
class measures 81.82% line and 100% branch at T024 — every implemented method is fully covered and
every decision it makes is exercised. The old failure was the INSTRUCTION counter charging it for the
bodies of three `TODO()` stubs, which is precisely the distortion the counter fix removed. The class
is `--src` selected from T022 onward with no deferral.

### Phase D — Transport and announcement

`ControlTransport`/`KtorControlTransport`, `SessionAnnouncer`/`JmdnsSessionAnnouncer`, `LocalAddressProvider`, `MulticastLease`, and the session-scoped lease move in `MainActivity`. Satisfies FR-002, FR-004 through FR-006, FR-008.

**This is the slice's one risky new thing.** Adapters carry no logic and are excluded from `--src`.

*Gate*: **L** — `LoopbackJoinGateTest` starts the real server and drives the real `mockphone` process over `127.0.0.1`. Per FR-038 the scoped `testBranch` does not complete this phase on its own.

### Phase E — Host surface

`SongListScreen` shell, `JoinOverlay`, `JoinViewModel`, `JoinUiState`, `QrPayloadEncoder`, `QrBitmapRenderer`, and the FR-028 blocking notice. Satisfies FR-007, FR-028 through FR-033, FR-035.

*Gate*: `testBranch` including `JoinViewModel` and `QrPayloadEncoder`, plus `JoinOverlayBoundsTest` asserting structural layout numerically — every expected element present, nothing exceeding the 960×540dp viewport, the QR's share of the screen within tolerance, and a quiet zone of at least four modules. No screenshot baseline is recorded; Slice 3 owns that (spec Assumptions, readiness item 5).

### Phase F — Full gate

The feature-level `testBranch` above, the loopback gate, and a device run. Satisfies FR-038, FR-039, SC-009, SC-010.

## Loopback gate design

`LoopbackJoinGateTest` is the only thing that can satisfy SC-003, SC-005 and SC-010, because FR-039 bars an in-process fake from proving the transport. It starts the real `KtorControlTransport` on an ephemeral port and invokes `mockphone` as a subprocess, asserting on its documented exit status and its single-line `JOIN_RESULT` JSON.

| Case | Peer invocation | Expect |
|---|---|---|
| Happy join | `--join-only` | exit 0, `sessionState`, `connectionId` ≥ 1 |
| Wrong token | `--join-only --token WRONG-WORD` | exit 3, `invalid_token`, close 1008 |
| Version mismatch | `--join-only --protocol-version 2` | exit 3, `protocol_mismatch` |
| Incomplete `hello` | `--join-only --malformed-hello clientId` | exit 3, `invalid_message` |
| Unparseable `hello` | `--join-only --malformed-hello invalid-json` | exit 3, `invalid_message` |
| Handshake deadline | `--silent-handshake --join-timeout 10` | exit 3, `invalid_message`, close 1008, within 5 s |
| Capacity | 10 × `--join-only --client-id phone-NN`, then an 11th | 11th exits 3 with `session_full` |
| Reclaim at capacity | re-run one of the original ten | exit 0, roster size unchanged, new `connectionId` |
| Connected count | 3 × `--hold 30` concurrently | connected list reports 3 |

Two of the peer's exit statuses are assertions against us rather than diagnostics: **exit 4** means a refusal closed without delivering its reason, violating FR-016/FR-017, and **exit 6** means the 5 s deadline was never enforced. Both must fail the gate.

The roster distinction matters for the capacity case: because a disconnected entry keeps its slot (FR-023, FR-024) and neither Kick nor songs exist in this slice, ten sequential `--join-only` peers fill the roster without any needing to stay connected. `--hold` is needed only for the connected-count case, which counts live connections rather than roster entries.

## Complexity Tracking

No constitution violations require justification. Two decisions are constraints rather than violations, recorded because they shape the design:

| Decision | Why | Simpler alternative rejected because |
|---|---|---|
| Adapters excluded from `--src` coverage selection | `KtorControlTransport` and `JmdnsSessionAnnouncer` bind real sockets and multicast; JVM tests cannot reach 80% line coverage of them without a fake, which FR-039 bars | Including them would force either an in-process fake (forbidden) or a suppression (Principle IV). Excluding them is honest only because the loopback gate proves them against a real peer — the coverage is moved, not skipped |
| Manual composition root instead of Hilt | One activity, one view model, one session lifetime | A DI framework alongside the first real transport breaks `plan.md`'s one-risky-thing rule, and its build-time step would slow every gate in this slice for no functional gain |
