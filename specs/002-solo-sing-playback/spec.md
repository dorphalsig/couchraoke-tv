# Feature Specification: Solo Sing Playback

**Feature Branch**: `002-solo-sing-playback`  
**Created**: 2026-04-29  
**Status**: Draft  
**Input**: User description: "We are starting implementing iteration 1 - solo sing fron §7.3. The spec is fairly complete so focus on extracting the relevant slices verbatim from the existing tv_app.md. Do not abstract away details that are already present. Make sure to note any gaps / inconsistencies when extracting the slices. DO NOT FIX ANYTHING, just note them in the chat at the end, along with recommended solutions and a brief rationale behind the choice. Make sure to include wireframes, behaviors, ui, etc."

## Clarifications

### Session 2026-04-29

- Q: How should duet/medley controls from shared screens behave in Iteration 1? → A: Show duet/medley controls but disable them in Iteration 1.
- Q: What pitch content should Iteration 1 draw on the Singing screen? → A: Static note lanes from the song file only; live pitch from pitch frames is out of scope.
- Q: What happens on normal song completion in Iteration 1? → A: Results screen is out of scope; return to Song List.
- Q: How should the Join QR be generated? → A: Use `qrcode-kotlin` for actual QR rendering, but account for qrcode-kotlin issue #197 (`Unable to consistently generate an image of a specific size`) by testing short and full endpoint payload sizing instead of relying blindly on fixed-canvas helpers.
- Q: What owns preview/playback volume in Iteration 1? → A: The TV/system media volume owns audibility; the app MUST NOT add an app-level preview preamp or depend on an out-of-scope Settings > Audio Preview Volume source.
- Q: How should OK on Search invoke text entry? → A: Keep the ViewModel framework-free and use the simplest presentation/platform-facing Android TV text input launcher; if the platform text dialog is not practical on target TV, use the native IME behavior of the Search field rather than building a custom keyboard.
- Q: How should optional video behave? → A: Video is best-effort and, when available, renders full-screen as the Singing screen background. Audio is required for a playable start; video/background failure is non-fatal and falls back to a static background.
- Q: How should out-of-scope visible affordances be documented? → A: Every visible no-op caused by Iteration 1 scope MUST have a code comment naming that it is intentionally no-op and the iteration that should wire it: Iteration 2 for scoring/live pitch/Results, Iteration 3 for duet/two-player/settings routes, and Iteration 4 for medley execution/prebuffer/crossfade.
- Q: Should Iteration 1 include pitch-lane performance coverage? → A: No. The lane has no moving live-pitch content in this iteration; remove pitch-lane-specific performance tasks while preserving lyrics behavior constraints.
- Q: How should Settings behave in Iteration 1? → A: Settings remains visible as a header control but is intentionally inert/no-op. Pressing Settings MUST NOT open a Settings route, menu, screen, or submenu; the control should carry a code comment naming Iteration 3 as the wiring target.
- Q: What should Select Players do when no phones are connected? → A: The action opens the same Join QR overlay as the Song List Join button; it does not open Settings.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse and start a solo song (Priority: P1)

As a host using the Android TV app, I can discover a phone on the LAN, see its songs in the Song List, open the Join overlay for pairing, choose a song, select one singer phone, and start playback so the TV shows lyrics and begins song playback end-to-end.

**Why this priority**: This is the Iteration 1 goal stated in §7.3: `End-to-end: browse library → select song → play audio with lyrics.` Without this flow, there is no usable solo-sing MVP.

**Independent Test**: Can be fully tested by launching the app, discovering one phone via mDNS, loading the phone manifest into the Song List, opening Select Players for a song, assigning one phone as Player 1, starting playback, and verifying that audio plays and sentence-paged lyrics render on the Singing screen.

**Acceptance Scenarios**:

1. **Given** the app is on Song List and one phone is discovered on the LAN, **When** the phone handshake completes, **Then** the Song List displays songs from the phone manifest.
2. **Given** the Song List has at least one valid song, **When** the host presses OK on a song tile, **Then** the Select Players modal opens with title `SELECT PLAYERS` and subtitle `<Artist> — <Title>`.
3. **Given** a non-duet song and at least one connected phone, **When** the host selects a Player 1 phone and presses `Start`, **Then** the TV fetches `txtUrl`, parses the chart, hands `audioUrl` and optional `videoUrl` to playback, and begins the song flow.
4. **Given** song playback starts successfully, **When** the Singing screen appears, **Then** it shows song title/artist, elapsed time, one centered lane, and exactly two lyrics lines with sentence-based paging.

---

### User Story 2 - Control solo playback from the TV remote (Priority: P2)

As a host during a solo song, I can use the TV remote Back key to pause, resume, restart the song, or quit to Song List so playback remains TV-controlled and recoverable without leaving the singing flow.

**Why this priority**: Iteration 1 requires an end-to-end playback flow that remains navigable and recoverable from the TV UI, including `Back → returns to song list` and the in-song pause overlay behavior.

**Independent Test**: Can be tested by starting a solo song, pressing Back to open Pause, selecting Resume, Restart Song, and Quit to Song List on separate runs, and verifying the expected playback and navigation outcomes.

**Acceptance Scenarios**:

1. **Given** a solo song is actively playing, **When** the host presses Back, **Then** a pause overlay opens with `Resume`, `Restart Song`, and `Quit to Song List`.
2. **Given** the pause overlay is open, **When** the host selects `Resume`, **Then** playback resumes from the current position.
3. **Given** the pause overlay is open, **When** the host selects `Restart Song` and confirms `OK`, **Then** per-player scores reset, a new `songInstanceSeq` is used, `assignSinger` is re-sent, and playback restarts from `startSec`.
4. **Given** the pause overlay is open, **When** the host selects `Quit to Song List` and confirms `OK`, **Then** playback stops and the app returns to Song List.

---

### User Story 3 - Recover from startup and playback interruptions (Priority: P3)

As a host, I can see consistent blocking interruption UI when a song cannot start, a required singer disconnects during countdown, playback fails, or no phones are connected, so the app does not crash and always returns to a clear recovery path.

**Why this priority**: Iteration 1 explicitly includes interruption overlay shell reuse, playback error handling, countdown disconnect handling, and Select Players empty-state behavior as required delivery items.

**Independent Test**: Can be tested by forcing the no-phone Select Players path, simulating countdown disconnect, simulating playback error, and verifying the defined blocking modals and return destinations.

**Acceptance Scenarios**:

1. **Given** Select Players is opened with no connected phones, **When** the modal renders, **Then** it shows `No phones connected` with an action to open the same Join QR overlay as the Song List Join button.
2. **Given** a required singer disconnects during countdown, **When** the countdown is canceled, **Then** the app returns to Select Players and shows a blocking `DISCONNECTED` modal with a single `OK` action.
3. **Given** a song cannot be started because the audio file is unavailable on the phone, **When** Start is attempted, **Then** the app returns to Song List and shows a blocking `ERROR` modal with the specified body copy.
4. **Given** LibVLC encounters an error during singing, **When** the error event fires, **Then** playback stops, the app returns to Song List, a blocking `ERROR` modal is shown, and the session returns to Open rather than remaining Locked.

---

### Edge Cases

- Required singer disconnects during countdown: cancel countdown, return to Select Players, show blocking `DISCONNECTED` modal with single `OK`.
- Playback reaches `stopAtLyricsTimeMs`: UI stops the active `LibVlcPlayerHandle`, emits `PlaybackEvent.Ended`, and the coordinator treats it as the authoritative song-end trigger unless an explicit error or quit path overrides it.
- `LibVlcEvent.Playing` does not arrive within 500 ms of `play()`: UI uses `fallbackStartTvMs`, emits `PlaybackEvent.Ready(fallbackStartTvMs)`, and logs a warning.
- Audio focus is denied before playback: UI emits `PlaybackEvent.Error` and follows the playback error handling path.
- `audioUrl` is unreachable on Start: abort, return to Song List, and show the blocking `ERROR` modal.
- Optional `videoUrl` or background media fails to load during singing: keep audio playback active and fall back to a static background without showing an error modal.
- Spectator disconnects: do not trigger disconnect auto-pause.
- Song List has no phones connected: empty state body instructs the host to open the karaoke app on the phone and scan the QR code.
- Phones are connected but no valid songs are present: empty state shows `No songs found.` with the specified guidance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The feature MUST deliver the Iteration 1 goal from §7.3: `browse library → select song → play audio with lyrics` for a single phone and one player.
- **FR-002**: The app MUST discover the phone via mDNS, complete handshake, and display songs from the phone manifest in Song List.
- **FR-003**: Song List MUST be the landing screen and MUST display songs sorted by Artist → Album → Title.
- **FR-004**: Song List MUST use a two-column layout: left rail with preview pane, Medley playlist, and Play Medley; right body with Search field, Random actions row, and song grid.
- **FR-005**: Song List header MUST contain Search, Join, and Settings, with Search visually strongest and Join/Settings equal secondary controls.
- **FR-006**: Search MUST perform case-insensitive substring matching across artist, album, and title with 150 ms debounce, and OK on Search MUST invoke a presentation/platform-facing Android TV text input launcher while keeping the ViewModel free of Android framework types. If the system text dialog is not practical on target TV, the Search field MAY rely on native IME focus behavior; the feature MUST NOT add a custom in-app keyboard in Iteration 1.
- **FR-007**: OK on a song tile MUST open Select Players. Duet and medley controls from shared screens MUST remain visible but disabled in Iteration 1; duet/medley execution is out of scope.
- **FR-008**: Join button MUST open the Join overlay, and the QR code MUST be rendered as an actual QR using `qrcode-kotlin`. The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter; it MUST NOT encode an NSD/mDNS identifier. Implementation MUST account for qrcode-kotlin issue #197 (`Unable to consistently generate an image of a specific size`) by testing both short payloads and full endpoint payloads for centered/scaled output within the 400dp QR box: the rendered PNG bounds MUST fit within the requested square size, preserve a 4-module quiet zone, and center the QR content with opposite-side padding differing by no more than 1 pixel.
- **FR-009**: Join Overlay MUST render as a modal over Song List, with the QR as the dominant centered object, the short join code directly below it, a static QR, and Back closing the overlay.
- **FR-010**: Select Players MUST render as a modal overlay on `SurfaceLevel2` with title `SELECT PLAYERS` and subtitle `<Artist> — <Title>` for single-song play.
- **FR-011**: For non-duet songs, Select Players MUST require Player 1, keep Player 2 visible but disabled, and hide Player 2 Difficulty when Player 2 is `(none)`.
- **FR-012**: If no phones are connected, Select Players MUST show blocking message `No phones connected` with an action to open the same Join QR overlay as the Song List Join button.
- **FR-013**: On Start, the TV MUST fetch `txtUrl` synchronously, parse it, and hand `audioUrl` and optional `videoUrl` to playback.
- **FR-014**: If `audioUrl` is unreachable on Start, the app MUST abort, return to Song List, and show blocking error: title `ERROR`, body `This song can't be played.` / `Check Settings > Song Library — the song's phone may be disconnected.`, single `OK` action.
- **FR-015**: Before countdown or live playback, the UI MUST emit `PlaybackEvent.Prepared(effectivePlaybackDurationMs)` to the coordinator.
- **FR-016**: Before countdown or live playback, the coordinator MUST obtain at least one valid clock-sync sample for every assigned singer.
- **FR-017**: On Start, the TV MUST send `assignSinger` to each selected singer phone with `playerId`, `songInstanceSeq`, `startMode`, `countdownMs`, `udpPort`, and `stopAtLyricsTimeMs`, and MUST NOT send `assignSinger` to non-selected devices.
- **FR-018**: If Ready countdown is ON, the app MUST show an N-second countdown at 1 Hz using a centered `DisplayHeroNumber` numeral over a dimmed static background; if OFF, playback MUST begin immediately.
- **FR-019**: If a required singer disconnects during countdown, the app MUST cancel countdown, return to Select Players, and show blocking modal title `DISCONNECTED`, body `A required singer disconnected during countdown. Please reconnect and start again.`, single `OK`, with default focus on `OK`.
- **FR-020**: The Singing screen MUST use a full-screen video/background presentation with one centered lane for a single singer, a top metadata strip, and a full-width bottom lyrics band. Optional video MUST render full-screen as the background when available, but audio remains the required playable asset; video/background load failure MUST be non-fatal and fall back to a static background without surfacing an error modal.
- **FR-021**: The Singing screen MUST show the minimum content defined in §2.6.16: progressive-highlight lyrics, pitch bars per active singer, live score using display format `XXXXX`, elapsed time, and song title/artist.
- **FR-022**: For Iteration 1, the pitch lane MUST draw static note lanes from the parsed song file only. Live pitch from pitch frames, pitch cursor driven by phone input, hit/miss feedback, and scoring calculations are out of scope.
- **FR-023**: For Iteration 1, the score component MUST render in the screen layout and display constant `00000` until scoring is wired in.
- **FR-024**: During active solo singing, the screen MUST show exactly one centered lane band at `SingingSingleLaneHeight` (192dp), full width, vertically centered on screen.
- **FR-025**: Lyrics MUST render in the bottom lyrics band only and MUST always show exactly two lines: current line and next line. The app MUST NOT render a third line.
- **FR-026**: Lyrics MUST use sentence-based paging, remain spatially stable during a sentence, and during instrumental gaps the completed sentence MUST remain at 100% highlight rather than pre-paging or showing blank.
- **FR-027**: The top metadata MUST start in the intro strip at `SingingTopIntroStripHeight` and collapse during active singing to the minimal strip at `SingingTopMinimalStripHeight`.
- **FR-028**: Elapsed time MUST be shown bottom-right in `MM:SS` zero-padded format.
- **FR-029**: Pressing Back during singing MUST open the Pause overlay with `Resume`, `Restart Song`, and `Quit to Song List`.
- **FR-030**: `Resume` MUST resume playback from the current position.
- **FR-031**: `Restart Song` MUST confirm with default focus on Cancel; on OK it MUST reset per-player scores, create a new `songInstanceSeq`, re-send `assignSinger`, and restart the normal song from `startSec`.
- **FR-032**: `Quit to Song List` MUST confirm with default focus on Cancel and on OK MUST return to Song List.
- **FR-033**: The interruption surfaces named in §2.6.11 — including Pause, Countdown disconnect, Start-failure / Playback error, Song-Library refresh errors, and Select-Players no-phone state — MUST reuse the same centered elevated interruption shell.
- **FR-034**: The interruption shell MUST use `SurfaceLevel2` with `PrimaryModalPadding` (32dp), dim the underlying scene with a dark scrim, MUST NOT use runtime blur, and MUST keep focus movement disabled until entry completes.
- **FR-035**: The UI layer MUST register a single event listener on the audio `LibVlcPlayerHandle` before calling `play()` and MUST compute `songStartTvMs = (System.nanoTime() / 1_000_000) − playerHandle.timeMs` on the first `LibVlcEvent.Playing` after `play()`.
- **FR-036**: The UI layer MUST capture `fallbackStartTvMs` at the moment `play()` is called and MUST emit `PlaybackEvent.Ready(fallbackStartTvMs)` if `LibVlcEvent.Playing` has not fired within 500 ms.
- **FR-037**: The coordinator MUST wait for `PlaybackEvent.Ready(songStartTvMs)` before calling `ScoringEngine.setSongStart(songStartTvMs)`, and ScoringEngine MUST NOT finalize any notes until `songStartTvMs` has been set.
- **FR-038**: The UI MUST enforce `stopAtLyricsTimeMs` as the authoritative playback stop boundary, call `LibVlcPlayerHandle.stop()` when reached, and emit `PlaybackEvent.Ended`.
- **FR-039**: On song end in Iteration 1, the app MUST return to Song List. Results screen and score finalization are out of scope for this iteration, even though §4.1 defines the full future FSM route through Results.
- **FR-040**: Pitch-frame ingestion and pitch-frame-driven behavior are out of scope for Iteration 1; later scoring/pitch-pipeline work MUST handle ignoring pitch frames at or beyond `stopAtLyricsTimeMs`.
- **FR-041**: On `LibVlcEvent.EncounteredError` during singing, the app MUST stop playback and scoring immediately, return to Song List, and show blocking `ERROR` modal whose second body line is the last LibVLC warning/error log line truncated to 120 chars when available.
- **FR-042**: A playback error MUST NOT crash the app, corrupt session state, or leave the session Locked; session returns to Open on error exit.
- **FR-043**: The UI layer MUST request `AUDIOFOCUS_GAIN` before playback, pause on transient losses, resume on `AUDIOFOCUS_GAIN`, follow the playback error path on permanent loss, and abandon audio focus on song end, error exit, or Restart.
- **FR-044**: App-wide UI shell MUST consist of `MainActivity`, `CouchraokeTheme`, and `AppNavHost`, with `MainActivity` as the single Android Activity and `setContent {}` as the only theme/navigation host instantiation site.
- **FR-045**: Navigation MUST use `AppNavHost` routes for `SongList`, `SelectPlayers`, `Singing`, and inert `Results`, while `SelectPlayersModal` and `JoinOverlay` remain modal composables shown within `SongListScreen`, not separate navigation destinations. Settings MUST remain a visible inert/no-op header affordance in Iteration 1 and MUST NOT open a Settings route, menu, screen, or submenu. Iteration 1 MUST keep out-of-scope visible routes or affordances inert with code comments naming the intended wiring iteration.
- **FR-046**: Back handling MUST follow §2.6.9 rules, including: Song List Back clears filter first then exits app, Singing Back opens Pause, Results Back returns to Song List, and overlays close without affecting the navigation stack.
- **FR-047**: The feature MUST satisfy the §7.3 Definition of Done items in scope for Iteration 1 solo sing, including F15 session lifecycle pass, minimal song-start clock-sync gate test pass, F22 GamePhase FSM pass, emulator run, and cumulative TV-owned end-to-end flow through the TV UI.

### Key Entities *(include if feature involves data)*

- **SongEntry**: A manifest-provided song record used for Song List display and start handoff. Relevant fields in the extracted scope include artist, album, title, cover, tags, `canMedley`, `audioUrl`, optional `videoUrl`, `previewStartSec`, and `txtUrl`.
- **PlaybackEvent**: UI-to-coordinator playback events in the extracted flow: `Prepared(effectivePlaybackDurationMs)`, `Ready(songStartTvMs)`, `Error(cause)`, and `Ended`.
- **PlaybackIntent**: Coordinator-to-UI playback commands used by the extracted flow: `Prepare`, `Play`, `Pause`, `Stop`, and `Seek`; medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` may exist in the shared contract but MUST remain inert/no-op in Iteration 1 with Iteration 4 wiring comments.
- **SelectPlayers selection**: The TV-owned assignment state for chosen singer phone(s), difficulty, and `playerId` mapping used when sending `assignSinger`.
- **Interruption overlay shell**: The shared modal surface reused by pause, disconnect, playback error, and no-phone states.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the Iteration 1 end-to-end TV flow, the host can complete launch app → pair one phone → load phone manifest → select one song → start playback with lyrics → return to Song List without leaving the TV UI.
- **SC-002**: In solo playback, the Singing screen consistently shows one centered singer lane, elapsed time, static note lanes from the song file, constant `00000` score, and exactly two lyrics lines for the full duration of the song.
- **SC-003**: In song starts where playback succeeds, the UI reports prepared playback duration before playback begins and reports a playback-ready start time before scoring is allowed to begin.
- **SC-004**: At normal song end, the app returns to Song List without showing Results.
- **SC-005**: In countdown-disconnect, start-failure, and playback-error cases, the app returns the host to a defined recovery destination with a blocking modal and does not remain stuck in an in-song locked state.
- **SC-006**: The in-scope validation gates for Iteration 1 pass: F15 session lifecycle, minimal song-start clock-sync gate coverage, F22 GamePhase FSM, actual Join QR rendering with short/full payload sizing coverage, and emulator execution for the solo-sing flow.

## Assumptions

- Iteration 1 scope is restricted to solo-sing playback for one phone and one player; duet and medley controls from shared screens remain visible but disabled.
- Scoring remains out of Iteration 1; displayed scores stay at zero until a later scoring task wires real scoring.
- Results screen is out of scope for Iteration 1; normal song completion returns to Song List.
- Live pitch drawing from pitch frames is out of scope for Iteration 1. The pitch lane draws static note targets from the parsed song file only.
- Join QR rendering uses qrcode-kotlin, with explicit coverage for qrcode-kotlin issue #197 around inconsistent fixed-size output.
- Preview and playback volume are controlled by the TV/system media volume in Iteration 1; the app does not add a preview preamp or depend on Settings > Audio.
- Optional video is best-effort full-screen background media; audio is the required playable asset and video/background failure falls back to a static background.
- Visible out-of-scope no-ops carry code comments naming the target wiring iteration: Iteration 2 for scoring/live pitch/Results, Iteration 3 for duet/two-player/settings, and Iteration 4 for medley execution/prebuffer/crossfade.
- The new feature spec intentionally preserves source details from `tv_app.md` rather than simplifying or reinterpreting them.
- The app continues to treat the TV as the authoritative host, the phone as the source of manifest/audio/chart assets, and the flow as LAN-only with no cloud dependencies.
- The feature spec includes shared screen structure and wireframes where those are part of the solo-sing path, even when the source section also contains adjacent out-of-iteration behaviors.
- The source spec’s Iteration 1 DOD remains the authoritative acceptance gate for this extracted feature scope.
- No changes are made here to resolve source ambiguities or scope overlaps; those are reported separately as gaps/inconsistencies rather than corrected in the specification.

## Extracted UI Wireframes

### Song List Wireframe

```wireframe
+------------------------------------------------------------------------------------------------------+
|  Search: [________________________________________________________]     [ JOIN ]   [ ⚙ SETTINGS ]   |
|                                                                                                      |
|  +--------------------------------------+   [ Random Song ] [ Random Duet ] [ Random Medley ]       |
|  | PREVIEW PANE (16:9)                  |                                                           |
|  | (display-only; non-focusable)        |   +-----------------------------------------------------+ |
|  |                                      |   | SONG GRID                                           | |
|  +--------------------------------------+   |  +---------+  +---------+  +---------+              | |
|  | Title                                |   |  | Cover   |  | Cover   |  | Cover   |   (focused)  | |
|  | Artist                               |   |  |     [D] |  |   [R][V]|  |  [D][M] |              | |
|  +--------------------------------------+   |  +---------+  +---------+  +---------+              | |
|  | MEDLEY PLAYLIST                      |   |  Title        Title        Title                    | |
|  | (5 visible rows; scrolls)            |   |  Artist                                             | |
|  |  +-------------------------------+   |   |  (only focused card shows artist line)              | |
|  |  | <artist>  <song>              |   |   |                                                     | |
|  |  | <artist>  <song>              |   |   |  +---------+  +---------+  +---------+              | |
|  |  | <artist>  <song>              |   |   |  | Cover   |  | Cover   |  | Cover   |              | |
|  |  | <artist>  <song>              |   |   |  |     [V] |  |     [I] |  |   [D][M]|              | |
|  |  | <artist>  <song>              |   |   |  +---------+  +---------+  +---------+              | |
|  |  +-------------------------------+   |   |  Title        Title        Title                    | |
|  |  [Play Medley]                   |   |   |                                                     | |
|  +--------------------------------------+   |  Tags: D=Duet  M=Medley  R=Rap  I=Instr  V=Video    | |
|                                              +-----------------------------------------------------+ |
+------------------------------------------------------------------------------------------------------+
|                     Hints: OK=Select   Long OK=Add Medley   Back=Search/Exit                         |
+------------------------------------------------------------------------------------------------------+
```

### Select Players Wireframe

```wireframe
Non-duet song
+--------------------------------------------------------------------------------+
| SELECT PLAYERS                                                   <Artist> — <Title> |
+--------------------------------------------------------------------------------+
| Player 1 (required)                                                             |
|  Phone:      [ Pixel-7 ▾ ]                                                      |
|  Difficulty: [ Medium ▾ ]                                                       |
+--------------------------------------------------------------------------------+
| Player 2                                                                        |
|  Phone:      [ (disabled) ]                                                     |
+--------------------------------------------------------------------------------+
| [Start]   [Cancel]                                                              |
+--------------------------------------------------------------------------------+
| Hints: OK=Select   Back=Cancel                                                  |
+--------------------------------------------------------------------------------+

No phones connected
+--------------------------------------------------------------------------------+
| SELECT PLAYERS                                              <Artist> — <Title> |
+--------------------------------------------------------------------------------+
|                                                                                |
|   ⚠ No phones connected.                                                       |
|   Open Join and scan the QR code with the phone app.                           |
|                                                                                |
| [Open Join QR]   [Cancel]                                                      |
+--------------------------------------------------------------------------------+
```

### Countdown Disconnect Modal Wireframe

```wireframe
+--------------------------------------+
| DISCONNECTED                         |
| A required singer disconnected      |
| during countdown.                   |
| Please reconnect and start again.   |
|                                      |
|  > OK                               |
+--------------------------------------+
```

### Singing Screen Wireframe

```wireframe
Active singing screen — single singer (vertically centered lane)
+--------------------------------------------------------------------------------+
|                          (FULLSCREEN VIDEO / BACKGROUND)                       |
|                                                                                |
|                                                                                |
| P1 [badge]                                                                     |
|  ───────────────────────────────────────────────────────────────────────────   |
|   [note bars / pitch lane P1 — vertically centered]                            |
|                                                                +--------+      |
|                                                                | 00710  |      |
|                                                                +--------+      |
|                                                                  Perfect!      |
|                                                                                |
+--------------------------------------------------------------------------------+
| Lyrics                                                                          |
|   CUz this life is too short                                                   |
+--------------------------------------------------------------------------------+
|                                                                      00:35     |
+--------------------------------------------------------------------------------+

Countdown overlay (before playback and scoring start; 1 Hz)
+--------------------------------------------------------------------------------+
|                                                                                |
|                                     3                                          |
|                                                                                |
+--------------------------------------------------------------------------------+
(then 2, 1; after showing 1, playback + scoring start)

Pause overlay (Back key)
+--------------------------------------+
| PAUSED                               |
|  > Resume                            |
|    Restart Song                      |
|    Quit to Song List                 |
+--------------------------------------+

Restart confirm (default focus Cancel)
+--------------------------------------+
| CONFIRM                              |
| Restart song?                        |
|                                      |
|  > Cancel     OK                     |
+--------------------------------------+

Quit confirm (default focus Cancel)
+--------------------------------------+
| CONFIRM                              |
| Quit to Song List?                   |
|                                      |
|  > Cancel     OK                     |
+--------------------------------------+
```

## Extracted Interaction Contracts

### Song Start Flow

```text
User selects song in UI
         │
         ▼
┌─────────────────────┐
│ PlaybackCoordinator │
│   startSong()       │
└─────────┬───────────┘
          │
    ┌─────┴─────┬──────────────┬─────────────────┐
    ▼           ▼              ▼                 ▼
NetworkCtrl  UsdxParser    ScoringEngine      UI Layer
fetchTxt()   parse()       loadChart(config) (via Intent)
    │           │              │            Prepare()
    │           │              │                │
    └─────┬─────┘              │                │
          ▼                    │                │
    ParsedSong ────────────────┘                │
                                                │
    ┌───────────────────────────────────────────┘
    ▼
PlaybackEvent.Prepared(durationMs)
    │
    ▼
NetworkCtrl.broadcastPlaybackState()
NetworkCtrl.sendAssignSinger()
    │
    ▼
UI.Play() ──→ LibVLC starts ──→ PlaybackEvent.Ready(songStartTvMs)
                                        │
                                        ▼
                              ScoringEngine.setSongStart()
                              ScoringEngine.start()
```

### PlaybackCoordinator ↔ UI Layer

- Coordinator emits `PlaybackIntent` (Prepare, Play, Pause, etc.).
- UI/playback layer observes intents and executes them through one authoritative audio `LibVlcPlayerHandle` plus an optional best-effort decorative full-screen video/background handle.
- UI is responsible for enforcing the active `stopAtLyricsTimeMs` boundary on the audio handle using the current playback plan.
- When UI detects that playback has reached the active `stopAtLyricsTimeMs`, it MUST stop the active audio handle and any decorative video/background handle, then emit `PlaybackEvent.Ended`.
- UI emits `PlaybackEvent` (`Prepared` with effective playback-plan duration, `Ready` with songStartTvMs, `Error`, `Ended`).
- UI exposes `currentPositionMs: StateFlow<Long>` for observation; this is always the audio handle position.
