# Tasks: Solo Sing Playback

**Input**: Design documents from `/home/paavum/Couchraoke/tv/specs/002-solo-sing-playback/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: This feature specification includes independent tests, success criteria, and validation gates, so test tasks are included before implementation work in each phase. Each implementation task still requires a fresh scoped `testBranch` pass before it is marked complete; the validation tasks below are story and feature gates.

**Organization**: Tasks are grouped by user story to enable MVP-first implementation and independent validation. Paths are repository-relative.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel when staffed separately because the task writes different files and does not depend on an incomplete task.
- **[Story]**: User story label from `spec.md`; setup, foundational, and polish tasks have no story label.
- **Execution default**: Work sequentially in task ID order unless parallel execution is explicitly assigned.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared dependency and fixture groundwork, including the planned QR rendering dependency.

- [X] T001 Add QRose (`io.github.alexzhirkevich:qrose:1.1.2`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`, and confirm the remaining planned dependencies already exist
- [X] T002 Configure Android cleartext LAN HTTP support for phone-hosted manifest/chart/media URLs in `app/src/main/AndroidManifest.xml` and `app/src/main/res/xml/network_security_config.xml`
- [X] T003 [P] Create connected-phone, manifest, and playback test fixtures in `app/src/test/kotlin/com/couchraoke/tv/fixtures/SoloSingFixtures.kt`
- [X] T004 [P] Create static USDX chart fixture helpers for solo playback tests in `app/src/test/kotlin/com/couchraoke/tv/fixtures/SoloSingUsdxFixtures.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define core contracts and model types that all user stories depend on.

**Critical**: No user story implementation should begin until this phase is complete and audited.

### Validation for Foundational Contracts

- [X] T005 [P] Add network protocol contract tests in `app/src/test/kotlin/com/couchraoke/tv/data/network/NetworkControllerContractTest.kt`
- [X] T006 [P] Add playback domain contract tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackContractsTest.kt`
- [X] T007 [P] Add playback UI seam contract tests for Iteration 1 intents/events; audio-handle Prepared duration and Ready timing ownership; decorative video never driving timing/session state; Song List preview seam using LibVLC-backed screen-scoped playback; and no-op medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` intent handling with comments that they must be wired in Iteration 4 in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContractsTest.kt`
- [X] T008 [P] Add singing rendering contract tests for single-singer layout tokens, lyrics band constraints, score placeholder contract, SurfaceView/Compose visual stack contract, decorative video fallback state, and no live pitch/scoring state in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderContractsTest.kt`
- [X] T008A [P] Add visual-system contract tests for Iteration 1 spacing, radius, border, layout, typography, color/surface roles, motion-budget definitions, focus treatment, and prohibited effects in `app/src/test/kotlin/com/couchraoke/tv/ui/theme/ThemeContractsTest.kt`

### Implementation for Foundational Contracts

- [X] T009 Implement network protocol model types in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkModels.kt`
- [X] T010 Implement `NetworkController` interface in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkController.kt`
- [X] T011 Implement playback domain model types in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackModels.kt`
- [X] T012 Implement `PlaybackCoordinator` interface in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinator.kt`
- [X] T013 Implement playback UI intent/event types, Song List preview seam, and LibVLC seam in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContract.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/LibVlcPlayerHandle.kt`, and preview-controller contract/state files, including audio-handle Prepared duration and Ready ownership, decorative video never driving timing/session state, screen-scoped preview lifetime, and medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` intents as no-ops with code comments that they must be wired in Iteration 4
- [X] T014 Implement singing render models and renderer contracts in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModels.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModelBuilder.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/PitchLaneRenderer.kt`, preserving Iteration 1 static-note/no-live-pitch/no-scoring state and carrying layout data needed for top metadata, single lane, score placeholder, elapsed time, two-line lyrics, SurfaceView/Compose visual stack, and non-blocking decorative video fallback state
- [X] T015 Implement shared route and modal-state contracts in `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppRoute.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/common/UiModalState.kt`
- [X] T016 Implement common UI definitions for Iteration 1 colors, typography, surfaces, background roles, focus treatment, spacing/dimension tokens, visual-system motion-budget definitions, and preview/theme wiring in `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Color.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Type.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Theme.kt`, and `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Dimensions.kt`
- [X] T016A Implement reusable focus, surface, and motion-policy UI helpers that enforce border-plus-plate focus, unfocused border opacity, disabled non-focusability support, flat rendering, no runtime blur/glow/bloom/shadow/elevation focus treatment, and the Iteration 1 V0/V1/V2 motion budgets in `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Focus.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Surfaces.kt`, and `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Motion.kt`
- [X] T017 Run scoped foundational `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkController.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContract.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModels.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Color.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Type.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Theme.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Dimensions.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Focus.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Surfaces.kt`, `app/src/main/kotlin/com/couchraoke/tv/ui/theme/Motion.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

**Checkpoint**: Foundation ready; user story work can start only after common UI definitions in T017 are complete for any visual UI task.

---

## Phase 3: User Story 1 - Browse and start a solo song (Priority: P1) MVP

**Goal**: Discover one phone, load its manifest into Song List, open Join and Select Players UI, assign one P1 singer, prepare playback, and show the Singing screen with streamed media, two-line lyrics, static note lanes, and a constant `00000` score.

**Independent Test**: Launch the app, discover one LAN phone, load the manifest, select a non-duet song, assign one Player 1 phone, start playback, verify audio playback starts, and verify sentence-paged lyrics plus static note lanes render on Singing.

### Source-extracted UI requirements for User Story 1

The Song List, preview, Join, and Back tasks below preserve these source descriptions from `original_spec/tv_app.md` §2.6.9, §2.6.10, §2.6.12, and §2.6.13:

- **Back behavior**: "From Song List: if filter active, clear filter; otherwise exit app." "From modal dialogs/overlays: close overlay, return to underlying screen." "From Singing: open Pause overlay (Resume / Restart Song / Quit to Song List)." "From Results: return to Song List."
- **Song Preview Playback / When preview plays**: "Song tile is focused AND focus remains on same song for **500ms** (debounce)." Iteration 1 does not implement Settings > Audio Preview Volume or an app-level preview preamp; preview audibility follows TV/system media volume.
- **Song Preview Playback / Preview stops immediately when**: "Focus moves to a different song tile, leaves grid, or overlay/modal/settings/singing opens."
- **Song Preview Playback / What plays**: "Uses `audioUrl` from cached manifest, seeks to `previewStartSec`." "If `previewStartSec > 0.0`, use it; otherwise start at 0 seconds." "Plays from start position until stopped (no fixed 10s limit)." Preview uses TV/system media volume only in Iteration 1; Settings > Audio Preview Volume zero-disable is deferred to Iteration 3 Settings scope. "If HTTP fails, suppress silently."
- **Song Preview Playback / LibVLC handle lifetime**: "Media players are screen-scoped." For Iteration 1 this means LibVLC handles: "A preview player belongs only to SongListScreen and MUST be torn down when SongListScreen exits." "Any media player created for SingingScreen, including medley transition players, MUST be torn down when SingingScreen exits."
- **SongListScreen Purpose**: "always the landing screen. Displays songs sorted by Artist → Album → Title. Maintains a transient Medley playlist (initialized empty each time shown, cleared on leaving for non-modal screen)."
- **SongListScreen Layout**: "two-column. Left rail: preview pane (16:9, display-only, non-focusable), Medley playlist, Play Medley. Right body: Search field, Random actions row (Random Song / Random Duet / Random Medley), song grid. Screen uses `AppMarginHorizontal` / `AppMarginVertical` margins."
- **SongListScreen Layout proportions and tokens**: `SongListLeftRailFraction` = 0.34; `SongListGridFraction` = 0.66; `SongListRailGridGap` = 32dp; `SongListHeaderToBodyGap` = 24dp; `SongListRandomRowHeight` = 72dp; `SongListRandomRowGap` = 24dp; `SongListGridColumns1080` = 3; `SongListGridColumns4K` = 4; `SongListGridColumnGap` = 24dp; `SongListGridRowGap` = 24dp; `SongListPreviewAspect` = 16:9; `SongListPreviewToMetaGap` = 16dp; `SongListMetaToPlaylistGap` = 24dp; `SongListPlaylistRowHeight` = 52dp; `SongListPlaylistVisibleRows` = 5; `SongListPlayMedleyTopGap` = 16dp.
- **Focus indicator**: "Focused element: draw a `FocusBorderWidth` (3dp) solid border in `BorderFocus`, inset `FocusBorderInset` (2dp) from the component edge, plus a subtle filled plate on the component body for additional contrast against video/background content." "Unfocused enabled element: draw a `BorderThin` (1dp) border using `BorderSubtle` at `UnfocusedBorderOpacity` (20%) so grid/list structure remains legible at TV distance." "Focus MUST be border-plus-plate only; do not add focus shadows, blur, elevation changes, glow, or background pulse." "Disabled elements MUST NOT be focusable."
- **SongListScreen Header composition**: "Contents: Search field, Join button, Settings button. Header uses `HeaderHeight` (76dp), operational sans only." "Emphasis: Search is the visually strongest control. Join and Settings are equal secondary controls."
- **SongListScreen Empty states**: "No phones connected: `No phones connected.` / `Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code.`" "Phones connected but no valid songs: `No songs found.` / `Open the karaoke app on your phone and make sure the songs folder is set.`"
- **SongListScreen Search**: "case-insensitive substring match across {artist, album, title}. Debounce 150ms." OK on Search field uses a presentation/platform-facing Android TV text input launcher seam or native IME focus behavior; ViewModels remain framework-free and Iteration 1 does not build a custom keyboard.
- **SongListScreen Primary actions**: "OK on song tile → Select Players. Long-press OK → Add to Medley (if `canMedley=false`, show blocking modal with exact text: `This song can't be used in a medley. Look for songs with an M tag in the lower right corner`)."
- **SongListScreen Random actions row**: "Contents: Random Song, Random Duet, Random Medley. All three use equal visual weight (equal sizing, equal emphasis). Row uses `SongListRandomRowHeight`." "Disabled when no eligible songs exist." "Random Medley lives in this row, not in the left rail."
- **SongListScreen Left rail**: "Preview pane uses `SongListPreviewAspect` (16:9), is display-only and non-focusable." "Focused-song preview metadata always shows full title (`PreviewTitle`) and artist (`PreviewArtist`) — no truncation of the preview metadata block." "Medley playlist occupies the lower half of the rail with `SongListPlaylistVisibleRows` (5) visible rows at `SongListPlaylistRowHeight` (52dp). Rows: `<Artist>  <Title>` in operational sans." "`Play Medley` sits directly below the playlist at `SongListPlayMedleyTopGap`."
- **Song grid**: "`SongListGridColumns1080` (3) at 1080p, `SongListGridColumns4K` (4) at 4K. Column count fixed per resolution. Gaps use `SongListGridColumnGap` / `SongListGridRowGap`."
- **Song cards**: "Default (unfocused) state shows: cover image, title (`SongCardTitle`, up to 2 lines), up to 3 tag chips." "Focused state additionally shows: one artist line in the reserved artist slot (`SongCardArtistFocused`)." "The artist slot is always reserved (`SongCardFocusedArtistSlotHeight`); revealing the artist on focus MUST NOT reflow the card." "Weak-artwork fallback: if a card's cover is missing, placeholder, or unusable, keep title primary, keep tag chips visible, and show the artist in the default state for that card."
- **Song card tokens**: `SongCardHeight` = 252dp; `SongCardPadding` = 12dp; `SongCardImageHeight` = 148dp; `SongCardImageCornerRadius` = 8dp; `SongCardTitleMaxLines` = 2; `SongCardFocusedArtistSlotHeight` = 20dp; `SongCardTitleToArtistGap` = 4dp; `SongCardTagCornerInset` = 8dp; `SongCardTagGap` = 6dp; `SongCardMaxVisibleTags` = 3.
- **Tag placement and priority**: "Tag chips are rendered **on-image, in the lower-right corner** of the cover image, inset by `SongCardTagCornerInset` with `SongCardTagGap` between chips." "Maximum visible chips: `SongCardMaxVisibleTags` (3)." "Priority order when more than three apply: `D`, `M`, `R`, `I`, `V`. `V` is always the first chip omitted."
- **SongListScreen focus/navigation**: "Initial focus: first grid tile (top-left); if empty, Search field." "Left-panel entry target (from leftmost grid column): 1) first Medley playlist row if present, 2) Play Medley if empty, 3) Random Medley as fallback." "Back from grid/left panel: move focus to Search field. Back from top controls: clear filter if active; else exit app."
- **SongListScreen motion and focus behavior**: "Settled state (V2): local preview crossfade and restrained chip or border fade." "Active navigation (V1): focus transition only." "No per-card ambient animation." "No animated background behind the grid." "No card scale on focus (card stays at fixed `SongCardHeight`; focus is border + plate only)."
- **QR payload**: "The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter (e.g., `ws://192.168.1.10:8080/?token=ABCDEFGH`). It MUST NOT encode an NSD/mDNS service-discovery identifier. Phones that scan the QR code connect directly to the encoded URL without any additional discovery step." Use QRose for actual QR rendering with the QR-only artifact `io.github.alexzhirkevich:qrose`; do not add `qrose-oned`. Implementers must not infer the API: baseline Compose usage is `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`; for the Join overlay, use a QRose painter inside the fixed QR box with `colors { dark = QrBrush.solid(Color.Black); light = QrBrush.solid(Color.White) }`, `errorCorrectionLevel = QrErrorCorrectionLevel.Medium`, and `scale = qrContentScale`. Validation prioritizes a working rendered QR in the overlay and short/full endpoint payload sizing/centering; PNG byte export is not required.
- **Join Overlay Presentation**: "modal over Song List. Uses `SurfaceLevel2` shell at `PrimaryModalWidth` (960dp) with `PrimaryModalPadding` (32dp). Balanced tier typography, operational sans only."
- **Join Overlay Visual rules**: "QR is the dominant object on the overlay. It uses `JoinQRCodeSize` (400dp), 4-module quiet zone, high-contrast dark-on-light, centered." "The short join code sits directly below the QR at `JoinCodeTopGap` using `PanelTitle` or `DisplayAccentTitle` weight." "The QR MUST remain static. No animation or overlay element may intrude into the QR or its quiet zone." "Entrance animation is a single short fade or scale-fade of the modal shell only (V1 budget)." "Back behavior: closes overlay, returns to Song List."

### Source-extracted Singing requirements for User Story 1

The Singing tasks below preserve these source descriptions from `original_spec/tv_app.md` §2.6.16, with scoring and live pitch kept out of Iteration 1 as required by `spec.md`:

- **Overall layout**: "top metadata strip, lane region, full-width bottom lyrics band. The screen is designed for video backgrounds; overlay surfaces remain readable over moving footage via `SurfaceLaneBand` / `SurfaceLyricsBand` at `LaneBandAlpha` / `LyricsBandAlpha`." Optional video is best-effort full-screen background media; audio is required, and video/background failure falls back to a static background without affecting audio/playback/session state.
- **Video surface z-order**: "the video surface MUST be a `SurfaceView` with `setZOrderMediaOverlay(true)`." "`TextureView` MUST NOT be used for fullscreen video on the singing screen".
- **Layout tokens**: `SingingTopIntroStripHeight` = 72dp; `SingingTopMinimalStripHeight` = 40dp; `SingingBottomLyricsBandHeight` = 160dp; `SingingBodyToLyricsGap` = 16dp; `SingingSingleLaneHeight` = 192dp; `SingingSingleLaneVerticalPosition` = centered; `SingingLaneHorizontalPadding` = 20dp; `SingingLaneVerticalPadding` = 16dp; `SingingScoreBoxWidth` = 144dp; `SingingScoreBoxHeight` = 88dp; `SingingScoreBoxRightInset` = 16dp; `SingingScoreBoxToRatingGap` = 8dp; `SingingBadgeHeight` = 40dp; `SingingBadgeTopInset` = 8dp; `LyricsBandPaddingHorizontal` = 24dp; `LyricsBandPaddingTop` = 20dp; `LyricsBandLineGap` = 8dp.
- **Minimum content adjusted for Iteration 1 scope**: Source minimum content includes progressive-highlight lyrics, pitch bars per active singer, live score per singer (`XXXXX`), elapsed time, and song title/artist. Iteration 1 MUST render static note bars from the parsed song file only and a constant `00000` score placeholder; live pitch, live score updates, hit/miss feedback, and scoring calculations remain out of scope.
- **Top metadata rules**: "On song start and on medley segment change, render the top metadata in the intro strip at `SingingTopIntroStripHeight` (title + artist, oversized tier)." "During active singing, collapse metadata to the minimal strip at `SingingTopMinimalStripHeight` using `TopMetadataMinimal`." Medley metadata is out of scope.
- **Singer lane layout rules**: "Single singer: exactly one centered lane band at `SingingSingleLaneHeight` (192dp), full width, vertically centered on screen." "Lane bands are long horizontal plates with `RadiusMedium` corners." "Lane fill uses `SurfaceLaneBand` with `LaneBandAlpha`." "Lane bodies remain neutral. Player color appears only in accents (singer badge, score-box accents, pitch cursor, note markers). MUST NOT tint the full lane body with `Player1Accent` / `Player2Accent`." P1 uses `Player1Accent` (cyan).
- **Score and sentence rating**: "Each lane has exactly one score box anchored on the right edge at `SingingScoreBoxWidth` × `SingingScoreBoxHeight`, inset by `SingingScoreBoxRightInset` from the lane's right edge." "Sentence rating is rendered directly under the score box at `SingingScoreBoxToRatingGap`." Iteration 1 keeps score `00000` and does not implement score/rating calculation.
- **Elapsed time**: "displayed bottom-right using `Timer` typography, formatted as `MM:SS` (always two digits each, zero-padded; e.g., `00:35`, `01:23`). This is elapsed time from song start."
- **Lyrics rendering**: "Lyrics render in the bottom lyrics band only, at `SingingBottomLyricsBandHeight` using `SurfaceLyricsBand` with `LyricsBandAlpha`. Band uses `LyricsBandPaddingHorizontal` / `LyricsBandPaddingTop` / `LyricsBandLineGap`." "The band always shows exactly two lines: current line and next line. MUST NOT render a third line." "Current line uses `LyricsCurrent` and is stronger in contrast/emphasis; next line uses `LyricsNext` and is muted." "Lyrics MUST remain spatially stable during a sentence (no continuous scroll)." "Sentence-based paging. Current sentence stays in place while highlight progresses." "Page to next sentence when lyrics beat position reaches `startBeat` of first note in next sentence." "During instrumental gaps, completed sentence remains at 100% highlight — MUST NOT pre-page or show blank." "Implementations MAY render the active highlight as a clipped reveal over an inactive base text pass."
- **Singing motion budget**: Allowed during active singing for Iteration 1: lyric highlight progression and note lane rendering required by gameplay. Not allowed: background animation over video, blur or bloom transitions on lyric change, multi-panel HUD entrance sequences, particle feedback, full-lane pulses, or layout-affecting animation of the lane, lyrics region, or score placement.
- **Countdown**: "if Ready countdown ON, show an N-second countdown at 1 Hz then begin playback + scoring. If OFF, begin immediately." "The numeral is centered and rendered in `DisplayHeroNumber`." "Background remains dimmed and static (no video motion during countdown)." "Only the numeral animates: one scale-pop per count. MUST NOT add secondary full-screen pulses, flashes, or particle effects."
- **Song end adjusted for Iteration 1 scope**: Source full route is `Stopped → scoring finalization → Results`, but Iteration 1 returns to Song List and does not finalize scoring or show Results. The UI still enforces `stopAtLyricsTimeMs`, calls `LibVlcPlayerHandle.stop()`, and emits `PlaybackEvent.Ended`.
- **Playback error handling**: On `LibVlcEvent.EncounteredError`, stop playback, return to Song List, and show blocking `ERROR` modal with body line 1 `This song can't be played.` and body line 2 as the most recent LibVLC warning/error log line truncated to 120 chars when present; session returns to Open.
- **Audio focus**: Request `AUDIOFOCUS_GAIN` before playback; transient loss pauses; gain resumes; permanent loss follows playback error; abandon focus on song end, error exit, or Restart.

### Validation for User Story 1

- [X] T018 [P] [US1] Add token-gated WebSocket, protocol mismatch, session-locked join rejection, reconnect connectionId replacement, mDNS advertisement, non-cached manifest fetch, and TXT fetch tests in `app/src/test/kotlin/com/couchraoke/tv/data/network/KtorNetworkControllerTest.kt`
- [X] T019 [P] [US1] Add manifest aggregation, invalid-entry rejection including required field and URL invariants, phone disconnect removal, fetch-failure previous-catalog retention, active-song reconnect catalog-stale/deferred-refresh, and Artist→Album→Title sort tests in `app/src/test/kotlin/com/couchraoke/tv/data/library/ManifestLibraryManagerTest.kt`
- [X] T020 [P] [US1] Add Song List tests covering FR-003 through FR-009a for case-insensitive search across artist/album/title with 150ms debounce, framework-free text-input-launch request or native IME fallback on Search OK, empty-state copy, disabled duet/medley controls with comments naming Iteration 3 for duet and Iteration 4 for medley wiring, Settings header affordance remaining inert/no-op with no Settings route/menu/screen/submenu opened and an Iteration 3 wiring comment, Back clearing filter before exit, LibVLC-backed preview debounce/stop/fallback rules using `audioUrl`, `previewStartSec`, fallback to 0 seconds when absent or non-positive, TV/system media volume only, screen-scoped lifetime and silent failure, and Join overlay QR payload/state in `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt`
- [X] T021 [P] [US1] Add QR renderer tests for QRose using short and full endpoint payloads, asserting a working QRose painter is produced for the Join overlay, the rendered QR metadata fits within the requested square, preserves at least a 4-module quiet zone, and centers QR content with opposite-side padding differing by no more than 1 pixel in `app/src/test/kotlin/com/couchraoke/tv/presentation/join/QrCodeRendererTest.kt`
- [X] T022 [P] [US1] Add non-duet Player 1 selection and start-handoff tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModelTest.kt`
- [X] T023 [P] [US1] Add happy-path start-song, clock-sync gate, `Prepare.chartEndLyricsTimeMs` assertions for parsed positive `#END` and null when absent, audio-sourced `Prepared` duration finalizing `stopAtLyricsTimeMs`, `assignSinger` field assertions for serialized `type="assignSinger"`, `protocolVersion=1`, `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `startMode`, `countdownMs` required only for countdown, finalized `stopAtLyricsTimeMs`, `udpPort`, `songTitle`, `songArtist`, no `connectionId`, selected-device-only delivery after `Prepared` and before `Play`, playback-state assertions for serialized `type="playbackState"`, `protocolVersion=1`, finalized `stopAtLyricsTimeMs`, `countdownRemainingMs`, optional `tsTvMs`, countdown ON/OFF, `Play(stopAtLyricsTimeMs)` emitted only after finalization, `Ready` gate with scoring no-op comment only, and `PlaybackEvent.Ended` returning to Song List without Results tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorStartTest.kt`
- [X] T024 [P] [US1] Add sentence paging, spatially stable current/next lyrics, instrumental-gap completed-sentence 100% highlight, clipped-reveal highlight model, parsed-chart timing using file beats and `BPM_internal = BPM_file * 4`, static P1 note target difficulty thickness, rest-gap indicator, and no live pitch/scoring state tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModelBuilderTest.kt`
- [X] T025 [P] [US1] Add prepare/play, audio duration sourced `Prepared`, no-usable-duration prepare failure when `Prepare.chartEndLyricsTimeMs` is null and audio duration is unavailable, single audio listener before `play()`, `Play(stopAtLyricsTimeMs)` supplying the finalized stop boundary, `songStartTvMs` first-playing formula, 500ms `Ready` fallback, stop-boundary end, optional decorative video never driving timing/session events, non-fatal video fallback, and LibVLC warning/error payload plumbing tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerTest.kt`

### Implementation for User Story 1

- [X] T026 [US1] Implement `KtorNetworkController` WebSocket `hello`, protocol mismatch handling, token validation, session-locked join rejection, `sessionState`, reconnect connectionId replacement, mDNS advertisement, and lifecycle in `app/src/main/kotlin/com/couchraoke/tv/data/network/KtorNetworkController.kt`
- [X] T027 [US1] Implement manifest JSON mapping plus non-cached streamed manifest/TXT fetch helpers and document LAN media range-response assumptions in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkJsonModels.kt` and `app/src/main/kotlin/com/couchraoke/tv/data/network/KtorNetworkController.kt`
- [X] T028 [US1] Implement manifest-to-`IndexedSong` aggregation, invalid-entry rejection including required field and URL invariants, phone disconnect removal, fetch-failure previous-catalog retention, active-song reconnect catalog-stale/deferred-refresh behavior, and Artist→Album→Title sorting in `app/src/main/kotlin/com/couchraoke/tv/data/library/ManifestLibraryManager.kt`
- [X] T029 [US1] Implement Song List catalog sorted by Artist → Album → Title; transient Medley playlist state; case-insensitive substring search across artist, album, and title with 150ms debounce; framework-free Search OK text-input-launch request or native IME fallback; empty states `No phones connected.` / `Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code.` and `No songs found.` / `Open the karaoke app on your phone and make sure the songs folder is set.`; FR-009a LibVLC-backed preview playback state using 500ms focused-tile debounce, `audioUrl`, `previewStartSec`, fallback to 0 seconds when `previewStartSec` is absent or non-positive, TV/system media volume only with no app-level preamp or Settings dependency, screen-scoped lifetime, silent HTTP/player failure, and immediate stop on focus/grid/overlay/modal/settings/singing transitions; Join state whose QR payload is the full WebSocket endpoint URL including `token` and not an NSD/mDNS identifier; Settings header action as an inert/no-op that opens no Settings route/menu/screen/submenu and carries an Iteration 3 wiring comment; Back state that moves grid/left-panel focus to Search, clears active filter from top controls, then exits; and visible-disabled duet/medley execution state with code comments naming Iteration 3 for duet and Iteration 4 for medley wiring in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt`
- [X] T030 [US1] Implement Song List two-column Compose UI and Join overlay UI after T016 common UI definitions are complete and using only shared theme/background/color/typography/surface/focus tokens in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/join/JoinOverlay.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/join/QrCodeRenderer.kt`: left rail preview pane 16:9 display-only non-focusable, visible Medley playlist/Play Medley as disabled no-ops with comments naming Iteration 4 wiring, right body Search field with platform text-input launcher or native IME fallback, Random Song plus disabled/no-op Random Duet and Random Medley with Iteration 3/4 comments, and fixed 3-column 1080p / 4-column 4K song grid; header with Search visually strongest and Join/Settings equal secondary controls, where Settings remains visible but inert/no-op and opens no Settings route/menu/screen/submenu; card default state cover/title/up to 3 lower-right on-image tag chips, focused state reserved artist slot without reflow, weak-artwork fallback showing artist in default state, tag priority `D`, `M`, `R`, `I`, `V`; initial focus first grid tile or Search when empty; DPAD left-panel entry target first Medley row, Play Medley, then Random Medley; no card scale, no per-card ambient animation, no animated grid background; Join overlay as `SurfaceLevel2` modal over Song List with QRose-rendered 400dp static high-contrast QR, at least a 4-module quiet zone, short join code directly below, explicit QRose usage contract (use only `io.github.alexzhirkevich:qrose`, baseline Compose usage `Image(painter = rememberQrCodePainter(payload), contentDescription = ...)`, explicit Join-overlay painter options, and direct painter rendering rather than PNG byte export), short/full payload sizing validation, no animation/overlay intruding into QR quiet zone, and Back closing overlay to Song List
- [X] T031 [US1] Implement non-duet P1 phone selection, Medium difficulty default, P2 visible-disabled state with P2 Difficulty hidden when Player 2 is `(none)`, and Start handoff state in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt`
- [X] T032 [US1] Implement Select Players modal overlay after T016 common UI definitions are complete and using only shared theme/background/color/typography/surface/focus tokens on `SurfaceLevel2` with title `SELECT PLAYERS`, subtitle `<Artist> — <Title>`, active visually primary Player 1 required block, visible disabled Player 2 block for non-duet with a code comment naming Iteration 3 wiring, hidden Player 2 Difficulty when Player 2 is `(none)`, emphasized Start through placement/size/surface contrast not `RewardAccent`, and disabled duet/medley execution with comments naming Iteration 3 for duet and Iteration 4 for medley in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersModal.kt`
- [X] T033 [US1] Implement sentence-paged current/next lyrics, spatially stable sentence state, instrumental-gap completed-sentence 100% highlight, optional clipped-reveal highlight data, parsed-chart timing using file beats, `BPM_internal = BPM_file * 4`, `micDelayMs=0`, static P1 note target conversion with difficulty thickness, rest-gap indicator ranges, `stopAtLyricsTimeMs`, and no live pitch/scoring data in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/DefaultSingingRenderModelBuilder.kt`
- [X] T034 [US1] Implement static single-lane note drawing for one centered full-width neutral lane band at `SingingSingleLaneHeight` (192dp) with `SurfaceLaneBand` / `LaneBandAlpha`, `SingingLaneHorizontalPadding`, `SingingLaneVerticalPadding`, `RadiusMedium`, difficulty-reflective target thickness, rest-gap indicators, P1 accent only on badge/score-box/note markers, no full-lane tint, and no live pitch, hit/miss feedback, or scoring state in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/DefaultPitchLaneRenderer.kt`
- [X] T035 [US1] Implement start-song happy path, TXT fetch, parse, render-model build, `Prepare(chartEndLyricsTimeMs)` with parsed positive `#END` in milliseconds or null when absent/non-positive, audio-sourced `Prepared` wait and stop-boundary finalization as `chartEndLyricsTimeMs ?: effectivePlaybackDurationMs`, clock-sync gate, `assignSinger` serialized after `Prepared` and before `Play` with `type="assignSinger"`, `protocolVersion=1`, `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `startMode`, `countdownMs`, finalized `stopAtLyricsTimeMs`, `udpPort`, `songTitle`, and `songArtist`, with `countdownMs` required only for countdown, no `connectionId`, and delivery only to selected singer phones; implement playback state serialized after `Prepared` and before `Play` with `type="playbackState"`, `protocolVersion=1`, finalized `stopAtLyricsTimeMs`, `countdownRemainingMs`, optional `tsTvMs`, countdown ON mapping to `startMode="countdown"` and `countdownMs = countdownSeconds * 1000`, countdown OFF mapping to `startMode="live"` with omitted `countdownMs`, `Play(stopAtLyricsTimeMs)` only after finalization, an explicit blank/no-op scoring integration stub with documentation that Iteration 2 must implement chart loading and scoring start, and `PlaybackEvent.Ended` returning to Song List without Results with an Iteration 2 Results comment in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [X] T036 [US1] Implement prepare/play handling, screen-scoped LibVLC handle lifetime, no-op handling for medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` with code comments that they must be wired in Iteration 4, single audio handle event listener registered before `play()`, `Prepared(effectivePlaybackDurationMs)` sourced from the prepared audio handle duration, prepare failure when no usable duration exists and `Prepare.chartEndLyricsTimeMs` is null, first-playing `Ready` with `songStartTvMs = (System.nanoTime() / 1_000_000) − playerHandle.timeMs`, 500 ms fallback `Ready`, current position, `Play(stopAtLyricsTimeMs)` storing the finalized stop boundary, `stopAtLyricsTimeMs` end event, best-effort decorative full-screen video/background handling that never drives timing and falls back to static background on video failure or runtime gameplay-degradation report, LibVLC diagnostic payload mapping for blocking audio errors, and teardown of the LibVLC SongList preview handle on SongList exit plus Singing LibVLC handles on Singing exit in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [X] T037 [US1] Implement LibVLC streamed audio, Song List preview, and optional decorative full-screen video handle adapter in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/VlcLibVlcPlayerHandle.kt`, with one handle per LibVLC MediaPlayer, audio as required playable media, video configured without audio and treated as non-fatal static-background fallback, `durationMs` exposed for `Prepared`, warning/error log capture truncated to 120 chars for audio error payloads, static admission/load-time optional-video disablement when video is >720p and hardware decoder support cannot be confirmed with Android codec capability inspection, runtime gameplay-degradation report support that disables decorative video and falls back to static background without affecting audio/session state, and no dropped-decorative-video-frame tests or requirements
- [X] T038 [US1] Implement Singing ViewModel happy-path state from coordinator, playback controller, render model, countdown overlay state at 1 Hz when Ready countdown is ON, immediate playback state when Ready countdown is OFF, normal-end Song List return state, and Back-to-Pause state handoff in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`
- [X] T039 [US1] Implement Singing screen happy-path UI after T016 common UI definitions are complete and using only shared theme/background/color/typography/surface/focus tokens with best-effort full-screen video/background presentation using `SurfaceView` with `setZOrderMediaOverlay(true)` and not `TextureView`, visual stack ordered as video/background SurfaceView behind a separate pitch-lane renderer SurfaceView behind Compose metadata/score/lyrics/countdown/pause overlays, fallback priority admitted video then song static background then bundled app singing background with no black-empty final fallback, optional video disabled by static admission policy or runtime gameplay-degradation report, top metadata intro strip at `SingingTopIntroStripHeight` (72dp), active minimal strip at `SingingTopMinimalStripHeight` (40dp), lane region, full-width bottom lyrics band at `SingingBottomLyricsBandHeight` (160dp) with `SurfaceLyricsBand` / `LyricsBandAlpha`, one centered neutral full-width lane band at `SingingSingleLaneHeight` (192dp), score box `144dp × 88dp` inset 16dp from lane right edge, sentence rating reserved below the score box, exactly two lyrics lines with current `LyricsCurrent` stronger and next `LyricsNext` muted, progressive clipped-reveal highlight, no third line, no pre-page/blank during instrumental gaps, constant `00000` score with an Iteration 2 scoring comment, elapsed time bottom-right in zero-padded `MM:SS`, allowed active motion limited to lyric highlight and static note lane rendering, no dropped-decorative-video-frame requirements, no background animation/blur/bloom/multi-panel entrance/particles/full-lane pulses/layout-affecting animation, and countdown overlay using a centered `DisplayHeroNumber` numeral over a dimmed static background at 1 Hz with only numeral scale-pop in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingScreen.kt`
- [X] T040 [US1] Wire `MainActivity`, `CouchraokeTheme`, and `AppNavHost` after T016 common UI definitions are complete for Song List, Select Players modal, Singing, no Settings route/menu/screen/submenu, and inert Results route declaration; implement global Back behavior where Song List clears filter before exit, modal dialogs/overlays close to the underlying screen, Singing opens Pause overlay, Results returns to Song List, and include code comments that the Settings header no-op must be wired in Iteration 3 and Results must be wired in Iteration 2 in `app/src/main/kotlin/com/couchraoke/tv/MainActivity.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt`
- [X] T041 [US1] Run scoped US1 `testBranch` for the changed files under `app/src/main/kotlin/com/couchraoke/tv/data/network/`, `app/src/main/kotlin/com/couchraoke/tv/data/library/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/`, Android manifest/network-security config, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

### Spot-Check Targets: US1

These are the behaviours most likely to be incomplete or subtly wrong. Orchestration should verify these explicitly, not just check that tests pass.

**Protocol / session**
- **`assignSinger` ordering** — must be sent after `Prepared` (so `stopAtLyricsTimeMs` is finalized) and before `Play`. Sending before `Prepared` means the phone gets an unfinalized stop boundary; sending after `Play` means the phone misses the cue. Verify the coordinator sequence explicitly.
- **`assignSinger` has no `connectionId`** — the field must be absent, not null or empty. Check the serialized JSON, not just the data class.
- **`countdownMs` absent in live mode** — for `startMode="live"`, `countdownMs` must not appear in the serialized JSON at all. An absent field and a `null`/`0` field behave differently on the phone.
- **`stopAtLyricsTimeMs` failure path** — when `#END` is absent/non-positive AND the audio handle cannot report a usable duration, preparation MUST fail through the error path. Verify the controller does not silently fall back to a hardcoded or zero value.
- **QR payload is a full WebSocket URL** — the QR must encode the complete `ws://…/?token=…` URL, not just the token code, not an mDNS service name. Scan the rendered QR with a phone to verify the decoded string.

**Playback timing**
- **`songStartTvMs` formula** — must be `(System.nanoTime() / 1_000_000) − playerHandle.timeMs` on the first-playing event, not just the wall clock at start. If `playerHandle.timeMs` is not subtracted, the phone's sync will be off by the pre-buffer duration.
- **`Play(stopAtLyricsTimeMs)` is enforced by the UI** — the controller must stop the audio handle when `currentPositionMs ≥ stopAtLyricsTimeMs` and emit `PlaybackEvent.Ended`. Verify it does not play to end-of-file when `#END` is shorter than the audio file.
- **Single audio event listener registered before `play()`** — if the listener is attached after `play()`, the first-playing event may fire and be missed, leaving the coordinator waiting for `Ready` forever.

**Song List preview**
- **Preview stops when the Join overlay opens** — not just when focus moves to another tile. The stop trigger is any overlay/modal/settings/singing transition, not only focus change. Verify `SongListViewModel` stops preview on Join state becoming active.
- **Preview seeks to `previewStartSec`** — if `previewStartSec` is absent or ≤ 0, must seek to 0. Verify the controller does not skip the seek call entirely when the value is 0.
- **Preview LibVLC handle is torn down on SongList exit** — not paused, torn down. Navigate from Song List to Singing and back; verify no orphaned LibVLC handle continues buffering in the background.

**Lyrics**
- **Instrumental gap keeps 100% highlight, no blank** — when the current sentence ends and the next sentence has not started, the completed sentence must remain fully highlighted. The most common failure is briefly showing blank or pre-paging to the next sentence.
- **Exactly two lines, never three** — both in the data model and in the Compose layout. Verify the layout enforces a hard maximum regardless of text length.
- **Spatially stable during a sentence** — the two displayed lines must not shift position as the highlight progresses through syllables. Only the highlight moves; the text anchors stay fixed.

**Singing screen**
- **Background fallback is never black** — fallback priority is admitted video → song `backgroundUrl` → bundled app singing background. If all three are unavailable, the bundled background still shows. A black or transparent final fallback is a spec violation.
- **Video surface is `SurfaceView` with `setZOrderMediaOverlay(true)`, not `TextureView`** — read the factory lambda in `SingingScreen`; `TextureView` is explicitly prohibited.
- **Metadata strip collapses once `isPlaying = true`** — intro strip (`SingingTopIntroStripHeight` 72dp) must collapse to minimal strip (`SingingTopMinimalStripHeight` 40dp) when active playback begins. Verify the Compose layout switches on the `isPlaying` flag, not on a timer.

**Select Players**
- **P2 Difficulty is hidden, not disabled, when Player 2 is `(none)`** — the widget must not be composed at all, not merely set to non-focusable or greyed out. Check with `composeRule.onNodeWithTag(…).assertDoesNotExist()`, not just `assertIsNotEnabled()`.
- **Start emphasis uses placement/surface contrast, not `RewardAccent`** — read the Start button composable and verify no `RewardAccent` colour token appears on it.

**Checkpoint**: User Story 1 is independently functional and is the MVP stopping point.

---

## Phase 4: User Story 2 - Control solo playback from the TV remote (Priority: P2)

**Goal**: Allow Back during singing to open Pause, Resume playback, Restart with confirmation and a new `songInstanceSeq`, and Quit to Song List with confirmation.

**Independent Test**: Start a solo song, press Back, test Resume, Restart Song, and Quit to Song List on separate runs, and verify playback/session/navigation outcomes.

### Validation for User Story 2

- [X] T042 [P] [US2] Add pause, resume, restart-from-`startSec`, quit, score reset, new `songInstanceSeq`, and `assignSinger` resend coordinator tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorControlsTest.kt`
- [X] T043 [P] [US2] Add pause, resume, seek-to-`startSec` on restart, stop, audio-focus release, pause on transient audio-focus loss, and resume on `AUDIOFOCUS_GAIN` controller tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerControlsTest.kt`
- [X] T044 [P] [US2] Add Back, Pause, Resume, Restart Confirm, Quit Confirm, Cancel-default-focus, modal Back-close, and Results Back-to-Song-List state tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModelControlsTest.kt`

### Implementation for User Story 2

- [X] T045 [US2] Extend pause, resume, restart-from-`startSec`, quit-to-song-list, new `songInstanceSeq`, `assignSinger` resend, playback-state broadcast, score reset placeholder state, and session-open behavior in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [X] T046 [US2] Extend pause, resume, stop, seek-to-`startSec` on restart, audio-focus abandonment, pause on transient audio-focus loss, and resume on `AUDIOFOCUS_GAIN` behavior in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [X] T047 [US2] Implement Back, Pause, Resume, Restart Confirm, Quit Confirm, Cancel-default-focus, modal Back-close, and Results Back-to-Song-List UI state in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`
- [X] T048 [US2] Implement Pause overlay, Restart confirmation, and Quit confirmation Compose UI after T016 common UI definitions are complete and using only shared theme/background/color/typography/surface/focus tokens in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingOverlays.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingScreen.kt`
- [X] T049 [US2] Run scoped US2 `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingOverlays.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

### Spot-Check Targets: US2

These are the behaviours most likely to be incomplete or subtly wrong. Orchestration should verify these explicitly, not just check that tests pass.

- **`songInstanceSeq` increments on Restart** — confirm the coordinator produces a new `songInstanceSeq` value (not the same one) and that `assignSinger` is resent to the phone with the new value before `Play` is emitted. The phone silently ignores duplicate seq values.
- **Restart seeks to `startSec`, not 0** — `DefaultPlaybackController` must call `seek(startSec)` using the plan's `song.startSec`, not zero. Test with a song whose `#START > 0`.
- **Audio-focus resume gate** — on `AUDIOFOCUS_GAIN`, the controller must only resume if the pause was caused by audio-focus loss, not a user-initiated pause. Resuming a user-paused song on focus gain is a regression.
- **Audio-focus is abandoned on Restart** — `AUDIOFOCUS_GAIN` must be re-requested after restart, not assumed to be held. Check that focus is abandoned then re-requested, not just held through restart.
- **Pause overlay default focus is Cancel/Resume** — when the Pause overlay opens, focus MUST land on Resume/Cancel, not on Restart or Quit. Destructive actions must not be the default.
- **Back closes the open modal, not the screen** — Back while Pause/Restart-confirm/Quit-confirm is showing must close the modal only. A second Back then opens Pause. Verify the ViewModel state machine handles this without collapsing both in one press.
- **`playbackState` is broadcast on every transition** — pause, resume, restart, and quit must each broadcast a `playbackState` message to phones. Verify the coordinator does not skip broadcast on the restart path (easy to only broadcast on the new `Play`).
- **Session is Open after Quit** — after Quit to Song List, `coordinator.state.phase` must be `GamePhase.Open`. If it remains `Paused` or `Stopped`, the next song start will misbehave.

**Checkpoint**: User Stories 1 and 2 work independently and together.

---

## Phase 5: User Story 3 - Recover from startup and playback interruptions (Priority: P3)

**Goal**: Provide blocking recovery UI for no connected phones, countdown disconnect, start failure, playback failure, and audio-focus failure while returning the session to a clear open state.

**Independent Test**: Force no-phone Select Players, countdown disconnect, unreachable audio URL, LibVLC error, and audio-focus denial paths, then verify blocking modal copy, destination, and session-open state.

### Validation for User Story 3

- [X] T050 [P] [US3] Add Select Players no-phone recovery tests for blocking message `No phones connected`, `Open Join QR` action opening the same Join QR overlay as the Song List Join button, and no Settings route/menu/screen/submenu appearing in `app/src/test/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersRecoveryTest.kt`
- [X] T051 [P] [US3] Add countdown disconnect, spectator disconnect, start failure, playback error, and session-open tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorRecoveryTest.kt`
- [X] T052 [P] [US3] Add LibVLC error, warning-line truncation, audio-focus denial, transient loss pause, `AUDIOFOCUS_GAIN` resume, permanent loss, and focus abandonment tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerErrorTest.kt`
- [X] T053 [P] [US3] Add interruption shell layout and modal-copy tests for Pause, Countdown disconnect, Start-failure / Playback error, and Select-Players no-phone state in `app/src/test/kotlin/com/couchraoke/tv/presentation/common/InterruptionShellTest.kt`; verify the shell contract is reusable for future Song-Library refresh errors without implementing Song-Library refresh behavior or any Settings screen/menu/submenu

### Implementation for User Story 3

- [X] T054 [US3] Implement no-phone Select Players state and `Open Join QR` action that opens the same Join QR overlay as the Song List Join button, with no Settings route/menu/screen/submenu in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersModal.kt`
- [X] T055 [US3] Implement required-singer countdown disconnect, spectator disconnect no-op, and reconnect event handling in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [X] T056 [US3] Implement start-failure and unreachable-audio recovery state with specified `ERROR` modal copy in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [X] T057 [US3] Implement LibVLC warning/error log capture, 120-character truncation, encountered-error event handling for required audio, non-fatal video/background failure fallback, audio-focus denial, transient loss, gain resume, permanent loss, focus abandonment, and no-op medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` error-path safety with comments that they must be wired in Iteration 4 in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [X] T058 [US3] Implement shared centered elevated interruption shell after T016 common UI definitions are complete and using only shared theme/background/color/typography/surface/focus tokens with `SurfaceLevel2`, 32dp padding, dark scrim, no runtime blur, and entry focus lock for Pause, Countdown disconnect, Start-failure / Playback error, and Select-Players no-phone state in `app/src/main/kotlin/com/couchraoke/tv/presentation/common/InterruptionShell.kt`; keep the shell reusable for future Song-Library refresh errors without implementing Song-Library refresh behavior or any Settings screen/menu/submenu
- [X] T059 [US3] Wire blocking no-phone, disconnected, and error modal routing while reopening the session in `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt` and `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [X] T060 [US3] Run scoped US3 `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/common/InterruptionShell.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

### Spot-Check Targets: US3

These are the behaviours most likely to be incomplete or subtly wrong. Orchestration should verify these explicitly, not just check that tests pass.

- **Exact ERROR modal copy** — body line 1 must be exactly `This song can't be played.` (period included). Body line 2 is the most recent LibVLC warning/error log entry truncated to **exactly** 120 characters. Verify both the truncation length and that it is the most recent entry, not the first.
- **Spectator disconnect is a no-op** — disconnecting a phone that is not an assigned required singer must not pause, error, or interrupt playback in any way. Verify the coordinator explicitly ignores non-singer disconnect events.
- **Open Join QR reuses the Song List overlay** — the `Open Join QR` action in the no-phone Select Players state must open the same `JoinOverlay` composable that the Song List Join button opens, not a separate QR screen or a new implementation. Check that no second QR code path exists in the codebase.
- **Video failure does not surface as an error** — `LibVlcEvent.EncounteredError` on the decorative video handle must trigger fallback to static background only. It must not propagate to `PlaybackEvent.Error`, must not stop audio, and must not open the `ERROR` modal. Verify the controller distinguishes audio vs video handle errors.
- **Audio-focus abandonment on all exit paths** — focus must be abandoned on song end, error exit, AND Restart (before re-requesting). Check each of the three paths independently; missing the Restart path is the most common gap.
- **Session is Open after every recovery path** — after each of: no-phone error, countdown disconnect, start failure, playback error — `coordinator.state.phase` must be `GamePhase.Open`. Verify each path independently rather than assuming a shared code path covers all.
- **InterruptionShell locks focus inside** — DPAD navigation must not reach Song List, Song Grid, or any underlying composable while a blocking modal is showing. Confirm `focusProperties { canFocus = false }` or equivalent is applied to the background.
- **No Settings route appears anywhere in US3** — T054 adds `Open Join QR` to Select Players. Confirm no Settings screen, menu, or submenu is reachable from that path or from the interruption shell actions.

**Checkpoint**: All user stories are independently functional and recover from defined interruption paths.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate UI fidelity, lyrics behavior constraints, and final feature completion without expanding Iteration 1 scope.

- [ ] T061 [P] Add Compose previews after T016 common UI definitions are complete for Song List, Join overlay, Select Players, no-phone, Singing single-singer layout, countdown, pause, restart confirm, quit confirm, disconnect, and error states in `app/src/main/kotlin/com/couchraoke/tv/presentation/previews/SoloSingPreviews.kt`, excluding medley, live pitch, scoring, Results, Settings, and Song-Library refresh error screens
- [ ] T062 Compare rendered UI previews or screenshots after T016 common UI definitions are complete against wireframes in `specs/002-solo-sing-playback/spec.md` using preview definitions in `app/src/main/kotlin/com/couchraoke/tv/presentation/previews/SoloSingPreviews.kt`
- [ ] T062A Audit rendered UI previews or screenshots against the visual-system definitions in `specs/002-solo-sing-playback/spec.md`: all visible Iteration 1 UI uses declared spacing/radius/border/layout/typography/color/surface tokens; focus is border-plus-plate only; Search/Join/Settings emphasis matches spec; disabled no-op controls are visible but non-focusable where required; active singing contains no prohibited blur/glow/bloom/particle/background animation/full-lane pulse/layout-affecting animation; and player/reward accents are not used for generic focus, Start emphasis, general chrome, or lane-body fill
- [ ] T063 Run final feature-scoped `testBranch` for all changed feature files under `app/src/main/kotlin/com/couchraoke/tv/` and `app/src/test/kotlin/com/couchraoke/tv/` using validation selectors derived from `specs/002-solo-sing-playback/quickstart.md`; the passing selector set MUST explicitly include F15-style session lifecycle coverage, minimal song-start clock-sync gate coverage, F22 GamePhase FSM coverage, actual Join QR renderer short/full payload sizing coverage, Song List LibVLC preview debounce/stop/fallback/lifetime coverage, audio-duration Prepared/Ready/stop-boundary coverage, optional-video static admission/fallback/runtime gameplay-degradation report coverage, visual-system token/focus/motion contract coverage, no dropped-decorative-video-frame requirements, and the cumulative TV-owned end-to-end flow through the TV UI
- [ ] T064 Run the lowest-practical-layer solo-sing quickstart verification from `specs/002-solo-sing-playback/quickstart.md` against the runtime entry point in `app/src/main/kotlin/com/couchraoke/tv/MainActivity.kt`, using emulator/device validation only for Android TV runtime behavior that cannot be meaningfully covered by JVM, Robolectric, direct component, or screenshot validation, and capture evidence for launch app → pair one phone → load manifest → verify Song List preview starts/stops from focused song → select one non-duet song → select P1 → start playback with lyrics/static lane/`00000` score → verify optional video falls back to static background without stopping audio when disabled by static admission, video failure, or runtime gameplay-degradation report → return to Song List

### Spot-Check Targets: Phase 6

These are the visual and cross-cutting issues most likely to slip through automated tests. Orchestration should verify these in the rendered screenshots and by reading the source, not just by checking that `testBranch` passes.

- **Lane body is neutral, not cyan** — `Player1Accent` (cyan) must appear only on the singer badge, score-box accents, and note markers. The lane band fill must use `SurfaceLaneBand`. Any cyan tint on the lane body is a scope violation.
- **Score placeholder is `00000`** — five zeros, not `XXXXX`, not blank, not a live value. An Iteration 2 wiring comment must be present in the source alongside it.
- **Disabled controls are visible and non-focusable** — Random Duet, Random Medley, Player 2 block, and Play Medley must render visibly but DPAD must skip over them. Verify both: they appear in the screenshot AND focus cannot land on them. Invisible-but-non-focusable is a separate failure mode from focusable-but-greyed.
- **Settings button is visible and inert everywhere** — the Settings button in the Song List header must appear but open no screen, menu, or submenu on any button press. Read the click handler in source to confirm it is a no-op with an Iteration 3 comment, not just unlinked from a nav graph.
- **Focus is border-plus-plate only, no glow or elevation** — check focused song cards, focused buttons in all overlays, and focused modals. The Compose default Material focus indicators (ripple, elevation, shadow) must not appear. Roborazzi screenshots alone will not catch this reliably; read `Focus.kt` and verify no `elevation`, `shadow`, or `BlurMaskFilter` is used in focus handling.
- **All required preview states exist in `SoloSingPreviews.kt`** — verify one preview per: Song List, Join overlay, Select Players (normal + no-phone), Singing (single singer + countdown + pause + restart-confirm + quit-confirm + disconnect + error). Missing previews mean those states were never screenshot-validated.
- **No prohibited singing-screen effects** — during active singing, verify the source contains no `blur`, `BlurMaskFilter`, `Animatable` or `animate*AsState` on the lane, lyrics region, or score position, no particle or pulse effects, and no background animation tied to video content.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on Phase 1; blocks all user-story implementation.
- **Phase 3 US1**: Depends on Phase 2; delivers the MVP end-to-end browse/start/playback slice.
- **Phase 4 US2**: Depends on Phase 2 for contracts and on US1 runtime screens for end-to-end manual validation; controller/coordinator tests can be developed against fakes after Phase 2.
- **Phase 5 US3**: Depends on Phase 2 for contracts and on the relevant US1/US2 runtime paths for end-to-end recovery validation; no-phone recovery can start after Phase 2.
- **Phase 6 Polish**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: MVP; can start after Foundational.
- **US2 (P2)**: Adds controls to the active Singing flow; validate after US1, though isolated controller/coordinator tasks can be worked independently.
- **US3 (P3)**: Adds interruption handling across Select Players, countdown, and playback; validate after the paths it interrupts exist.

### Within Each User Story

- Write and run the story tests first; confirm they fail for missing behavior before implementation.
- Implement models/contracts before services/controllers.
- Implement common UI definitions before any visual UI task, then state/ViewModels before Compose UI when both are needed.
- Run the scoped story `testBranch` gate after the story implementation is complete.
- Audit changed files against scope before moving to the next story.

---

## Parallel Opportunities

- T002 through T004 can run after T001; T003 and T004 can run in parallel because they write separate fixture files.
- T005 through T008 can run in parallel because they write separate test files.
- T009 through T017 are mostly contract/common definition files and should be integrated sequentially unless assigned to isolated worktrees; T017 blocks all visual UI tasks.
- T018 through T025 can run in parallel after Phase 2 because they write separate test files, except visual UI tests must validate against T016 common UI definitions.
- T042 through T044 can run in parallel after Phase 2 and relevant US1 interfaces are stable.
- T050 through T053 can run in parallel after Phase 2 and relevant recovery seams are stable.
- T062 depends on T061 because screenshot comparison uses the preview definitions created there; all visual UI implementation and preview tasks depend on T016 common UI definitions.

---

## Parallel Example: User Story 1

```text
Task: "Add token-gated WebSocket, mDNS advertisement, manifest fetch, and TXT fetch tests in app/src/test/kotlin/com/couchraoke/tv/data/network/KtorNetworkControllerTest.kt"
Task: "Add manifest aggregation, invalid-entry rejection, and Artist→Album→Title sort tests in app/src/test/kotlin/com/couchraoke/tv/data/library/ManifestLibraryManagerTest.kt"
Task: "Add sentence paging and static P1 note target tests in app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModelBuilderTest.kt"
```

## Parallel Example: User Story 2

```text
Task: "Add pause, resume, restart, quit, score reset, and new songInstanceSeq coordinator tests in app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorControlsTest.kt"
Task: "Add pause, resume, seek, stop, and audio-focus release controller tests in app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerControlsTest.kt"
Task: "Add Back, Pause, Resume, Restart Confirm, Quit Confirm, and default-focus state tests in app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModelControlsTest.kt"
```

## Parallel Example: User Story 3

```text
Task: "Add Select Players no-phone recovery tests in app/src/test/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersRecoveryTest.kt"
Task: "Add countdown disconnect, spectator disconnect, start failure, playback error, and session-open tests in app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorRecoveryTest.kt"
Task: "Add LibVLC error, warning-line truncation, audio-focus denial, transient loss, permanent loss, and focus abandonment tests in app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerErrorTest.kt"
```

---

## Validation Commands

Use scoped validation after each implementation task and story gate. Command shape:

```bash
rtk timeout 10m ./gradlew :app:testBranch \
  --src=com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator,com.couchraoke.tv.presentation.playback.DefaultPlaybackController \
  --test=com.couchraoke.tv.domain.playback.PlaybackCoordinatorStartTest,com.couchraoke.tv.presentation.playback.PlaybackControllerTest
```

Final feature validation should include every changed production/test FQCN from the feature, using the selector pattern in `specs/002-solo-sing-playback/plan.md` and `specs/002-solo-sing-playback/quickstart.md`.

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 Setup.
2. Complete Phase 2 Foundational contracts.
3. Complete Phase 3 User Story 1.
4. Stop and validate the end-to-end MVP independently with scoped `testBranch` plus emulator UI verification.

### Incremental Delivery

1. Setup + Foundational contracts establish shared seams.
2. US1 delivers phone discovery, manifest browsing, player selection, playback start, Singing happy path, and Song List return on end.
3. US2 adds TV-remote playback controls without changing the US1 happy path.
4. US3 adds defined recovery paths and blocking modals without adding scoring, Results, live pitch, duet, or medley execution.
5. Polish verifies previews/screenshots, lyrics behavior constraints, and final scoped validation.

### Scope Guard

- Do not implement live UDP pitch ingestion, pitch cursor, hit/miss feedback, scoring updates, Results screen, duet execution, or medley execution in these tasks.
- Do not persist remote song assets; stream manifest, chart, audio, video, covers, and backgrounds from phone-provided LAN URLs.
- Do not add dependencies unless the plan is amended and `gradle/libs.versions.toml` is updated first.
- Do not commit changes unless explicitly requested.
