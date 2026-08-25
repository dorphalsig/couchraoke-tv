# Data Model: Phone Joins

**Feature**: `003-phone-joins` | **Date**: 2026-08-19

All types below live in `com.couchraoke.tv.domain` and import nothing from Android, Ktor, jmDNS or Compose.

---

## Value types

`com.couchraoke.tv.domain.session.model`

| Type | Representation | Invariants |
|---|---|---|
| `SessionId` | `value class SessionId(val value: String)` | Non-empty. Opaque — no format is normative (Appendix B.2.2 types it as a bare string and the fixtures use `sess-001`, `S1` and `sess-1`). Unique among sessions this TV creates. Phones must not parse it. |
| `DeviceId` | `value class DeviceId(val value: String)` | Length ≥ 8 (B.2.2 `clientId.minLength`). Phone-supplied but never authoritative — it keys the roster and nothing else. |
| `ConnectionId` | `value class ConnectionId(val value: Int)` | 1 ≤ value ≤ 65535. uint16. Never reused within a session. |
| `JoinCode` | `data class JoinCode(adjective: String, noun: String)` | Both non-empty, single words. `display` renders `ADJECTIVE-NOUN` uppercase. `noun` alone forms the mDNS instance name. |
| `AssetPort` | `value class AssetPort(val value: Int)` | 1024 ≤ value ≤ 65535. Recorded, never contacted in this slice. |

`JoinCode` equality is structural, but **token comparison is not** — see `JoinCodeMatcher` in [contracts/domain-api.md](./contracts/domain-api.md).

---

## Session

`com.couchraoke.tv.domain.session.model.SessionSnapshot`

| Field | Type | Notes |
|---|---|---|
| `sessionId` | `SessionId` | Assigned at creation |
| `joinCode` | `JoinCode` | Assigned at creation, never reused across sessions (FR-003) |
| `lifecycle` | `SessionLifecycle` | `Open` \| `Locked` \| `Ended`. Only `Open` is reachable in this slice |
| `phase` | `GamePhase` | Stays `Open` throughout this slice (FR-027) |
| `roster` | `List<RosterEntry>` | Ordered by admission. Size ≤ 10 |
| `connected` | `List<ConnectedDevice>` | The subset of `roster` with a live connection |

`SessionLifecycle` and `GamePhase` are deliberately separate. `SessionLifecycle.Locked` is what produces the `session_locked` refusal; `GamePhase` is the nine-state playback machine. Neither derives from the other.

---

## SessionRoster

The durable membership list. Keyed by `DeviceId`, capacity 10.

`RosterEntry`:

| Field | Type | Notes |
|---|---|---|
| `deviceId` | `DeviceId` | Key. Stable across reconnects |
| `displayName` | `String` | Non-empty. Refreshed from the latest `hello` |
| `appVersion` | `String` | Non-empty. Recorded only |
| `assetPort` | `AssetPort` | Recorded, never contacted this slice |
| `connection` | `ConnectionId?` | `null` when the device is retained but disconnected |

**Invariants:**

1. Size never exceeds 10 (FR-015).
2. An entry survives its connection ending; `connection` becomes `null` and the entry keeps its capacity slot (FR-023).
3. Admitting a `DeviceId` already present is a **reclaim**: the entry is reused, size is unchanged, and a fresh `ConnectionId` is issued (FR-020, FR-021).
4. Reclaim is decided from presence in the roster, never from whether a live connection exists (FR-021). A device at capacity can therefore always return.
5. Capacity is checked only for a **previously-unseen** `DeviceId`. A full roster refuses new devices with `session_full` but still readmits its own members.
6. Entries are released by Kick, session end, or song end — none of which exist in this slice, so here entries are only ever added or have their `connection` nulled (FR-024).

**Derived**: `connectedDevices` is `entries.filter { it.connection != null }`. Retained-but-disconnected entries never appear in the connected list or the overlay count (FR-025).

---

## Connection supersession

When a second connection arrives for a `DeviceId` that already has a live one, the old connection is superseded. Cleanup of the superseded connection must remove state **only if it is still the active connection for that device** (FR-022).

This is enforced by comparing `ConnectionId` on close: `onClose(deviceId, connectionId)` is a no-op unless `roster[deviceId].connection == connectionId`. Without this guard, the old socket's late close event would evict the new connection that just replaced it.

Ordering matters too: a drop is removed from the connected list **as part of handling the close, before any subsequent admission decision** (FR-023). This is what makes SC-005's "one of the ten is disconnected at the time" case deterministic.

---

## GamePhase

`com.couchraoke.tv.domain.session.GamePhase` — nine states: `Open`, `Preparing`, `Countdown`, `Live`, `Paused`, `DisconnectPaused`, `Stopped`, `Results`, `Error`.

The transition table is `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json`, which the machine is tested against directly rather than restated here.

Edges touching this slice's two states:

| Direction | Valid | Invalid |
|---|---|---|
| Out of `Open` | `Open→Preparing` | `Open→Paused`, `Open→Live` |
| Into `Open` | `Countdown→Open`, `Paused→Open`, `DisconnectPaused→Open`, `Results→Open`, `Error→Open` | `Preparing→Open` |
| Into `Error` | `Preparing→Error`, `Countdown→Error`, `Live→Error` | — |
| Out of `Error` | `Error→Open` | — |

**There is no `Open→Error` edge.** This is the whole reason FR-028 makes a session-start failure a blocking modal rather than a phase change: entering `Error` from `Open` would be an invalid transition, and FR-026 requires invalid transitions to be rejected rather than silently applied.

In this slice the phase is created as `Open` and never leaves it (FR-027). The machine is fully implemented and fully tested against F22 anyway, because FR-026 requires the rules to be correct, not merely the reachable ones.

---

## Wire payloads

Field names and types come from `tv_app.md` Appendix B.2. All three schemas set `additionalProperties: false`, so an unknown field is a hard error rather than something to ignore.

### `hello` — Phone → TV

| Field | Type | Required | Validation |
|---|---|---|---|
| `type` | `"hello"` | yes | Exact match |
| `protocolVersion` | integer, const `1` | yes | ≠ 1 → `protocol_mismatch` |
| `clientId` | string, minLength 8 | yes | Missing → `invalid_message` |
| `deviceName` | string | yes | Missing → `invalid_message` |
| `appVersion` | string | yes | Missing → `invalid_message` |
| `httpPort` | integer 1024–65535 | yes | Missing → `invalid_message` |

Every missing-field message names the field: `"Missing required field: clientId"` (F20).

**Validation order is normative**, because a `hello` can fail two ways at once. `protocolVersion` is checked **before** field presence: F20's `case_bad_protocolVersion` carries a complete payload, but a version-2 phone may legitimately have a different field set, so version is the outer gate. Within field presence, order follows the schema's `required` array so the reported field is deterministic.

### `sessionState` — TV → Phone

| Field | Type | Required | This slice |
|---|---|---|---|
| `type` | `"sessionState"` | yes | Constant |
| `protocolVersion` | integer, const `1` | yes | Constant |
| `tsTvMs` | number \| null | no | Omitted — clock sync is Slice 8 |
| `sessionId` | string, minLength 1 | yes | The session identity |
| `slots` | `{P1, P2}`, each `{connected, deviceName}` | yes | **Both report `connected: false`, `deviceName: ""`** |
| `connectedDevices` | array, maxItems 10 | yes | Live connections only |
| `inSong` | boolean | yes | Always `false` |
| `songTimeSec` | number \| null | no | Always omitted |
| `connectionId` | integer \| null | no | Present **only** in the direct reply to a `hello` (FR-014) |

`connectedDevices[]` items: `clientId` (minLength 8), `displayName`, `state` ∈ `assigned` \| `connected_unassigned`, optional `slot` ∈ `P1` \| `P2` \| null. In this slice every device is `connected_unassigned` with no slot, because slot assignment requires Select Players (Slice 5).

### `error` — TV → Phone

| Field | Type | Required |
|---|---|---|
| `type` | `"error"` | yes |
| `protocolVersion` | integer, const `1` | yes |
| `tsTvMs` | number | no — omitted this slice |
| `code` | string, minLength 1 | yes |
| `message` | string, minLength 1 | yes |

`code` ∈ `invalid_token`, `protocol_mismatch`, `invalid_message`, `session_full`, `session_locked`. The last is defined for completeness and unreachable here.

Every refusal delivers this frame **before** closing, then closes with code `1008` and close reason equal to `code` (FR-016).

---

## ⚠ Fixture drift: F20's accept case contradicts Appendix B.2.2

`fixtures/F20_websocket_message_validation/case_valid_hello/expected.sessionState.json` disagrees with the normative schema in two ways:

1. **It omits `connectedDevices`**, which B.2.2 lists in `required`. The payload as written is invalid against its own schema.
2. **It sets `slots.P1` to `{connected: true, deviceName: "Pixel 7"}`** — the joining phone. But line 893 of `tv_app.md` describes `slots` as *singer assignment slots*, and assignment happens through `assignSinger` from Select Players (Slice 5). Joining does not assign a slot. F15's README confirms the flow: phones join, and a slot is assigned later when a song is selected.

**Resolution — the normative schema wins.** Phase B therefore:

- asserts the three **refusal** cases against F20 byte-for-byte, since they match Appendix B.2.5 exactly and are the fixture's real subject;
- asserts the accept case's **decision** — admitted, `connectionId` = 1 — but builds the `sessionState` payload per B.2.2, with `connectedDevices` present and both slots empty.

This is recorded rather than fixed: `fixtures/` is protected, and the conflict is in the shared fixture corpus, so it belongs to the spec repository. It is filed in the spec's Out-of-Scope Observations.

It also confirms the spec's "singer slots are reported empty" assumption, which was inferred during `/speckit.specify` and is now backed by line 893 and by F15's ordering.
