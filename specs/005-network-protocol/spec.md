# Feature Specification: Network Protocol

**Feature Branch**: `005-network-protocol`
**Created**: 2026-03-16
**Status**: Draft
**Input**: User description: "network-protocol — implements §8 of the Couchraoke spec, updated with authoritative playbackState and simplified assignSinger playback semantics"

## Clarifications

### Session 2026-03-16

- Q: Does feature 005 own the Locked session state, or does it read it from feature 006? → A: Feature 005 reads an `isLocked` flag from a session model owned by feature 006 and sends `error(code="session_locked")` when set. Feature 005 has no ownership of session lifecycle state.
- Q: On disconnect, remove songs immediately or hold for a grace period? → A: Remove immediately; no grace period. Phone re-sends its full manifest on reconnect.
- Q: On manifest fetch failure, retain prior catalog or clear? → A: Retain previous catalog unchanged and show a brief error toast. Confirmed by §3.1 of original spec. Note: TV always pulls the manifest via GET /manifest.json — the phone never pushes it.

### Session 2026-04-07

- Q: Should `assignSinger` continue to represent live playback progression? → A: No. `assignSinger` is reduced to role/config for an assignment; live countdown, play, pause, stop, and reconnect synchronization move to the new `playbackState` message.
- Q: `assignSinger` update request listed `thresholdIndex` as both kept and removed. Which interpretation applies? → A: Remove it. The final field list and example make microphone sensitivity phone-managed, so `thresholdIndex` is no longer part of `assignSinger`.
- Q: Is `playbackState` consumed in feature 005? → A: The TV must offer it now as part of the network protocol contract, but richer phone-side consumption is expected in later features.
- Q: On singer reconnect, does `playbackState.revision` increment? → A: No. Reconnect re-sends the latest `playbackState` for the current `songInstanceSeq` without incrementing `revision` unless some separate playback contract change also occurs.
- Q: Should `assignSinger` keep local pre-roll fields like `startMode` or `countdownMs`? → A: No. Countdown representation moves entirely to `playbackState`; `assignSinger` remains static assignment/config only.
- Q: Is `playbackState.reason` a fixed enum? → A: No. It is an optional open-ended hint; phones may use known values when helpful but must not require exhaustive handling of a closed set.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Phone Joins a Karaoke Session (Priority: P1)

A guest takes out their phone, opens the Couchraoke app, and types the join code displayed on the TV screen. Within a few seconds their device name appears on the TV and their song library is added to the shared song list.

**Why this priority**: Foundational interaction — nothing else works until a phone can connect. Includes the join-code handshake and the immediate song catalog transfer that follows.

**Independent Test**: Can be fully tested by simulating a phone sending a valid join request and verifying the TV responds with session state (including a unique sender ID) then fetches and ingests the phone's song catalog.

**Acceptance Scenarios**:

1. **Given** the TV has started a session and is advertising a join code, **When** a phone sends a valid join request with the correct code, **Then** the TV responds with current session state including a unique sender ID for that connection.
2. **Given** a phone has joined, **When** the TV retrieves the phone's song catalog, **Then** all songs reported by that phone are added to the shared library attributed to that device.
3. **Given** a phone is connected, **When** the phone disconnects, **Then** all songs contributed by that phone are immediately removed from the shared library.
4. **Given** a phone is connected, **When** the phone's catalog is re-fetched, **Then** the TV replaces (not appends) that phone's songs with the new catalog.

---

### User Story 2 — Invalid or Unauthorized Join Attempts Are Rejected (Priority: P1)

A guest types the wrong code, or a phone running an incompatible version tries to join. The TV rejects the connection immediately with a clear error and the session is not disrupted.

**Why this priority**: Security and stability — accidental joins on a shared LAN must be blocked without manual host intervention.

**Independent Test**: Can be tested by sending join requests with wrong codes, mismatched versions, or when the session is full, and verifying the correct rejection error is sent.

**Acceptance Scenarios**:

1. **Given** a session is active, **When** a phone sends a join request with the wrong code, **Then** the TV rejects it with an `invalid_token` error.
2. **Given** a session is active, **When** a phone sends a join request with an incompatible protocol version, **Then** the TV rejects it with a `protocol_mismatch` error.
3. **Given** a session already has the maximum number of connected phones, **When** another phone tries to join, **Then** the TV rejects it with a `session_full` error.
4. **Given** a session is locked, **When** a new phone tries to join, **Then** the TV rejects it with a `session_locked` error.
5. **Given** a phone sends a join request without declaring its HTTP server port, **Then** the TV rejects it with an error.

---

### User Story 3 — Assigned Phones Stay Synchronized with Playback (Priority: P2)

The host selects a song and assigns singers. Each assigned phone receives stable assignment/configuration details plus authoritative playback-state updates that tell it whether the song is counting down, playing, paused, or stopped, along with the current paused-aware song clock.

**Why this priority**: Core karaoke gameplay now depends on a single playback contract for countdown, pause/resume, reconnect, and early stop. Without it, the phone cannot reliably align its UI and microphone behavior with the TV.

**Independent Test**: Can be tested by verifying the TV sends a well-formed `assignSinger` message for assignment setup, then sends `playbackState` updates with correct song instance, revision, state, and paused-aware song time across countdown, play, pause, seek, reconnect, and stop transitions.

**Acceptance Scenarios**:

1. **Given** a song starts and a phone is assigned as P1, **When** the TV sends `assignSinger`, **Then** the message includes all required assignment/config fields: `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `effectiveMicDelayMs`, `expectedPitchFps`, `stopAtLyricsTimeMs`, `udpPort`, and optional song metadata.
2. **Given** an assigned phone is preparing to sing, **When** the TV enters countdown, **Then** it sends `playbackState` with `state="countdown"`, the current `songInstanceSeq`, a revision for that song instance, `lyricsTimeMs`, `stopAtLyricsTimeMs`, `countdownRemainingMs`, and `tsTvMs`.
3. **Given** a song is active, **When** the host pauses, resumes, or seeks playback, **Then** the TV sends a new `playbackState` with an incremented revision, the updated `state`, and the authoritative paused-aware `lyricsTimeMs`.
4. **Given** the host stops a song early or the song reaches its end, **When** playback transitions to stopped, **Then** the TV sends `playbackState` with `state="stopped"`, the final `lyricsTimeMs`, `stopAtLyricsTimeMs`, and an optional stop `reason`.
5. **Given** a singer reconnects during an active assignment, **When** the TV restores control messages for that device, **Then** it re-sends the current `assignSinger` details and the latest `playbackState` for the same `songInstanceSeq` and revision.
6. **Given** the phone streams pitch frames, **When** a valid frame arrives, **Then** the TV decodes all fields correctly and routes it to the correct player slot.
7. **Given** a pitch frame arrives for an unknown sender or mismatched player slot, **When** the TV processes it, **Then** the frame is silently dropped.
8. **Given** a pitch frame arrives for an old or completed song instance, **When** the TV processes it, **Then** the frame is silently dropped.
9. **Given** a pitch frame is not exactly 16 bytes, **When** the TV receives it, **Then** it is silently dropped.

---

### User Story 4 — Song Files Stream from Phone to TV (Priority: P2)

When a song from a phone's catalog is selected and started, the TV streams audio and lyrics directly from that phone over HTTP. No files are downloaded or stored on the TV.

**Why this priority**: Without this, phone songs cannot play. Also includes the local-network HTTP cleartext configuration required on Android.

**Independent Test**: Can be tested by verifying the TV passes song asset URLs directly to the media player, that range requests are honoured by the phone's server, and that asset failures are handled gracefully.

**Acceptance Scenarios**:

1. **Given** a song entry has an audio URL, **When** the TV starts the song, **Then** the URL is passed directly to the media player with no intermediate file download.
2. **Given** the media player seeks, **When** it issues a byte-range request to the phone's HTTP server, **Then** the server responds with a partial-content response covering the requested range.
3. **Given** the phone's HTTP server becomes unreachable, **When** an asset request fails, **Then** the TV surfaces a recoverable error for audio; missing images are suppressed silently.
4. **Given** the TV requests `/manifest.json`, **When** the phone responds, **Then** the response is not served from cache — the TV always receives the phone's current song list.

---

### User Story 5 — TV and Phone Clocks Are Synchronized (Priority: P2)

Before singing begins, the TV runs a brief timing calibration with each connected phone so that pitch frames carry timestamps that map accurately to the TV's internal playback timeline. This allows the scoring engine to match pitches to the correct beat window.

**Why this priority**: Without clock sync, pitch frame timestamps drift and scoring becomes inaccurate. It is a prerequisite for beat-accurate scoring.

**Independent Test**: Can be tested by simulating a full ping/pong/clockAck exchange (fixture F14v2) and verifying all four timestamps are present and sufficient for the phone to compute a valid clock offset.

**Acceptance Scenarios**:

1. **Given** a phone has just connected, **When** the TV runs the initial clock sync, **Then** 5 rapid exchanges complete before any song starts.
2. **Given** a song is in progress, **When** the TV considers clock sync, **Then** sync is suspended for the duration of the song.
3. **Given** a song ends or a phone reconnects, **When** clock sync resumes, **Then** a single exchange re-establishes the offset.
4. **Given** a full ping/pong/clockAck cycle completes, **When** the phone uses all four timestamps, **Then** it can compute a valid offset mapping its monotonic time to the TV's monotonic time.

---

### Edge Cases

- What happens when the TV stops a song during countdown before playback has started?
- What happens when a singer reconnects while the current playback state is paused?
- What happens when pitch frames continue arriving after the TV has already emitted `playbackState(state="stopped")` for that song instance?
- What happens when the join code contains mixed case or formatting characters (hyphens, spaces)?
- What happens if the phone's HTTP server returns an error for `/manifest.json`?
- What happens when an empty manifest is sent (zero songs)?

## Requirements *(mandatory)*

### Functional Requirements

**Discovery & Advertising**

- **FR-001**: The TV MUST advertise itself on the local network for the duration of an active session so phones can locate it without knowing its IP address.
- **FR-002**: The TV's advertisement MUST include the full join code and a stable instance name derived from it so phones can match a user-entered code to the correct TV.
- **FR-003**: The TV MUST acquire a multicast network resource before starting the advertisement and release it when the session ends.

**Session Token & Handshake**

- **FR-004**: The TV MUST generate a random join code per session with at least 32 bits of entropy; codes MUST NOT be reused across sessions.
- **FR-005**: The join code MUST be displayable in a human-enterable format (case-insensitive, groupable with hyphens or spaces).
- **FR-006**: Join code comparison MUST be case-insensitive after stripping spaces and hyphens.
- **FR-007**: The TV MUST reject connections with a missing or incorrect join code with an `invalid_token` error before closing.
- **FR-008**: The TV MUST reject connections whose declared protocol version is not `1` with a `protocol_mismatch` error.
- **FR-009**: The TV MUST reject join requests that do not declare an HTTP server port.
- **FR-010**: The TV MUST assign a unique 16-bit sender ID to each phone upon successful handshake and deliver it in the initial session-state response.
- **FR-011**: On reconnect, the TV MUST assign a **new** sender ID and deliver it in the session-state response for that reconnect.

**Song Catalog Integration**

- **FR-012**: After a successful handshake, the TV MUST fetch the phone's manifest from `/manifest.json` and add the phone's songs to the shared library. If the fetch fails (HTTP error, timeout, or unreachable), the TV MUST retain the previous catalog for that phone unchanged and show a brief error toast.
- **FR-013**: When a phone disconnects, the TV MUST immediately remove all songs contributed by that phone from the shared library.
- **FR-014**: On re-fetch, the TV MUST replace that phone's prior shared-library entries rather than appending duplicates.

**Singing Assignment, Playback State & Pitch Stream**

- **FR-015**: When a song starts and singers are assigned, the TV MUST send each assigned phone an `assignSinger` message containing the current session, song instance, player slot, singing difficulty, microphone delay, expected pitch cadence, logical stop point in song time, pitch listener port, and optional song metadata.
- **FR-016**: `assignSinger` MUST remain a role/configuration message only. Live playback progression — including countdown, playing, paused, stopped, the current playback clock, and countdown timing — MUST NOT be represented by `assignSinger`.
- **FR-017**: For each active singer assignment, the TV MUST offer a `playbackState` message containing the current session, song instance, revision, playback state, paused-aware lyrics clock, assignment stop point in song time, countdown remaining time when relevant, an optional reason hint, optional song metadata, and the TV monotonic timestamp for the snapshot.
- **FR-018**: The playback-state revision MUST increase monotonically within each song instance whenever the authoritative playback contract changes, including pause, resume, seek, and early stop. Reconnect by itself MUST re-send the latest revision without incrementing it.
- **FR-019**: Playback state MUST be one of `countdown`, `playing`, `paused`, or `stopped`.
- **FR-020**: The paused-aware lyrics clock MUST use `0` to mean the start of the audio file, matching the existing lyrics-time semantics already used elsewhere in the specification.
- **FR-021**: The assignment stop point in song time MUST replace `endTimeTvMs` as the value phones use for UI completion and exit behavior.
- **FR-022**: Countdown remaining time MUST be present when playback state is `countdown` and absent otherwise.
- **FR-023**: The TV MUST send or resend the latest `playbackState` whenever a singer first becomes active, whenever countdown/play/pause/stop state changes, whenever the authoritative song clock is corrected by a seek, and whenever a singer reconnects during the assignment.
- **FR-024**: When playback stops before the assignment stop point, the TV MUST send a stopped playback state and MAY include a reason hint appropriate to the stop condition; when playback stops because the song naturally ends, the reason hint SHOULD indicate song completion.
- **FR-025**: The TV MUST bind its UDP pitch listener before any phone connects so the port is stable for the entire session.
- **FR-026**: The TV MUST decode incoming 16-byte little-endian pitch frames and route each to the correct player slot.
- **FR-027**: The TV MUST silently drop any pitch frame that: is not exactly 16 bytes; carries an unrecognised sender ID; carries a sender ID that does not match the expected sender for the declared player slot; carries a song instance that does not match the active assignment; or arrives after the assignment has already entered the stopped playback state.
- **FR-028**: The TV MUST interpret a pitch frame with MIDI note `255` as unvoiced (no pitch detected) and convert all other MIDI note values into the internal tone scale.
- **FR-029**: When a singer reconnects mid-song, the TV MUST re-send `assignSinger` with the new sender ID and current assignment stop point, then re-send the latest `playbackState` with the current song instance, existing revision, playback state, and paused-aware lyrics clock.

**Song File Delivery**

- **FR-030**: The TV MUST pass song asset locations from the catalog directly to playback without intermediate file storage.
- **FR-031**: The TV MUST allow direct local-network retrieval of song assets from connected phones.
- **FR-032**: The phone song server contract MUST support partial retrieval of audio and video files so playback can seek within media.
- **FR-033**: The phone manifest contract MUST always expose the most recent song scan state rather than stale cached results.

**Clock Sync**

- **FR-034**: The TV MUST initiate clock sync immediately after a phone connects, running 5 rapid exchanges before singing begins.
- **FR-035**: The TV MUST suspend clock sync while a song is in progress and resume with a single exchange on song end or reconnect.
- **FR-036**: Each clock-sync cycle MUST provide the phone with the four timestamps needed to map its monotonic clock to the TV's monotonic clock.

**Validation & Error Handling**

- **FR-037**: The TV MUST ignore unknown message types with a warning; during handshake, an unexpected type is a fatal error.
- **FR-038**: The TV MUST send distinct rejection codes for invalid token, protocol mismatch, full session, and locked session conditions. Locked-session behavior remains read-only with respect to the broader session lifecycle.

### Key Entities

- **Session Token / Join Code**: A short, random, human-enterable code generated per session. It gates phone connections, is displayed on the TV, entered on the phone, and is never reused.
- **Connection**: An active phone-to-TV link carrying a unique sender ID, a stable client identity, device name, declared song-serving port, and any current player-slot assignment.
- **Sender ID**: A unique 16-bit identifier assigned by the TV per connection. It is embedded in pitch frames and changes on reconnect.
- **Song Catalog / Manifest**: The current list of songs a phone exposes for shared library ingestion.
- **Singer Assignment**: A role/configuration message from TV to phone containing the static parameters needed for one singing assignment. It does not carry live playback progression or countdown timing.
- **Playback State**: An authoritative TV-to-phone playback snapshot for one song instance. It includes a monotonically revisioned state (`countdown`, `playing`, `paused`, `stopped`), the current paused-aware lyrics clock, the logical stop point, an optional open-ended reason hint, optional song metadata, and the TV monotonic timestamp used to interpret the snapshot.
- **Pitch Frame**: One pitch sample from a phone, carrying enough information for the TV to attribute it to the correct connection, song instance, and player slot.
- **Clock Offset**: The mapping a phone maintains between its monotonic clock and the TV's monotonic clock so pitch timing stays aligned.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A phone with the correct join code can complete the handshake and have its songs visible on the TV within 3 seconds of initiating the connection on a typical home LAN.
- **SC-002**: Join requests with invalid credentials are rejected within 200 ms.
- **SC-003**: After the initial 5-exchange clock sync, the phone's estimated TV timestamp is accurate to within 10 ms under normal LAN conditions — sufficient for beat-accurate scoring.
- **SC-004**: Assigned phones receive authoritative `playbackState` updates for countdown, pause, resume, seek correction, and stop within 250 ms of the TV transition on a typical home LAN.
- **SC-005**: A singer that reconnects during an active assignment receives a fresh `assignSinger` plus the latest `playbackState` for the current `songInstanceSeq` within 1 second, allowing the phone to resume the correct UI state without manual intervention.
- **SC-006**: 100% of pitch frames that fail any validation rule (wrong size, stale sender ID, wrong player slot, wrong song instance, or post-stop arrival) are silently dropped with no effect on scoring state.
- **SC-007**: Song catalog changes (connection, disconnection, re-scan) are reflected in the shared library within 1 second of the triggering event.
- **SC-008**: All 13 handshake acceptance tests (T8.3.1–T8.3.13), 5 sender-ID acceptance tests (T8.5.1–T8.5.5), and 8 pitch-stream acceptance tests (T8.6.1–T8.6.8) from the spec pass against fixtures F12v2, F13, F14v2, F15, and F18.

## Dependencies

- **Feature 001 (usdx-parser)**: Supplies validated song metadata needed during manifest ingestion.
- **Feature 004 (song-library)**: Supplies the shared library model this feature populates and updates through network events.

## Assumptions

- Existing acceptance fixtures already cover handshake, sender identity, clock sync, pitch streaming, and manifest refresh behavior for this feature update.
- This feature covers TV-side protocol responsibilities only. Phone-side song serving, richer playback UX, and pitch-streaming behavior remain out of scope.
- `playbackState` is added to the protocol in this feature as an offered TV-to-phone contract; later features may consume it more fully on the phone side.
- The session roster remains capped at two active singer slots.
- Song file delivery in this feature covers TV-side retrieval behavior only.
- Clock sync remains TV-initiated only.
- Protocol version 1 remains the only supported version for this scope.
- Microphone sensitivity is managed locally on the phone, so `thresholdIndex` is no longer part of `assignSinger`.
