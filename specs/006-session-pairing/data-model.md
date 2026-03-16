# Data Model: Session Pairing

**Date**: 2026-03-16
**Branch**: `006-session-pairing`

---

## Entities

### SessionState (NEW — `domain/session/SessionState.kt`)

```kotlin
enum class SessionState { Open, Locked, Ended }
```

| State | Meaning | Transitions to |
|-------|---------|---------------|
| `Open` | Phones may join; roster accepts new `hello` messages | `Locked` (song starts), `Ended` (explicit end/app close) |
| `Locked` | Song in progress; new joins rejected with `session_locked` | `Open` (song ends/quit), `Ended` |
| `Ended` | Session token invalid; no new joins | — (terminal) |

---

### SessionEvent (NEW — `domain/session/SessionEvent.kt`)

```kotlin
sealed class SessionEvent {
    data class RequiredSingerDisconnected(val clientId: String, val slot: String) : SessionEvent()
    data class SpectatorDisconnected(val clientId: String) : SessionEvent()
    data class PlaybackSourceLost(val clientId: String) : SessionEvent()
    data class PhoneConnected(val clientId: String, val deviceName: String) : SessionEvent()
    data class PhoneReconnected(val clientId: String) : SessionEvent()
    data class PhoneDisconnected(val clientId: String) : SessionEvent()
    data class RosterChanged(val clientId: String) : SessionEvent()
}
```

---

### Session (NEW — `domain/session/Session.kt`)

Primary aggregate. Implements `ISessionGate`.

| Field | Type | Description |
|-------|------|-------------|
| `state` | `SessionState` | Current session state (backing field) |
| `token` | `String` | Session join code (generated once on construction via `SessionToken.generate()`) |
| `displayNames` | `MutableMap<String, String>` | Rename overlay: clientId → display name |
| `assignedSlots` | `MutableMap<String, String>` | clientId → "P1" or "P2"; set by `lockForSong()` |
| `activeSourceClientId` | `String?` | clientId of the phone serving the active song's audio stream; set by `lockForSong()` |
| `connectedClientIds` | `MutableSet<String>` | Internal set of currently-connected clientIds. Managed by `onPhoneConnected`/`onPhoneDisconnected`/`onPhoneReconnected`. Used to derive `SlotInfo.connected` without depending on `ConnectionRegistry`. |
| `connectionCloser` | `IConnectionCloser?` | Callback to close a WebSocket by clientId (injected) |
| `_events` | `MutableSharedFlow<SessionEvent>` | Internal event bus |
| `events` | `SharedFlow<SessionEvent>` | Public event stream for UI and feature 005 consumers |

**Computed ISessionGate properties:**

| Property | Derivation |
|----------|-----------|
| `isLocked` | `state == SessionState.Locked` |
| `sessionId` | `token` (the raw token string) |
| `maxConnections` | Constant `10` |
| `inSong` | `state == SessionState.Locked` |
| `slots` | `SlotMap(P1 = slotInfoFor("P1"), P2 = slotInfoFor("P2"))` where `slotInfoFor` looks up `assignedSlots.entries.firstOrNull { it.value == slot }` and builds `SlotInfo(connected = connectedClientIds.contains(clientId), deviceName = <displayName or hello name>)` |

**Methods:**

| Method | Parameters | Effect |
|--------|-----------|--------|
| `lockForSong(p1ClientId, p2ClientId?, sourceClientId?)` | clientIds | state → Locked; populate `assignedSlots`; set `activeSourceClientId`; `inSong = true` (derived) |
| `unlockAfterSong()` | — | state → Open; clear `assignedSlots`; clear `activeSourceClientId` |
| `endSession()` | — | state → Ended; emit disconnect events for all connected phones; clear roster state |
| `onPhoneConnected(clientId, deviceName, connectionId)` | — | set `displayNames[clientId]` if not already renamed; emit `PhoneConnected` |
| `onPhoneDisconnected(clientId)` | — | emit appropriate event (RequiredSingerDisconnected / SpectatorDisconnected / PlaybackSourceLost); update `SlotMap` `connected` flag |
| `onPhoneReconnected(clientId, newConnectionId)` | — | emit `PhoneReconnected`; if singer, triggers re-send of `assignSinger` (via event) |
| `rename(clientId, newName)` | — | `displayNames[clientId] = newName`; emit `RosterChanged` |
| `kick(clientId)` | — | `connectionCloser?.closeConnection(clientId)`; remove from `displayNames` and `assignedSlots` |
| `forget(clientId)` | — | kick + clear `displayNames[clientId]` |
| `releaseSlot(clientId)` | — | Remove `clientId` from `assignedSlots` only; no disconnect, no state change. Called by singing-screen UI when host chooses "continue without them" after singer disconnect. |

---

### ISessionCallbacks (NEW — `domain/session/ISessionCallbacks.kt`)

```kotlin
interface ISessionCallbacks {
    fun onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort)
    fun onPhoneDisconnected(clientId: String)
    fun onPhoneReconnected(clientId: String, newConnectionId: UShort)
}
```

`Session` implements this interface. `WebSocketServer` (005) accepts an `ISessionCallbacks?` constructor parameter and calls these methods after register, in the `finally` block, and when a reconnect is detected. This keeps `WebSocketServer` testable without a real `Session` (existing tests pass `null` or a fake).

**Reconnect detection in WebSocketServer**: Before applying the `isLocked` / cap checks, `WebSocketServer` calls `registry.getByClientId(hello.clientId)`. If a match is found the connection is a reconnect — `isLocked` and cap checks are skipped, and `callbacks?.onPhoneReconnected(...)` is called. If no match, it is a fresh join — normal checks apply and `callbacks?.onPhoneConnected(...)` is called.

**Duplicate active clientId**: If `registry.getByClientId(hello.clientId)` returns a connection that is still active, `WebSocketServer` closes the old WebSocket session before re-registering. `onPhoneReconnected` is then called (not `onPhoneConnected`).

---

### IConnectionCloser (NEW — `domain/session/IConnectionCloser.kt`)

```kotlin
fun interface IConnectionCloser {
    fun closeConnection(clientId: String)
}
```

Implemented by 005's `WebSocketServer` and injected into `Session` at startup. Keeps `Session` testable in isolation.

---

## Validation Rules

- `lockForSong()` MUST be a no-op if `state != Open` (guard against double-lock).
- `unlockAfterSong()` MUST be a no-op if `state != Locked`.
- `endSession()` MUST be idempotent — calling it in `Ended` state does nothing.
- `rename()`, `kick()`, `forget()` MUST be no-ops if `clientId` is not in the current roster.
- Singer disconnect: a phone is a "required singer" if `assignedSlots.containsKey(clientId)` AND `state == Locked`.
- Playback source disconnect: emitted when `clientId == activeSourceClientId` (in addition to the singer/spectator event).

---

## Pre-existing Types (Do Not Redefine)

| Type | Location | Role in 006 |
|------|----------|------------|
| `ISessionGate` | `domain/session/ISessionGate.kt` | Implemented by `Session` |
| `SessionToken` | `domain/session/SessionToken.kt` | Used to generate `token` in `Session` constructor |
| `ConnectionRegistry` | `domain/session/ConnectionRegistry.kt` | Called by `WebSocketServer` (005) for connection routing. Session does NOT depend on it directly — uses internal `connectedClientIds` set instead. |
| `SlotMap` | `domain/network/protocol/ProtocolMessages.kt` | Returned by `Session.slots` |
| `SlotInfo` | `domain/network/protocol/ProtocolMessages.kt` | Built by `Session.slotInfoFor()` |
