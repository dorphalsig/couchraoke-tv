# Contract: ISessionGate

**Defined in**: `app/src/main/kotlin/com/couchraoke/tv/domain/session/ISessionGate.kt` (feature 005)
**Implemented by**: `Session` (feature 006)
**Consumed by**: `WebSocketServer` (feature 005) — read on every `hello` handshake

---

## Interface

```kotlin
interface ISessionGate {
    val isLocked: Boolean       // true when state == Locked; gated by WebSocketServer on hello
    val sessionId: String       // raw session token string; included in sessionState response
    val maxConnections: Int     // always 10 for MVP; enforced by WebSocketServer
    val slots: SlotMap          // P1/P2 singer slot assignment; included in sessionState response
    val inSong: Boolean         // true when a song is in progress; used to suspend clock sync
}
```

---

## Consumption by WebSocketServer (feature 005)

| Field | When read | What 005 does with it |
|-------|-----------|----------------------|
| `isLocked` | On every `hello` | If `true`, reject join with `error(code="session_locked")` |
| `maxConnections` | On every `hello` | If `ConnectionRegistry.size >= maxConnections`, reject with `error(code="session_full")` |
| `sessionId` | After successful `hello` | Included in `sessionState` response sent to joining phone |
| `slots` | After successful `hello` | Included in `sessionState` response sent to joining phone |
| `inSong` | During clock sync | If `true`, suspend further clock sync exchanges |

---

## IConnectionCloser (companion contract)

```kotlin
// Defined in domain/session/IConnectionCloser.kt (feature 006)
fun interface IConnectionCloser {
    fun closeConnection(clientId: String)
}
```

**Implemented by**: `WebSocketServer` (feature 005)
**Consumed by**: `Session.kick()` and `Session.forget()` (feature 006)

`WebSocketServer` registers itself as the `IConnectionCloser` when the session is initialised. `Session` calls `closeConnection(clientId)` when the host kicks or forgets a phone; `WebSocketServer` then closes that WebSocket connection.

---

## Test Double

`FakeSessionGate` (feature 005 test support) provides a mutable stub for all five fields. It resides in the test source set and must not be modified by feature 006.
