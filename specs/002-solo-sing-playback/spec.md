# Feature Specification: Solo Sing Playback

**Feature Branch**: `002-solo-sing-playback`  
**Created**: 2026-04-29  
**Status**: Draft  
**Input**: Iteration 1 solo-sing playback feature for the Android TV host app.

This file is the implementation source of truth for Iteration 1. It is intentionally self-contained: implementers should not need to inspect broader product specifications to determine Iteration 1 behavior.

## Clarifications

### Session 2026-04-29

- Q: How should duet/medley controls from shared screens behave in Iteration 1? → A: Show duet/medley controls but disable them in Iteration 1.
- Q: What pitch content should Iteration 1 draw on the Singing screen? → A: Static note lanes from the song file only; live pitch from pitch frames is out of scope.
- Q: What happens on normal song completion in Iteration 1? → A: Results screen is out of scope; return to Song List.
- Q: How should the Join QR be generated? → A: Use `QRose` for actual QR rendering, with the QR-only dependency `io.github.alexzhirkevich:qrose` and not `qrose-oned`. The basic on-screen usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`; for the Join overlay, keep a QRose painter inside the fixed 400dp QR box and supply explicit QR options: `colors { dark = QrBrush.solid(Color.Black); light = QrBrush.solid(Color.White) }`, `errorCorrectionLevel = QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale`. Render the painter directly; PNG byte export is not required. Validate both short and full endpoint payloads for centered output within the requested square, preserving at least a 4-module quiet zone.
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

**Why this priority**: This is the Iteration 1 goal: `End-to-end: browse library → select song → play audio with lyrics.` Without this flow, there is no usable solo-sing MVP.

**Independent Test**: Can be fully tested by launching the app, discovering one phone via mDNS, loading the phone manifest into the Song List, opening Select Players for a song, assigning one phone as Player 1, starting playback, and verifying that audio plays and sentence-paged lyrics render on the Singing screen.

**Acceptance Scenarios**:

1. **Given** the app is on Song List and one phone is discovered on the LAN, **When** the phone handshake completes, **Then** the Song List displays songs from the phone manifest.
2. **Given** the Song List has at least one valid song, **When** the host presses OK on a song tile, **Then** the Select Players modal opens with title `SELECT PLAYERS` and subtitle `<Artist> — <Title>`.
3. **Given** a non-duet song and at least one connected phone, **When** the host selects a Player 1 phone and presses `Start`, **Then** the TV fetches `txtUrl`, parses the chart, hands `audioUrl` and optional `videoUrl` to playback, and begins the song flow.
4. **Given** song playback starts successfully, **When** the Singing screen appears, **Then** it shows song title/artist, elapsed time, one centered lane, and exactly two lyrics lines with sentence-based paging.
5. **Given** focus remains on the same Song List tile for 500 ms, **When** preview starts, **Then** preview uses that song's manifest `audioUrl` and `previewStartSec` when `previewStartSec > 0.0`, otherwise starts at 0 seconds; preview stops immediately when focus changes, focus leaves the grid, an overlay/modal/settings/singing opens, or Song List exits; preview HTTP failures are suppressed silently and preview audibility follows TV/system media volume.

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
- Singing video exceeds 720p and hardware decoder support cannot be confirmed before load: start with audio plus static background instead of attaching the video handle.
- Optional video is disabled by policy or encounters a playback failure during singing: disable the decorative video for the current song, fall back to static background, and continue audio/playback/session state without a blocking error modal.
- Spectator disconnects: do not trigger disconnect auto-pause.
- Song List has no phones connected: empty state body instructs the host to open the karaoke app on the phone and scan the QR code.
- Phones are connected but no valid songs are present: empty state shows `No songs found.` with the specified guidance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The feature MUST deliver the Iteration 1 goal: `browse library → select song → play audio with lyrics` for a single phone and one player.
- **FR-002**: The TV MUST remain the authoritative host for session state, session token, phone roster, song lifecycle, selected singer assignment, playback state, and song-end routing. Phones provide manifest/chart/audio/video assets and receive host-authored session/playback messages.
- **FR-003**: The app MUST discover the phone via mDNS, complete a token-gated WebSocket handshake, fetch the phone manifest over cleartext LAN HTTP, and display valid songs from that manifest in Song List. The TV app MUST include Android cleartext LAN HTTP configuration so phone-hosted `http://` manifest and media URLs can be fetched on supported Android TV versions.
- **FR-004**: The TV MUST advertise `_karaoke._tcp` over mDNS for the active session using a unique instance name, the WebSocket server port, `v=1`, and TXT `code=<JOIN_CODE>` where the code is uppercase and normalized without hyphens. The TV MUST acquire a multicast lock before jmDNS use and release it when the network controller stops.
- **FR-005**: The WebSocket endpoint MUST be `ws://<tv-ip>:<wsPort>/?token=<sessionToken>`. The token MUST have at least 32 bits of entropy, SHOULD have 64+ bits, MUST be regenerated for each session, and MUST be the same value represented by the displayed join code, QR payload token query parameter, and mDNS TXT code. Missing or incorrect token MUST return `error(code="invalid_token")` and close the connection.
- **FR-006**: The WebSocket `hello` handshake MUST require `protocolVersion=1`, `clientId`, `deviceName`, and `httpPort`; protocol mismatches MUST return `error(code="protocol_mismatch")` and close the connection. Unknown message types after handshake MUST be ignored with a warning; unexpected message types during handshake are fatal and close the connection.
- **FR-007**: A successful phone handshake MUST assign a new unique `connectionId` (`uint16`) and return it only in the initial `sessionState` response. Reconnects within the same TV session use the same `clientId`, receive a new `connectionId`, reclaim their roster identity, and must not rely on an old socket registry entry.
- **FR-008**: The session MUST start Open. On song start it MUST become Locked, and new non-reconnect `hello` attempts MUST receive `error(code="session_locked")`. On normal song end, quit, start failure, or playback error exit in Iteration 1, the session MUST return to Open.
- **FR-009**: The TV MUST bind the UDP port before phone connections so the port is stable for the session lifetime and can be included in `assignSinger`. UDP pitch-frame ingestion remains out of Iteration 1 except for preserving the protocol surface needed by `assignSinger`.
- **FR-010**: Song List MUST be the landing screen and MUST display songs sorted by Artist → Album → Title.
- **FR-011**: The library MUST maintain an in-memory index derived from phone manifests. On phone connection after successful handshake, it MUST fetch `/manifest.json` before making that phone's songs visible. A manifest refresh for a phone MUST replace that phone's prior entries rather than appending. A phone disconnect MUST immediately remove all songs from that phone. Fetch failure MUST retain the previous catalog for that phone when one exists. If a phone reconnects while a song is in countdown, live playback, or paused playback, the TV MUST mark that phone's catalog stale and defer manifest refresh until the session returns Open so gameplay is not interrupted by library refresh work.
- **FR-012**: Manifest entries MUST be rejected if required fields are missing or invalid. Required fields are `relativeTxtPath`, `modifiedTimeMs`, `title`, `artist`, `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`, `startSec`, `previewStartSec`, `txtUrl`, and `audioUrl`. `txtUrl` and `audioUrl` must be non-null; `hasVideo` must equal whether `videoUrl` is non-null; `instrumentalUrl` and `vocalsUrl` MUST NOT appear in TV-facing manifest data. The TV relies on phone-served media URLs supporting HTTP `Range` requests with `206 Partial Content`, `Content-Range`, `Accept-Ranges: bytes`, and `Content-Length`; `/manifest.json` MUST be fetched as non-cached data so library refreshes observe the phone's latest scan.
- **FR-013**: `songId` MUST equal `phoneClientId + "::" + relativeTxtPath`. `relativeTxtPath` MUST use `/`, MUST NOT start with `/`, MUST NOT contain `.` or `..` path segments, and MUST preserve case. `previewStartSec` uses the manifest value and falls back to `0.0` only when absent or non-positive in Iteration 1.
- **FR-014**: Song List MUST use a two-column layout: left rail with preview pane, Medley playlist, and Play Medley; right body with Search field, Random actions row, and song grid.
- **FR-015**: Song List header MUST contain Search, Join, and Settings, with Search visually strongest and Join/Settings equal secondary controls.
- **FR-016**: Search MUST perform case-insensitive substring matching across artist, album, and title with 150 ms debounce, and OK on Search MUST invoke a presentation/platform-facing Android TV text input launcher while keeping the ViewModel free of Android framework types. If the system text dialog is not practical on target TV, the Search field MAY rely on native IME focus behavior; the feature MUST NOT add a custom in-app keyboard in Iteration 1.
- **FR-017**: OK on a song tile MUST open Select Players. Duet and medley controls from shared screens MUST remain visible but disabled in Iteration 1; duet/medley execution, long-press Add to Medley, medley row reorder/delete, Random Duet execution, Random Medley execution, and Play Medley execution are out of scope and MUST be inert/non-focusable where disabled.
- **FR-018**: The Random actions row MUST show Random Song, Random Duet, and Random Medley with equal visual weight. Random Song MAY select a random valid song from the filtered set and open Select Players. Random Duet and Random Medley MUST be visible but disabled/no-op in Iteration 1.
- **FR-019**: The left rail preview pane MUST be 16:9, display-only, and non-focusable. Focused-song preview metadata MUST show full title and artist without truncating the preview metadata block. The Medley playlist area and Play Medley control remain visible but disabled/inert in Iteration 1.
- **FR-020**: Song cards MUST show cover image, title up to two lines, and up to three on-image lower-right tag chips in default state. Focused state additionally shows one artist line in a reserved artist slot without reflow. If artwork is missing, placeholder, or unusable, the card MUST keep title primary, keep tag chips visible, and show artist in default state.
- **FR-021**: Song card tag chips MUST use priority `D`, `M`, `R`, `I`, `V` when more than three apply, so `V` is first omitted. Tags mean duet, medley, rap, instrumental, and video respectively.
- **FR-022**: Song List preview playback MUST be screen-scoped and LibVLC-backed for codec consistency with Singing playback. It MUST start only after focus remains on the same song tile for 500 ms, prepare the manifest `audioUrl`, seek to `previewStartSec` when `previewStartSec > 0.0`, otherwise seek to 0 seconds, play until stopped with no fixed 10-second cap, stop immediately when focus changes, focus leaves the grid, an overlay/modal/settings/singing opens, or Song List exits, suppress HTTP/playback failures silently, and use only TV/system media volume in Iteration 1.
- **FR-023**: Join button MUST open the Join overlay, and the QR code MUST be rendered as an actual QR using `QRose` (`io.github.alexzhirkevich:qrose`). Do not add `qrose-oned` for this feature. The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter; it MUST NOT encode an NSD/mDNS identifier. The implementation contract is mandatory: the basic Compose usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`, and the Join overlay MUST use a QRose painter with explicit options: `colors { dark = QrBrush.solid(Color.Black); light = QrBrush.solid(Color.White) }`, `errorCorrectionLevel = QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale`. The renderer MUST validate both short payloads and full endpoint payloads for centered/scaled output within the 400dp QR box: the rendered QR metadata MUST fit within the requested square size, preserve at least a 4-module quiet zone, and center the QR content with opposite-side padding differing by no more than 1 pixel. PNG byte export is not required.
- **FR-024**: Join Overlay MUST render as a modal over Song List, with the QR as the dominant centered object, the short join code directly below it, a static QR, and Back closing the overlay.
- **FR-025**: Select Players MUST render as a modal overlay on `SurfaceLevel2` with title `SELECT PLAYERS` and subtitle `<Artist> — <Title>` for single-song play.
- **FR-026**: For non-duet songs, Select Players MUST require Player 1, default difficulty to Medium, keep Player 2 visible but disabled, and hide Player 2 Difficulty when Player 2 is `(none)`. `Start` is emphasized through placement, size, and surface contrast, not `RewardAccent`.
- **FR-027**: If no phones are connected, Select Players MUST show blocking message `No phones connected` with an action to open the same Join QR overlay as the Song List Join button. It MUST NOT open Settings in Iteration 1.
- **FR-028**: On Start, the TV MUST fetch `txtUrl` synchronously, parse it, and hand `audioUrl` and optional `videoUrl` to playback.
- **FR-029**: If `audioUrl` is unreachable on Start, the app MUST abort, return to Song List, and show blocking error: title `ERROR`, body `This song can't be played.` / `Check Settings > Song Library — the song's phone may be disconnected.`, single `OK` action.
- **FR-030**: Before countdown or live playback, the UI MUST emit `PlaybackEvent.Prepared(effectivePlaybackDurationMs)` to the coordinator. `PlaybackIntent.Prepare` MAY be emitted immediately after parse with `chartEndLyricsTimeMs` set to parsed positive `#END` in milliseconds, or null when `#END` is absent or non-positive. For audio-only playback, `effectivePlaybackDurationMs` comes from the prepared audio duration exposed by the audio LibVLC handle; for audio+video playback, audio remains authoritative and the prepared duration still comes from the audio handle because video is decorative. The coordinator MUST finalize `stopAtLyricsTimeMs` after `Prepared` as `chartEndLyricsTimeMs ?: effectivePlaybackDurationMs`. If `chartEndLyricsTimeMs` is null and the audio handle cannot report a usable duration, preparation MUST fail through the playback error path rather than guessing a stop boundary.
- **FR-031**: Before countdown or live playback, the coordinator MUST obtain at least one valid clock-sync sample for every assigned singer. Countdown or live playback MUST NOT begin without this sample. Full best-of-5 retry/failure coverage is deferred, but the Iteration 1 start gate is mandatory.
- **FR-032**: On Start, after `PlaybackEvent.Prepared` and before `PlaybackIntent.Play`, the TV MUST send `assignSinger` to each selected singer phone with `type="assignSinger"`, `protocolVersion=1`, `sessionId`, `playerId`, `songInstanceSeq`, `difficulty`, `startMode`, `countdownMs` when countdown mode is used, `udpPort`, `songTitle`, `songArtist`, and finalized `stopAtLyricsTimeMs`. The TV MUST NOT send `assignSinger` to non-selected devices and MUST NOT include `connectionId` in `assignSinger`.
- **FR-033**: The coordinator MUST construct and broadcast every `playbackState`; the network layer MUST NOT construct it autonomously. `playbackState` MUST include `type="playbackState"`, `protocolVersion=1`, `sessionId`, `songInstanceSeq`, monotonically increasing `revision` per `songInstanceSeq`, `state`, `lyricsTimeMs`, `stopAtLyricsTimeMs`, optional `countdownRemainingMs`, `reason`, and optional `tsTvMs`.
- **FR-034**: If Ready countdown is ON, the app MUST show an N-second countdown at 1 Hz using a centered `DisplayHeroNumber` numeral over a dimmed static background; if OFF, playback MUST begin immediately. Countdown mode maps to `startMode="countdown"` and `countdownMs = countdownSeconds * 1000`; countdown OFF maps to `startMode="live"` with omitted `countdownMs`.
- **FR-035**: If a required singer disconnects during countdown, the app MUST cancel countdown, return to Select Players, and show blocking modal title `DISCONNECTED`, body `A required singer disconnected during countdown. Please reconnect and start again.`, single `OK`, with default focus on `OK`. Spectator disconnects MUST NOT trigger auto-pause.
- **FR-036**: The Singing screen MUST use a full-screen video/background presentation with one centered lane for a single singer, a top metadata strip, and a full-width bottom lyrics band. The visible background fallback priority is: admitted `videoUrl`, then song static `backgroundUrl`, then bundled app singing background. A black empty background MUST NOT be the final fallback.
- **FR-037**: Songs with optional video MUST use two independent LibVLC players in the UI/playback layer: an authoritative audio player for `audioUrl` and timing, plus a decorative video player for `videoUrl` when admitted. The video player MUST be configured without audio, MUST NOT influence `currentPositionMs`, `songStartTvMs`, `Ready`, `Prepared`, `stopAtLyricsTimeMs`, scoring, or session state, and MUST be released/fallback to static background if it fails.
- **FR-038**: Before attaching optional singing video, the app MUST run a static admission gate that disables the video path at load time and uses static background when the video is greater than 720p and hardware decoder support cannot be confirmed via Android codec capability inspection. During singing, video playback failure or a runtime gameplay-degradation report MUST disable decorative video for the current song and fall back to static background while audio continues unaffected. Iteration 1 MUST NOT test dropped decorative-video frames; gameplay degradation checks apply to gameplay signals such as future pitch-frame/render quality, not decorative video frame drops.
- **FR-039**: The Singing screen MUST show the minimum content adjusted for Iteration 1 scope: sentence-paged lyrics with progressive highlight, static note bars from the parsed song file, constant `00000` score placeholder, elapsed time, and song title/artist. Live pitch cursor, live score updates, hit/miss feedback, sentence rating calculation, and score calculation are out of scope.
- **FR-040**: For Iteration 1, the pitch lane MUST draw static note lanes from the parsed song file only. Lane rendering MUST use a separate `SurfaceView`-backed renderer behind Compose overlays; Compose MUST NOT own pitch-lane frame rendering. Static note geometry MUST use the parsed song's lyrics-time axis: file beat values are used as-authored, `BPM_internal = BPM_file * 4`, note activity uses `[startBeat, endBeat)`, horizontal positions come from `HorizontalTimeMapping`, vertical positions come from `VerticalPitchMapping`, and difficulty MUST NOT change lane height or coordinate-system bounds. Static note target vertical thickness MUST reflect selected difficulty as Easy = ±2 semitones, Medium = ±1 semitone, and Hard = ±0 semitones around the target pitch. The static lane MUST also render instrumental/rest gap indicators derived from the parsed note schedule for regions where the current singer has no scorable notes for more than 2 continuous seconds; the indicator is visual only and has no effect on audio or scoring. Live pitch cursor and hit/miss feedback are deferred with live pitch/scoring work.
- **FR-041**: For Iteration 1, the score component MUST render in the screen layout and display constant `00000` until scoring is wired in.
- **FR-042**: During active solo singing, the screen MUST show exactly one centered lane band at `SingingSingleLaneHeight` (192dp), full width, vertically centered on screen. Lane bodies remain neutral; P1 accent appears only on badge, score-box accents, and note markers, not as a full-lane tint.
- **FR-043**: Lyrics MUST render in the bottom lyrics band only and MUST always show exactly two lines: current line and next line. The app MUST NOT render a third line.
- **FR-044**: Lyrics MUST use sentence-based paging, remain spatially stable during a sentence, and during instrumental gaps the completed sentence MUST remain at 100% highlight rather than pre-paging or showing blank. Lyrics highlight timing MUST use the lyrics beat with `micDelayMs=0`, while static lane target geometry uses the lane/note timing axis derived from the same parsed chart. Implementations MAY render active highlight as a clipped reveal over an inactive base text pass.
- **FR-045**: The top metadata MUST start in the intro strip at `SingingTopIntroStripHeight` and collapse during active singing to the minimal strip at `SingingTopMinimalStripHeight`.
- **FR-046**: Elapsed time MUST be shown bottom-right in `MM:SS` zero-padded format.
- **FR-047**: Pressing Back during singing MUST open the Pause overlay with `Resume`, `Restart Song`, and `Quit to Song List`.
- **FR-048**: `Resume` MUST resume playback from the current position.
- **FR-049**: `Restart Song` MUST confirm with default focus on Cancel; on OK it MUST reset per-player scores, create a new `songInstanceSeq`, re-send `assignSinger`, and restart the normal song from `startSec`.
- **FR-050**: `Quit to Song List` MUST confirm with default focus on Cancel and on OK MUST stop playback, send session state that removes phones from in-song mode, return to Song List, and leave the session Open.
- **FR-051**: Pause, Restart confirmation, Quit confirmation, Countdown disconnect, Start-failure / Playback error, and Select-Players no-phone states MUST reuse the same centered elevated interruption shell.
- **FR-052**: The interruption shell MUST use `SurfaceLevel2` with `PrimaryModalPadding` (32dp), dim the underlying scene with a dark scrim, MUST NOT use runtime blur, and MUST keep focus movement disabled until entry completes. Destructive or irreversible actions such as Restart and Quit default focus to Cancel.
- **FR-053**: The UI layer MUST register a single event listener on the audio `LibVlcPlayerHandle` before calling `play()` and MUST compute `songStartTvMs = (System.nanoTime() / 1_000_000) − playerHandle.timeMs` on the first `LibVlcEvent.Playing` after `play()`.
- **FR-054**: The UI layer MUST capture `fallbackStartTvMs` at the moment `play()` is called and MUST emit `PlaybackEvent.Ready(fallbackStartTvMs)` if `LibVlcEvent.Playing` has not fired within 500 ms.
- **FR-055**: The coordinator MUST wait for `PlaybackEvent.Ready(songStartTvMs)` before calling the scoring start seam. Because scoring is out of Iteration 1, the seam remains a no-op placeholder but the ordering contract MUST still be represented and tested.
- **FR-056**: `PlaybackIntent.Play` MUST carry the finalized `stopAtLyricsTimeMs`. The UI MUST enforce that value as the authoritative playback stop boundary, call `LibVlcPlayerHandle.stop()` when reached, and emit `PlaybackEvent.Ended`.
- **FR-057**: On song end in Iteration 1, the app MUST return to Song List. Results screen and score finalization are out of scope for this iteration. The Iteration 1 host phase transition for normal song completion is `Live` to `Open` after `PlaybackEvent.Ended`, with session state returned to Open and navigation returned to Song List.
- **FR-058**: Pitch-frame ingestion and pitch-frame-driven behavior are out of scope for Iteration 1; later scoring/pitch-pipeline work MUST handle ignoring pitch frames at or beyond `stopAtLyricsTimeMs`.
- **FR-059**: On `LibVlcEvent.EncounteredError` from the authoritative audio player during singing, the app MUST stop playback immediately, return to Song List, and show blocking `ERROR` modal whose second body line is the last LibVLC warning/error log line truncated to 120 chars when available. `LibVlcEvent.EncounteredError` from the decorative video player MUST be logged and converted to static-background fallback, not the blocking playback-error modal.
- **FR-060**: The LibVLC adapter MUST maintain the most recent warning/error diagnostic line from LibVLC logging, truncated to 120 characters, and include that value in audio `PlaybackEvent.Error` / `LibVlcEvent.EncounteredError` payloads used by the blocking error modal.
- **FR-061**: A playback error MUST NOT crash the app, corrupt session state, or leave the session Locked; session returns to Open on error exit.
- **FR-062**: The UI layer MUST request `AUDIOFOCUS_GAIN` before playback, pause on transient losses, resume on `AUDIOFOCUS_GAIN`, follow the playback error path on permanent loss, and abandon audio focus on song end, error exit, or Restart.
- **FR-063**: App-wide UI shell MUST consist of `MainActivity`, `CouchraokeTheme`, and `AppNavHost`, with `MainActivity` as the single Android Activity and `setContent {}` as the only theme/navigation host instantiation site. No other file may create a nested app theme or navigation host.
- **FR-064**: Navigation MUST use `AppNavHost` routes for `SongList`, `SelectPlayers`, `Singing`, and inert `Results`, while `SelectPlayersModal` and `JoinOverlay` remain modal composables shown within `SongListScreen`, not separate navigation destinations. Settings MUST remain a visible inert/no-op header affordance in Iteration 1 and MUST NOT open a Settings route, menu, screen, or submenu. Iteration 1 MUST keep out-of-scope visible routes or affordances inert with code comments naming the intended wiring iteration.
- **FR-065**: Back handling MUST use TV remote Back semantics: overlays close without affecting the navigation stack; Song List grid/left-panel Back moves focus to Search; Song List top-control Back clears the filter when active and otherwise exits the app; Singing Back opens Pause; Results Back returns to Song List.
- **FR-066**: DPAD focus MUST follow the Song List focus model: initial focus first grid tile or Search when empty; from leftmost grid column, left-panel entry targets first Medley row if present, then Play Medley if empty, then Random Medley fallback; disabled elements MUST NOT be focusable.
- **FR-067**: Focus treatment MUST be border-plus-plate only: `FocusBorderWidth` 3dp solid `BorderFocus`, inset `FocusBorderInset` 2dp, plus a subtle filled plate. Unfocused enabled elements use `BorderThin` 1dp `BorderSubtle` at 20% opacity. Focus MUST NOT use player accent colors, reward gold, shadows, blur, elevation changes, glow, scale, or background pulse.
- **FR-068**: The feature MUST satisfy the Definition of Done items in scope for Iteration 1 solo sing: F15-style session lifecycle coverage, minimal song-start clock-sync gate test pass, F22-style GamePhase FSM coverage, actual Join QR rendering with short/full payload sizing coverage, and cumulative TV-owned end-to-end flow through the TV UI. Prefer JVM, Robolectric, and direct WebSocket/UDP or component contract tests for these gates; use emulator/device validation only for behavior that depends on Android TV runtime integration and cannot be meaningfully validated at lower cost.

### Key Entities *(include if feature involves data)*

- **ConnectedPhone**: TV-owned roster entry with `clientId`, `connectionId`, `deviceName`, `httpPort`, and `ipAddress`.
- **SongEntry**: A manifest-provided song record used for Song List display and start handoff. Iteration 1 fields are `relativeTxtPath`, `modifiedTimeMs`, `title`, `artist`, optional `album`, optional `year`, optional `genre`, `isDuet`, `hasRap`, `hasVideo`, `hasInstrumental`, `canMedley`, optional `medleySource`, optional `medleyStartBeat`, optional `medleyEndBeat`, `startSec`, `previewStartSec`, `txtUrl`, `audioUrl`, optional `videoUrl`, optional `coverUrl`, and optional `backgroundUrl`.
- **IndexedSong**: TV-side library entry keyed by `phoneClientId + "::" + relativeTxtPath`; derived from one manifest entry and used by Song List, Select Players, playback start, and Singing render-model construction.
- **SessionToken / JoinCode**: One random token represented as the short join code, WebSocket `token` query parameter, QR payload token, and mDNS TXT `code` value.
- **PreviewPlayback**: Song List screen-scoped LibVLC preview state for the focused song's `audioUrl`, start offset, debounce state, silent failure handling, and teardown on Song List exit.
- **PlaybackEvent**: UI-to-coordinator playback events in the extracted flow: `Prepared(effectivePlaybackDurationMs)`, `Ready(songStartTvMs)`, `Error(cause)`, and `Ended`.
- **PlaybackIntent**: Coordinator-to-UI playback commands used by the extracted flow: `Prepare(audioUrl, videoUrl, videoGapSec, seekToSec, chartEndLyricsTimeMs)`, `Play(stopAtLyricsTimeMs)`, `Pause`, `Stop`, and `Seek`; medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` may exist in the shared contract but MUST remain inert/no-op in Iteration 1 with Iteration 4 wiring comments.
- **LibVLC player handles**: UI/playback-owned wrappers where the audio handle is authoritative for preparation duration, timing, stop-boundary enforcement, and blocking audio errors, while optional preview/video handles are screen-scoped and never own Singing timing/session state.
- **SelectPlayers selection**: The TV-owned assignment state for chosen singer phone, difficulty, `playerId=P1`, and countdown mode used when sending `assignSinger`.
- **GamePhase**: Host-owned phase state for Iteration 1: Open, Preparing, Countdown, Live, Paused, and Error. Results may exist as an inert route only; normal song end transitions from Live to Open and returns to Song List.
- **SingingRenderModel**: Immutable chart-derived UI model containing title/artist, sentence-paged lyrics, static note targets, horizontal lyrics-time mapping, vertical pitch mapping, lane layout data, score placeholder state, and optional background/video metadata.
- **Interruption overlay shell**: The shared modal surface reused by pause, restart/quit confirms, countdown disconnect, playback error, start failure, and no-phone states.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the Iteration 1 end-to-end TV flow, the host can complete launch app → pair one phone → load phone manifest → select one song → start playback with lyrics → return to Song List without leaving the TV UI.
- **SC-002**: In solo playback, the Singing screen consistently shows one centered singer lane, elapsed time, static note lanes from the song file, constant `00000` score, and exactly two lyrics lines for the full duration of the song.
- **SC-003**: In song starts where playback succeeds, the UI reports prepared playback duration before playback begins and reports a playback-ready start time before scoring is allowed to begin.
- **SC-004**: At normal song end, the app returns to Song List without showing Results.
- **SC-005**: In countdown-disconnect, start-failure, and playback-error cases, the app returns the host to a defined recovery destination with a blocking modal and does not remain stuck in an in-song locked state.
- **SC-006**: The in-scope validation gates for Iteration 1 pass: F15 session lifecycle, minimal song-start clock-sync gate coverage, F22 GamePhase FSM, actual Join QR rendering with short/full payload sizing coverage, and one end-to-end solo-sing flow validation at the lowest practical test layer. Emulator/device validation is required only for Android TV runtime behavior that cannot be covered by JVM, Robolectric, or direct component/contract tests.

## Assumptions

- Iteration 1 scope is restricted to solo-sing playback for one phone and one player; duet and medley controls from shared screens remain visible but disabled.
- Scoring remains out of Iteration 1; displayed scores stay at zero until a later scoring task wires real scoring.
- Results screen is out of scope for Iteration 1; normal song completion returns to Song List.
- Live pitch drawing from pitch frames is out of scope for Iteration 1. The pitch lane draws static note targets from the parsed song file only.
- Join QR rendering uses the QR-only QRose artifact (`io.github.alexzhirkevich:qrose`), not `qrose-oned`, with an explicit implementation contract for working on-screen Compose painter rendering so implementers do not need to infer library usage details.
- Preview and playback volume are controlled by the TV/system media volume in Iteration 1; the app does not add a preview preamp or depend on Settings > Audio.
- Optional video is best-effort full-screen background media; audio is the required playable asset and video/background failure falls back to a static background. Hardware-specific acceptance is not an Iteration 1 gate, but the app still disables optional video when decoder support cannot be confirmed or the decorative video path fails. Dropped-frame degradation checks are only for gameplay pitch frames in future iterations, not decorative video frames.
- Visible out-of-scope no-ops carry code comments naming the target wiring iteration: Iteration 2 for scoring/live pitch/Results, Iteration 3 for duet/two-player/settings, and Iteration 4 for medley execution/prebuffer/crossfade.
- The app continues to treat the TV as the authoritative host, the phone as the source of manifest/audio/chart assets, and the flow as LAN-only with no cloud dependencies.
- This specification includes shared screen structure and wireframes where those structures are visible in the solo-sing path, even when the disabled visible controls belong to later iterations.
- This specification overrides any broader-product behavior that would route Iteration 1 users into Settings, Results, live scoring, duet execution, or medley execution.

## Visual System and UI Tokens

These tokens and rules are part of Iteration 1 acceptance whenever the referenced UI surface is visible.

### Design Intent and Performance

Visual priorities, in order:

1. gameplay readability during singing
2. stable remote navigation and focus clarity
3. song recognition from the browse grid
4. restrained motion that does not compete with playback, navigation, or video decode

The app uses flat rendering only. Runtime blur, bloom, glow, frosted glass, shader-heavy full-screen effects, gameplay particles, and background animation that repaints large parts of the screen during active singing are forbidden.

### Spacing, Radius, Border, and Layout Tokens

| Token | Value |
|---|---:|
| Space8 | 8dp |
| Space12 | 12dp |
| Space16 | 16dp |
| Space24 | 24dp |
| Space32 | 32dp |
| Space48 | 48dp |
| RadiusSmall | 8dp |
| RadiusMedium | 12dp |
| RadiusLarge | 16dp |
| BorderThin | 1dp |
| FocusBorderWidth | 3dp |
| FocusBorderInset | 2dp |
| UnfocusedBorderOpacity | 20% |
| FocusInDuration | 150ms |
| FocusOutDuration | 100ms |
| AppMarginHorizontal | 48dp |
| AppMarginVertical | 36dp |
| HeaderHeight | 76dp |
| StandardButtonHeight | 72dp |
| StandardRowHeight | 76dp |
| DenseRowHeight | 56dp |
| PrimaryModalWidth | 960dp |
| PrimaryModalPadding | 32dp |
| QRCodeSize | 400dp |
| InterruptionModalWidth | 960dp |
| InterruptionModalTitleBottomGap | 16dp |
| InterruptionModalBodyBottomGap | 24dp |
| InterruptionActionRowHeight | 72dp |

### Typography

Two faces are allowed:

- **Display face**: decorative squared face for hero numerals and hero titles only.
- **Operational sans**: high-legibility sans-serif for all other text.

| Token | Value |
|---|---:|
| DisplayHeroNumber | 160sp |
| DisplayHeroTitle | 56sp |
| DisplayAccentTitle | 44sp |
| ScreenTitle | 40sp |
| SectionTitle | 32sp |
| PanelTitle | 28sp |
| SongCardTitle | 24sp |
| SongCardArtistFocused | 18sp |
| PreviewTitle | 32sp |
| PreviewArtist | 24sp |
| TagChipLabel | 16sp |
| BodyPrimary | 24sp |
| BodySecondary | 20sp |
| ButtonLabel | 22sp |
| FieldLabel | 20sp |
| Caption | 18sp |
| LyricsCurrent | 40sp |
| LyricsNext | 32sp |
| LiveScore | 56sp |
| SentenceRating | 28sp |
| TopMetadataMinimal | 20sp |
| SingerBadge | 22sp |
| Timer | 24sp |

### Color and Surface Roles

| Role | Use |
|---|---|
| AppBackground | darkest cool graphite base |
| SurfacePrimary | standard dark surface |
| SurfaceElevated | elevated modal and overlay surface |
| SurfaceLaneBand | dark translucent graphite lane plate |
| SurfaceLyricsBand | darkest overlay plate on singing screen |
| BorderSubtle | unfocused structure border |
| BorderFocus | focused border |
| TextPrimary | highest-priority text |
| TextSecondary | secondary metadata |
| TextDisabled | disabled state |
| Player1Accent | cyan; P1 identity accents only |
| Player2Accent | magenta; future P2 identity accents only |
| RewardAccent | gold; reserved for future reward/result treatment only |
| Success | success state |
| Warning | warning state |
| Error | error state |

| Surface token | Use | Alpha |
|---|---|---:|
| SurfaceLevel0 | app background | — |
| SurfaceLevel1 | standard cards, rows, panels | — |
| SurfaceLevel2 | modal, pause, disconnect, error, and similar interruption surfaces | — |
| LaneBandAlpha | lane band over video/background | 68% |
| LyricsBandAlpha | lyrics band over video/background | 82% |

Player accent colors MUST NOT be used for generic focus border, general app chrome, default buttons, lane-body fill, or `Start` emphasis. `RewardAccent` MUST NOT be used for focus, `Start`, general buttons, or medley controls.

### Motion Budget

| Screen / state | Budget | Allowed pattern |
|---|---|---|
| Song List, settled | V2 | local preview crossfade and restrained chip or border fade only |
| Song List, active navigation | V1 | focus and state motion only |
| Join / QR overlay | V1 | modal entrance, then static |
| Select Players | V1 | focus and short row transitions |
| Loading / pre-song setup | V0 | static poster or simple progress only |
| Countdown | V2 | one hero number animation |
| Singing | V0 | functional lyric-highlight and static note-lane motion only |
| Pause / Disconnect / Error overlay | V1 | modal entrance and focus only |

### Song List Tokens

| Token | Value |
|---|---:|
| SongListLeftRailFraction | 0.34 |
| SongListGridFraction | 0.66 |
| SongListRailGridGap | 32dp |
| SongListHeaderToBodyGap | 24dp |
| SongListRandomRowHeight | 72dp |
| SongListRandomRowGap | 24dp |
| SongListGridColumns1080 | 3 |
| SongListGridColumns4K | 4 |
| SongListGridColumnGap | 24dp |
| SongListGridRowGap | 24dp |
| SongListPreviewAspect | 16:9 |
| SongListPreviewToMetaGap | 16dp |
| SongListMetaToPlaylistGap | 24dp |
| SongListPlaylistRowHeight | 52dp |
| SongListPlaylistVisibleRows | 5 |
| SongListPlayMedleyTopGap | 16dp |
| SongCardHeight | 252dp |
| SongCardPadding | 12dp |
| SongCardImageHeight | 148dp |
| SongCardImageCornerRadius | 8dp |
| SongCardTitleMaxLines | 2 |
| SongCardFocusedArtistSlotHeight | 20dp |
| SongCardTitleToArtistGap | 4dp |
| SongCardTagCornerInset | 8dp |
| SongCardTagGap | 6dp |
| SongCardMaxVisibleTags | 3 |

### Select Players and Join Tokens

| Token | Value |
|---|---:|
| SelectPlayersPanelWidth | 960dp |
| SelectPlayersSectionGap | 32dp |
| SelectPlayersFieldRowHeight | 76dp |
| SelectPlayersActionRowGap | 24dp |
| JoinPanelWidth | 960dp |
| JoinQRCodeSize | 400dp |
| JoinCodeTopGap | 16dp |
| JoinConnectedRowHeight | 56dp |

### Singing Tokens

| Token | Value |
|---|---:|
| SingingTopIntroStripHeight | 72dp |
| SingingTopMinimalStripHeight | 40dp |
| SingingBottomLyricsBandHeight | 160dp |
| SingingBodyToLyricsGap | 16dp |
| SingingSingleLaneHeight | 192dp |
| SingingSingleLaneVerticalPosition | centered |
| SingingLaneHorizontalPadding | 20dp |
| SingingLaneVerticalPadding | 16dp |
| SingingScoreBoxWidth | 144dp |
| SingingScoreBoxHeight | 88dp |
| SingingScoreBoxRightInset | 16dp |
| SingingScoreBoxToRatingGap | 8dp |
| SingingBadgeHeight | 40dp |
| SingingBadgeTopInset | 8dp |
| LyricsBandPaddingHorizontal | 24dp |
| LyricsBandPaddingTop | 20dp |
| LyricsBandLineGap | 8dp |

### Design Conformance Acceptance

A build conforms to this visual system when all visible Iteration 1 UI uses the tokens above, Song List preserves block placement and card/rail/action rules, Singing uses the fixed one-lane state, lyrics remain bottom-banded and two-line only, no prohibited glow/blur/bloom/particle effects appear, and no decorative effect interferes with singing smoothness or focus responsiveness.

## UI Wireframes

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
|                     Hints: OK=Select   Back=Search/Exit   Medley controls disabled in Iteration 1      |
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
|                                                                | 00000  |      |
|                                                                +--------+      |
|                                                                  (reserved)    |
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

## Interaction Contracts

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
NetworkCtrl  UsdxParser    Scoring seam       UI Layer
fetchTxt()   parse()       no-op placeholder  Prepare()
    │           │              │            │
    │           │              │            │
    └─────┬─────┘              │            │
          ▼                    │            │
    ParsedSong ────────────────┘            │
                                            │
    ┌───────────────────────────────────────────┘
    ▼
PlaybackEvent.Prepared(durationMs)
    │
    ▼
Clock-sync gate: at least one valid sample for selected singer
    │
    ▼
NetworkCtrl.sendAssignSinger()
NetworkCtrl.broadcastPlaybackState()
    │
    ▼
UI.Play(stopAtLyricsTimeMs) ──→ LibVLC starts ──→ PlaybackEvent.Ready(songStartTvMs)
                                                        │
                                                        ▼
                                            scoring start seam remains no-op
```

### PlaybackCoordinator ↔ UI Layer

- Coordinator emits `PlaybackIntent` (Prepare, Play, Pause, etc.).
- UI/playback layer observes intents and executes them through one authoritative audio `LibVlcPlayerHandle` plus an optional best-effort decorative full-screen video/background handle.
- `Prepare` carries `audioUrl`, optional `videoUrl`, optional `videoGapSec`, `seekToSec`, and optional `chartEndLyricsTimeMs`.
- `Play` carries finalized `stopAtLyricsTimeMs`.
- UI/playback emits `Prepared(effectivePlaybackDurationMs)` from the prepared audio handle duration. `#START` only changes the initial seek and does not shift the lyrics-time origin; `#END > 0` can still provide the stop boundary, but when `#END` is absent or non-positive the audio duration is required.
- UI/playback emits `Ready(songStartTvMs)` only from the first authoritative audio `Playing` event or the 500 ms fallback captured at `play()` time. Video player events MUST NOT emit `Ready` or affect `songStartTvMs`.
- UI is responsible for enforcing the active `stopAtLyricsTimeMs` boundary on the audio handle using the current playback plan.
- When UI detects that playback has reached the active `stopAtLyricsTimeMs`, it MUST stop the active audio handle and any decorative video/background handle, then emit `PlaybackEvent.Ended`.
- UI emits `PlaybackEvent` (`Prepared` with effective playback-plan duration, `Ready` with songStartTvMs, `Error`, `Ended`).
- UI exposes `currentPositionMs: StateFlow<Long>` for observation; this is always the audio handle position.
- The singing visual stack is ordered back-to-front as: optional full-screen video/background `SurfaceView`, pitch-lane rendering `SurfaceView`, then Compose metadata/score/lyrics/countdown/pause overlays. Full-screen video uses `SurfaceView.setZOrderMediaOverlay(true)` and not `TextureView`; pitch-lane drawing remains a separate SurfaceView-backed renderer so Compose does not own frame rendering.
- LibVLC warning/error diagnostics are captured by the LibVLC adapter and surfaced through audio error payloads for the blocking error modal; decorative video errors only drive static-background fallback.
