# Research: Session Pairing

**Date**: 2026-03-16
**Branch**: `006-session-pairing`

No external unknowns required investigation. All decisions are derivable from the existing codebase and spec. Findings below document what was discovered by reading the existing code.

---

## Finding 1 — ISessionGate is already defined by feature 005

**Decision**: Feature 006 implements `ISessionGate`, not redefines it.

**Rationale**: `ISessionGate` was created by 005 as the seam between the network layer and the session domain. It exposes exactly what 005 needs: `isLocked`, `sessionId`, `maxConnections`, `slots`, `inSong`. Feature 006's `Session` class must implement this interface.

```kotlin
// Already in domain/session/ISessionGate.kt (005)
interface ISessionGate {
    val isLocked: Boolean
    val sessionId: String
    val maxConnections: Int
    val slots: SlotMap
    val inSong: Boolean
}
```

**Alternatives considered**: Redefining the contract — rejected, would break 005's `WebSocketServer` and `FakeSessionGate`.

---

## Finding 2 — SlotMap and SlotInfo live in domain.network.protocol

**Decision**: `Session` imports `SlotMap` and `SlotInfo` from `com.couchraoke.tv.domain.network.protocol.ProtocolMessages`.

**Rationale**: `SlotMap(P1: SlotInfo, P2: SlotInfo)` and `SlotInfo(connected: Boolean, deviceName: String)` are already defined there and used by `ISessionGate.slots`. No duplication.

**Note for implementation**: `SlotInfo.connected` should be `true` when the singer phone is currently connected; `deviceName` should reflect the display name (from `Session.displayNames` if renamed, else the name from the `hello` message).

---

## Finding 3 — ConnectionRegistry is 005's concern; Session holds its own displayNames

**Decision**: `Session` maintains `private val displayNames: MutableMap<String, String>` keyed by `clientId` for the rename/forget feature. It does NOT modify `ConnectionRegistry` directly.

**Rationale**: `ConnectionRegistry` is owned by 005 and manages connection routing. Its `deviceName` field is set at registration time (from `hello`). For rename to persist within a session independently, `Session` holds its own overlay map. When building `SlotMap`, `Session` checks `displayNames[clientId]` first, falling back to the registered `deviceName`.

**Alternatives considered**: Adding a `rename()` method to `ConnectionRegistry` — rejected, would give 006 ownership of a 005 class.

---

## Finding 4 — FakeSessionGate confirms the full ISessionGate contract

**Decision**: The `FakeSessionGate` test double (used in 005 tests) shows the full shape: all five fields are mutable vars. The real `Session` will use computed properties where appropriate (`isLocked`, `inSong`) and mutable backing state for others.

**Rationale**: Confirms 006's implementation must be consistent with what 005 already tests against.

---

## Finding 5 — No new Gradle dependencies needed

**Decision**: No new library dependencies are added.

**Rationale**: `Session` is pure Kotlin domain logic. Coroutines (`kotlinx-coroutines-core`) and `kotlinx-coroutines-test` are already on the classpath from 005. JUnit4 test tooling is already configured.
