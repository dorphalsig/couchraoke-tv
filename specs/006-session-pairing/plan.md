# Implementation Plan: Session Pairing

**Branch**: `006-session-pairing` | **Date**: 2026-03-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-session-pairing/spec.md`

## Summary

Implement the `Session` domain class that satisfies the `ISessionGate` interface already consumed by feature 005's `WebSocketServer`. `Session` owns the Open/Locked/Ended state machine, maintains the singer slot assignment (`SlotMap`), manages the device roster (display names, kick/forget), and emits `SessionEvent`s for disconnect/reconnect that the UI layer (features 007 and 010) will consume. No new external dependencies are required — this is pure domain Kotlin.

## Technical Context

**Language/Version**: Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36)
**Primary Dependencies**: None new — `ISessionGate`, `SessionToken`, `ConnectionRegistry`, `SlotMap`/`SlotInfo` are already on the classpath from features 004–005
**Storage**: In-memory only; no persistence
**Testing**: JUnit4 + `kotlinx-coroutines-test` (`StandardTestDispatcher`), JaCoCo coverage
**Target Platform**: Android TV (module: `app`)
**Project Type**: Android app module — pure domain layer addition
**Performance Goals**: `isLocked` read is O(1); state transitions complete within one synchronous call
**Constraints**: Domain layer only — no Android framework types, no Ktor/JmDNS references, no UI code
**Scale/Scope**: Max 10 devices per session; 2 singer slots (P1, P2)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| LAN-only operating model | ✅ | Pure domain layer; no network calls introduced |
| Approved technology stack | ✅ | Kotlin only; no new library dependencies |
| Clean architecture boundaries | ✅ | Domain layer only; no Android framework types; UI consumers (007, 010) are separate features |
| Networking/streaming contracts | ✅ | Does not touch transport; `ISessionGate` is the seam 005 reads |
| Tests and quality gates | ✅ | JUnit4-only unit tests; TDD required; JaCoCo coverage gated |
| Branch hygiene | ✅ | Branch renamed `[✓] 006-session-pairing` after merge to master |

**No constitution violations. Phase 0 may proceed.**

## Project Structure

### Documentation (this feature)

```text
specs/006-session-pairing/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── contracts/           ← Phase 1 output
│   └── ISessionGate.md
└── tasks.md             ← Phase 2 output (/speckit.tasks)
```

### Source Code

```text
app/src/main/kotlin/com/couchraoke/tv/domain/session/
├── ISessionGate.kt          (005) — interface read by WebSocketServer [DO NOT MODIFY]
├── SessionToken.kt          (005) — token generation utility [DO NOT MODIFY]
├── ConnectionRegistry.kt    (005) — connection-layer roster [DO NOT MODIFY]
├── SessionState.kt          (NEW) — Open / Locked / Ended enum
├── SessionEvent.kt          (NEW) — sealed class for domain events
└── Session.kt               (NEW) — ISessionGate implementation; state machine + roster

app/src/test/kotlin/com/couchraoke/tv/domain/session/
├── FakeSessionGate.kt       (005) — test double for WebSocketServer tests [DO NOT MODIFY]
├── SessionTokenTest.kt      (005) — [DO NOT MODIFY]
├── ConnectionRegistryTest.kt (005) — [DO NOT MODIFY]
├── SessionStateTransitionTest.kt  (NEW) — state machine unit tests
├── SessionRosterTest.kt           (NEW) — rename / kick / forget / reconnect tests
└── SessionEventTest.kt            (NEW) — disconnect event emission tests
```

---

## Phase 0: Research

*All findings documented inline — no external research agents required. All decisions are derivable from the existing codebase and spec.*

See [research.md](./research.md).

---

## Phase 1: Design

### Data Model

See [data-model.md](./data-model.md).

### Key Design Decisions

#### D1 — Session does NOT own ConnectionRegistry

`ConnectionRegistry` is a 005 concern (connection routing by `connectionId`). `Session` holds its own `displayNames: Map<String, String>` keyed by `clientId` for rename/forget. `Session.slots` is derived from the assigned singer `clientId`s and their current display names.

#### D2 — IConnectionCloser interface for kick/forget

`Session` needs to close a phone's WebSocket on kick/forget. To avoid a hard dependency on 005's `WebSocketServer`, `Session` accepts an `IConnectionCloser` callback in its constructor (or via a `setConnectionCloser()` setter). This keeps `Session` testable in isolation.

```kotlin
fun interface IConnectionCloser {
    fun closeConnection(clientId: String)
}
```

#### D3 — SessionEvent as a sealed class, not Flow

`Session` emits events via a `SharedFlow<SessionEvent>`. The constitution mandates coroutines + Flow for reactive state. `Session` exposes `val events: SharedFlow<SessionEvent>` for UI and other layer consumers.

#### D4 — State transition triggers

| Trigger | Caller | Session method |
|---------|--------|----------------|
| Song starts, singers assigned | Feature 007 (game controller) | `lockForSong(p1ClientId, p2ClientId?)` |
| Song ends / quit to Song List | Feature 007 | `unlockAfterSong()` |
| End Session | Feature 010 (settings UI) | `endSession()` |
| Phone connects | Feature 005 (WebSocketServer) | `onPhoneConnected(clientId, deviceName, connectionId)` |
| Phone disconnects | Feature 005 | `onPhoneDisconnected(clientId)` |
| Phone reconnects | Feature 005 | `onPhoneReconnected(clientId, newConnectionId)` |

#### D5 — isLocked is a computed property

```kotlin
override val isLocked: Boolean get() = state == SessionState.Locked
```

Read by 005's `WebSocketServer` on every `hello` message — must be O(1).

### Interface Contracts

See [contracts/ISessionGate.md](./contracts/ISessionGate.md).
