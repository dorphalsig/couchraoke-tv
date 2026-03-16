# Feature Specification: Network Protocol

**Feature Branch**: `005-network-protocol`
**Created**: 2026-03-16
**Status**: Draft
**Input**: User description: "network-protocol — implements §8 of the Couchraoke spec"

## Clarifications

### Session 2026-03-16

- Q: Does feature 005 own the Locked session state, or does it read it from feature 006? → A: Feature 005 reads an `isLocked` flag from a session model owned by feature 006 and sends `error(code="session_locked")` when set. Feature 005 has no ownership of session lifecycle state.
- Q: On disconnect, remove songs immediately or hold for a grace period? → A: Remove immediately; no grace period. Phone re-sends its full manifest on reconnect.
- Q: On manifest fetch failure, retain prior catalog or clear? → A: Retain previous catalog unchanged and show a brief error toast. Confirmed by §3.1 of original spec. Note: TV always pulls the manifest via GET /manifest.json — the phone never pushes it.

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

### User Story 3 — Singing Instructions and Pitch Data Flow (Priority: P2)

The host selects a song and assigns singers. Each assigned phone receives instructions, activates its microphone, and streams pitch data back to the TV in real time so the scoring engine can compute scores.

**Why this priority**: Core karaoke gameplay — pitch frames are what the scoring engine consumes. Without this flow, scoring cannot work.

**Independent Test**: Can be tested by verifying the TV sends a well-formed singer assignment and correctly receives, routes, and validates the resulting binary pitch frames against fixture F12v2 and F15.

**Acceptance Scenarios**:

1. **Given** a song starts and a phone is assigned as P1, **When** the TV sends the singer assignment, **Then** the message includes all required fields: session ID, song instance counter, player slot, difficulty, sensitivity index, mic delay, expected frame rate, start mode, end time, and pitch listener port.
2. **Given** the phone streams pitch frames, **When** a valid frame arrives, **Then** the TV decodes all fields correctly and routes it to the correct player slot.
3. **Given** a pitch frame arrives for an unknown sender or mismatched player slot, **When** the TV processes it, **Then** the frame is silently dropped.
4. **Given** a pitch frame arrives for an old or completed song instance, **When** the TV processes it, **Then** the frame is silently dropped.
5. **Given** a pitch frame is not exactly 16 bytes, **When** the TV receives it, **Then** it is silently dropped.
6. **Given** a phone reconnects mid-song, **When** the TV re-sends the singer assignment, **Then** it includes the new sender ID and recomputed remaining end time.

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

- What happens when a pitch frame arrives with a timestamp that regresses more than 200 ms from the previous accepted frame for that player?
- What happens when a phone sends frames with the correct sender ID but the wrong player slot?
- What happens if the join code contains mixed case or formatting characters (hyphens, spaces)?
- What happens if the phone's HTTP server returns an error for `/manifest.json`?
- What happens if a phone assigned as a singer disconnects and reconnects before the song ends?
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
- **FR-010**: The TV MUST assign a unique sender ID (uint16) to each phone upon successful handshake and deliver it in the initial session-state response.
- **FR-011**: On reconnect, the TV MUST assign a **new** sender ID and deliver it in the session-state response for that reconnect.

**Song Catalog Integration**

- **FR-012**: After a successful handshake, the TV MUST fetch `GET /manifest.json` from the phone's HTTP server and call `SongLibrary.addPhone(clientId, entries)` to add the phone's songs. If the fetch fails (HTTP error, timeout, or unreachable), the TV MUST retain the previous catalog for that phone unchanged and show a brief error toast.
- **FR-013**: When a phone disconnects, the TV MUST call `SongLibrary.removePhone(clientId)` to immediately remove all songs for that phone.
- **FR-014**: On re-fetch, the TV calls `addPhone` again; the library replaces (not appends) that phone's prior entries per FR-002 of feature 004.

**Singing Assignment & Pitch Stream**

- **FR-015**: When a song starts and singers are assigned, the TV MUST send each assigned phone an `assignSinger` message containing: `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `thresholdIndex`, `effectiveMicDelayMs`, `expectedPitchFps`, `startMode`, `countdownMs` (if countdown mode), `endTimeTvMs`, and `udpPort`.
- **FR-016**: The TV MUST bind its UDP pitch listener before any phone connects so the port is stable for the entire session.
- **FR-017**: The TV MUST decode incoming 16-byte little-endian pitch frames and route each to the correct player slot.
- **FR-018**: The TV MUST silently drop any pitch frame that: is not exactly 16 bytes; carries an unrecognised sender ID; carries a sender ID that does not match the expected sender for the declared player slot; carries a `songInstanceSeq` that does not match the active song; or has a `tvTimeMs` that regresses more than 200 ms from the prior accepted frame for that player.
- **FR-019**: The TV MUST convert the `midiNote` field to the internal tone scale (`tone = midiNote − 36`); `midiNote = 255` signals unvoiced (no pitch detected).
- **FR-020**: When a singer reconnects mid-song, the TV MUST re-send `assignSinger` with the new sender ID and a recomputed `endTimeTvMs` based on remaining duration.

**Song File Delivery**

- **FR-021**: The TV MUST pass song asset URLs from the catalog directly to the media player; no intermediate file storage is permitted.
- **FR-022**: The TV MUST be configured to allow cleartext HTTP traffic to RFC-1918 local-network addresses.
- **FR-023**: The phone's HTTP song server MUST support HTTP byte-range requests for all audio and video files.
- **FR-024**: The phone's HTTP server MUST serve `/manifest.json` with `Cache-Control: no-cache`, reflecting the state of the most recent scan.

**Clock Sync**

- **FR-025**: The TV MUST initiate clock sync immediately after a phone connects, running 5 exchanges at 100 ms intervals.
- **FR-026**: The TV MUST suspend clock sync while a song is in progress and resume with a single exchange on song end or reconnect.
- **FR-027**: Each clock exchange is TV-initiated and consists of: `ping` (TV → phone, carrying TV send timestamp), `pong` (phone → TV, echoing ping data plus phone receive/send timestamps), `clockAck` (TV → phone, carrying TV receive timestamp). All four timestamps are required for the phone to compute `clockOffsetMs`.

**Validation & Error Handling**

- **FR-028**: The TV MUST ignore unknown message types with a warning; during handshake, an unexpected type is a fatal error.
- **FR-029**: The TV MUST send normative error codes for rejection conditions: `invalid_token`, `protocol_mismatch`, `session_full`, `session_locked`. The `session_locked` check MUST read an `isLocked` flag from the session model owned by feature 006; feature 005 does not own or transition this state.

### Key Entities

- **Session Token / Join Code**: A short, random, human-enterable code generated per session. Gating credential for all phone connections. Displayed on TV, entered on phone. Never reused.
- **Connection**: An active phone-to-TV link. Carries a unique sender ID (uint16), a stable client ID (device UUID), device name, declared HTTP port, and current player slot assignment.
- **Sender ID (connectionId)**: A uint16 assigned by the TV per connection. Embedded in every pitch frame to identify the source phone. Reassigned on reconnect.
- **Song Catalog / Manifest**: A list of `ManifestEntry` objects served by a phone at `GET /manifest.json`. Each entry is validated and ingested into the `SongLibrary` as a `SongEntry` attributed to the phone's client ID.
- **Singer Assignment**: A control message from TV to phone containing all parameters for microphone activation and pitch streaming: player slot, song instance, difficulty, timing parameters, end time, and pitch listener port.
- **Pitch Frame**: A 16-byte little-endian binary datagram carrying one pitch sample: frame counter, estimated TV timestamp, song instance, player slot, MIDI note (255 = unvoiced), and sender ID.
- **Clock Offset**: A value maintained by the phone that maps its monotonic time to estimated TV monotonic time (`tvTimeMs = phoneMonotonicMs + clockOffsetMs`). Established by a 3-message TV-initiated exchange.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A phone with the correct join code can complete the handshake and have its songs visible on the TV within 3 seconds of initiating the connection on a typical home LAN.
- **SC-002**: Join requests with invalid credentials are rejected within 200 ms.
- **SC-003**: After the initial 5-exchange clock sync, the phone's estimated TV timestamp is accurate to within 10 ms under normal LAN conditions — sufficient for beat-accurate scoring.
- **SC-004**: 100% of pitch frames that fail any validation rule (wrong size, stale sender ID, wrong song instance, timestamp regression) are silently dropped with no effect on scoring state.
- **SC-005**: Song catalog changes (connection, disconnection, re-scan) are reflected in the shared library within 1 second of the triggering event.
- **SC-006**: All 13 handshake acceptance tests (T8.3.1–T8.3.13), 5 sender-ID acceptance tests (T8.5.1–T8.5.5), and 8 pitch-stream acceptance tests (T8.6.1–T8.6.8) from the spec pass against fixtures F12v2, F13, F14v2, F15, and F18.

## Dependencies

- **Feature 001 (usdx-parser)**: `ParsedSong` and `SongHeader` — used by `SongValidator` when ingesting manifest entries.
- **Feature 004 (song-library)**: `SongLibrary` (with `addPhone` / `removePhone`), `SongEntry`, `ManifestEntry` — the library is the domain model this feature populates via network events.

## Assumptions

- Fixtures F12v2, F13, F14v2, F15, and F18 already exist in `original_spec/fixtures/`; no new fixtures are created for this feature.
- This feature covers the **TV-side** implementation only. Phone-side HTTP server and pitch-streaming logic are out of scope.
- The session roster is capped at 2 active singer slots (P1 and P2).
- Song file delivery (§8.7) includes TV-side URL handling and platform cleartext config; the phone HTTP server is out of scope.
- Clock sync is TV-initiated only.
- `protocolVersion = 1` is the only supported version for MVP.
