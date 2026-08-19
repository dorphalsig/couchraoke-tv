# Contract: Wire protocol serialization surface

**Feature**: `002-phone-joins` | **Date**: 2026-08-19

The JSON-level field contract is in [data-model.md](../data-model.md). This file pins the Kotlin types that produce and consume it: FQCN, exact property names, types, nullability, and defaults. Property names must match the schema exactly, so no `@SerialName` renaming is permitted on these classes.

All three schemas set `additionalProperties: false`. The `Json` instance is therefore configured:

```kotlin
Json {
    encodeDefaults = false   // absent optionals are omitted, not written as null
    explicitNulls = false    // a null optional serializes to absence
    ignoreUnknownKeys = false // an unknown inbound field is a violation, not noise
}
```

`ignoreUnknownKeys = false` matters for FR-011: an inbound `hello` carrying an extra field is a schema violation and must be refused, not silently accepted.

---

## Inbound: `hello`

**FQCN**: `com.couchraoke.tv.domain.control.model.Hello`

```kotlin
package com.couchraoke.tv.domain.control.model

@Serializable
data class Hello(
    val type: String,
    val protocolVersion: Int,
    val clientId: String,
    val deviceName: String,
    val appVersion: String,
    val httpPort: Int,
)
```

Every property is non-null and has no default, so kotlinx-serialization raises `MissingFieldException` for an absent field. `HandshakeValidator` does **not** rely on that exception to name the field, because kotlinx reports missing fields in declaration order and may batch them. Instead it decodes to `JsonObject` first, checks presence in `required`-array order, and only then decodes to `Hello`. That is what makes `"Missing required field: clientId"` deterministic and match F20 exactly.

---

## Outbound: `sessionState`

**FQCN**: `com.couchraoke.tv.domain.control.model.SessionState`

```kotlin
package com.couchraoke.tv.domain.control.model

@Serializable
data class SessionState(
    val type: String = "sessionState",
    val protocolVersion: Int = 1,
    val tsTvMs: Double? = null,
    val sessionId: String,
    val slots: Slots,
    val connectedDevices: List<ConnectedDeviceDto>,
    val inSong: Boolean,
    val songTimeSec: Double? = null,
    val connectionId: Int? = null,
)

@Serializable
data class Slots(val P1: SlotDto, val P2: SlotDto)

@Serializable
data class SlotDto(val connected: Boolean, val deviceName: String)

@Serializable
data class ConnectedDeviceDto(
    val clientId: String,
    val displayName: String,
    val state: String,
    val slot: String? = null,
)
```

`type` and `protocolVersion` carry defaults but are **required** by the schema, so `ControlMessageCodec` serializes this class with `encodeDefaults = true` for outbound `sessionState` specifically, while leaving the nullable optionals omitted via `explicitNulls = false`. This is the one place the two settings must differ, and it is why encoding goes through the codec rather than a bare `Json.encodeToString` call at each site.

`Slots.P1` / `Slots.P2` violate Kotlin property-naming convention deliberately: the schema names them `P1` and `P2`, and matching the wire exactly is worth more than the convention. Detekt's naming rules need a scoped `@Suppress` here with this reason — not a blanket suppression.

**This slice's values**: `tsTvMs` omitted, both slots `connected = false, deviceName = ""`, every `ConnectedDeviceDto.state = "connected_unassigned"` with `slot = null`, `inSong = false`, `songTimeSec` omitted. `connectionId` is set **only** on the direct reply to a `hello` and omitted everywhere else (FR-014).

---

## Outbound: `error`

**FQCN**: `com.couchraoke.tv.domain.control.model.Refusal`

```kotlin
package com.couchraoke.tv.domain.control.model

@Serializable
data class Refusal(
    val type: String = "error",
    val protocolVersion: Int = 1,
    val tsTvMs: Double? = null,
    val code: String,
    val message: String,
)
```

**FQCN**: `com.couchraoke.tv.domain.control.RefusalReason`

```kotlin
package com.couchraoke.tv.domain.control

enum class RefusalReason(val code: String) {
    INVALID_TOKEN("invalid_token"),
    PROTOCOL_MISMATCH("protocol_mismatch"),
    INVALID_MESSAGE("invalid_message"),
    SESSION_FULL("session_full"),
    SESSION_LOCKED("session_locked"),
}
```

`code` is the string on the wire **and** the WebSocket close reason (FR-016), so the two can never drift apart. `SESSION_LOCKED` is unreachable in this slice; it exists so the refusal vocabulary is complete.

`message` is always non-empty and human-readable. For missing fields it names the field.

---

## Refusal delivery sequence

Binding on `KtorControlTransport.refuse`:

1. Serialize the `Refusal` and send it as a text frame.
2. Flush.
3. Close with `CloseReason(VIOLATED_POLICY /* 1008 */, reason = code)`.

Steps 1 and 3 must not be separable by a caller, which is why `refuse(code, message)` is one port method. The peer distinguishes these outcomes: reason delivered then closed is exit 3; closed without a reason is **exit 4**, which the loopback gate treats as an FR-016 failure rather than a passing variant.

---

## Fixture correspondence

| Fixture case | Asserted against | Note |
|---|---|---|
| `F20/case_missing_clientId/expected.error.json` | `Refusal` byte-for-byte | Matches Appendix B.2.5 |
| `F20/case_bad_protocolVersion/expected.error.json` | `Refusal` byte-for-byte | Matches B.2.5 |
| `F20/case_missing_httpPort/expected.error.json` | `Refusal` byte-for-byte | Matches B.2.5 |
| `F20/case_valid_hello/expected.sessionState.json` | **decision only** | The payload is drifted — see below |
| `F22/expected.transitions.json` | `GamePhaseMachine` | Full table, valid and invalid |

**F20's accept case is not asserted byte-for-byte.** Its `expected.sessionState.json` omits `connectedDevices`, which Appendix B.2.2 lists as required, and it populates `slots.P1` with the joining phone even though `tv_app.md` line 893 defines `slots` as *singer assignment* slots filled by `assignSinger` from Select Players in Slice 5. The fixture contradicts the normative schema on both points, so the schema wins: the test asserts that the `hello` is accepted and that `connectionId` is 1, then validates the emitted payload against B.2.2 rather than against the stale file.

Recorded in the spec's Out-of-Scope Observations. `fixtures/` is protected and the corpus is shared with the phone repository, so the correction belongs there rather than here.
