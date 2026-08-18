# Feature Specification: Phone Joins (Slice 1)

**Feature Branch**: `DH1-slice-1-phone-joins`
**Created**: 2026-08-18
**Status**: Draft
**Input**: User description: "slice 1 of plan.md in ../couchraoke-spec/plan.md" — Slice 1 "Phone joins": WebSocket server, mDNS advertisement, join overlay, GamePhase `Open` and `Error` only.

**Source specifications**: `couchraoke-spec/plan.md` §1–§4 (Slice 1), `couchraoke-spec/tv_app.md` §2.3.5, §2.3.8, §2.3.9, §2.3.11, §2.3.13, §2.3.14, §2.3.15, §2.6.13, §2.6.15.1 (Join admission, Kick), §4.1, Appendix B.2.1, B.2.2, B.2.5. Fixtures F15, F20, F22.

## Clarifications

### Session 2026-08-18

- Q: `plan.md` says "a second phone joining an occupied session gets the specified rejection", but `tv_app.md` §2.6.15.1 caps the roster at 10 devices. How should the rejection gate read? → A: The roster cap is 10. The gate asserts an 11th device is rejected with `session_full` (per F15 `case_slot_taken`). The top-level `F15/transcript.jsonl`, which rejects a 3rd phone with `session_full`, is stale and contradicts both `case_slot_taken` and the 10-device cap.
- Q: `plan.md` requires a "specified close code" for protocol version mismatch, but `tv_app.md` never specifies one. → A: All rejections close with WebSocket code `1008` (Violated Policy), and the close reason carries the same code string as the `error` payload (e.g. `protocol_mismatch`).
- Q: Slice 1 declares GamePhase `Open` and `Error` only, but §4.1 has no `Open→Error` transition, so `Error` is unreachable in this slice. How is a session-start failure surfaced? → A: As a blocking modal that prevents gameplay and returns to the song-selection surface on dismiss. GamePhase remains `Open`; no new FSM transition is introduced.
- Q: §2.6.13 defines the join overlay as a modal over the Song List, which does not exist until Slice 3. What hosts it in Slice 1? → A: A minimal Song List shell — empty-state grid plus a header Join button — that later slices fill in. The overlay opens over it per §2.6.13.
- Q: Does Slice 1 include `clientId` reclaim on reconnect, or is that entirely Slice 12? → A: Slice 1 includes reclaim: a returning `clientId` reuses its existing roster entry and receives a new `connectionId`. Singer re-assignment, `playbackState` resend, and `DisconnectPaused` remain in Slice 12.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A phone finds the TV and joins the session (Priority: P1)

A host opens the app on the TV. The app immediately starts a session, shows a join code and QR code, and announces itself on the local network. A guest opens the companion app on their phone, which either finds the TV automatically on the LAN or scans the QR code, and connects. The guest's phone appears in the TV's connected list within a couple of seconds, and the guest's phone knows which session it is in and which slot it holds.

**Why this priority**: Nothing else in the product is reachable without it. Every later slice — song lists, previews, singing, scoring — depends on at least one phone being connected and identified. This is the first slice that produces something a person can watch happen.

**Independent Test**: Launch the app, run the real out-of-process peer against it over loopback with discovery enabled, and confirm the peer's device name appears in the TV's connected roster and the peer received a session identity. No song, library, or playback capability is required.

**Acceptance Scenarios**:

1. **Given** the app has just launched and no phone is connected, **When** the host opens the join surface, **Then** a QR code and a two-word join code are displayed, and the connected count reads zero.
2. **Given** the app is advertising on the local network, **When** a phone searches for the service, **Then** it finds an advertisement carrying the join code and the protocol version, pointing at the TV's control port.
3. **Given** a phone has discovered or scanned the TV, **When** it opens a control connection using the correct join code and sends a valid introduction, **Then** the TV admits it, assigns it a unique connection identity, replies with the current session state including that identity, and the phone appears in the TV's connected roster.
4. **Given** one phone is already connected, **When** a second phone joins with a different device identity, **Then** both phones appear in the roster and the second phone receives a distinct connection identity.
5. **Given** a phone is connected, **When** its connection drops, **Then** it is removed from the connected roster and the roster reflects the change immediately.

---

### User Story 2 - Bad or unwelcome connections are refused clearly (Priority: P2)

A phone that presents the wrong join code, an unsupported protocol version, or an incomplete introduction is turned away with a machine-readable reason rather than being silently dropped or silently admitted. Once the session is at capacity, further devices are refused for that reason.

**Why this priority**: This is the half of the join boundary that the previous attempt could not prove. Admission without rejection is not a working handshake — a server that accepts everything passes the same happy-path test. It is P2 only because a demo shows the happy path first.

**Independent Test**: Drive each rejection case from the real peer process over loopback and assert the exact reason code, the delivery of the refusal before the connection closes, and the close code. Independently, replay the F20 fixture cases against the message validator with no transport involved.

**Acceptance Scenarios**:

1. **Given** the session is open, **When** a phone connects with a missing or incorrect join code, **Then** it receives a refusal with reason `invalid_token` and the connection is closed.
2. **Given** the session is open, **When** a phone introduces itself with a protocol version other than the supported one, **Then** it receives a refusal with reason `protocol_mismatch` and the connection is closed.
3. **Given** the session is open, **When** a phone's introduction omits a required field, **Then** it receives a refusal with reason `invalid_message` and the connection is closed.
4. **Given** the roster already holds the maximum number of devices, **When** a device the session has not seen before tries to join, **Then** it receives a refusal with reason `session_full`.
5. **Given** a phone is mid-handshake, **When** it sends a message the handshake does not expect, **Then** the connection is closed as a failed handshake rather than the message being ignored.

---

### User Story 3 - A phone that drops off reclaims its place (Priority: P3)

A phone loses network briefly, or its app is backgrounded and returns. It reconnects on its own, keeps the identity it had, and does not consume a second place in the session. The host sees it return rather than seeing a stranger appear alongside a ghost.

**Why this priority**: Without it, the capacity limit is not meaningful — a phone that flaps twice would exhaust the roster, and the capacity rejection could not be tested honestly. It is P3 because it only becomes observable after joining and refusal both work.

**Independent Test**: Connect the real peer, force it to drop and reconnect with the same device identity, then assert the roster size is unchanged, the roster entry is the same one, and the connection identity is new and different from the previous one.

**Acceptance Scenarios**:

1. **Given** a phone was connected and its connection dropped, **When** it reconnects presenting the same device identity, **Then** it reuses its existing roster entry rather than creating a second one.
2. **Given** a phone reconnects with the same device identity, **When** the TV admits it, **Then** it issues a new connection identity that differs from the one the phone previously held.
3. **Given** the roster is at capacity and one of its devices has dropped its connection, **When** that same device reconnects, **Then** it is admitted, whereas an unseen device is refused with `session_full`.
4. **Given** a phone's replacement connection has been admitted, **When** the superseded connection finishes closing, **Then** that cleanup does not remove the phone from the roster or invalidate its new connection identity.

---

### Edge Cases

- **The session cannot start at all.** The control port cannot be bound, or the network announcement cannot be published. The host is shown a blocking notice that gameplay is unavailable and the reason; dismissing it returns to the song-selection surface. The session phase stays `Open` and no invalid phase transition is attempted.
- **Multicast is unavailable.** Announcement packets are silently dropped by the platform when the multicast lock is not held, producing a session that looks healthy but cannot be discovered. Acquiring the lock before announcing, and releasing it when the session ends, is a requirement rather than an optimisation.
- **A phone talks to a session that has already ended.** Its join code is no longer valid, so it is refused as if the code were wrong.
- **Two connections claim the same device identity at once.** The newer connection wins the roster entry; the older one is closed. Cleanup of the older connection must not tear down state that now belongs to the newer one.
- **A phone sends an unrecognised message after the handshake succeeded.** It is ignored with a warning; the connection survives. The same message during the handshake is fatal.
- **Host state versus companion-reported state.** The TV is the sole authority for the roster, connection identities, session identity, and the join code. Nothing a phone sends can change another phone's slot, its own connection identity, or the roster's contents.
- **A phone with a valid join code but no reachable asset server.** Out of scope for this slice: nothing here contacts the phone's asset server, so an unreachable one is not observable until the next slice.

## Requirements *(mandatory)*

### Functional Requirements

**Session identity and discoverability**

- **FR-001**: On launch, the system MUST create a session in the **Open** state and generate a join code of two random English words (adjective + noun) drawn from bundled word lists, displayed uppercase and hyphen-separated (e.g. `SWIFT-PANDA`).
- **FR-002**: The join code MUST be the session token: the same value is used to authorise control connections, encoded in the QR payload, and published in the network announcement.
- **FR-003**: A join code MUST be generated per session and MUST NOT be reused across sessions.
- **FR-004**: The system MUST publish a `_karaoke._tcp` network announcement for the session's duration, whose instance name is `KaraokeTV-<noun>` (the noun half of the join code, unique on the LAN), whose port is the control port, and whose attributes carry `code` (the full join code) and `v` (the protocol version).
- **FR-005**: The system MUST acquire the platform multicast lock before publishing the announcement and MUST release it when the session ends.
- **FR-006**: On platforms that require it, the system MUST obtain local-network permission before binding the control port or publishing the announcement.
- **FR-007**: The QR payload MUST encode the full control endpoint URL including the join code as the `token` query parameter, and MUST NOT encode a discovery-service identifier.

**Admission**

- **FR-008**: The system MUST validate the join code on every new control connection, and MUST refuse a connection whose token is missing or incorrect with reason `invalid_token`.
- **FR-009**: The system MUST refuse an introduction whose protocol version is not the supported version with reason `protocol_mismatch`.
- **FR-010**: The system MUST refuse an introduction missing any required field — device identity, device name, app version, or asset-server port — with reason `invalid_message`, naming the missing field in the human-readable message.
- **FR-011**: On a valid introduction the system MUST admit the phone, assign it a connection identity, and reply with the current session state carrying that identity.
- **FR-012**: Connection identities MUST be unique within a session, assigned from an incrementing counter starting at 1, and MUST be issued fresh on every accepted connection including reconnects.
- **FR-013**: The connection identity MUST be delivered only in the session state sent in direct reply to an introduction, and MUST be absent or null in any other session-state message.
- **FR-014**: The system MUST admit phones while the session is **Open** until the roster holds 10 devices, and MUST refuse any further previously-unseen device with reason `session_full`.
- **FR-015**: Every refusal MUST deliver the reason to the phone before the connection closes, and MUST close with WebSocket code `1008` and a close reason equal to the refusal's reason code.
- **FR-016**: During the handshake, a message of an unexpected type MUST be treated as fatal and close the connection. After the handshake has succeeded, a message of an unrecognised type MUST be ignored with a warning and MUST NOT close the connection.

**Roster and reconnect**

- **FR-017**: The system MUST expose the connected roster as observable host-owned state, and MUST emit connection, disconnection, and reconnection events as they occur.
- **FR-018**: An introduction presenting a device identity already in the roster MUST be admitted as a reclaim of that existing roster entry, MUST NOT increase the roster size, and MUST receive a new connection identity.
- **FR-019**: Reclaim admission MUST be decided from the device identity's presence in the session roster, not from whether a live connection for it currently exists.
- **FR-020**: When a new connection replaces an existing one for the same device identity, cleanup of the superseded connection MUST remove session or connection state only if that closing connection is still the active one for that device.
- **FR-021**: When a phone's connection drops and is not replaced, the system MUST remove it from the connected roster and reflect that in the observable roster state immediately.

**Phase authority**

- **FR-022**: The system MUST implement the game-phase state machine so that every transition into or out of `Open`, and every transition into or out of `Error`, is accepted or rejected exactly as the normative transition table specifies, and MUST reject invalid transitions rather than silently applying them.
- **FR-023**: During this slice the phase MUST remain `Open`; no runtime path may enter `Preparing`, `Countdown`, `Live`, `Paused`, `Stopped`, `Results`, `DisconnectPaused`, or `Error`.
- **FR-024**: A failure to bind the control port or publish the network announcement MUST surface as a blocking notice stating that gameplay is unavailable, dismissible with a single acknowledgement that returns to the song-selection surface. It MUST NOT change the game phase and MUST NOT attempt an undefined transition.

**Host surface**

- **FR-025**: The system MUST present a song-selection surface at launch containing a header Join action, shown in its empty state because no library exists in this slice.
- **FR-026**: The Join action MUST open the join overlay as a modal over that surface, presenting the QR code as the dominant element with at least a four-module quiet zone, in high-contrast dark-on-light, centred and static, with the join code directly beneath it.
- **FR-027**: No animation or overlay element may intrude into the QR code or its quiet zone; the overlay's only permitted entrance motion is a single short fade or scale-fade of the modal shell.
- **FR-028**: The overlay MUST show how many phones are currently connected, updating as phones join and leave.
- **FR-029**: Dismissing the overlay MUST return to the song-selection surface without ending the session or disconnecting any phone.

**Boundaries and validation**

- **FR-030**: The TV MUST remain the sole authority for session identity, join code, roster membership, and connection identities; no companion-supplied value may override them.
- **FR-031**: Protocol payload construction, validation, admission rules, and phase-transition rules MUST live outside UI code; the join surface MUST read its state from a view model and MUST NOT perform network I/O.
- **FR-032**: Platform and framework types (sockets, announcement library, multicast lock, permissions, Compose) MUST stay confined to platform-facing layers and MUST NOT appear in the admission or phase-transition logic.
- **FR-033**: Every dependency required by this slice MUST be declared in the shared version catalogue; implementation MUST NOT introduce ad hoc dependency or version changes.
- **FR-034**: Completion MUST be evidenced by a fresh scoped `testBranch` pass for the changed classes, **and** by a passing loopback gate driven by the real out-of-process peer. Neither substitutes for the other.
- **FR-035**: No test that claims the control transport, the network announcement, or the connection lifecycle works may be satisfied by an in-process fake, and no gate may be satisfied by a skipped or conditionally-skipped test.

### Key Entities

- **Session**: The host-owned unit a phone joins. Holds a session identity, a join code, a lifecycle state (**Open** while phones may join; **Locked** and **Ended** are defined but unreachable in this slice), and the roster. Created at launch, invalidated when it ends.
- **Join code**: Adjective-noun pair, displayed and published uppercase with a hyphen. Compared case-insensitively and with hyphens stripped, so `SWIFT-PANDA`, `swift-panda` and `swiftpanda` are the same code. Doubles as the session token.
- **Connected phone**: A device admitted to the session. Identified durably by its device identity (stable across reconnects) and transiently by its connection identity (fresh per connection). Carries a display name and the port of its asset server, which this slice records but does not contact.
- **Connection identity**: Unsigned 16-bit counter value from 1, unique within a session, never reused across connections. Delivered only in the reply to an introduction. Later slices use it to route pitch data; this slice only assigns and reports it.
- **Introduction (`hello`)**: Phone → TV. Requires message type, protocol version (must be `1`), device identity (≥ 8 characters), device name (non-empty), app version (non-empty), and asset-server port (1024–65535). No additional fields are permitted.
- **Session state (`sessionState`)**: TV → Phone. Carries message type, protocol version, session identity, the two singer slots each with a connected flag and device name, the connected-device list (at most 10, each with device identity, display name, and state `assigned` or `connected_unassigned`, plus an optional slot), an in-song flag, an optional song time, and the connection identity — the last present only in the direct reply to an introduction. In this slice the in-song flag is always false, song time is always absent, and every device is `connected_unassigned` with no slot, because no song can start.
- **Refusal (`error`)**: TV → Phone. Carries message type, protocol version, a reason code from `invalid_token`, `protocol_mismatch`, `invalid_message`, `session_full` (with `session_locked` defined but unreachable here), and a non-empty human-readable message.
- **Game phase**: Host-owned playback state. Nine states with a fixed transition table. This slice keeps it at `Open` and proves the transition rules touching `Open` and `Error`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A phone on the same local network finds the TV and completes joining without the host typing anything, and appears in the TV's connected list within 5 seconds of the phone starting to look.
- **SC-002**: A phone scanning the on-screen QR code joins without any further discovery step, in a single scan.
- **SC-003**: Every one of the four refusal reasons — wrong code, unsupported version, incomplete introduction, session at capacity — is produced against a real separate peer process, with the correct reason delivered before the connection closes, 100% of the time across repeated runs.
- **SC-004**: Ten devices join a single session successfully and the eleventh previously-unseen device is refused, with no device losing its place while the eleventh is refused.
- **SC-005**: A phone that drops and returns keeps its place: the roster size is unchanged across the round trip, and its connection identity differs from the one it held before.
- **SC-006**: The connected count on the join overlay matches the number of connected phones at all times, with no observable lag after a phone joins or leaves.
- **SC-007**: When the session cannot start, the host sees a blocking notice explaining that gameplay is unavailable and returns to the song-selection surface on acknowledgement, with no crash and no wedged screen.
- **SC-008**: The slice's demo runs end to end without anyone editing anything mid-run, and Slice 0's fixture gates still pass unchanged.
- **SC-009**: The join and refusal behaviour is proved by at least one gate driven by a real separate peer process over a real socket; no production path in this slice is backed by a fake, a stub, a demo seed, or a no-op.

## Assumptions

- **Numbering.** This feature is `002-phone-joins`. Slice 0's foundation spec occupied `001` and was removed from the working tree as stale in this change; it survives on the `done/001-tv-host-foundation` branch, so reusing `001` for a different feature would be ambiguous.
- **Asset fetching is excluded despite §2.3.19.** That section requires the TV to fetch `/manifest.json` immediately after a successful handshake, but `plan.md` explicitly puts song list and HTTP in Slice 2 and states they are out of scope here. The introduction's asset-server port is therefore recorded and left uncontacted, and the join overlay shows a placeholder count as `plan.md` permits.
- **The pitch data socket is not bound.** §2.3.5 requires the datagram socket to be bound at session start, but `plan.md` places clock sync and pitch ingress in Slice 8, and Slice 1's "one risky new thing" rule excludes a second transport. Binding it here would add an untested transport with no gate to prove it.
- **`session_locked` is defined but not exercised.** It is only reachable once a song can start, which is Slice 6 at the earliest. Its reason code is specified so the refusal vocabulary is complete, but no acceptance scenario claims it.
- **Control port default.** The `tv_app.md` QR example and the peer process both use `8080`, and no section pins the value normatively, so `8080` is the default control port with the actual bound port always taken from configuration rather than assumed.
- **Singer slots are reported empty.** §2.3.11 requires the two singer slots in every session-state message, but slot assignment happens in Select Players, which is Slice 5. Both slots therefore report disconnected with an empty device name throughout this slice.
- **The screenshot gate does not apply yet.** `plan.md` introduces the screenshot gate in Slice 3 and warns that the previous baselines were recorded at twice the intended viewport. Slice 1 therefore ships the join surface without a committed baseline, and Slice 3 owns establishing one at the correct viewport.
- **Kick is excluded.** §2.6.15.1 defines Kick on the Settings > Connect Phones screen, which `plan.md` schedules for Slice 17. Roster capacity in this slice is exercised through joins and reconnects only.
- **Contracts are deferred to planning, not omitted.** The constitution requires every material producer/consumer boundary to be pinned as fully-qualified name plus method signature before implementation. This specification stays behaviour-level by design; that pinning is the planning step's output and is a precondition for implementation starting.

## Out-of-Scope Observations *(reported, not fixed)*

Per the repository's scope policy these were found while specifying Slice 1 and are **not** addressed by this change.

1. **`couchraoke-spec/testing/fixtures/F15_session_lifecycle_disconnect_reconnect/transcript.jsonl` is stale.** It refuses a third phone with `session_full` while only two are connected, contradicting the 10-device cap in `tv_app.md` §2.6.15.1 and Appendix B.2.2, and contradicting its own sibling case `case_slot_taken`. *Suggested fix*: extend the transcript to ten admitted devices before the refusal, or re-label the case as a two-slot singer-assignment limit if that was the intent.
2. **`couchraoke-spec/plan.md` §5 claims a constitution bump that has not happened.** It states "Constitution → v2.0.0. Principle IV no longer makes `testBranch` the single source of truth and now requires an L or D gate per slice", but `.specify/memory/constitution.md` on disk is v1.0.0 and still names `testBranch` the single source of truth. This spec requires both gates so it is correct under either version. *Suggested fix*: either amend the constitution to v2.0.0 as `plan.md` describes, or correct `plan.md` §5 to record it as pending.
3. **`tv_app.md` never specifies a WebSocket close code**, although `plan.md`'s Slice 1 gate asserts "the specified close code". This spec pins `1008` by clarification. *Suggested fix*: record the close-code convention in `tv_app.md` §2.3.11 alongside the reason-code enum, so both specs and the peer process agree.
4. **`mockphone/readme.md` cites a retired specification.** It references `couchraoke_spec.md v4.20` and section numbers (§8.3, §8.5, §8.6, §7.4) that do not exist in the current `tv_app.md`. The frame table itself is correct at 20 bytes. *Suggested fix*: repoint the references at `tv_app.md` §2.3 and drop the version footer.
