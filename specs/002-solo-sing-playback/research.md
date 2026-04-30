# Research: Solo Sing Playback

## Decision: Preserve Iteration 1 scope as solo browse/play only

**Rationale**: `tv_app.md` §7.3 defines Iteration 1 as `browse library → select song → play audio with lyrics`, with DOD items for phone discovery, manifest display, song selection, audio playback, sentence-paged lyrics, Back return to Song List, F15, minimal clock-sync gate, F22, and emulator run. `tv_app.md` §7.4 explicitly assigns pitch frames, live cursor, live score display, and Results to Iteration 2.

**Alternatives considered**:
- Implement full SingingScreen minimum content including live pitch and score: rejected because it contradicts §7.4 allocation and expands scope.
- Implement Results because §4.1 full FSM includes Results: rejected for Iteration 1 because §7.3 DOD returns to Song List and §7.4 owns Results.

## Decision: Draw static note lanes from parsed song file only

**Rationale**: Iteration 1 includes Playback UI/SingingScreen and chart parsing for lyrics/note visuals, but does not include UDP pitch-frame ingestion or scoring pipeline. The simplest complete slice is to build `SingingRenderModel` from `ParsedSong` and render static note targets against current lyrics-time position. No live pitch cursor, hit/miss feedback, or pitch-frame-driven behavior is implemented.

**Alternatives considered**:
- Draw received live pitch from pitch frames: rejected because pitch frames flow and live cursor are Iteration 2 in §7.4.
- Hide note lanes entirely: rejected because the SingingScreen wireframe and playback UI expect a lane region with note bars.

## Decision: Return to Song List on normal song completion

**Rationale**: Iteration 1 DOD says the cumulative flow handles Back and returns to Song List, and Results is listed as an Iteration 2 deliverable. Returning to Song List on `PlaybackEvent.Ended` is the smallest behavior that completes the solo playback loop without introducing score finalization or Results UI.

**Alternatives considered**:
- Route `Stopped → Results` with zero scores: rejected because Results screen is Iteration 2 and would add UI/test scope.
- Show a song-complete modal: rejected because it is not in the extracted Iteration 1 spec and adds a new behavior.

## Decision: Use two LibVLC players for Singing songs with video

**Rationale**: The source playback contract defines audio as the master LibVLC playback handle and video as an optional LibVLC decoration. Keeping two independent LibVLC handles preserves audio timing authority, allows `songStartTvMs`, `Prepared`, `currentPositionMs`, and `stopAtLyricsTimeMs` to come from audio only, and lets video failure degrade to a static background without disrupting playback/session state.

**How to apply**:
- Prepare `audioUrl` on the authoritative audio handle and optional `videoUrl` on a separate video handle.
- Configure the video handle without audio so it cannot contend for audio focus.
- Emit `Prepared(effectivePlaybackDurationMs)` from the audio handle duration; if no usable duration exists and `#END` cannot define the stop boundary, preparation fails through the playback error path.
- Emit `Ready(songStartTvMs)` only from the first audio `Playing` event or the 500 ms fallback captured at `play()` time.
- Log video errors and fall back to static background; audio errors use the blocking playback error modal.
- Compose Singing visually as video/background SurfaceView behind pitch-lane SurfaceView behind Compose overlays.

**Alternatives considered**:
- Use one LibVLC player for video+audio: rejected because it makes video part of the authoritative playback path and weakens non-fatal video fallback.
- Ignore optional video in Iteration 1: rejected because optional best-effort video is in the extracted solo-sing path.

## Decision: Keep duet/medley controls visible but disabled

**Rationale**: Shared Song List and Select Players sections include duet/medley affordances, but Iteration 1 is one phone/one player solo sing. Keeping controls visible preserves layout and wireframe compatibility while disabling execution prevents scope creep.

**Alternatives considered**:
- Hide all duet/medley controls: rejected because it would diverge more from extracted shared-screen wireframes.
- Leave controls enabled with “Coming soon”: rejected because enabled controls imply interaction and extra modal behavior not needed for Iteration 1.

## Decision: Use LibVLC for Song List preview and Singing playback

**Rationale**: Singing playback already uses LibVLC for streamed audio plus optional decorative video. Song List preview uses the same backend so the TV has one media stack and broad codec behavior for USDX libraries, including older songs with legacy codecs where Media3 may be less reliable.

**How to apply**:
- Preview is screen-scoped to Song List and tears down on Song List exit.
- Preview starts after the focused tile remains stable for 500 ms, uses manifest `audioUrl`, and seeks to positive `previewStartSec` or 0 seconds when absent/non-positive.
- Preview stops immediately when focus changes, focus leaves the grid, an overlay/modal/settings/singing opens, or Song List exits.
- Preview HTTP/player failures are silent and preview volume follows TV/system media volume only in Iteration 1.

**Alternatives considered**:
- Use Media3 for preview: rejected because it splits the media stack and reduces confidence for legacy-codec preview parity with Singing playback.
- Use a fixed 10-second preview limit: rejected because the source behavior plays until stopped.

## Decision: Use QRose for Join QR rendering

**Rationale**: QRose provides a documented Compose painter API for on-screen rendering (`rememberQrCodePainter`) that fits this feature's needs better than qrcode-kotlin. The app needs a working 400dp Join overlay QR and explicit renderer contract for implementers.

**How to apply**:
- Add `implementation("io.github.alexzhirkevich:qrose:1.1.2")` through `gradle/libs.versions.toml` and `app/build.gradle.kts`; do not add `qrose-oned`, because this feature needs QR only.
- The baseline Compose usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`.
- In the Join overlay UI, keep that painter inside a fixed 400dp box and pass explicit QR options: `colors { dark = QrBrush.solid(Color.Black); light = QrBrush.solid(Color.White) }`, `errorCorrectionLevel = QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale`.
- Render the QRose painter directly in the Join overlay rather than requiring PNG byte export.
- Treat QRose `scale` as the implementation-controlled padding knob that keeps the actual QR content centered inside the requested square while preserving the required quiet zone.

**Alternatives considered**:
- Keep qrcode-kotlin and continue working around fixed-size rendering instability: rejected because the current direction is to replace it with a better-documented library.
- Add navigation/Hilt/DataStore immediately: rejected for this plan because current catalog/build file does not declare them and the simplest Iteration 1 implementation can use existing dependencies unless later implementation discovers a hard blocker that requires a plan amendment.

## Decision: Implement minimal clock-sync gate for selected singer before start

**Rationale**: §7.3 DOD requires one valid clock-sync sample for every assigned singer before countdown or live playback, with full F21 coverage deferred to Iteration 2. The coordinator should call the network seam to obtain a valid sample and block Start on failure.

**Alternatives considered**:
- Skip clock sync in Iteration 1 because scoring is out of scope: rejected because §7.3 explicitly includes the minimal gate.
- Implement full clock-sync fixture coverage: rejected because §7.3 defers full F21 coverage to Iteration 2.

## Decision: Keep UDP pitch transport contract documented but do not process pitch frames

**Rationale**: The constitution requires fixed-size UDP pitch transport assumptions to be preserved for touched flows. Iteration 1 sends `assignSinger.udpPort` and reserves the transport contract, but pitch-frame ingestion, validation, jitter buffer, live cursor, and scoring are out of scope.

**Alternatives considered**:
- Implement UDP listener and drop all frames: rejected unless needed by tests, because §7.4 owns UDP listener and frame validation.
- Omit UDP references from `assignSinger`: rejected because the wire schema requires `udpPort`.
