# Feature Specification: Session Pairing

**Feature Branch**: `006-session-pairing`
**Created**: 2026-03-16
**Status**: Draft
**Input**: User description: "006-session-pairing — §7 of the Couchraoke spec: session lifecycle (Open/Locked/Ended), phone join/reconnect identity, roster management, and mid-song disconnect handling."

## Clarifications

### Session 2026-03-16

- Q: What happens when a phone sends `hello` with a `clientId` that matches a currently-connected device? → A: Treat as a reconnect — disconnect the old connection silently and accept the new one. The phone drives reconnect via `hello`; if the TV still holds an open connection it hasn't detected as dropped, reclaiming is the correct path (§7.3).
- Q: What is the default display name for a newly joined phone? → A: The name sent by the phone in its `hello` message (§8.2). The TV uses this as the initial display label; Rename changes it from there.
- Q: Is End Session permitted while a song is in progress? → A: Yes. §7.1 places no restriction on when End Session may be triggered. It ends the session unconditionally, disconnecting all phones and invalidating the token regardless of playback state.
- Q: §7.1 references "Section 7.4" for reconnect support — does §7.4 exist? → A: No. §7.4 does not exist in the original spec. The reference is a typo; reconnect mechanics are defined in §7.3.
- Q: Does manifest re-fetch on reconnect apply during Locked state? → A: Yes. §7.3 explicitly states "During **Locked** state, the TV MUST update its in-memory library index immediately with the fetched manifest." Manifest re-fetch on reconnect is required regardless of session state.
- Q: Terminology — what is the "roster"? → A: `ConnectionRegistry` (owned by 005) = connection routing table. `Session.displayNames` (owned by 006) = display name overlay. "Roster" = the combined UI view merging both (surfaced by features 007 and 010). In domain code, use "registry" for 005's structure and "displayNames" for 006's.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Session Opens and Phones Can Join (Priority: P1)

The host launches the TV app. A new session is created in the **Open** state, advertising a join code and QR code. Guests scan the QR or type the code on their phones. Within a few seconds, each phone's name appears in the session roster and their songs are added to the shared library.

**Why this priority**: Nothing works until the session is Open and phones can be admitted. This is the gateway to all multiplayer interactions.

**Independent Test**: Can be tested by verifying that session creation produces an Open state with a valid token, and that a successful phone join adds the phone to the roster and triggers song catalog ingestion.

**Acceptance Scenarios**:

1. **Given** the TV app launches, **When** a new session is created, **Then** the session state is **Open**, a unique session token is generated, and the join code is displayable.
2. **Given** the session is Open and fewer than 10 phones are connected, **When** a phone sends a valid `hello`, **Then** the phone is added to the roster with a unique `clientId` and a default display name.
3. **Given** 10 phones are already connected, **When** an 11th phone tries to join, **Then** the join is rejected with `session_full` and the session state is unchanged.
4. **Given** the session is Open, **When** a phone joins, **Then** the network layer is notified to fetch that phone's manifest and ingest its songs.

---

### User Story 2 — Session Locks When a Song Starts and Unlocks When It Ends (Priority: P1)

The host picks a song and assigns singers. At the moment the TV sends the first singer assignment, the session transitions to **Locked**. Any phone that tries to join during the song is immediately rejected. When the song ends and the host returns to the Song List, the session goes back to **Open** and new phones can join again.

**Why this priority**: The `isLocked` flag is the seam consumed by feature 005's join-rejection logic. Without it, the protocol layer cannot enforce session locking.

**Independent Test**: Can be tested by verifying the `isLocked` flag is `false` before `assignSinger` and `true` immediately after, and that it returns to `false` when the session returns to Open.

**Acceptance Scenarios**:

1. **Given** the session is Open, **When** the TV dispatches `assignSinger` to the first assigned phone, **Then** `isLocked` becomes `true` and the session state is **Locked**.
2. **Given** the session is Locked, **When** a new phone attempts to join, **Then** the join is rejected with `session_locked`.
3. **Given** the song ends or the host quits to the Song List, **When** the session returns to Open, **Then** `isLocked` becomes `false` and new phones may join.
4. **Given** the session is Locked, **When** the host navigates between TV screens (Song List, Settings, overlays), **Then** the session state does NOT change.
5. **Given** the session is Open, **When** the host navigates between TV screens, **Then** the session state does NOT change.

---

### User Story 3 — Phone Reconnects and Reclaims Its Identity (Priority: P1)

A guest's phone drops off the network briefly due to a Wi-Fi blip. The phone automatically reconnects, sends the same device ID as before, and is recognised by the TV. If the guest was a singer, they resume that role. Their songs come back to the shared library.

**Why this priority**: Without reconnect identity, a brief network drop permanently removes a singer mid-song and breaks gameplay.

**Independent Test**: Can be tested by simulating a disconnect/reconnect with the same `clientId` and verifying the roster entry is updated in place, singer role is preserved, and manifest re-ingestion is triggered.

**Acceptance Scenarios**:

1. **Given** a phone was previously connected and disconnects due to a transport error, **When** it reconnects with the same `clientId`, **Then** the roster entry is updated in place (not added as a new device) and the phone's prior display name is preserved.
2. **Given** a phone reconnects with the same `clientId`, **When** the session processes the reconnect, **Then** a manifest re-fetch is triggered for that phone.
3. **Given** the reconnecting phone was previously assigned as a singer, **When** it reconnects mid-song, **Then** the TV resumes that singer role and re-sends `assignSinger` with updated timing.
4. **Given** the session roster is full and the reconnecting phone's `clientId` is not in the roster, **When** it reconnects, **Then** it is rejected with `session_full`.
5. **Given** a phone left voluntarily (tapped Leave) or was kicked/forgotten, **When** it reconnects with the same `clientId`, **Then** it is treated as a new, unapproved spectator and must re-join normally.

---

### User Story 4 — Roster Management: Rename, Kick, and Forget (Priority: P2)

The host goes to Settings > Connect Phones. They see the list of connected phones. They can rename a phone's display label, kick a disruptive phone (disconnect it immediately), or forget a phone (disconnect and clear its stored label so a future join is treated as a fresh device).

**Why this priority**: Roster management is a host-control feature required for managing multi-device sessions, but gameplay is not blocked if it is unavailable.

**Independent Test**: Can be tested by verifying each action mutates the correct roster entry state and triggers the appropriate downstream effect (disconnect, label clear).

**Acceptance Scenarios**:

1. **Given** a phone is in the roster, **When** the host renames it, **Then** the phone's display label in the roster is updated to the new name for the remainder of the session.
2. **Given** a phone is in the roster, **When** the host kicks it, **Then** the phone is disconnected immediately and its roster entry is removed.
3. **Given** a phone is in the roster, **When** the host forgets it, **Then** the phone is disconnected, its roster entry is removed, and its stored display label is cleared so a future join uses a default name.
4. **Given** the host initiates a Kick or Forget, **When** the action is triggered, **Then** a confirmation dialog is shown with default focus on **Cancel** before the action executes.

---

### User Story 5 — Mid-Song Disconnect Handling (Priority: P2)

A singer's phone drops mid-song. The TV automatically pauses the song and shows a disconnect overlay giving the host three choices: wait for reconnect, continue without the singer, or quit to Song List. If a spectator (non-singer) phone drops, the song continues unaffected, except for a brief toast if that phone was the source of the currently-playing audio stream.

**Why this priority**: Graceful mid-song disconnect keeps the session from silently breaking during gameplay.

**Independent Test**: Can be tested by verifying the session emits a "required singer disconnected" event when a singer phone drops mid-song, and a "spectator disconnected" event otherwise.

**Acceptance Scenarios**:

1. **Given** a song is in progress and an assigned singer phone disconnects, **When** the disconnect is detected, **Then** the session emits a required-singer-disconnected event that the UI layer uses to pause the song and show the overlay.
2. **Given** a song is in progress and a spectator or song-source-only phone disconnects, **When** the disconnect is detected, **Then** no pause or gameplay change is triggered; songs from that phone are removed from the library immediately.
3. **Given** the active song's audio is streaming from a phone that disconnects, **When** the stream breaks, **Then** the session emits a playback-source-lost event; the UI shows an error toast and continues (silent fallback) rather than ending the session.

---

### User Story 6 — Session Ends Explicitly (Priority: P3)

The host goes to Settings and taps **End Session**. The current session is terminated: all connected phones are disconnected, the session token is invalidated, and a fresh session is created. Alternatively, closing the app ends the session.

**Why this priority**: Explicit session end is a management feature; the session naturally resets across app launches.

**Independent Test**: Can be tested by verifying an End Session action transitions the session to **Ended**, disconnects all roster entries, and that a subsequent session creation produces a new, unused token.

**Acceptance Scenarios**:

1. **Given** a session is active, **When** the host triggers End Session, **Then** the session transitions to **Ended**, all phones in the roster are disconnected, and the session token is invalidated.
2. **Given** a session has ended, **When** a new session is created (e.g., on next app launch or after explicit restart), **Then** a fresh token is generated and the roster is empty.
3. **Given** the TV app is closed, **When** the session ends, **Then** the session state transitions to **Ended**.

---

### Edge Cases

- What happens when a phone reconnects with a `clientId` that was forgotten (cleared label)?
- What happens when a phone reconnects while the session is Locked but its `clientId` is in the roster as an assigned singer?
- What happens when the host kicks a phone that was mid-assignment (assignSinger just sent)?
- What happens if the host triggers End Session while a song is in progress?
- What happens when a phone sends `hello` with a `clientId` that matches a currently-connected device (duplicate join)?

## Requirements *(mandatory)*

### Functional Requirements

**Session Lifecycle**

- **FR-001**: On app launch, the session MUST be created in state **Open** with a unique, non-reused session token.
- **FR-002**: The session MUST expose an `isLocked: Boolean` flag; `isLocked` MUST be `true` if and only if the session is in **Locked** state.
- **FR-003**: The session MUST transition to **Locked** at the moment the TV dispatches `assignSinger` to the first assigned phone for a song. This is the single authoritative trigger; no other event may lock the session.
- **FR-004**: The session MUST return to **Open** when the current song ends (normal completion or quit to Song List).
- **FR-005**: The session MUST transition to **Ended** when the host explicitly ends it (Settings > End Session) or when the TV app process terminates. End Session is permitted at any time, including while a song is in progress; it disconnects all phones and invalidates the token unconditionally.
- **FR-006**: TV-side screen navigation (Song List, Settings, overlays) MUST NOT change session state.

**Roster Management**

- **FR-007**: The session MUST maintain a roster of connected phones, each identified by a stable `clientId` (device UUID) and carrying: display name, connection status, current player slot (spectator or P1/P2), and HTTP server port.
- **FR-008**: When a phone joins (valid `hello`), the session MUST add it to the roster or update its existing entry if the `clientId` is already present (reconnect path), including the case where the prior connection is still technically open (TV silently closes the old connection before accepting the new one). The roster MUST NOT exceed 10 entries. The initial display name is the name declared in the `hello` message.
- **FR-009**: The session MUST enforce the 10-device roster cap; a join that would exceed it MUST be rejected with `session_full`.
- **FR-010**: When a phone disconnects, its roster entry MUST be retained with a disconnected status so that a reconnect with the same `clientId` can reclaim the slot within the same session.
- **FR-011**: The Rename action MUST update the display label for the target `clientId` immediately; the updated label persists for the rest of the session.
- **FR-012**: The Kick action MUST close the phone's connection immediately and remove its roster entry. The removed `clientId` will be treated as a new device on any future join.
- **FR-013**: The Forget action MUST close the phone's connection, remove its roster entry, and clear any stored display label for that `clientId`. A future join by the same `clientId` MUST use the default name.
- **FR-014**: Kick and Forget actions MUST require confirmation via a dialog with default focus on **Cancel**.

**Reconnect Identity**

- **FR-015**: When a phone reconnects with a `clientId` present in the roster, the session MUST update the existing roster entry in place (new connection ID, refreshed port) rather than creating a new entry.
- **FR-016**: A transport-disconnect reconnect (not user-initiated, not kicked/forgotten) MUST preserve the phone's display name and player slot assignment. A manifest re-fetch MUST be triggered on reconnect regardless of whether the session is Open or Locked (§7.3).
- **FR-017**: A reconnect by a phone that was previously kicked or forgotten MUST be treated as a new device: a fresh roster entry with default name and spectator slot.
- **FR-018**: If the reconnecting phone was an assigned singer, the session MUST notify the network layer to re-send `assignSinger` with the new connection ID and a recomputed end time based on remaining song duration.
- **FR-019**: If the session roster is full and the reconnecting `clientId` is not in the roster (was kicked/forgotten), the reconnect MUST be rejected with `session_full`.

**Disconnect Events**

- **FR-020**: When an assigned singer phone disconnects mid-song, the session MUST emit a `RequiredSingerDisconnected` event for consumption by the UI layer.
- **FR-021**: When a spectator or song-source-only phone disconnects mid-song, the session MUST emit a `SpectatorDisconnected` event; no gameplay change is triggered by the session layer.
- **FR-022**: If the disconnecting phone is the HTTP source of the active song stream, the session MUST emit a `PlaybackSourceLost` event in addition to the disconnect event.
- **FR-023**: The session MUST expose a `releaseSlot(clientId)` operation that removes the singer assignment for the given phone without changing session state. This is called by the singing-screen UI (feature 009) when the host chooses "continue without them" after a singer disconnects mid-song.

### Key Entities

- **Session**: Top-level aggregate. Carries state (Open/Locked/Ended), session token, and the roster. Exposes `isLocked` as a derived boolean. Transitions state in response to lifecycle commands (song started, song ended, end session).
- **RosterEntry**: Represents one paired phone. Fields: `clientId` (device UUID), `displayName`, `connectionStatus` (connected/disconnected), `playerSlot` (spectator/P1/P2), `httpPort`, `connectionId` (current uint16, changes on reconnect).
- **SessionToken**: A randomly-generated, human-enterable join code associated with the current session. Never reused across sessions.
- **SessionEvent**: Domain event emitted by the session for upstream consumers (UI, network layer). Variants: `RequiredSingerDisconnected`, `SpectatorDisconnected`, `PlaybackSourceLost`, `PhoneJoined`, `PhoneReconnected`, `RosterChanged`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Session state transitions (Open → Locked → Open) are reflected within one event dispatch cycle of the triggering action, with no observable delay in the `isLocked` flag.
- **SC-002**: A reconnecting phone with a matching `clientId` reclaims its roster slot and resumes its singer role within the same time budget as a fresh join (≤ 3 seconds on a typical home LAN).
- **SC-003**: Kick and Forget actions disconnect the target phone and update the roster within one user interaction, with no impact on other connected phones or session state.
- **SC-004**: 100% of new join attempts beyond the 10-device cap are rejected before any roster mutation occurs.
- **SC-005**: Navigation between all TV screens produces zero session state changes, verified across all screen transitions defined in the UI spec.
- **SC-006**: All session lifecycle acceptance scenarios defined in §7.1–§7.3 of the original spec pass against the implemented session domain model.

## Dependencies

- **Feature 005 (network-protocol)**: Consumes `isLocked` from this feature's session model (FR-029 of feature 005). Also relies on `ConnectionRegistry` to initiate kicks and re-send `assignSinger` on reconnect.
- **Feature 004 (song-library)**: `SongLibrary.removePhone(clientId)` is called on disconnect; `addPhone` is called on reconnect manifest re-fetch.
- **Feature 007 (song-list-screen)**: Consumes join widget display data (token, QR payload) and emits the song-started trigger that locks the session.
- **Feature 010 (settings-screen)**: Hosts the Connect Phones roster management UI (rename/kick/forget actions) and the End Session action.

## Assumptions

- Session token generation (random, human-enterable) is already implemented by feature 005; this feature owns the session state machine and roster, not the token generation algorithm.
- The `clientId` is a stable device UUID generated by the companion app and sent in the `hello` message; it is not generated by the TV.
- The roster cap of 10 devices is the MVP limit from §7.2; it is not configurable.
- Singer slot assignment (who is P1/P2) is decided by the song-list/player-selection UI (feature 007), not by this feature. This feature stores the assignment and reapplies it on reconnect.
- Persistent singer assignment across sessions is NOT supported: on a new session, all phones join as spectators.
- The confirm dialog for Kick/Forget is specified here at the domain level; the dialog widget itself is implemented as part of the relevant UI feature.
- This feature does not implement the QR code rendering widget (belongs to features 007 and 010); it exposes the session token string for those features to display.
