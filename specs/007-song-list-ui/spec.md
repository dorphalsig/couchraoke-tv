# Feature Specification: Song List UI (Landing Screen)

**Feature Branch**: `007-song-list-ui`
**Created**: 2026-03-17
**Status**: Draft
**Input**: User description: "007-song-list-ui — §3.4 + §9.1–9.2 of the Couchraoke spec: the Song List landing screen, including the join widget, song grid, inline search, song preview playback, medley playlist, random actions, and the Select Players modal triggered by song selection."

---

## Clarifications

### Session 2026-03-17

- Q: The §9.2 preview fallback computes `audioLengthSec / 4` when `previewStartSec = 0`, but `SongEntry` has no `audioLengthSec` field. How should this be handled? → A: When `previewStartSec = 0`, start preview from position 0. Skip the fallback computation entirely; no `audioLengthSec` field is needed on `SongEntry`.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Browse the Song Grid and Select a Song to Sing (Priority: P1)

The host opens the TV app. The landing screen shows a grid of song tiles from all connected phones. The host uses the TV remote to navigate to a song and presses OK. The Select Players modal appears so they can assign singers before starting.

**Why this priority**: The song grid and song selection are the core purpose of this screen. Nothing else in the feature is useful without this working.

**Independent Test**: Can be fully tested by populating `SongLibrary` with songs from two phones, verifying all songs appear sorted with correct tile content, pressing OK on a tile, and confirming Select Players opens with the correct song subtitle.

**Acceptance Scenarios**:

1. **Given** the TV app launches, **When** the Song List screen is shown, **Then** it is always the landing screen — even if no phones are connected and the library is empty.
2. **Given** songs from multiple phones are in the library, **When** the grid is shown, **Then** songs are sorted by Artist → Album → Title and each tile shows the cover image (or placeholder), title, artist, and applicable tag chips (`D` / `R` / `V` / `I` / `M`).
3. **Given** a song tile is focused, **When** the host presses OK, **Then** the Select Players modal opens with subtitle `<Artist> — <Title>`.
4. **Given** Select Players is open for a non-duet song, **When** viewed, **Then** Player 1 (required) is shown, the Player 2 phone selector is visible but disabled, and the Player 2 difficulty selector is hidden.
5. **Given** Select Players is open for a duet song, **When** viewed, **Then** both Player 1 (P1) and Player 2 (P2) selectors are shown; Player 2 is optional; a "Swap Parts" button appears if both players are assigned; a solo duet-part selector (P1 / P2) appears if only one player is assigned.
6. **Given** Select Players opens and no phones are connected, **When** viewed, **Then** a blocking message "No phones connected" is shown with an action to open Settings > Connect Phones.
7. **Given** the host presses Start in Select Players and the song's audio is unreachable, **When** playback fails, **Then** the app returns to Song List and shows a blocking error modal: title "ERROR"; body line 1: "This song can't be played."; body line 2: "Check Settings > Song Library — the song's phone may be disconnected."
8. **Given** the Song List is shown on app launch or return from Singing/Results, **When** the grid is non-empty, **Then** initial focus is on the first tile (top-left); if the grid is empty, initial focus is on the Search field.

---

### User Story 2 — Search and Filter Songs (Priority: P1)

The library has hundreds of songs. The host types in the Search field to narrow the grid to matching songs instantly.

**Why this priority**: Without filtering, large libraries are unusable. This is a required usability gate for real-world sessions.

**Independent Test**: Can be fully tested by loading 20+ songs, typing a search term, verifying only matching songs remain visible in sorted order, and verifying Back clears the filter without exiting.

**Acceptance Scenarios**:

1. **Given** the Search field contains text, **When** the visible grid is checked, **Then** only songs whose artist, album, or title contains the search text (case-insensitive substring) are shown; Artist → Album → Title sort order is preserved.
2. **Given** the host types in the Search field, **When** the text changes, **Then** the grid updates within 300 ms (150 ms debounce + render).
3. **Given** the host presses OK on the Search field, **When** the system text input dialog opens and is confirmed, **Then** focus returns to the Search field and the filter applies immediately.
4. **Given** a filter is active, **When** the host presses Back, **Then** the filter is cleared and the host remains on the Song List (app does not exit).
5. **Given** no filter is active, **When** the host presses Back, **Then** the app exits (returns to Android launcher).
6. **Given** the filtered set is empty, **When** the Random Song and Random Duet buttons are checked, **Then** both are disabled and non-focusable.

---

### User Story 3 — Join Widget Always Visible for Phone Onboarding (Priority: P1)

During a session, friends may want to connect their phones at any time. The QR code and join code are visible in the left panel throughout — no navigation required to invite a new phone.

**Why this priority**: Phones must be able to join during an active session. Requiring navigation to show the QR code would break the party flow.

**Independent Test**: Can be fully tested by rendering the Song List screen with an active session and verifying the QR code and join code are present, non-focusable, and correctly sized.

**Acceptance Scenarios**:

1. **Given** a session is active, **When** the Song List is displayed, **Then** the left panel shows a QR code and join code at all times.
2. **Given** the QR code is rendered at 1080p, **When** measured, **Then** it is at least 16% of screen height and not less than 280 px (square), has a quiet zone of at least 4 modules on each side, and the join code character height is at least 3.5% of screen height.
3. **Given** the QR is scanned by a phone, **When** the payload is decoded, **Then** it contains the full WebSocket endpoint URL including the `token` query parameter (matching `SessionToken`) — not a service-discovery identifier.
4. **Given** the TV remote is used to navigate, **When** navigating across the screen, **Then** the QR/join code widget cannot receive focus.
5. **Given** the header is visible, **When** checked, **Then** a text-only join code (e.g. `Code: ABCD-EFGH`) is shown in the top-right of the header as a quick-glance alternative.
6. **Given** the Song List is shown, **When** checked, **Then** the connected-device roster is NOT displayed (device management is in Settings > Connect Phones only).

---

### User Story 4 — Preview Song Audio While Browsing (Priority: P2)

The host pauses on a song tile and hears a clip of the song after a short delay. This lets them identify tracks quickly without selecting each one.

**Why this priority**: Audio preview significantly improves the browsing experience for large libraries but is not required for the app to function.

**Independent Test**: Can be fully tested by focusing a tile for 600+ ms and verifying audio starts at `previewStartSec`, then moving focus away and verifying audio stops immediately.

**Acceptance Scenarios**:

1. **Given** a song tile is focused and focus holds for 500 ms, **When** audio preview begins, **Then** it starts at the song's `previewStartSec` (from `SongEntry`). If `previewStartSec` is 0, preview starts from position 0.
2. **Given** preview is playing, **When** focus moves to a different tile, leaves the grid, a modal opens, Settings opens, singing starts, or the screen loses focus, **Then** preview stops immediately.
3. **Given** a song's audio URL is null or the HTTP request fails, **When** preview is attempted, **Then** it is suppressed silently with no error shown to the host.
4. **Given** the Android system volume is set to 0, **When** a tile is focused for 500+ ms, **Then** no audio plays (controlled by system volume, not by this feature).

---

### User Story 5 — Build and Play a Medley (Priority: P2)

The host wants to queue up several songs and play them back-to-back without returning to the Song List between each one.

**Why this priority**: Medley mode is a differentiating feature but not a launch blocker.

**Independent Test**: Can be fully tested by long-pressing eligible songs to build a playlist, pressing Play Medley, and verifying Select Players opens with subtitle `Medley — <n> songs` and no Player 2 section.

**Acceptance Scenarios**:

1. **Given** a song tile with `canMedley=true` is long-pressed (≥ 500 ms), **When** the action completes, **Then** the song is appended to the end of the Medley playlist.
2. **Given** a song tile with `canMedley=false` is long-pressed, **When** the action completes, **Then** a blocking modal shows with exact text: "This song can't be used in a medley. Look for songs with an M tag in the lower right corner" and a single OK button.
3. **Given** the Medley playlist is non-empty, **When** the host presses Play Medley, **Then** Select Players opens with subtitle `Medley — <n> songs` (where `n` is the current playlist count) with no Player 2 section.
4. **Given** the Medley playlist is empty, **When** Play Medley is checked, **Then** it is disabled and non-focusable.
5. **Given** the host navigates away to Settings, starts a song, starts a medley, or arrives at Results, **When** Song List is next shown, **Then** the Medley playlist is empty.
6. **Given** a modal overlay (Select Players, error dialog) is opened and closed, **When** Song List is back in view, **Then** the Medley playlist is unchanged.
7. **Given** the playlist exceeds the visible area (max 7 lines or 25% screen height, min 3 lines always visible), **When** viewed, **Then** the list scrolls.

---

### User Story 6 — Pick a Random Song (Priority: P3)

The host can't decide. They press a button and a random song from the currently visible filtered set is chosen for them.

**Why this priority**: A convenience feature for spontaneous play; not essential to the core experience.

**Independent Test**: Can be fully tested by pressing Random Song with a known filtered set and verifying Select Players opens for one of the eligible songs.

**Acceptance Scenarios**:

1. **Given** the host presses "Sing Random Song", **When** the filtered set is non-empty, **Then** a random valid song from the visible set is selected and Select Players opens.
2. **Given** the host presses "Sing Random Duet", **When** the filtered set contains at least one duet song (`isDuet=true`), **Then** a random duet song is selected and Select Players opens.
3. **Given** no eligible songs exist for the action (e.g., no duets in the filtered set), **When** the button is pressed, **Then** a blocking modal with a single OK is shown.

---

### Edge Cases

- What happens when a phone disconnects mid-session? → Its songs are removed from `SongLibrary` immediately; the grid re-renders. A `SessionEvent.SpectatorDisconnected` or `RequiredSingerDisconnected` is emitted, but no action is required from Song List other than removing the songs.
- What happens if a song in the Medley playlist belonged to a phone that disconnects? → The playlist entry remains; the error surfaces at playback time via the song start failure flow.
- What happens in Reorder mode if focus moves out of the playlist via a system event? → Reorder is implicitly cancelled and the original order is restored.
- What happens if Random Song/Duet is pressed but the eligible set produces no valid entry? → A blocking modal with OK is shown; focus is unchanged.
- What happens when navigating DPAD Left from the leftmost grid tile? → Focus moves to the Medley playlist.
- What happens if the session ends (Ended state) while the Song List is visible? → Out of scope for this feature; session lifecycle is owned by feature 006.

---

## Requirements *(mandatory)*

### Functional Requirements

**Screen & Layout**

- **FR-001**: The Song List MUST always be the landing screen on app launch, on return from the Singing screen, and on return from the Results screen.
- **FR-002**: The screen MUST use a two-column layout: left panel contains the join widget and Medley playlist; right panel contains the Search field, action buttons, and song grid.
- **FR-003**: The Settings button (gear icon) MUST be rendered in the **left side of the header**, left-aligned alongside a `◀ Back` visual indicator (per §3.4 wireframe: `⚙ Settings  ◀ Back` on the left, `Code: XXXX-XXXX` on the right). For this feature, render the icon as a **non-functional stub** — navigation to Settings is implemented by the Settings feature.
- **FR-004**: The current session join code MUST be shown as text in the header (top-right) at all times in the format `Code: XXXX-XXXX`, read from the active `SessionToken`.

**Song Grid**

- **FR-005**: The song grid MUST display all valid `SongEntry` records from `SongLibrary`, sorted by Artist → Album → Title.
- **FR-006**: Each song tile MUST show: cover image (placeholder if `coverUrl` is null), title, artist, and tag chips in the lower-right — `D` (`isDuet`), `R` (`hasRap`), `V` (`hasVideo`), `I` (`hasInstrumental`), `M` (`canMedley`).
- **FR-007**: Grid column count MUST be fixed at 3 columns at 1080p and 4 columns at 4K. It MUST NOT change while the screen is displayed.
- **FR-008**: On entering the Song List, initial focus MUST be placed on the first tile in the grid (top-left). If the grid is empty, initial focus MUST be placed on the Search field.
- **FR-009**: DPAD navigation MUST follow the normative map:

  | Current focus | DPAD Up | DPAD Down | DPAD Left | DPAD Right |
  |---|---|---|---|---|
  | Search field | — (no action) | First grid tile | — | — |
  | Random Song / Random Duet button | Search field | First grid tile | — | — |
  | Grid tile (top row) | Search field | Tile below (or no action if last row) | Tile to the left, or Medley playlist if at leftmost column | Tile to the right; no action if at rightmost column |
  | Grid tile (non-top row) | Tile above | Tile below (or no action if last row) | Tile to the left, or Medley playlist if at leftmost column | Tile to the right; no action if at rightmost column |
  | Medley playlist row | Previous row (or Play Medley if at top) | Next row (or no action if at bottom) | — | Search field |
  | Play Medley button | Last playlist row (or no action if empty) | — (no action) | — | Search field |

**Empty States**

- **FR-010**: When no phones are connected, the grid area MUST display: message "No phones connected." and hint "Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code."
- **FR-011**: When phones are connected but `SongLibrary` contains no valid songs, the grid area MUST display: message "No songs found." and hint "Open the karaoke app on your phone and make sure the songs folder is set."

**Search & Filter**

- **FR-012**: The screen MUST provide a Search text field. Filtering MUST be case-insensitive substring match across artist, album, and title fields of each `SongEntry`.
- **FR-013**: The filter MUST be debounced by 150 ms and MUST preserve Artist → Album → Title sort order.
- **FR-014**: Pressing OK on the Search field MUST open the system text input dialog; on confirming, focus returns to the Search field and the filter applies immediately.
- **FR-015**: If a filter is active, pressing Back MUST clear the filter and keep the host on the Song List. If no filter is active, pressing Back MUST exit the app.

**Song Selection & Select Players Modal**

- **FR-016**: Pressing OK on a focused song tile MUST open the Select Players modal with subtitle `<Artist> — <Title>`.
- **FR-017**: In Select Players for a non-duet song: Player 1 (required) MUST be shown; the Player 2 phone selector MUST be visible but disabled; the Player 2 difficulty selector MUST be hidden.
- **FR-018**: In Select Players for a duet song: both Player 1 and Player 2 selectors MUST be shown; Player 2 is optional; if both are assigned, a "Swap Parts" button MUST appear; if only one is assigned, a duet-part selector (P1 / P2) MUST appear.
- **FR-019**: The phone dropdown in Select Players MUST be populated from the connected `RosterEntry` records (those with `connectionStatus = connected`), showing each phone's `displayName`.
- **FR-020**: If no phones are connected when Select Players opens, the modal MUST display a blocking state matching §9.3 exactly: ⚠ `"No phones connected."` with secondary text `"Connect phones in Settings to sing."`, a `[Open Settings > Connect Phones]` button (**Not Implemented** in this feature — stub with no navigation), and a `[Cancel]` button.
- **FR-021**: If song start fails after the host presses Start (audio URL unreachable, or phone disconnected between modal and playback), the app MUST return to Song List and show a blocking error modal: title "ERROR"; body line 1 (exact): "This song can't be played."; body line 2: "Check Settings > Song Library — the song's phone may be disconnected."
- **FR-022**: Pressing Cancel or Back in Select Players MUST close the modal and return focus to the Song List without changing session state.

**Random Actions**

- **FR-023**: The screen MUST provide a "Sing Random Song" button that selects a random valid `SongEntry` from the currently visible filtered set and opens Select Players.
- **FR-024**: The screen MUST provide a "Sing Random Duet" button that selects a random valid `SongEntry` with `isDuet=true` from the currently visible filtered set and opens Select Players.
- **FR-025**: If the relevant eligible set is empty, the random action MUST show a blocking modal with a single OK button. If the filtered grid result set is empty, both random buttons MUST be disabled and non-focusable.

**Song Preview**

- **FR-026**: When a song tile holds focus for 500 ms, audio preview MUST begin at `SongEntry.previewStartSec`. If `previewStartSec` is 0, preview MUST start from position 0. *(Intentional delta from §9.2: `SongEntry` has no `audioLengthSec` field; the `audioLengthSec/4` fallback in §9.2 is not implemented. `previewStartSec=0` → seek to 0 is the accepted behaviour.)*
- **FR-027**: Preview MUST stop immediately when: focus moves away from the current tile, focus leaves the song grid, any modal opens, Settings opens, singing starts, or the screen loses focus.
- **FR-028**: If `SongEntry.audioUrl` is null or the HTTP request fails, preview MUST be suppressed silently — no error is shown to the host.
- **FR-029**: Preview audio volume is controlled exclusively by the Android system volume (TV remote volume buttons). No app-level preview volume control is implemented in this feature (Settings > Audio > Preview Volume, §9.4.3, is out of scope). Do NOT implement any volume guard in the preview trigger logic.

**Join Widget**

- **FR-030**: The left panel MUST show a join widget (QR code + join code text) at all times while the Song List is displayed.
- **FR-031**: The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter (the value of `SessionToken`). It MUST NOT encode a service-discovery identifier.
- **FR-032**: The QR code MUST be at least 16% of screen height (square) and not less than 280 px at 1080p; quiet zone MUST be at least 4 modules on each side; join code character height MUST be at least 3.5% of screen height.
- **FR-033**: The join widget MUST be display-only and non-focusable; TV remote navigation MUST NOT be able to place focus on it.

**Medley Playlist**

- **FR-034**: Long-pressing OK (≥ 500 ms) on a song tile with `canMedley=true` MUST append that `SongEntry` to the end of the Medley playlist.
- **FR-035**: Long-pressing OK on a song tile with `canMedley=false` MUST show a blocking modal with exact text: "This song can't be used in a medley. Look for songs with an M tag in the lower right corner" and a single OK button.
- **FR-036**: The Medley playlist area MUST be a fixed-height scrollable list: height = lesser of 7 lines or 25% of screen height, with a minimum of 3 lines always visible. Each row shows `<Artist>  <Title>` with no row number prefix.
- **FR-037**: Pressing OK on a playlist row MUST enter Reorder mode: Up/Down moves the item in the list; OK confirms the new position; Back cancels and restores the prior order. DPAD Left and Right MUST do nothing while in Reorder mode. Moving focus out of the playlist by any other mechanism MUST implicitly cancel Reorder and restore the original order. While in Reorder mode, the bottom-of-screen context hint bar MUST display: `Up/Down=Move  OK=Accept  Back=Cancel` (per §3.4).
- **FR-038**: Long-pressing OK on a playlist row MUST immediately delete that row (no confirmation dialog).
- **FR-039**: The Play Medley button MUST be disabled and non-focusable when the playlist is empty. When non-empty, pressing it MUST open Select Players with subtitle `Medley — <n> songs` and no Player 2 section.
- **FR-040**: *(Out of scope for this feature — Auto Medley (Random 5) is deferred. The Auto Medley button MUST NOT be rendered. See §3.4 for the full deferred spec.)*
- **FR-041**: The Medley playlist MUST be cleared (reset to empty) when the host navigates to Settings, starts a song, starts a medley, or the Results screen is shown. It MUST NOT be cleared when modal overlays (Select Players, error dialogs) are opened or closed.

**Navigation & Input Constraints**

- **FR-042**: Navigating between Song List, Settings, and any overlay MUST NOT change the session state (Open/Locked/Ended).
- **FR-043**: A long-press OK is defined as pressing and holding OK/Enter for ≥ 500 ms. When no long-press action is defined for the focused element, long-press MUST behave the same as a normal OK press.

---

### Key Entities

- **SongLibrary** *(owned by feature 004)*: In-memory aggregate index of all songs from connected phones. This feature reads the sorted list of valid `SongEntry` records and subscribes to changes. Read-only from this feature's perspective.
- **SongEntry** *(owned by feature 004)*: One song record from `SongLibrary`. Key fields consumed by this feature: `title`, `artist`, `album`, `coverUrl`, `audioUrl`, `previewStartSec`, `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`. Not modified by this feature.
- **Session** *(owned by feature 006)*: Active session aggregate. This feature reads `state` (Open/Locked/Ended) for context and observes `SessionEvent` to keep the UI current. Read-only from this feature's perspective.
- **SessionToken** *(owned by feature 006)*: The human-enterable join code and the WebSocket URL token. This feature reads these values to render the QR code and join code text.
- **RosterEntry** *(owned by feature 006)*: Represents one paired phone. This feature reads `displayName` and `connectionStatus` to populate the phone dropdowns in Select Players.
- **MedleyPlaylist**: Transient, in-memory ordered list of `SongEntry` references. Owned and managed by this feature for the lifetime of the Song List screen visit. Cleared on non-modal navigation away.
- **PlayerAssignment**: The host's selections in the Select Players modal — Player 1 `RosterEntry` + difficulty (required), Player 2 `RosterEntry` + difficulty (optional), and duet-part choice when applicable. Produced by this feature and consumed by the Singing screen.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Songs from a newly connected phone appear in the grid within 2 seconds of the phone's join being confirmed by the session.
- **SC-002**: The grid updates to reflect a typed search term within 300 ms of the last keystroke.
- **SC-003**: On every entry to the Song List screen, the correct initial focus element is active before the host presses any button.
- **SC-004**: Pressing OK on a song tile opens the Select Players modal in a single action — no intermediate screens or confirmations.
- **SC-005**: Song preview audio begins within 600 ms of a tile receiving and holding focus (measured from the end of the 500 ms focus-hold window).
- **SC-006**: The QR code is successfully scanned by a standard smartphone camera from 3 metres in front of a 55″ TV running at 1080p.
- **SC-007**: 100% of mandatory acceptance tests T3.4.1–T3.4.6 (§3.4) pass.

---

## Assumptions

- `SongLibrary` (feature 004) is already populated in memory. This feature only reads from it and does not trigger manifest fetches.
- Session state, `SessionToken`, and `RosterEntry` list (feature 006) are already live when this screen renders. This feature reads them but does not modify them.
- Preview audio volume is controlled by Android system volume only. No app-level Preview Volume setting is read or stored by this feature.
- The Select Players modal is fully in scope for this feature. The actual Singing screen handoff (§9.5) is out of scope and is handled by a subsequent feature.
- `canMedley` and `previewStartSec` are pre-computed at library-build time (feature 004) and are read-only by this feature.
- Advanced Search (referenced in Back-key behavior) is POST-MVP per the project constitution and is explicitly excluded from this feature.

## Scope

**In scope:**
- Full Song List landing screen (§3.4): grid, search, join widget, medley playlist, random actions, and the complete DPAD navigation map
- Song preview playback (§9.2)
- Global navigation behaviour as it applies to the Song List (§9.1 — Back key, long-press OK definition)
- Select Players modal (§9.3) — opening, field rendering, gating rules, song start failure flow

**Out of scope:**
- Singing screen handoff and playback (§9.5)
- Settings screen and sub-screens (§9.4)
- Results screen (§9.6)
- Device roster management: Rename / Kick / Forget (§10.4.1 — Settings > Connect Phones only)
- Advanced Search overlay — POST-MVP per constitution
- Auto Medley (Random 5) — deferred feature; the Auto Medley button is not rendered in this feature
- Medley playback engine and per-segment transitions — Singing screen feature
- Session state changes (Open ↔ Locked ↔ Ended) — owned by feature 006
