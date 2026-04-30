# Implementation Plan: Solo Sing Playback

**Branch**: `002-solo-sing-playback` | **Date**: 2026-04-29 | **Spec**: `/home/paavum/Couchraoke/tv/specs/002-solo-sing-playback/spec.md`
**Input**: Feature specification from `/specs/002-solo-sing-playback/spec.md`

## Summary

Deliver Iteration 1 solo-sing playback for the TV host app: one phone can join over LAN, the TV aggregates that phone's manifest into Song List, the host selects one non-duet song and one singer, the TV fetches and parses the song file, prepares streamed audio/video playback, displays sentence-paged lyrics plus static note lanes from the parsed song file, and returns to Song List on end, Back/Quit, or playback failure. The plan intentionally keeps live pitch from UDP frames, scoring, duet execution, medley execution, and Results out of scope for this iteration.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11  
**Primary Dependencies**: AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle Runtime, Kotlin Coroutines, Kotlinx Serialization JSON, Ktor server/websockets/client, jmDNS, LibVLC, Coil, QRose (`io.github.alexzhirkevich:qrose:1.1.2`), JUnit 4, quality-conventions `testBranch`
**Storage**: DataStore preferences only if needed for existing settings defaults; no persistence for remote song assets; manifest/chart/audio/video assets are streamed from phone-provided LAN URLs  
**Testing**: JVM unit tests, Robolectric/Compose UI tests or Roborazzi screenshots where available, direct Ktor WebSocket/HTTP tests, focused fake playback-handle tests, scoped `:app:testBranch` validation  
**Target Platform**: Android TV 11-14, min API 30, single `:app` Android module  
**Project Type**: Android TV mobile app with TV-hosted game/session authority  
**Performance Goals**: Song List scroll remains responsive for ≥1000 songs; lyrics highlight remains spatially stable during active singing; LAN manifest fetch target <2s; WebSocket message latency target <50ms; no disk writes during playback  
**Constraints**:
- TV remains authoritative host for session state, song lifecycle, playback state, and score placeholders.
- LAN-only operation; no cloud dependencies or external network requirements.
- Remote song assets remain streamed from phone URLs; TV MUST NOT persist remote audio/video/chart assets.
- Pitch transport remains fixed-size UDP by contract, but UDP pitch-frame ingestion and live-pitch drawing are out of Iteration 1 implementation scope.
- Results screen, real scoring, note finalization, line bonus, live pitch cursor, hit/miss feedback, duet execution, and medley execution are out of Iteration 1 scope.
- Shared UI may show duet/medley controls, but they remain disabled in Iteration 1 with code comments naming the intended wiring iteration: Iteration 3 for duet/two-player behavior and Iteration 4 for medley behavior.
- Optional video renders full-screen as the Singing background when available, but audio is the required playable asset; video/background failures are non-fatal and fall back to a static background.
- Preview playback is LibVLC-backed for stack consistency and wide codec support, including legacy USDX song codecs that Media3 may not cover as reliably. Preview/playback audibility follows TV/system media volume; Iteration 1 must not add an app-level preview preamp or depend on out-of-scope Settings > Audio Preview Volume state.
- Hardware-specific LibVLC acceptance is not an Iteration 1 gate. The runtime still guards optional video through two gates: a static admission gate that disables video at load time when video is greater than 720p and hardware decoder support cannot be confirmed, and a runtime gameplay-degradation report path that disables decorative video during singing, falls back to static background, and keeps audio/playback/session state running. Gameplay degradation is about gameplay quality, not dropped decorative-video frames.
- Android framework types stay in platform/presentation/data-adapter layers; ViewModels own UI state.
- Dependency changes must go through `gradle/libs.versions.toml`; this plan adds QRose 1.1.2 through the catalog for actual Join QR rendering and short/full payload sizing validation.
**Scale/Scope**:
- One feature slice in `specs/002-solo-sing-playback`.
- One connected phone, one selected singer, one normal non-duet song.
- Implementation expected under `app/src/main/kotlin/com/couchraoke/tv/...` and tests under `app/src/test/kotlin/com/couchraoke/tv/...`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Host Authority**: PASS. The TV owns session token, connected roster, selected singer assignment, `songInstanceSeq`, playback lifecycle, song end, and return-to-Song-List behavior. Phones only supply manifest/assets and receive host-authored session/playback messages.
- **Architecture Boundaries**: PASS. Domain contracts remain pure. Ktor, jmDNS, LibVLC, Android audio focus, multicast lock, and cleartext HTTP concerns stay in data/platform/presentation-facing adapters. ViewModels are planned as the single source of UI state for screens and modals.
- **Dependency Governance**: PASS. One dependency addition is planned: QRose 1.1.2 via `gradle/libs.versions.toml` and `app/build.gradle.kts`. Existing catalog entries cover Ktor, jmDNS, LibVLC, Coil, Compose TV, coroutines, and serialization.
- **Contracts First**: PASS. Material producer/consumer boundaries are listed below with FQCN + method + signature and payload contracts.
- **Workflow Units**: PASS. This remains exactly one `spec.md`, one `plan.md`, and one future `tasks.md`; phases are research, design/contracts, and later implementation tasks.
- **Validation Gate**: PASS. The scoped `testBranch` command is defined in the Validation Gate section and must pass fresh before completion claims.

### Material Contracts Planned

1. `com.couchraoke.tv.data.network.NetworkController#connectedPhones: StateFlow<List<ConnectedPhone>>`
   - Producer: network/session data layer.
   - Consumers: Song List, Join overlay, Select Players, PlaybackCoordinator.
   - Data contract: `ConnectedPhone(clientId: String, connectionId: UShort, deviceName: String, httpPort: Int, ipAddress: String)`.
2. `com.couchraoke.tv.data.network.NetworkController#phoneEvents: SharedFlow<PhoneEvent>`
   - Producer: network/session data layer.
   - Consumer: PlaybackCoordinator.
   - Data contract: connected/disconnected/reconnected events with `clientId`, `wasAssignedSinger`, and replacement `connectionId` where applicable.
3. `com.couchraoke.tv.data.network.NetworkController#start(udpPort: Int, wsPort: Int): Unit`
4. `com.couchraoke.tv.data.network.NetworkController#stop(): Unit`
   - Producer: app/session lifecycle.
   - Consumer: app bootstrap and tests.
   - Contract: starts/stops WebSocket server, mDNS advertisement, and stable UDP port reservation without implementing Iteration 2 UDP frame processing behavior.
5. `com.couchraoke.tv.data.network.NetworkController#fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>>`
6. `com.couchraoke.tv.data.network.NetworkController#fetchTxt(url: String): Result<ByteArray>`
   - Producer: Ktor HTTP client adapter.
   - Consumers: LibraryManager and PlaybackCoordinator.
   - Data contract: `SongEntry` mirrors `/manifest.json`; `fetchTxt` returns raw USDX bytes.
7. `com.couchraoke.tv.data.network.NetworkController#sendAssignSinger(phoneId: String, message: AssignSingerMessage): Unit`
8. `com.couchraoke.tv.data.network.NetworkController#broadcastPlaybackState(message: PlaybackStateMessage): Unit`
9. `com.couchraoke.tv.data.network.NetworkController#sendSessionState(phoneId: String): Unit`
   - Producer: PlaybackCoordinator/session lifecycle.
   - Consumer: selected phone(s).
   - Payload contracts: `AssignSingerMessage` and `PlaybackStateMessage` follow Appendix B schema and protocol version `1`.
10. `com.couchraoke.tv.data.network.NetworkController#sendPing(phoneId: String): PongResponse`
11. `com.couchraoke.tv.data.network.NetworkController#sendClockAck(phoneId: String, ack: ClockAckMessage): Unit`
   - Producer: PlaybackCoordinator clock-sync gate.
   - Consumer: connected phone.
   - Iteration 1 contract: one valid sample per assigned singer before countdown/live start; full F21 coverage remains later.
12. `com.couchraoke.tv.domain.library.LibraryManager#songs: StateFlow<List<IndexedSong>>`
13. `com.couchraoke.tv.domain.library.LibraryManager#getSong(songId: String): IndexedSong?`
   - Existing contract reused.
   - Producer: library aggregation from phone manifests.
   - Consumers: SongListViewModel, SelectPlayers, PlaybackCoordinator.
14. `com.couchraoke.tv.domain.usdx.UsdxParser#parse(songId: String, txtBytes: ByteArray): Result<ParsedSong>`
   - Existing contract reused.
   - Producer: parser domain.
   - Consumers: PlaybackCoordinator and Singing render-model builder.
15. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#startSong(selection: SongStartSelection): Unit`
16. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#pause(): Unit`
17. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#resume(): Unit`
18. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#restart(): Unit`
19. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#quitToSongList(): Unit`
20. `com.couchraoke.tv.domain.playback.PlaybackCoordinator#onPlaybackEvent(event: PlaybackEvent): Unit`
   - Producer: host game coordinator.
   - Consumers: ViewModels, playback controller.
   - Data contract: `SongStartSelection(songId, playerPhoneId, difficulty, startMode/countdown)`, `GamePhase`, `songInstanceSeq`, `stopAtLyricsTimeMs`.
21. `com.couchraoke.tv.presentation.playback.PlaybackController#intents: StateFlow<PlaybackIntent>` or equivalent ViewModel-observed stream.
22. `com.couchraoke.tv.presentation.playback.PlaybackController#events: SharedFlow<PlaybackEvent>` or callback to coordinator.
23. `com.couchraoke.tv.presentation.playback.PlaybackController#currentPositionMs: StateFlow<Long>` or equivalent observable audio position.
   - Producer/consumer boundary between coordinator and UI/playback layer.
   - Payload contracts: `PlaybackIntent.Prepare(audioUrl, videoUrl, videoGapSec, seekToSec, chartEndLyricsTimeMs?)`, `Play(stopAtLyricsTimeMs)`, `Pause`, `Stop`, `Seek`; `PlaybackEvent.Prepared(effectivePlaybackDurationMs)`, `Ready(songStartTvMs)`, `Error(cause)`, `Ended`.
   - Prepared contract: `effectivePlaybackDurationMs` is read from the prepared authoritative audio handle duration. `#START` changes seek position only and does not shift lyrics-time origin; `chartEndLyricsTimeMs` is parsed positive `#END` in milliseconds or null. Coordinator finalizes `stopAtLyricsTimeMs = chartEndLyricsTimeMs ?: effectivePlaybackDurationMs`; if `chartEndLyricsTimeMs` is null and no usable audio duration is available, preparation fails through `PlaybackEvent.Error`. `Play(stopAtLyricsTimeMs)` is emitted only after finalization, `assignSinger`, and playback-state emission.
   - Ready contract: `Ready(songStartTvMs)` is emitted only from the first audio-handle `Playing` event or the 500 ms fallback captured at `play()` time. Decorative video events never influence `Ready`, `songStartTvMs`, `currentPositionMs`, scoring, or session state.
24. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#setEventListener(listener: (LibVlcEvent) -> Unit): Unit`
25. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#prepare(url: String): Unit`
26. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#play(): Unit`
27. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#pause(): Unit`
28. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#stop(): Unit`
29. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#seekTo(positionMs: Long): Unit`
30. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#timeMs: Long`
31. `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle#durationMs: Long?`
   - Producer: LibVLC adapter.
   - Consumers: Singing playback controller, Song List preview controller/state, and tests.
   - Iteration 1 contract: one handle wraps one LibVLC MediaPlayer. The audio handle is authoritative for `Prepared`, `Ready`, `currentPositionMs`, stop-boundary enforcement, audio errors, and audio focus. Optional video handle is decorative full-screen background, configured without audio, and video/background failure falls back to static background without emitting a blocking playback error. The adapter owns LibVLC warning/error log capture and maps the most recent warning/error line, truncated to 120 chars, into `LibVlcEvent.EncounteredError(lastWarningOrError)` for audio error modals.
32. `com.couchraoke.tv.presentation.playback.SongPreviewController#preparePreview(audioUrl: String, startPositionMs: Long): Unit`
33. `com.couchraoke.tv.presentation.playback.SongPreviewController#play(): Unit`
34. `com.couchraoke.tv.presentation.playback.SongPreviewController#stop(): Unit`
35. `com.couchraoke.tv.presentation.playback.SongPreviewController#release(): Unit`
   - Producer: Song List screen/ViewModel focus state.
   - Consumer: screen-scoped LibVLC preview player.
   - Contract: after a 500 ms same-tile focus debounce, preview prepares the focused song `audioUrl`, seeks to `previewStartSec` when positive or 0 seconds when absent/non-positive, and plays until stopped. It stops immediately when focus changes, focus leaves grid, overlay/modal/settings/singing opens, or Song List exits; HTTP/LibVLC failures are silent; volume is TV/system media volume only. This uses LibVLC rather than Media3 to match Singing playback's wide codec support, especially for older USDX libraries with legacy codecs.
36. `com.couchraoke.tv.presentation.join.QrCodeRenderer#render(payload: String, sizePx: Int): RenderedQrCode`
   - Producer: presentation/platform QR adapter backed by the QR-only QRose artifact (`io.github.alexzhirkevich:qrose`), not `qrose-oned`.
   - Consumer: Join overlay UI and QR renderer tests.
   - Data contract: the basic Compose usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`; the Join overlay must configure a QRose painter with `colors { dark = QrBrush.solid(Color.Black); light = QrBrush.solid(Color.White) }`, `errorCorrectionLevel = QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale`. The implementation renders the painter directly and returns computed content bounds for validation. The implementation must keep the QR centered/scaled inside the requested box for both short and full endpoint payloads while preserving at least a 4-module quiet zone; PNG byte export is not required.
37. `com.couchraoke.tv.presentation.search.TvTextInputLauncher#launch(initialValue: String, onResult: (String) -> Unit): Unit`
   - Producer: presentation/platform text input adapter or native IME focus fallback.
   - Consumer: Song List Search UI.
   - Contract: invokes Android TV text entry without exposing Android framework types to `SongListViewModel`; no custom in-app keyboard in Iteration 1.
38. `com.couchraoke.tv.presentation.singing.VideoAdmissionPolicy#shouldUseVideo(metadata: VideoMetadata, codecs: List<CodecCapability>): Boolean`
   - Producer: presentation/platform media-capability adapter backed by Android codec capability inspection (`MediaCodecList` / `MediaCodecInfo`).
   - Consumer: Singing playback controller/UI before creating the decorative video handle.
   - Contract: returns false when optional video is greater than 720p and hardware decoder support for the video codec cannot be confirmed; this is a load-time best-effort guard, not a target-device certification gate.
39. `com.couchraoke.tv.presentation.singing.GameplayDegradationReporter#reportDegradation(reason: GameplayDegradationReason): Unit`
   - Producer: gameplay-quality monitors such as future pitch-frame/render-quality checks.
   - Consumer: Singing playback controller/UI during active singing.
   - Contract: when gameplay degradation is reported, disable decorative video for the current song, fall back to static background, and optionally surface a non-blocking degradation notice; audio playback/session state continues unaffected. Iteration 1 does not test dropped decorative-video frames.
40. `com.couchraoke.tv.presentation.singing.SingingRenderModelBuilder#build(song: IndexedSong, parsedSong: ParsedSong, playerId: PlayerId): SingingRenderModel`
   - Producer: presentation/domain adapter for singing UI.
   - Consumer: SingingViewModel and pitch-lane renderer.
   - Data contract: immutable title/artist, lyrics sentences, static note targets for P1, playback timing metadata, optional background/video assets.
41. `com.couchraoke.tv.presentation.singing.PitchLaneRenderer#drawPitchLane(canvas: Canvas, viewport: Rect, state: LaneRenderState): Unit`
   - Producer: renderer.
   - Consumer: Singing screen surface.
   - Iteration 1 data contract: `LaneRenderState` contains static note targets and current lyrics-time position only; no live pitch, no hit/miss feedback.

## Project Structure

```text
specs/002-solo-sing-playback/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── network-protocol.md
    ├── playback-coordinator.md
    ├── playback-ui.md
    └── singing-rendering.md

app/
└── src/
    ├── main/
    │   └── kotlin/com/couchraoke/tv/
    │       ├── MainActivity.kt
    │       ├── data/network/
    │       ├── data/library/
    │       ├── domain/library/
    │       ├── domain/model/
    │       ├── domain/playback/
    │       ├── domain/scoring/
    │       ├── domain/usdx/
    │       ├── presentation/playback/
    │       ├── presentation/singing/
    │       ├── presentation/songlist/
    │       ├── presentation/selectplayers/
    │       ├── presentation/join/
    │       ├── presentation/settings/
    │       └── ui/theme/
    └── test/
        └── kotlin/com/couchraoke/tv/
            ├── data/network/
            ├── data/library/
            ├── domain/playback/
            ├── presentation/playback/
            ├── presentation/singing/
            ├── presentation/songlist/
            └── fixtures/
```

**Structure Decision**:
- Reuse existing `domain/library`, `domain/usdx`, `domain/scoring`, and `domain/model` contracts instead of duplicating them.
- Add `domain/playback` for host-owned game/session lifecycle that is not UI and not network-specific.
- Add `data/network` and `data/library` for Ktor/jmDNS/manifest adapters and aggregation.
- Add `presentation/*` for ViewModels, Compose screens/modals, playback adapters, and singing render model/renderer.
- Keep UI state in ViewModels and keep Android/LibVLC/Ktor framework types out of domain models.

## Phase 0: Research & Decision Consolidation

1. Resolve Iteration 1 scope boundaries against `tv_app.md`: static note lanes only, no live pitch, no scoring, no Results, disabled duet/medley controls.
2. Add QRose 1.1.2 through `gradle/libs.versions.toml` and `app/build.gradle.kts` for actual QR rendering; use only the QR artifact (`io.github.alexzhirkevich:qrose`) and do not add `qrose-oned`; document the exact QRose usage contract (basic Compose usage `Image(painter = rememberQrCodePainter(payload), ...)`, explicit Join-overlay painter options, black-on-white colors, `QrErrorCorrectionLevel.Medium`, direct painter rendering, and implementation-controlled `scale` for visual padding) and verify short/full WebSocket endpoint payloads against the centered 400dp quiet-zone requirements.
3. Lock the minimal network/session behavior: mDNS advertisement, token-gated WebSocket handshake, manifest fetch, selected-singer assign/playbackState, and clock-sync gate.
4. Lock the playback behavior: UI emits Prepared from the authoritative audio handle duration and Ready from first audio `Playing` or 500 ms fallback, captures `songStartTvMs` from audio only, enforces `stopAtLyricsTimeMs`, handles audio focus, treats video as decorative, and carries LibVLC warning/error diagnostics into audio error payloads.
5. Lock the UI behavior and screenshot states: Song List, LibVLC-backed preview playback, Join overlay with actual QR, Select Players non-duet/no-phone, Singing single-singer/countdown/pause/confirm/error, disabled/no-op scoped affordances with comments naming their target iteration, and best-effort full-screen video background fallback.
6. Lock the visual compositing stack: optional full-screen video/background `SurfaceView` behind a separate pitch-lane rendering `SurfaceView`, with Compose metadata, score, lyrics, countdown, and overlays above both surfaces. Full-screen video uses `SurfaceView.setZOrderMediaOverlay(true)` and never `TextureView`; pitch-lane drawing remains SurfaceView-backed and renderer-owned.
7. Lock optional-video runtime guardrails: no hardware-specific sign-off gate in this Iteration 1 feature slice, but load-time video admission must disable video when resolution is >720p and hardware decoder support cannot be confirmed, and active singing must expose a gameplay-degradation report path that disables decorative video and falls back to static background without testing dropped decorative-video frames.

## Phase 1: Design & Contracts

1. Create `research.md` with the decisions above, alternatives considered, and source-alignment rationale.
2. Create `data-model.md` for connected phones, session state, manifest entries, indexed songs, song start selection, game phase, playback events/intents, singing render model, lane render state, and modal states.
3. Create `contracts/network-protocol.md` for WebSocket, mDNS, HTTP manifest/txt, assignSinger, playbackState, sessionState, and explicit Iteration 1 UDP non-scope.
4. Create `contracts/playback-coordinator.md` for coordinator, FSM subset, start/pause/resume/restart/quit/song-end, and clock-sync gate.
5. Create `contracts/playback-ui.md` for playback intents/events, LibVLC handle seam, Song List preview playback seam, audio focus, stop boundary, prepared duration source, Ready timing, video fallback, degradation guardrails, and error diagnostics.
6. Create `contracts/singing-rendering.md` for static note lane rendering, lyrics paging, score placeholder, SurfaceView/Compose compositing, and excluded live-pitch/scoring behavior.
7. Create `quickstart.md` with implementation order, validation commands, UI verification checklist, and known OOS issues.
8. Run `.specify/scripts/bash/update-agent-context.sh claude` after artifacts are written.
9. Re-check constitution compliance after design artifacts are complete.

## Validation Gate

Feature completion must use fresh scoped `testBranch` validation for the production and test classes touched by each task. The full-feature gate should include the Iteration 1 classes introduced or modified, with representative selectors like:

```bash
timeout 10m ./gradlew :app:testBranch \
  --src com.couchraoke.tv.MainActivity \
  --src com.couchraoke.tv.data.network.NetworkController \
  --src com.couchraoke.tv.data.network.KtorNetworkController \
  --src com.couchraoke.tv.data.library.ManifestLibraryManager \
  --src com.couchraoke.tv.domain.library.LibraryManager \
  --src com.couchraoke.tv.domain.library.IndexedSong \
  --src com.couchraoke.tv.domain.playback.PlaybackCoordinator \
  --src com.couchraoke.tv.domain.playback.GamePhase \
  --src com.couchraoke.tv.domain.usdx.UsdxParser \
  --src com.couchraoke.tv.presentation.playback.PlaybackController \
  --src com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle \
  --src com.couchraoke.tv.presentation.playback.SongPreviewController \
  --src com.couchraoke.tv.presentation.singing.VideoAdmissionPolicy \
  --src com.couchraoke.tv.presentation.singing.VideoDegradationMonitor \
  --src com.couchraoke.tv.presentation.singing.SingingRenderModelBuilder \
  --src com.couchraoke.tv.presentation.singing.PitchLaneRenderer \
  --src com.couchraoke.tv.presentation.songlist.SongListViewModel \
  --src com.couchraoke.tv.presentation.singing.SingingViewModel \
  --test com.couchraoke.tv.data.network.NetworkControllerTest \
  --test com.couchraoke.tv.data.library.ManifestLibraryManagerTest \
  --test com.couchraoke.tv.domain.playback.PlaybackCoordinatorTest \
  --test com.couchraoke.tv.presentation.playback.PlaybackControllerTest \
  --test com.couchraoke.tv.presentation.singing.SingingRenderModelBuilderTest \
  --test com.couchraoke.tv.presentation.singing.PitchLaneRendererTest \
  --test com.couchraoke.tv.presentation.songlist.SongListViewModelTest \
  --test com.couchraoke.tv.presentation.singing.SingingViewModelTest
```

Task-level gates may narrow selectors to the exact files changed, but must not omit source/test counterparts for changed behavior. UI tasks must also compare rendered previews/screenshots against spec wireframes before completion claims.

## Post-Design Constitution Re-Check

- **Host Authority**: Maintained. The TV owns session, song lifecycle, playback state, assignment, and song-end routing. Phones provide assets and receive host state.
- **Architecture Boundaries**: Maintained. Domain contracts avoid Android/Ktor/LibVLC types; platform adapters and ViewModels isolate framework concerns.
- **Dependency Governance**: Maintained. QRose 1.1.2 is the only planned dependency addition and must be routed through `gradle/libs.versions.toml` plus `app/build.gradle.kts`.
- **Contracts First**: Satisfied by generated contracts artifacts and explicit FQCN/signature boundaries, including Song List preview, two-handle LibVLC singing playback, prepared duration, Ready timing, error diagnostics, and optional-video admission/degradation seams.
- **Workflow Units**: Maintained as one feature with bounded design outputs and future task list.
- **Validation Gate**: Scoped `testBranch` and UI verification requirements are defined.

## Out-of-Scope Issues Noted

1. `tv_app.md` §4.1 defines the full future FSM route through Results, but §7.3 Iteration 1 DOD and §7.4 allocation make Results an Iteration 2 concern.
   - Suggested fix: implement an Iteration 1 song-end transition that returns to Song List, and add the `Stopped → Results` route when the Iteration 2 Results task lands.
2. `tv_app.md` §2.6.16 minimum content mentions live score and pitch cursor, but §7.4 explicitly assigns pitch frames, live cursor, live score, and Results to Iteration 2.
   - Suggested fix: in Iteration 1, render static note targets and a constant `00000` score only; add comments at visible scoring/live-pitch placeholders that Iteration 2 wires live pitch, scoring, and Results.
3. Shared Song List wireframe includes duet/medley affordances while Iteration 1 is solo-sing only.
   - Suggested fix: render those controls disabled/no-op in Iteration 1 with code comments naming their target iteration; enable duet/two-player behavior in Iteration 3 and medley playlist/execution/prebuffer/crossfade behavior in Iteration 4.
