# Feature Specification: Song List UI (Landing Screen)

**Feature Branch**: `007-song-list-ui`
**Created**: 2026-03-17
**Status**: Draft
**Input**: User description: "007-song-list-ui — §3.4 + §9.1–9.2 of the Couchraoke spec: the Song List landing screen, including the join widget, song grid, inline search, song preview playback, medley playlist, random actions, and the Select Players modal triggered by song selection."

---

## Clarifications

### Session 2026-03-17

- Q: The §9.2 preview fallback computes `audioLengthSec / 4` when `previewStartSec = 0`, but `SongEntry` has no `audioLengthSec` field. How should this be handled? → A: When `previewStartSec = 0`, start preview from position 0. Skip the fallback computation entirely; no `audioLengthSec` field is needed on `SongEntry`.

### Session 2026-03-30

- Q: How should dynamic session-bound content (join code, QR bitmap) be handled in screenshot tests to produce stable baselines? → A: Mock `SessionToken` to a fixed, known value in all screenshot test fixtures so that QR code and join code render deterministically in every baseline capture.
- Q: Does the Song List screen have a distinct loading/initialising state requiring a VR-001 screenshot baseline? → A: No. The screen renders immediately into either the empty or populated state; `SongLibrary` and `Session` are always live before the screen is shown. No loading state exists.

### Session 2026-03-31

- Q: The original spec wireframe includes a contextual hints bar at the bottom of the screen. Is this in scope? → A: Yes. Added as FR-044. The hints bar changes based on the currently focused element (song tile vs playlist row vs reorder mode).

### Session 2026-03-31 (spec revision — §3.4 wireframe update)

The original spec (§3.4) was updated with wireframe and behavioral changes. The following deltas were identified and reconciled:

- **Join Widget → Join Button + Modal**: The QR code and join code are no longer always visible in the left panel. Instead, a focusable `[ JOIN ]` button in the header opens a pairing overlay/dialog that displays the QR code + join code. The left panel now contains the preview pane instead. Updated FR-002, FR-003, FR-030–FR-033. New FR-045–FR-049 added.
- **Preview Pane added**: A 16:9 display-only, non-focusable preview pane is now required in the left panel above the Medley playlist, showing the focused song's cover (centered with blurred background fill), title, artist, and tag chips. When no song tile is focused, the pane retains the last focused song's info (sticky). New FR-045–FR-049 added.
- **Header layout restructured**: Header now contains (left-to-right): join code text, Search field, `[ JOIN ]` button, `[ ⚙ SETTINGS ]` button. Updated FR-003, FR-004.
- **Random Medley button in scope**: `Sing Random Medley` is no longer deferred. It selects 5 random medley-eligible songs (all if < 5), requiring ≥ 2 eligible songs to be active. FR-040 updated.
- **DPAD navigation map expanded**: Join button and Settings button added as focusable header elements. Search left → Join, Search right → Settings. Left-panel entry target priority updated with Auto Medley fallback. FR-009 updated.
- **Back key cascade updated**: Back from grid or left panel now moves focus to the Search field before clearing filters or exiting. FR-015 updated.
- **Disabled button styling**: Random action buttons that are disabled should be greyed out. FR-025 updated.
- Q: What should the preview pane show when no song tile is focused? → A: Show the last focused song's info (sticky behavior). Before any song has been focused, show a placeholder.
- Q: How should square cover art display in the 16:9 preview pane? → A: Cover image centered with blurred/dimmed background fill (standard media app pattern).
- Q: Should Search move into the header bar per the updated wireframe? → A: Yes. Search field is in the header row alongside Join code, Join button, and Settings button.

### Session 2026-03-31 (clarify pass)

- Q: Can the same song be added to the Medley playlist more than once? → A: No duplicates allowed. When a song already in the playlist is long-pressed again on the same focused tile, the first duplicate attempt is silently ignored. If the user continues long-pressing the same focused tile, a brief feedback message is shown (e.g., "Already in medley"). Tracking is per focused tile, not by song name/identity.
- Q: Should SC-007 reference the expanded test set (T3.4.1–T3.4.12) from the updated §3.4.9? → A: Yes. Updated SC-007 to reference T3.4.1–T3.4.12.
- Q: What should the preview pane placeholder state look like before any song has been focused? → A: App logo/icon centered on a dimmed background (branding placeholder).

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
4. **Given** focus is in the grid or left panel, **When** the host presses Back, **Then** focus moves to the Search field (no filter change, no app exit).
5. **Given** focus is on the Search field or header controls and a filter is active, **When** the host presses Back, **Then** the filter is cleared and the host remains on the Song List (app does not exit).
6. **Given** focus is on the Search field or header controls and no filter is active, **When** the host presses Back, **Then** the app exits (returns to Android launcher).
7. **Given** the filtered set is empty, **When** the Random Song, Random Duet, and Random Medley buttons are checked, **Then** all are disabled, non-focusable, and greyed out.

---

### User Story 3 — Join Button for Phone Onboarding (Priority: P1)

During a session, friends may want to connect their phones at any time. A Join button in the header opens a pairing overlay with the QR code and join code. The join code text is also always visible in the header as a quick-glance alternative.

**Why this priority**: Phones must be able to join during an active session. The Join button must be prominently placed and always accessible from the Song List.

**Independent Test**: Can be fully tested by pressing the Join button and verifying the pairing overlay opens with a correctly sized QR code and join code, and that the overlay dismisses on Back.

**Acceptance Scenarios**:

1. **Given** a session is active, **When** the host presses the Join button, **Then** a pairing overlay opens showing the QR code and join code.
2. **Given** the QR code is rendered in the pairing overlay at 1080p, **When** measured, **Then** it is at least 320 dp × 320 dp (square), has a quiet zone of at least 4 modules on each side, uses high contrast (dark on light), is centered within the overlay, and the join code text below it has a minimum text size of 24 sp.
3. **Given** the QR is scanned by a phone, **When** the payload is decoded, **Then** it contains the full WebSocket endpoint URL including the `token` query parameter (matching `SessionToken`) — not a service-discovery identifier.
4. **Given** the pairing overlay is open, **When** the host presses Back, **Then** the overlay closes and focus returns to the Song List.
5. **Given** the header is visible, **When** checked, **Then** a text-only join code (e.g. `Code: ABCD-EFGH`) is shown in the left side of the header as a quick-glance alternative.
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

### User Story 6 — Pick a Random Song or Medley (Priority: P3)

The host can't decide. They press a button and a random song from the currently visible filtered set is chosen for them. For medley mode, a Random Medley button auto-selects up to 5 medley-eligible songs.

**Why this priority**: A convenience feature for spontaneous play; not essential to the core experience.

**Independent Test**: Can be fully tested by pressing Random Song with a known filtered set and verifying Select Players opens for one of the eligible songs. Random Medley can be tested with a set of medley-eligible songs.

**Acceptance Scenarios**:

1. **Given** the host presses "Sing Random Song", **When** the filtered set is non-empty, **Then** a random valid song from the visible set is selected and Select Players opens.
2. **Given** the host presses "Sing Random Duet", **When** the filtered set contains at least one duet song (`isDuet=true`), **Then** a random duet song is selected and Select Players opens.
3. **Given** the host presses "Sing Random Medley", **When** the filtered set contains at least 2 medley-eligible songs (`canMedley=true`), **Then** up to 5 random medley-eligible songs are selected and Select Players opens with subtitle `Medley — <n> songs` and no Player 2 section. If fewer than 5 eligible songs exist, all are selected.
4. **Given** no eligible songs exist for the action (e.g., no duets in the filtered set, fewer than 2 medley songs), **When** the button is checked, **Then** it is disabled, non-focusable, and greyed out.
5. **Given** an eligible set exists but pressing the random action produces an unexpected empty result, **When** the button is pressed, **Then** a blocking modal with a single OK is shown.

---

### Edge Cases

- What happens when a phone disconnects mid-session? → Its songs are removed from `SongLibrary` immediately; the grid re-renders. A `SessionEvent.SpectatorDisconnected` or `RequiredSingerDisconnected` is emitted, but no action is required from Song List other than removing the songs.
- What happens if a song in the Medley playlist belonged to a phone that disconnects? → The playlist entry remains; the error surfaces at playback time via the song start failure flow.
- What happens in Reorder mode if focus moves out of the playlist via a system event? → Reorder is implicitly cancelled and the original order is restored.
- What happens if Random Song/Duet/Medley is pressed but the eligible set produces no valid entry? → The button should already be disabled and greyed out if no eligible songs exist. If somehow pressed, a blocking modal with OK is shown; focus is unchanged.
- What happens when navigating DPAD Left from the leftmost grid tile? → Focus enters the left panel per the entry target priority: (1) first Medley playlist row, if present; (2) Play Medley button, if the playlist is empty.
- What happens if the session ends (Ended state) while the Song List is visible? → Out of scope for this feature; session lifecycle is owned by feature 006.

---

## Requirements *(mandatory)*

### Functional Requirements

**Screen & Layout**

- **FR-001**: The Song List MUST always be the landing screen on app launch, on return from the Singing screen, and on return from the Results screen.
- **FR-002**: The screen MUST use a two-column layout: left panel contains the preview pane and Medley playlist (with Play Medley action); right panel contains the song grid. The header row spans the full width above both panels and contains (left-to-right): join code text, Search field, `[ JOIN ]` button, `[ ⚙ SETTINGS ]` button. Below the header, a secondary row contains the Random Song / Random Duet / Random Medley action buttons.
- **FR-003**: The Settings button (gear icon) MUST be rendered on the **right side of the header**. For this feature, render the icon as a **non-functional stub** — navigation to Settings is implemented by the Settings feature.
- **FR-004**: The current session join code MUST be shown as text in the header (left side) at all times in the format `Code: XXXX-XXXX`, read from the active `SessionToken`.

**Song Grid**

- **FR-005**: The song grid MUST display all valid `SongEntry` records from `SongLibrary`, sorted by Artist → Album → Title.
- **FR-006**: Each song tile MUST show: cover image (placeholder if `coverUrl` is null), title, artist, and tag chips in the lower-right — `D` (`isDuet`), `R` (`hasRap`), `V` (`hasVideo`), `I` (`hasInstrumental`), `M` (`canMedley`).
- **FR-007**: Grid column count MUST be fixed at 3 columns at 1080p and 4 columns at 4K. It MUST NOT change while the screen is displayed.
- **FR-008**: On entering the Song List, initial focus MUST be placed on the first tile in the grid (top-left). If the grid is empty, initial focus MUST be placed on the Search field.
- **FR-009**: DPAD navigation MUST follow the normative map:

  | Current focus | DPAD Up | DPAD Down | DPAD Left | DPAD Right |
  |---|---|---|---|---|
  | Search field | — (no action) | First grid tile | Join button | Settings button |
  | Join button | — (no action) | First grid tile | — (no action) | Search field |
  | Settings button | — (no action) | First grid tile | Search field | — (no action) |
  | Random Song / Random Duet / Random Medley button | Search field | First grid tile | — | — |
  | Grid tile (top row) | Random buttons row | Tile below (or no action if last row) | Tile to the left, or left panel entry target if at leftmost column | Tile to the right; no action if at rightmost column |
  | Grid tile (non-top row) | Tile above | Tile below (or no action if last row) | Tile to the left, or left panel entry target if at leftmost column | Tile to the right; no action if at rightmost column |
  | Medley playlist row | Previous row (or Play Medley if at top) | Next row (or no action if at bottom) | — | Search field |
  | Play Medley button | Last playlist row (or no action if empty) | — (no action) | — | Search field |

  **Left-panel entry target priority**: When moving left from the leftmost grid column, focus MUST enter the left panel with the following priority: (1) first Medley playlist row, if present; (2) Play Medley button, if the playlist is empty.

**Empty States**

- **FR-010**: When no phones are connected, the grid area MUST display: message "No phones connected." and hint "Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code."
- **FR-011**: When phones are connected but `SongLibrary` contains no valid songs, the grid area MUST display: message "No songs found." and hint "Open the karaoke app on your phone and make sure the songs folder is set."

**Search & Filter**

- **FR-012**: The screen MUST provide a Search text field. Filtering MUST be case-insensitive substring match across artist, album, and title fields of each `SongEntry`.
- **FR-013**: The filter MUST be debounced by 150 ms and MUST preserve Artist → Album → Title sort order.
- **FR-014**: Pressing OK on the Search field MUST open the system text input dialog; on confirming, focus returns to the Search field and the filter applies immediately.
- **FR-015**: Back key behavior follows a cascade: (1) If a modal or overlay is open, Back MUST close it. (2) If focus is in the song grid or left panel, Back MUST move focus to the Search field. (3) If focus is in the header/top controls and a filter is active, Back MUST clear the filter and keep the host on the Song List. (4) If focus is in the header/top controls and no filter is active, Back MUST exit the app (return to Android launcher).

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
- **FR-025**: If the relevant eligible set is empty, the random action MUST show a blocking modal with a single OK button. If the filtered grid result set is empty, all random buttons MUST be disabled, non-focusable, and visually greyed out. If no eligible songs exist for a specific random action (e.g., no duets for Random Duet, fewer than 2 medley-eligible songs for Random Medley), that specific button MUST be disabled and greyed out.

**Song Preview**

- **FR-026**: When a song tile holds focus for 500 ms, audio preview MUST begin at `SongEntry.previewStartSec`. If `previewStartSec` is 0, preview MUST start from position 0. *(Intentional delta from §9.2: `SongEntry` has no `audioLengthSec` field; the `audioLengthSec/4` fallback in §9.2 is not implemented. `previewStartSec=0` → seek to 0 is the accepted behaviour.)*
- **FR-027**: Preview MUST stop immediately when: focus moves away from the current tile, focus leaves the song grid, any modal opens, Settings opens, singing starts, or the screen loses focus.
- **FR-028**: If `SongEntry.audioUrl` is null or the HTTP request fails, preview MUST be suppressed silently — no error is shown to the host.
- **FR-029**: Preview audio volume is controlled exclusively by the Android system volume (TV remote volume buttons). No app-level preview volume control is implemented in this feature (Settings > Audio > Preview Volume, §9.4.3, is out of scope). Do NOT implement any volume guard in the preview trigger logic.

**Join Button & Pairing Overlay**

- **FR-030**: The header MUST contain a focusable `[ JOIN ]` button. Pressing OK on the Join button MUST open a pairing overlay/dialog showing the session QR code and join code.
- **FR-031**: The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter (the value of `SessionToken`). It MUST NOT encode a service-discovery identifier.
- **FR-032**: The QR code in the pairing overlay MUST be rendered at a minimum of **320 dp × 320 dp** (recommended 360–420 dp square at 1080p). Quiet zone MUST be at least **4 modules** on all sides. The QR MUST use high contrast (dark on light), no transparency or gradients, and MUST be static (no animation or scaling). The QR MUST be centered within the overlay and not occluded by UI chrome. The join code text MUST be displayed below the QR with a minimum text size of **24 sp** and sufficient character spacing for readability at distance.
- **FR-033**: The pairing overlay MUST be dismissed by pressing Back or a Cancel/Close action, returning focus to the Song List. The overlay does not affect the navigation stack (modal behavior).

**Medley Playlist**

- **FR-034**: Long-pressing OK (≥ 500 ms) on a song tile with `canMedley=true` MUST append that `SongEntry` to the end of the Medley playlist. Duplicate entries are not allowed: if the song is already in the playlist and the same focused tile is long-pressed again, the first duplicate attempt MUST be silently ignored; subsequent duplicate long-presses on the same focused tile MUST show a brief feedback message (e.g., "Already in medley"). Duplicate tracking is per focused tile instance, not by song name or ID.
- **FR-035**: Long-pressing OK on a song tile with `canMedley=false` MUST show a blocking modal with exact text: "This song can't be used in a medley. Look for songs with an M tag in the lower right corner" and a single OK button.
- **FR-036**: The Medley playlist area MUST be a fixed-height scrollable list: height = lesser of 7 lines or 25% of screen height, with a minimum of 3 lines always visible. Each row shows `<Artist>  <Title>` with no row number prefix.
- **FR-037**: Pressing OK on a playlist row MUST enter Reorder mode: Up/Down moves the item in the list; OK confirms the new position; Back cancels and restores the prior order. DPAD Left and Right MUST do nothing while in Reorder mode. Moving focus out of the playlist by any other mechanism MUST implicitly cancel Reorder and restore the original order. While in Reorder mode, the bottom-of-screen context hint bar MUST display: `Up/Down=Move  OK=Accept  Back=Cancel` (per §3.4).
- **FR-038**: Long-pressing OK on a playlist row MUST immediately delete that row (no confirmation dialog).
- **FR-039**: The Play Medley button MUST be disabled and non-focusable when the playlist is empty. When non-empty, pressing it MUST open Select Players with subtitle `Medley — <n> songs` and no Player 2 section.
- **FR-040**: The screen MUST provide a **Sing Random Medley** button. Pressing it MUST select 5 random medley-eligible songs (`canMedley=true`) from the currently visible filtered set. If fewer than 5 medley-eligible songs exist, all of them are selected. The button MUST require at least 2 eligible medley songs in the filtered set to be active; otherwise it MUST be disabled, non-focusable, and greyed out. When active, pressing the button MUST open Select Players with subtitle `Medley — <n> songs` and no Player 2 section.
- **FR-041**: The Medley playlist MUST be cleared (reset to empty) when the host navigates to Settings, starts a song, starts a medley, or the Results screen is shown. It MUST NOT be cleared when modal overlays (Select Players, error dialogs) are opened or closed.

**Contextual Hints Bar**

- **FR-044**: The Song List screen MUST render a contextual help bar at the bottom of the screen. The hints MUST change based on the currently focused element:
  - When a song grid tile is focused: `OK = Sing   Long-Press OK = Add to Medley`
  - When a Medley playlist row is focused: `OK = Reorder   Long-Press OK = Delete`
  - When in Reorder mode: `Up/Down = Move   OK = Accept   Back = Cancel` (per FR-037)
  - When no context-specific hint applies (e.g., Search field, buttons): the hints bar MAY be hidden or show a default hint.

**Navigation & Input Constraints**

- **FR-042**: Navigating between Song List, Settings, and any overlay MUST NOT change the session state (Open/Locked/Ended).
- **FR-043**: A long-press OK is defined as pressing and holding OK/Enter for ≥ 500 ms. When no long-press action is defined for the focused element, long-press MUST behave the same as a normal OK press.

**Preview Pane**

- **FR-045**: The left panel MUST contain a **preview pane** positioned above the Medley playlist. The preview pane MUST use a **16:9** aspect ratio.
- **FR-046**: The preview pane MUST be **display-only and non-focusable**. It MUST NOT participate in the DPAD focus graph.
- **FR-047**: The preview pane MUST be driven by the currently focused song tile in the grid. When a song tile gains focus, the preview pane MUST update to show that song's cover image (centered with blurred/dimmed background fill for the 16:9 aspect ratio), title, artist, and applicable tag chips (`D` / `R` / `V` / `I` / `M`).
- **FR-048**: When focus leaves the song grid (e.g., moves to Search field, buttons, Medley playlist), the preview pane MUST retain the last focused song's information (sticky behavior). Before any song has been focused in the current screen visit, the preview pane MUST show a placeholder state: the app logo/icon centered on a dimmed background.
- **FR-049**: Leaving the song grid MUST stop preview audio playback per FR-027, but the visual preview pane content MUST remain (per FR-048 sticky behavior).

**Visual Regression Testing**

Every distinct screen state of the Song List MUST have a committed screenshot baseline. This is a mandatory quality gate required by the project constitution and applies alongside all other test categories.

- **VR-001 — Screen state coverage**: The screen has no loading state — it renders immediately into one of the states below from first frame. Each of the following states MUST have a dedicated screenshot:
  - Empty state: no phones connected (FR-010)
  - No-songs state: phones connected but library empty (FR-011)
  - Populated state: songs visible, no active filter
  - Populated state: songs visible, filter active with matching results
  - Populated state: songs visible, filter active with zero results
  - Select Players modal open — non-duet song (FR-016/FR-017)
  - Select Players modal open — duet song, both players assigned (FR-018)
  - Select Players modal open — duet song, one player assigned (FR-018)
  - Select Players modal open — no phones blocking state (FR-020)
  - Error modal open — song start failure (FR-021)
  - Medley playlist visible with entries (FR-036)
  - Medley Reorder mode active (FR-037)
  - Pairing overlay open — QR code and join code visible (FR-030)
  - Preview pane showing focused song info (FR-047)

- **VR-004 — Stable test fixtures**: All screenshot tests MUST use deterministic, fixed-value test fixtures for any session-bound or runtime-dynamic data:
  - `SessionToken` MUST be injected as a fixed, known value (e.g. `"TEST-1234"`) so the QR code bitmap and join code text are identical on every test run.
  - Cover images loaded from URLs MUST use a local placeholder or a pre-seeded test asset — no live network calls in screenshot tests.
  - Where the screen depends on contracts owned by future (unimplemented) features, mocks MUST strictly implement the published contract interface. If a required contract has not been published, the screenshot test task MUST NOT begin.

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
- **SC-007**: 100% of mandatory acceptance tests T3.4.1–T3.4.12 (§3.4.9) pass.
- **SC-008**: Screenshot baselines for all screen states (VR-001) are committed to the repository and passing in CI.

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
- Full Song List landing screen (§3.4): grid, search, join button + pairing overlay, preview pane, medley playlist, random actions (including Random Medley), and the complete DPAD navigation map
- Song preview playback — audio (§9.2) and visual preview pane (§3.4.3)
- Global navigation behaviour as it applies to the Song List (§9.1 — Back key cascade, long-press OK definition)
- Select Players modal (§9.3) — opening, field rendering, gating rules, song start failure flow

**Out of scope:**
- Singing screen handoff and playback (§9.5)
- Settings screen and sub-screens (§9.4)
- Results screen (§9.6)
- Device roster management: Rename / Kick / Forget (§10.4.1 — Settings > Connect Phones only)
- Advanced Search overlay — POST-MVP per constitution
- Medley playback engine and per-segment transitions — Singing screen feature
- Session state changes (Open ↔ Locked ↔ Ended) — owned by feature 006
