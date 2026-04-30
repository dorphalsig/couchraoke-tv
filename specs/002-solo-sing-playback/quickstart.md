# Quickstart: Solo Sing Playback

## Scope Guard

Implement only Iteration 1 solo-sing playback:
- one phone
- one selected P1 singer
- browse manifest songs
- select a non-duet song
- stream audio/video from phone URL
- show sentence-paged lyrics
- draw static note lanes from parsed song file
- constant `00000` score placeholder
- return to Song List on normal end

Do not implement:
- live pitch from UDP frames
- UDP pitch-frame validation/processing
- scoring, line bonus, result totals
- Results screen
- duet execution
- medley execution

## Suggested Implementation Order

1. **Network/session foundation**
   - Create network contracts/models.
   - Add token-gated WebSocket `hello` / `sessionState` path.
   - Add jmDNS advertisement and multicast lock lifecycle.
   - Add HTTP manifest and TXT fetch helpers.
   - Validate F15-style session lifecycle behavior.

2. **Manifest aggregation and Song List state**
   - Implement manifest-to-`IndexedSong` aggregation.
   - Display Song List with header, Join, inert/no-op Settings affordance, disabled duet/medley controls, empty states, search, and song grid.
   - Render Join overlay QR/join-code state.
   - Keep Settings from opening any route, menu, screen, or submenu in Iteration 1.

3. **Select Players modal**
   - Implement non-duet one-player selection.
   - Show no-phone state and an action that opens the same Join QR overlay as the Song List Join button.
   - Keep Player 2 visible but disabled for non-duet songs.

4. **PlaybackCoordinator and FSM subset**
   - Implement `Idle`, `Loading`, `Countdown`, `Playing`, `Paused`, `DisconnectPaused`.
   - Fetch TXT, parse chart, build render model, prepare playback.
   - Obtain one valid clock-sync sample for selected singer.
   - Send `assignSinger` and `playbackState` from coordinator only.
   - Return to Song List on `PlaybackEvent.Ended`.

5. **LibVLC playback seam**
   - Implement `PlaybackIntent` / `PlaybackEvent` flow.
   - Implement LibVLC-backed Song List preview with 500ms focus debounce, `audioUrl`/offset fallback, immediate stop conditions, silent failure, and screen-scoped teardown.
   - Capture `Prepared` duration from the authoritative audio handle before countdown/live playback.
   - Capture `Ready(songStartTvMs)` from first audio `Playing`, with 500ms fallback; decorative video must never drive timing/session events.
   - Enforce `stopAtLyricsTimeMs` and emit `Ended`.
   - Implement two-handle Singing playback for songs with video: required audio plus optional no-audio decorative video.
   - Disable optional video at load time when video is >720p and hardware decoder support cannot be confirmed; expose a runtime gameplay-degradation report path that disables decorative video during singing, falls back to static background, and optionally shows a non-blocking notice without stopping audio.
   - Implement audio-focus request/loss behavior and LibVLC warning/error diagnostic plumbing for the error modal.

6. **Singing screen rendering**
   - Build `SingingRenderModel` from `IndexedSong` + `ParsedSong`.
   - Render one centered P1 static note lane.
   - Render current/next lyrics only, elapsed timer, title/artist, and constant `00000` score.
   - Implement countdown, pause, restart confirm, quit confirm, and countdown-disconnect modal states.

7. **UI verification**
   - Add previews for screen/modal states matching wireframes.
   - Compare screenshots/previews to spec: Song List, Join, Select Players, no phones, Singing, countdown, pause, restart/quit confirm, error modal.

## Validation

Use scoped `testBranch` for each task and the final feature. Final feature validation should use selectors matching the implemented production and test classes, based on the plan’s Validation Gate, and must explicitly include F15-style session lifecycle coverage, minimal song-start clock-sync gate coverage, F22 GamePhase FSM coverage, actual Join QR renderer short/full payload sizing coverage, Song List LibVLC preview coverage, audio-duration Prepared/Ready/stop-boundary coverage, optional-video static admission/fallback/gameplay-degradation report coverage, emulator execution, and the cumulative TV-owned end-to-end flow through the TV UI.

Command shape:

```bash
timeout 10m ./gradlew :app:testBranch \
  --src <changed production FQCNs> \
  --test <changed test FQCNs>
```

For UI tasks, also verify rendered screens against spec wireframes before claiming completion.

## Key Acceptance Checks

- App discovers phone via mDNS and completes token-gated handshake.
- Song List displays songs from phone manifest.
- Song List preview starts after 500ms same-tile focus, uses the focused song `audioUrl` and offset fallback rules, stops on focus/grid/overlay/singing transitions, fails silently, and tears down with Song List.
- QR encodes full WebSocket URL with token and short/full endpoint payloads render centered inside the requested square with at least a 4-module quiet zone.
- QRose usage is explicit and aligned across UI and tests: use only the QR artifact (`io.github.alexzhirkevich:qrose`), not `qrose-oned`; the baseline Compose usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`; the Join overlay uses black/white, `QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale` options with direct painter rendering. PNG byte export is not required.
- Settings does not open any route, menu, screen, or submenu in Iteration 1.
- Select Players no-phone action opens the same Join QR overlay as the Song List Join button.
- Select song -> Select Players -> selected P1 phone -> playback starts.
- UI emits `Prepared` from the audio handle duration before countdown/live playback.
- UI emits `Ready(songStartTvMs)` from audio only and coordinator gates `ScoringEngine.setSongStart` on it.
- UI enforces `stopAtLyricsTimeMs` and returns to Song List on normal end.
- Optional video uses a separate no-audio decorative LibVLC handle, falls back to static background on failure, is disabled for >720p without confirmed hardware decoding, and can be disabled by a runtime gameplay-degradation report with optional non-blocking notice.
- Playback error returns to Song List with blocking modal and session Open, including the latest LibVLC warning/error diagnostic line when available.
- Back during singing opens Pause; Resume continues from the current position, Restart uses a new `songInstanceSeq` and restarts from `startSec`, and Quit returns to Song List.
- Static note lanes are drawn from song file; no live pitch from pitch frames.
- Score display remains `00000`.
- Duet/medley controls are visible but disabled.
