# Contract: Domain API

**Feature**: `003-phone-joins` | **Date**: 2026-08-19

Every class below is pure Kotlin — no Android, Ktor, jmDNS or Compose types. All are selected for coverage by the `testBranch` gate.

---

## SessionCoordinator

**FQCN**: `com.couchraoke.tv.domain.session.SessionCoordinator`

The single host-owned surface. Owns the roster, the phase, the connection allocator, and both observable flows.

```kotlin
package com.couchraoke.tv.domain.session

class SessionCoordinator(
    private val roster: SessionRoster,
    private val phaseMachine: GamePhaseMachine,
    private val connectionIds: ConnectionIdAllocator,
    private val validator: HandshakeValidator,
    private val codeMatcher: JoinCodeMatcher,
    private val sessionId: SessionId,
    private val joinCode: JoinCode,
) {
    val snapshot: StateFlow<SessionSnapshot>
    val connectedDevices: StateFlow<List<ConnectedDevice>>
    val events: SharedFlow<SessionEvent>

    fun authorize(token: String?): AdmissionDecision
    fun admit(hello: Hello): AdmissionDecision
    fun onDisconnected(deviceId: DeviceId, connectionId: ConnectionId)
    fun requestPhase(target: GamePhase): PhaseTransitionResult
    fun end()
}
```

`authorize` and `admit` are separate because they happen at different moments: the token is checked at connection time, the introduction after the first frame arrives. A connection that passes `authorize` but never reaches `admit` holds no roster slot (FR-017).

`onDisconnected` takes both identifiers so it can enforce FR-022 — it is a no-op unless `connectionId` is still the active one for `deviceId`. It completes removal from the connected list before returning, which is what gives FR-023 its ordering guarantee relative to the next `admit`.

**`SessionEvent`**: `sealed interface` with `Connected(deviceId, connectionId)`, `Disconnected(deviceId, connectionId)`, `Reconnected(deviceId, connectionId, previous: ConnectionId?)` (FR-019).

> Corrected during implementation (T058). `previous` was specified non-null, which contradicted `RosterAdmission.Reclaimed(entry, previous: ConnectionId?)` two sections below. FR-021 decides reclaim from roster presence alone and FR-023 keeps a dropped device's entry with `connection = null`, so the ordinary reconnect reaches `Reclaimed` with no connection to supersede. `ConnectionId` requires `1..65535`, so no sentinel exists to fill the gap. `null` now means "no live connection was displaced"; non-null means one was (FR-022). See spec.md Observation 22.

**`PhaseTransitionResult`**: `Accepted(from, to)` | `Rejected(from, to)`. Rejected leaves the phase unchanged (FR-026).

**`AdmissionDecision`**: `com.couchraoke.tv.domain.control.AdmissionDecision`

```kotlin
sealed interface AdmissionDecision {
    data object Authorized : AdmissionDecision
    data class Admitted(val connectionId: ConnectionId) : AdmissionDecision
    data class Refused(val reason: RefusalReason, val message: String) : AdmissionDecision
}
```

`Authorized` is `authorize`'s accept case and carries nothing, because nothing is allocated yet.
`Admitted` is `admit`'s accept case and carries the allocated `ConnectionId`.
`Admitted` carries only the allocated `ConnectionId`. It deliberately does **not** embed the
`sessionState` reply: that is a wire DTO, and putting it here would drag the wire schema into a pure
`domain.control` type. The transport-facing caller builds the reply from `Admitted.connectionId` and
`SessionCoordinator.snapshot` (T035).

`Refused` carries the `RefusalReason` and the exact human-readable message, because F20 pins those
strings and both halves travel together into `ControlConnection.refuse(code, message)`.

> Added during implementation (T022). The original contract named `AdmissionDecision` in
> `authorize`/`admit` without ever defining it; T049 was to create it "alongside"
> `HandshakeValidator`, which left the type undefined for every earlier task that had to compile
> against it. Shape inferred from T050's refusal vocabulary.
>
> `Authorized` added during T036. With only `Admitted` and `Refused`, `authorize` had no
> representable accept value — `Admitted` requires a `ConnectionId`, which does not exist until
> `admit` mints one, and `ConnectionId` cannot encode "none" (its range starts at 1). See
> spec.md Observation 19.

---

## SessionRoster

**FQCN**: `com.couchraoke.tv.domain.session.SessionRoster`

```kotlin
package com.couchraoke.tv.domain.session

class SessionRoster(private val capacity: Int = 10) {
    val entries: List<RosterEntry>
    val size: Int
    val connected: List<RosterEntry>

    fun admit(
        deviceId: DeviceId,
        displayName: String,
        appVersion: String,
        assetPort: AssetPort,
        connectionId: ConnectionId,
    ): RosterAdmission

    fun detach(deviceId: DeviceId, connectionId: ConnectionId): Boolean
    fun release(deviceId: DeviceId)
    fun releaseDisconnected()
    fun clear()
}
```

**`RosterAdmission`**: `Admitted(entry)` | `Reclaimed(entry, previous: ConnectionId?)` | `AtCapacity`.

`admit` returns `AtCapacity` **only** for a previously-unseen `deviceId` when `size == capacity`. A known `deviceId` always reclaims regardless of capacity and regardless of whether it currently has a live connection (FR-020, FR-021). This is what makes SC-006 hold at capacity.

`detach` returns `false` and changes nothing when `connectionId` is not the entry's current one — the FR-022 guard against a superseded connection's late close evicting its replacement. On `true` the entry is retained with `connection = null`, keeping its capacity slot (FR-023).

`release` is Kick and `releaseDisconnected` is the song-end sweep (FR-024). Neither is reachable in this slice; both exist so capacity semantics are complete and testable now rather than retrofitted later.

---

## GamePhaseMachine

**FQCN**: `com.couchraoke.tv.domain.session.GamePhaseMachine`

```kotlin
package com.couchraoke.tv.domain.session

class GamePhaseMachine(initial: GamePhase = GamePhase.Open) {
    val current: GamePhase
    fun canTransition(from: GamePhase, to: GamePhase): Boolean
    fun transition(to: GamePhase): PhaseTransitionResult
}
```

The transition table is a static allow-list matching `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json`. Anything not on it is rejected (FR-026). Note there is no `Open→Error` edge, which is why session-start failure is a modal rather than a transition.

`transition` never throws. A rejected transition returns `Rejected` and leaves `current` untouched — silently applying an invalid transition is exactly what FR-026 forbids.

---

## HandshakeValidator

**FQCN**: `com.couchraoke.tv.domain.control.HandshakeValidator`

```kotlin
package com.couchraoke.tv.domain.control

class HandshakeValidator(private val supportedProtocolVersion: Int = 1) {
    fun validate(raw: String): HelloValidation
}
```

**`HelloValidation`**: `Valid(hello: Hello)` | `Invalid(reason: RefusalReason, message: String)`.

Validation order is normative because one payload can fail two ways:

1. Parse failure → `invalid_message`, `"Malformed message"`.
2. `type != "hello"` → `invalid_message` (during the handshake this is fatal, FR-018).
3. `protocolVersion != 1` → `protocol_mismatch`, `"Unsupported protocolVersion: N"`.
4. Missing required field, in `required`-array order → `invalid_message`, `"Missing required field: X"`.
5. Field present but out of range (`clientId` < 8 chars, `httpPort` outside 1024–65535) → `invalid_message`.

Version precedes field presence deliberately: F20's `case_bad_protocolVersion` carries a complete payload, but a future-version phone may legitimately send a different field set, so version is the outer gate. Messages match F20's expected strings exactly.

---

## JoinCodeMatcher and JoinCodeGenerator

**FQCN**: `com.couchraoke.tv.domain.control.JoinCodeMatcher`

```kotlin
package com.couchraoke.tv.domain.control

object JoinCodeMatcher {
    fun matches(expected: JoinCode, presented: String?): Boolean
    fun normalize(value: String): String
}
```

`normalize` uppercases and strips hyphens and surrounding whitespace, so `SWIFT-PANDA`, `swift-panda` and `swiftpanda` all match. `matches` returns `false` for `null` or blank, producing `invalid_token` (FR-009).

**FQCN**: `com.couchraoke.tv.domain.session.JoinCodeGenerator`

```kotlin
package com.couchraoke.tv.domain.session

class JoinCodeGenerator(private val random: Random = Random.Default) {
    fun next(): JoinCode
}
```

`random` is injected so tests are deterministic. The generator retains codes already issued this process and never repeats one (FR-003).

---

## ConnectionIdAllocator

**FQCN**: `com.couchraoke.tv.domain.session.ConnectionIdAllocator`

```kotlin
package com.couchraoke.tv.domain.session

class ConnectionIdAllocator {
    fun next(): ConnectionId
}
```

Starts at 1, increments per accepted connection, never reuses a value within a session, including across reconnects (FR-013). Wraps within uint16.

---

## ControlMessageCodec

**FQCN**: `com.couchraoke.tv.domain.control.ControlMessageCodec`

```kotlin
package com.couchraoke.tv.domain.control

class ControlMessageCodec(private val json: Json) {
    fun decodeHello(raw: String): Result<Hello>
    fun encodeSessionState(state: SessionState): String
    fun encodeError(refusal: Refusal): String
}
```

Encoding omits absent optionals rather than writing `null`, because all three schemas set `additionalProperties: false` and `connectionId` must be **absent** outside the direct reply to a `hello` (FR-014).

The injected `Json` is configured `explicitNulls = false`, `ignoreUnknownKeys = false` — the last because an unknown field is a schema violation, not something to tolerate.

`encodeDefaults` cannot be satisfied by a single `Json`, so the codec derives a second one internally
for the two encode paths. `sessionState` and `error` both declare `type` and `protocolVersion` with
Kotlin defaults while their schemas mark those fields **required**, so they must be written even when
they hold the default value — that needs `encodeDefaults = true`. But every schema also sets
`additionalProperties: false`, so absent nullable optionals must be omitted rather than written as
`null` — that needs `explicitNulls = false`. `encodeDefaults = false` would drop the required
constants; `encodeDefaults = true` alone would emit the optionals as explicit nulls.

The constructor therefore keeps its single-`Json` signature and uses the injected instance for
`decodeHello`, deriving `Json(from = json) { encodeDefaults = true; explicitNulls = false }` for
`encodeSessionState` and `encodeError`. `ControlMessageCodecFixtureTest` asserts the resulting bytes.

> Corrected during implementation (T014). The contract previously specified
> `encodeDefaults = false`, which contradicts T014's own requirement that `sessionState` write its
> required constants.

---

## JoinViewModel

**FQCN**: `com.couchraoke.tv.presentation.join.JoinViewModel`

```kotlin
package com.couchraoke.tv.presentation.join

class JoinViewModel(
    private val coordinator: SessionCoordinator,
    private val qrEncoder: QrPayloadEncoder,
    private val endpoint: ControlEndpoint,
) : ViewModel() {
    val uiState: StateFlow<JoinUiState>
    fun onOverlayDismissed()
    fun onStartFailureAcknowledged()
}
```

**`JoinUiState`**: `data class(joinCodeDisplay: String, qrPayload: String, connectedCount: Int, startFailure: SessionStartFailure?)`.

**`ControlEndpoint`**: `com.couchraoke.tv.presentation.join.ControlEndpoint`

```kotlin
data class ControlEndpoint(val address: Inet4Address, val port: Int)
```

The resolved address from `LocalAddressProvider` paired with the **actual** `StartedTransport.boundPort`.
It exists so `JoinViewModel` can hand `QrPayloadEncoder` the endpoint a phone should dial without
reaching for a port or a socket itself (FR-035).

> Added during implementation. `JoinViewModel`'s binding constructor named `endpoint: ControlEndpoint`
> but no task in `tasks.md` created the type. T040 owns constructing it.

`connectedCount` is derived from `coordinator.connectedDevices.size` — the live connections, never the roster size (FR-025, SC-007). The view model performs no network I/O (FR-035); it maps host-owned state and nothing more.


---

## QrPayloadEncoder

**FQCN**: `com.couchraoke.tv.presentation.qr.QrPayloadEncoder`

```kotlin
package com.couchraoke.tv.presentation.qr

object QrPayloadEncoder {
    fun encode(address: Inet4Address, port: Int, joinCode: JoinCode): String
}
```

Returns `ws://<address>:<port>/?token=<CODE>`. Pure string construction, no ZXing type in the signature, so it is JVM-testable and coverage-selected. It encodes no discovery-service identifier (FR-007).

Rendering to pixels is `com.couchraoke.tv.presentation.qr.QrBitmapRenderer`, which owns the ZXing call and the `EncodeHintType.MARGIN = 4` quiet zone (FR-030).
