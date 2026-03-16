# Tasks: Session Pairing

**Input**: Design documents from `/specs/006-session-pairing/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: TDD mandate in force — all test tasks appear before their corresponding implementation tasks. Tests MUST be written to fail before implementation begins.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared state dependencies)
- **[Story]**: Which user story this task belongs to (US1–US6)

## Path Conventions

All source paths are under `app/src/main/kotlin/com/couchraoke/tv/domain/session/`
All test paths are under `app/src/test/kotlin/com/couchraoke/tv/domain/session/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm no new Gradle dependencies are needed and document files in scope.

- [X] T001 Confirm no new Gradle dependencies needed; verify `kotlinx-coroutines-core` and `kotlinx-coroutines-test` are already declared in `app/build.gradle.kts` (read file, do not modify)
- [X] T002 Define files in scope: `SessionState.kt`, `SessionEvent.kt`, `IConnectionCloser.kt`, `Session.kt` (new); `SessionStateTransitionTest.kt`, `SessionRosterTest.kt`, `SessionEventTest.kt` (new tests); do NOT modify 005-owned files: `ISessionGate.kt`, `SessionToken.kt`, `ConnectionRegistry.kt`, `FakeSessionGate.kt`

---

## Phase 2: Foundational (Shared Types)

**Purpose**: Domain types used by all user stories. MUST complete before any user story implementation begins.

**⚠️ CRITICAL**: All three files are independent and can be written in parallel. No user story work until this phase is complete.

- [X] T003 [P] Create `SessionState.kt` with enum `SessionState { Open, Locked, Ended }` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/SessionState.kt`
- [X] T004 [P] Create `SessionEvent.kt` with sealed class `SessionEvent` containing variants: `RequiredSingerDisconnected(clientId: String, slot: String)`, `SpectatorDisconnected(clientId: String)`, `PlaybackSourceLost(clientId: String)`, `PhoneConnected(clientId: String, deviceName: String)`, `PhoneReconnected(clientId: String, wasSinger: Boolean)`, `PhoneDisconnected(clientId: String)`, `RosterChanged(clientId: String)` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/SessionEvent.kt`
- [X] T005 [P] Create `IConnectionCloser.kt` with `fun interface IConnectionCloser { fun closeConnection(clientId: String) }` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/IConnectionCloser.kt`
- [X] T005a [P] Create `ISessionCallbacks.kt` with interface `ISessionCallbacks { fun onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort); fun onPhoneDisconnected(clientId: String); fun onPhoneReconnected(clientId: String, newConnectionId: UShort) }` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/ISessionCallbacks.kt`
- [X] T005b Update `FakeSessionGate.kt` to also implement `ISessionCallbacks` with no-op overrides, so existing 005 tests remain green after `WebSocketServer` gains the callbacks parameter in `app/src/test/kotlin/com/couchraoke/tv/domain/session/FakeSessionGate.kt`

**Checkpoint**: Foundational types compiled — user story implementation may begin.

---

## Phase 3: User Story 1 — Session Opens and Phones Can Join (Priority: P1) 🎯 MVP

**Goal**: A `Session` can be created in Open state with a valid token; phones can join up to the 10-device cap; joins beyond the cap are rejected; `isLocked` is false.

**Independent Test**: Create a `Session`, call `onPhoneConnected(...)` up to 10 times, verify roster cap enforcement and `isLocked == false`.

### Tests for User Story 1 *(write first — must FAIL before implementation)*

- [X] T006 [US1] Write `SessionStateTransitionTest.kt` with tests: `given new session, then state is Open and isLocked is false`; `given Open session and fewer than 10 phones, when onPhoneConnected called, then PhoneConnected event emitted`; `given 10 phones connected, when 11th onPhoneConnected called, then roster size stays 10 and no PhoneConnected event emitted` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionStateTransitionTest.kt`
- [X] T007 [US1] Write `SessionRosterTest.kt` with tests: `given new session, when onPhoneConnected with clientId and deviceName, then displayName defaults to deviceName`; `given phone connected, when onPhoneConnected called again with same clientId, then roster entry updated not duplicated` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionRosterTest.kt`

### Implementation for User Story 1

- [X] T008 [US1] Create `Session.kt` skeleton: class `Session(private val connectionCloser: IConnectionCloser? = null)` implementing `ISessionGate`; constructor generates token via `SessionToken.generate()`; `state: SessionState = SessionState.Open`; `private val displayNames: MutableMap<String, String>`; `private val assignedSlots: MutableMap<String, String>`; `private var activeSourceClientId: String?`; `private val _events: MutableSharedFlow<SessionEvent>`; `val events: SharedFlow<SessionEvent>`. Implement `isLocked`, `sessionId`, `maxConnections = 10`, `inSong`, `slots` computed properties in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T009 [US1] Implement `Session.onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort)`: add `clientId` to `connectedClientIds`; set `displayNames[clientId]` only if not already present (preserves renames on reconnect); emit `PhoneConnected` event. Do NOT enforce roster cap here — 005's `WebSocketServer` already rejects over-cap joins before calling this method in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T010 [US1] Implement `Session.slots` computed property: builds `SlotMap(P1 = slotInfoFor("P1"), P2 = slotInfoFor("P2"))` where `slotInfoFor(slot)` finds the `clientId` in `assignedSlots` mapped to that slot, looks up the display name from `displayNames`, and sets `connected = connectedClientIds.contains(clientId)`. No `ConnectionRegistry` dependency. in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`

**Checkpoint**: `SessionStateTransitionTest` and `SessionRosterTest` (US1 cases) pass. `./gradlew :app:testDebugUnitTest --tests "*.SessionStateTransitionTest" --tests "*.SessionRosterTest"` should show BUILD SUCCESSFUL.

---

## Phase 4: User Story 2 — Session Locks When a Song Starts and Unlocks When It Ends (Priority: P1)

**Goal**: `lockForSong()` transitions state to Locked and sets `isLocked = true`; `unlockAfterSong()` returns to Open; navigation does not change state.

**Independent Test**: Call `lockForSong(...)`, assert `isLocked == true` and `state == Locked`; call `unlockAfterSong()`, assert `isLocked == false`.

### Tests for User Story 2 *(write first — must FAIL before implementation)*

- [X] T011 [US2] Add to `SessionStateTransitionTest.kt`: `given Open session, when lockForSong called with p1ClientId, then state is Locked and isLocked is true`; `given Locked session, when unlockAfterSong called, then state is Open and isLocked is false`; `given Locked session, when lockForSong called again, then state remains Locked (no-op guard)`; `given Open session, when unlockAfterSong called, then state remains Open (no-op guard)`; `given Open session, then inSong is false`; `given Locked session, then inSong is true` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionStateTransitionTest.kt`

### Implementation for User Story 2

- [X] T012 [US2] Implement `Session.lockForSong(p1ClientId: String, p2ClientId: String? = null, sourceClientId: String? = null)`: guard `if (state != SessionState.Open) return`; set `state = Locked`; populate `assignedSlots`; set `activeSourceClientId` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T013 [US2] Implement `Session.unlockAfterSong()`: guard `if (state != SessionState.Locked) return`; set `state = Open`; clear `assignedSlots`; clear `activeSourceClientId` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`

**Checkpoint**: All US2 test cases in `SessionStateTransitionTest` pass.

---

## Phase 5: User Story 3 — Phone Reconnects and Reclaims Identity (Priority: P1)

**Goal**: A phone that disconnects and reconnects with the same `clientId` reclaims its roster entry and singer role; prior display name is preserved.

**Independent Test**: Simulate `onPhoneConnected` → `onPhoneDisconnected` → `onPhoneReconnected` with same `clientId`; verify `PhoneReconnected` event emitted and `displayNames[clientId]` unchanged.

### Tests for User Story 3 *(write first — must FAIL before implementation)*

- [X] T014 [US3] Add to `SessionRosterTest.kt`: `given phone was connected and disconnected, when onPhoneReconnected with same clientId, then PhoneReconnected event emitted and displayName preserved`; `given singer phone was assigned, when it reconnects, then assignedSlots entry retained`; `given phone was forgotten, when reconnects with same clientId, then treated as new device with fresh displayName` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionRosterTest.kt`

### Implementation for User Story 3

- [X] T015 [US3] Implement `Session.onPhoneDisconnected(clientId: String)`: remove `clientId` from `connectedClientIds`; retain `displayNames` entry (for reconnect); emit appropriate event (logic deferred to T021; for now emit `PhoneDisconnected`) in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T016 [US3] Implement `Session.onPhoneReconnected(clientId: String, newConnectionId: UShort)`: add `clientId` back to `connectedClientIds`; if `clientId` in `displayNames`, emit `PhoneReconnected(clientId, wasSinger = assignedSlots.containsKey(clientId))`; the `wasSinger` flag lets `WebSocketServer` decide whether to re-send `assignSinger`. If `clientId` not in `displayNames` (was forgotten/kicked), call `onPhoneConnected` path instead in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T016a [US3] Update `WebSocketServer.handleConnection` in `app/src/main/kotlin/com/couchraoke/tv/data/network/WebSocketServer.kt`: (1) add `private val callbacks: ISessionCallbacks?` constructor param (default `null`); (2) before the `isLocked`/cap checks, call `registry.getByClientId(hello.clientId)` — if a prior entry exists it is a reconnect: skip both checks, and if the prior connection's WebSocket is still active close it first; (3) after successful `registry.register(...)`, call `callbacks?.onPhoneConnected(...)` for fresh joins or `callbacks?.onPhoneReconnected(...)` for reconnects; (4) in the `finally` block, after `registry.deregister(...)`, call `callbacks?.onPhoneDisconnected(hello.clientId)`

**Checkpoint**: US3 test cases in `SessionRosterTest` pass alongside US1/US2 tests.

---

## Phase 6: User Story 4 — Roster Management: Rename, Kick, and Forget (Priority: P2)

**Goal**: Host can rename a phone's display label; kick disconnects it immediately; forget disconnects and clears its stored label.

**Independent Test**: Call `rename(clientId, "New Name")`, verify `displayNames[clientId] == "New Name"` and `RosterChanged` event emitted. Call `kick(clientId)`, verify `connectionCloser.closeConnection(clientId)` called and entry removed.

### Tests for User Story 4 *(write first — must FAIL before implementation)*

- [X] T017 [US4] Add to `SessionRosterTest.kt`: `given phone in roster, when rename called, then displayNames updated and RosterChanged emitted`; `given phone in roster, when kick called, then connectionCloser invoked with clientId and roster entry removed`; `given phone in roster, when forget called, then connectionCloser invoked and displayName cleared`; `given clientId not in roster, when rename/kick/forget called, then no-op` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionRosterTest.kt`

### Implementation for User Story 4

- [X] T018 [US4] Implement `Session.rename(clientId: String, newName: String)`: guard no-op if `clientId` not in `displayNames`; `displayNames[clientId] = newName`; emit `RosterChanged` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T019 [US4] Implement `Session.kick(clientId: String)`: guard no-op if not in roster; `connectionCloser?.closeConnection(clientId)`; remove from `assignedSlots`; remove roster entry (do NOT remove from `displayNames` — kick lets the phone rejoin with prior name) in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T020 [US4] Implement `Session.forget(clientId: String)`: `kick(clientId)` + `displayNames.remove(clientId)` (clears stored name; future join treated as fresh device) in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T020a [US4] Wire `WebSocketServer` as `IConnectionCloser` into `Session`: update `WebSocketServer` to implement `IConnectionCloser { override fun closeConnection(clientId: String) { ... } }` by looking up the active `DefaultWebSocketServerSession` for that `clientId` (store a `sessions: MutableMap<String, DefaultWebSocketServerSession>` in `WebSocketServer`, populated on connect and cleaned in `finally`) and calling `session.close()`. Inject `webSocketServer` as the `IConnectionCloser` when constructing `Session` in `app/src/main/kotlin/com/couchraoke/tv/data/network/WebSocketServer.kt`

**Checkpoint**: US4 test cases in `SessionRosterTest` pass.

---

## Phase 7: User Story 5 — Mid-Song Disconnect Handling (Priority: P2)

**Goal**: When an assigned singer disconnects mid-song the session emits `RequiredSingerDisconnected`; spectator disconnects emit `SpectatorDisconnected`; disconnecting audio source additionally emits `PlaybackSourceLost`.

**Independent Test**: Lock session with a singer assigned; call `onPhoneDisconnected(singerClientId)`, verify `RequiredSingerDisconnected` event collected; call `onPhoneDisconnected(spectatorClientId)`, verify `SpectatorDisconnected` event.

### Tests for User Story 5 *(write first — must FAIL before implementation)*

- [X] T021 [US5] Create `SessionEventTest.kt`: `given locked session and assigned singer disconnects, when onPhoneDisconnected called, then RequiredSingerDisconnected event emitted with correct slot`; `given locked session and spectator disconnects, when onPhoneDisconnected called, then SpectatorDisconnected emitted`; `given activeSourceClientId set and that phone disconnects, when onPhoneDisconnected called, then PlaybackSourceLost emitted in addition to disconnect event` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionEventTest.kt`

### Implementation for User Story 5

- [X] T022 [US5] Update `Session.onPhoneDisconnected(clientId: String)` (refine T015 implementation): determine event type — if `assignedSlots.containsKey(clientId) && state == Locked` emit `RequiredSingerDisconnected(clientId, slot)`; else emit `SpectatorDisconnected(clientId)`; if `clientId == activeSourceClientId` also emit `PlaybackSourceLost(clientId)`. Also emit `PhoneDisconnected(clientId)` always in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`
- [X] T022a [US5] Add to `SessionEventTest.kt`: `given roster-full session and reconnecting phone's clientId not in displayNames (was forgotten), when reconnect attempted, then session rejects via cap check (no PhoneReconnected event emitted)` — verifies FR-019 in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionEventTest.kt`
- [X] T022b [US5] Write test and implement `Session.releaseSlot(clientId: String)`: add test `given locked session with assigned singer, when releaseSlot called, then clientId removed from assignedSlots and state remains Locked`; implement `releaseSlot` as `assignedSlots.remove(clientId)` with no-op guard if not present in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt` and `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionRosterTest.kt`

**Checkpoint**: All `SessionEventTest` cases pass.

---

## Phase 8: User Story 6 — Session Ends Explicitly (Priority: P3)

**Goal**: `endSession()` transitions to Ended, disconnects all phones, and invalidates the token. Calling it again is a no-op.

**Independent Test**: Connect 2 phones, call `endSession()`, verify state is Ended and `connectionCloser.closeConnection(...)` called for each phone.

### Tests for User Story 6 *(write first — must FAIL before implementation)*

- [X] T023 [US6] Add to `SessionStateTransitionTest.kt`: `given active session with connected phones, when endSession called, then state is Ended and closeConnection invoked for each phone`; `given ended session, when endSession called again, then no-op (idempotent)` in `app/src/test/kotlin/com/couchraoke/tv/domain/session/SessionStateTransitionTest.kt`

### Implementation for User Story 6

- [X] T024 [US6] Implement `Session.endSession()`: guard `if (state == SessionState.Ended) return`; set `state = Ended`; for each clientId in `displayNames` call `connectionCloser?.closeConnection(clientId)`; emit `PhoneDisconnected` for each; clear `displayNames`, `assignedSlots`, `activeSourceClientId` in `app/src/main/kotlin/com/couchraoke/tv/domain/session/Session.kt`

**Checkpoint**: All US6 test cases pass.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T025 Run `./gradlew ciUnitTests` (tests + JaCoCo report + ≥80%/≥60% coverage gates); confirm BUILD SUCCESSFUL with 0 failures; report test counts and coverage %
- [X] T026 Run `./gradlew detekt` and confirm BUILD SUCCESSFUL (no new violations beyond `app/detekt-baseline.xml`)
- [X] T027 Run `./gradlew :app:lintDebug` and confirm BUILD SUCCESSFUL
- [X] T028 Verify permitted changes to 005 files: only `WebSocketServer.kt` and `FakeSessionGate.kt` should be modified; `ISessionGate.kt`, `SessionToken.kt`, `ConnectionRegistry.kt` MUST be unmodified (`git diff --name-only`)
- [X] T029 Run `./gradlew :app:testDebugUnitTest` (full suite) after all changes; confirm no regressions in features 001–005
- [ ] T030 Mark feature branch closed after merge to master by renaming it to `[✓] 006-session-pairing`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user story phases
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 3 (Session.kt must exist)
- **Phase 5 (US3)**: Depends on Phase 3; T016a (WebSocketServer reconnect bypass) depends on T005a/T005b (ISessionCallbacks)
- **Phase 6 (US4)**: Depends on Phase 3; T020a (IConnectionCloser wiring) depends on T005 (IConnectionCloser interface)
- **Phase 7 (US5)**: Depends on Phase 4 (lockForSong must exist to test singer vs spectator distinction)
- **Phase 8 (US6)**: Can begin after Phase 3 (Session.kt exists)
- **Phase 9 (Polish)**: Depends on all desired user stories complete; T025 requires ciUnitTests Gradle task (already configured)

### User Story Dependencies

- **US1 (P1)**: Foundational complete → can start
- **US2 (P1)**: US1 complete (Session.kt skeleton must exist)
- **US3 (P1)**: US1 complete (onPhoneConnected must exist)
- **US4 (P2)**: US1 complete (displayNames must exist)
- **US5 (P2)**: US2 complete (lockForSong + assignedSlots must exist)
- **US6 (P3)**: US1 complete (Session.kt exists)

### Within Each User Story

- Tests MUST be written and fail before implementation (TDD — constitution mandate)
- Core methods before derived state
- Commit after each task or logical group

### Parallel Opportunities

- T003, T004, T005 (Foundational types): all parallel
- T006, T007 (US1 tests): parallel
- T009, T010 (US1 impl after T008): T009 and T010 both extend Session.kt — sequential
- US3, US4, US6 can begin in parallel once US1 is complete (different concerns of Session.kt)
- US5 can begin once US2 is complete

---

## Parallel Example: Phases 5, 6, 8 (after US1 + US2 complete)

```
# After Phase 4 (US2) is complete, these can proceed in parallel:

Developer A: Phase 5 (US3 — reconnect identity)
  T014 → T015 → T016

Developer B: Phase 6 (US4 — roster management)
  T017 → T018 → T019 → T020

Developer C: Phase 8 (US6 — explicit session end)
  T023 → T024
```

---

## Implementation Strategy

### MVP First (P1 stories only: US1 + US2 + US3)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US1 (Session created, phones join)
4. Complete Phase 4: US2 (isLocked state machine)
5. Complete Phase 5: US3 (reconnect identity)
6. **STOP and VALIDATE**: Feature 005's `FakeSessionGate` tests still pass; run full suite
7. `Session` is now a functional `ISessionGate` implementation — 005 can consume it

### Full Delivery (all stories)

After MVP, add Phase 6 (US4), Phase 7 (US5), Phase 8 (US6) in any order.

---

## Notes

- [P] tasks = different files, no shared write dependencies
- Do NOT modify `ISessionGate.kt`, `SessionToken.kt`, or `ConnectionRegistry.kt` — these are owned by feature 005 and must remain unchanged
- `FakeSessionGate.kt` MUST be updated (T005b) to implement `ISessionCallbacks` with no-ops so 005 tests stay green
- `WebSocketServer.kt` MUST be updated (T016a, T020a) to add callbacks and IConnectionCloser — this is the intentional seam between 005 and 006
- `Session` does NOT depend on `ConnectionRegistry` — use internal `connectedClientIds: MutableSet<String>` instead
- `SlotMap` and `SlotInfo` are imported from `com.couchraoke.tv.domain.network.protocol.ProtocolMessages` — do not redefine
- Use `runTest { }` + `StandardTestDispatcher` for any coroutine-based event emission tests (constitution mandate)
- `org.json` is Android-stubbed — use `kotlinx.serialization.json` if JSON is needed in tests (not expected here)
- Mark merged branch closed: `[✓] 006-session-pairing`
