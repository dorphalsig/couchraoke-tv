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

- [X] T001 Add qrcode-kotlin (`io.github.g0dkar:qrcode-kotlin:4.5.0`) to `gradle/libs.versions.toml` and `app/build.gradle.kts`, and confirm the remaining planned dependencies already exist
- [X] T002 [P] Create connected-phone, manifest, and playback test fixtures in `app/src/test/kotlin/com/couchraoke/tv/fixtures/SoloSingFixtures.kt`
- [X] T003 [P] Create static USDX chart fixture helpers for solo playback tests in `app/src/test/kotlin/com/couchraoke/tv/fixtures/SoloSingUsdxFixtures.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Define core contracts and model types that all user stories depend on.

**Critical**: No user story implementation should begin until this phase is complete and audited.

### Validation for Foundational Contracts

- [X] T004 [P] Add network protocol contract tests in `app/src/test/kotlin/com/couchraoke/tv/data/network/NetworkControllerContractTest.kt`
- [X] T005 [P] Add playback domain contract tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackContractsTest.kt`
- [X] T006 [P] Add playback UI seam contract tests for Iteration 1 intents/events and no-op medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` intent handling with comments that they must be wired in Iteration 4 in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContractsTest.kt`
- [X] T007 [P] Add singing rendering contract tests for single-singer layout tokens, lyrics band constraints, score placeholder contract, and no live pitch/scoring state in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderContractsTest.kt`

### Implementation for Foundational Contracts

- [X] T008 Implement network protocol model types in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkModels.kt`
- [X] T009 Implement `NetworkController` interface in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkController.kt`
- [X] T010 Implement playback domain model types in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackModels.kt`
- [X] T011 Implement `PlaybackCoordinator` interface in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinator.kt`
- [X] T012 Implement playback UI intent/event types and LibVLC seam in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContract.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/LibVlcPlayerHandle.kt`, including medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` intents as no-ops with code comments that they must be wired in Iteration 4
- [X] T013 Implement singing render models and renderer contracts in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModels.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModelBuilder.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/PitchLaneRenderer.kt`, preserving Iteration 1 static-note/no-live-pitch/no-scoring state and carrying layout data needed for top metadata, single lane, score placeholder, elapsed time, and two-line lyrics
- [X] T014 Implement shared route and modal-state contracts in `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppRoute.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/common/UiModalState.kt`
- [X] T015 Run scoped foundational `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkController.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/PlaybackContract.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModels.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

**Checkpoint**: Foundation ready; user story work can start.

---

## Phase 3: User Story 1 - Browse and start a solo song (Priority: P1) MVP

**Goal**: Discover one phone, load its manifest into Song List, open Join and Select Players UI, assign one P1 singer, prepare playback, and show the Singing screen with streamed media, two-line lyrics, static note lanes, and a constant `00000` score.

**Independent Test**: Launch the app, discover one LAN phone, load the manifest, select a non-duet song, assign one Player 1 phone, start playback, verify audio playback starts, and verify sentence-paged lyrics plus static note lanes render on Singing.

### Source-extracted UI requirements for User Story 1

The Song List, preview, Join, and Back tasks below preserve these source descriptions from `original_spec/tv_app.md` §2.6.9, §2.6.10, §2.6.12, and §2.6.13:

- **Back behavior**: "From Song List: if filter active, clear filter; otherwise exit app." "From modal dialogs/overlays: close overlay, return to underlying screen." "From Singing: open Pause overlay (Resume / Restart Song / Quit to Song List)." "From Results: return to Song List."
- **Song Preview Playback / When preview plays**: "Song tile is focused AND focus remains on same song for **500ms** (debounce)." Iteration 1 does not implement Settings > Audio Preview Volume or an app-level preview preamp; preview audibility follows TV/system media volume.
- **Song Preview Playback / Preview stops immediately when**: "Focus moves to a different song tile, leaves grid, or overlay/modal/settings/singing opens."
- **Song Preview Playback / What plays**: "Uses `audioUrl` from cached manifest, seeks to `previewStartSec`." "If `previewStartSec > 0.0`, use it; otherwise fallback: `pos = audioLengthSec / 4` (clamped to 60s if > 120s)." "Plays from start position until stopped (no fixed 10s limit)." Preview uses TV/system media volume only in Iteration 1; Settings > Audio Preview Volume zero-disable is deferred to Iteration 3 Settings scope. "If HTTP fails, suppress silently."
- **Song Preview Playback / Media player lifetime**: "Media players are screen-scoped." "A preview player belongs only to SongListScreen and MUST be torn down when SongListScreen exits." "Any media player created for SingingScreen, including medley transition players, MUST be torn down when SingingScreen exits."
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
- **QR payload**: "The QR code MUST encode the full WebSocket endpoint URL including the `token` query parameter (e.g., `ws://192.168.1.10:8080/?token=ABCDEFGH`). It MUST NOT encode an NSD/mDNS service-discovery identifier. Phones that scan the QR code connect directly to the encoded URL without any additional discovery step." Use qrcode-kotlin for actual QR rendering and cover issue #197 by testing both short and full endpoint payload sizing/centering.
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

- [ ] T016 [P] [US1] Add token-gated WebSocket, mDNS advertisement, manifest fetch, and TXT fetch tests in `app/src/test/kotlin/com/couchraoke/tv/data/network/KtorNetworkControllerTest.kt`
- [ ] T017 [P] [US1] Add manifest aggregation, invalid-entry rejection, and Artist→Album→Title sort tests in `app/src/test/kotlin/com/couchraoke/tv/data/library/ManifestLibraryManagerTest.kt`
- [ ] T018 [P] [US1] Add Song List tests for case-insensitive search across artist/album/title with 150ms debounce, framework-free text-input-launch request or native IME fallback on Search OK, empty-state copy, disabled duet/medley controls with comments naming Iteration 3 for duet and Iteration 4 for medley wiring, Settings header affordance remaining inert/no-op with no Settings route/menu/screen/submenu opened and an Iteration 3 wiring comment, Back clearing filter before exit, preview debounce/stop/fallback rules using TV/system media volume only, and Join overlay QR payload/state in `app/src/test/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModelTest.kt`
- [ ] T018a [P] [US1] Add QR renderer tests for qrcode-kotlin issue #197 using short and full endpoint payloads, asserting rendered PNG bounds fit within the requested square, preserve a 4-module quiet zone, and center QR content with opposite-side padding differing by no more than 1 pixel in `app/src/test/kotlin/com/couchraoke/tv/presentation/join/QrCodeRendererTest.kt`
- [ ] T019 [P] [US1] Add non-duet Player 1 selection and start-handoff tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModelTest.kt`
- [ ] T020 [P] [US1] Add happy-path start-song, clock-sync gate, `assignSinger` field assertions for `type="assignSinger"`, `protocolVersion=1`, `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `startMode`, `countdownMs` required only for countdown, `stopAtLyricsTimeMs`, `udpPort`, `songTitle`, `songArtist`, no `connectionId`, selected-device-only delivery, countdown ON/OFF, `Ready` gate with scoring-call comment only, and `PlaybackEvent.Ended` returning to Song List without Results tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorStartTest.kt`
- [ ] T021 [P] [US1] Add sentence paging, spatially stable current/next lyrics, instrumental-gap completed-sentence 100% highlight, clipped-reveal highlight model, static P1 note target, and no live pitch/scoring state tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingRenderModelBuilderTest.kt`
- [ ] T022 [P] [US1] Add prepare/play, single listener before `play()`, `songStartTvMs` first-playing formula, `Prepared`, 500ms `Ready` fallback, and stop-boundary end tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerTest.kt`

### Implementation for User Story 1

- [ ] T023 [US1] Implement `KtorNetworkController` WebSocket `hello`, token validation, `sessionState`, mDNS advertisement, and lifecycle in `app/src/main/kotlin/com/couchraoke/tv/data/network/KtorNetworkController.kt`
- [ ] T024 [US1] Implement manifest JSON mapping plus streamed manifest/TXT fetch helpers in `app/src/main/kotlin/com/couchraoke/tv/data/network/NetworkJsonModels.kt` and `app/src/main/kotlin/com/couchraoke/tv/data/network/KtorNetworkController.kt`
- [ ] T025 [US1] Implement manifest-to-`IndexedSong` aggregation, invalid-entry rejection, and Artist→Album→Title sorting in `app/src/main/kotlin/com/couchraoke/tv/data/library/ManifestLibraryManager.kt`
- [ ] T026 [US1] Implement Song List catalog sorted by Artist → Album → Title; transient Medley playlist state; case-insensitive substring search across artist, album, and title with 150ms debounce; framework-free Search OK text-input-launch request or native IME fallback; empty states `No phones connected.` / `Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code.` and `No songs found.` / `Open the karaoke app on your phone and make sure the songs folder is set.`; preview playback state using 500ms focused-tile debounce, `audioUrl`, `previewStartSec`, fallback `audioLengthSec / 4` clamped to 60s if >120s, TV/system media volume only with no app-level preamp or Settings dependency, silent HTTP failure, and immediate stop on focus/grid/overlay/modal/settings/singing transitions; Join state whose QR payload is the full WebSocket endpoint URL including `token` and not an NSD/mDNS identifier; Settings header action as an inert/no-op that opens no Settings route/menu/screen/submenu and carries an Iteration 3 wiring comment; Back state that moves grid/left-panel focus to Search, clears active filter from top controls, then exits; and visible-disabled duet/medley execution state with code comments naming Iteration 3 for duet and Iteration 4 for medley wiring in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListViewModel.kt`
- [ ] T027 [US1] Implement Song List two-column Compose UI and Join overlay UI in `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/SongListScreen.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/join/JoinOverlay.kt`, and `app/src/main/kotlin/com/couchraoke/tv/presentation/join/QrCodeRenderer.kt`: left rail preview pane 16:9 display-only non-focusable, visible Medley playlist/Play Medley as disabled no-ops with comments naming Iteration 4 wiring, right body Search field with platform text-input launcher or native IME fallback, Random Song plus disabled/no-op Random Duet and Random Medley with Iteration 3/4 comments, and fixed 3-column 1080p / 4-column 4K song grid; header with Search visually strongest and Join/Settings equal secondary controls, where Settings remains visible but inert/no-op and opens no Settings route/menu/screen/submenu; card default state cover/title/up to 3 lower-right on-image tag chips, focused state reserved artist slot without reflow, weak-artwork fallback showing artist in default state, tag priority `D`, `M`, `R`, `I`, `V`; initial focus first grid tile or Search when empty; DPAD left-panel entry target first Medley row, Play Medley, then Random Medley; no card scale, no per-card ambient animation, no animated grid background; Join overlay as `SurfaceLevel2` modal over Song List with qrcode-kotlin-rendered 400dp static high-contrast QR, 4-module quiet zone, short join code directly below, tests/workaround for qrcode-kotlin issue #197 short/full payload sizing, no animation/overlay intruding into QR quiet zone, and Back closing overlay to Song List
- [ ] T028 [US1] Implement non-duet P1 phone selection, Medium difficulty default, P2 visible-disabled state with P2 Difficulty hidden when Player 2 is `(none)`, and Start handoff state in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt`
- [ ] T029 [US1] Implement Select Players modal overlay on `SurfaceLevel2` with title `SELECT PLAYERS`, subtitle `<Artist> — <Title>`, active visually primary Player 1 required block, visible disabled Player 2 block for non-duet with a code comment naming Iteration 3 wiring, hidden Player 2 Difficulty when Player 2 is `(none)`, emphasized Start through placement/size/surface contrast not `RewardAccent`, and disabled duet/medley execution with comments naming Iteration 3 for duet and Iteration 4 for medley in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersModal.kt`
- [ ] T030 [US1] Implement sentence-paged current/next lyrics, spatially stable sentence state, instrumental-gap completed-sentence 100% highlight, optional clipped-reveal highlight data, static P1 note target conversion, `stopAtLyricsTimeMs`, and no live pitch/scoring data in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/DefaultSingingRenderModelBuilder.kt`
- [ ] T031 [US1] Implement static single-lane note drawing for one centered full-width neutral lane band at `SingingSingleLaneHeight` (192dp) with `SurfaceLaneBand` / `LaneBandAlpha`, `SingingLaneHorizontalPadding`, `SingingLaneVerticalPadding`, `RadiusMedium`, P1 accent only on badge/score-box/note markers, no full-lane tint, and no live pitch, hit/miss feedback, or scoring state in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/DefaultPitchLaneRenderer.kt`
- [ ] T032 [US1] Implement start-song happy path, TXT fetch, parse, render-model build, `Prepare`, `Prepared` wait, clock-sync gate, `assignSinger` with `type="assignSinger"`, `protocolVersion=1`, `sessionId`, `songInstanceSeq`, `playerId`, `difficulty`, `startMode`, `countdownMs`, `stopAtLyricsTimeMs`, `udpPort`, `songTitle`, and `songArtist`, with `countdownMs` required only for countdown, no `connectionId`, and delivery only to selected singer phones; implement playback state, countdown ON mapping to `startMode="countdown"` and `countdownMs = countdownSeconds * 1000`, countdown OFF mapping to `startMode="live"` with omitted `countdownMs`, `Play`, a code comment marking where Iteration 2 scoring-scope `ScoringEngine.setSongStart()` / `ScoringEngine.start()` calls should go without implementing them, and `PlaybackEvent.Ended` returning to Song List without Results with an Iteration 2 Results comment in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [ ] T033 [US1] Implement prepare/play handling, screen-scoped media-player lifetime, no-op handling for medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` with code comments that they must be wired in Iteration 4, single audio handle event listener registered before `play()`, `Prepared`, first-playing `Ready` with `songStartTvMs = (System.nanoTime() / 1_000_000) − playerHandle.timeMs`, 500 ms fallback `Ready`, current position, `stopAtLyricsTimeMs` end event, best-effort decorative full-screen video/background handling that never drives timing and falls back to static background on video failure, and teardown of SongList preview player on SongList exit plus Singing media players on Singing exit in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [ ] T034 [US1] Implement LibVLC streamed audio and optional decorative full-screen video handle adapter in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/VlcLibVlcPlayerHandle.kt`, with audio as required playable media and video/background failure treated as non-fatal static-background fallback
- [ ] T035 [US1] Implement Singing ViewModel happy-path state from coordinator, playback controller, render model, countdown overlay state at 1 Hz when Ready countdown is ON, immediate playback state when Ready countdown is OFF, normal-end Song List return state, and Back-to-Pause state handoff in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`
- [ ] T036 [US1] Implement Singing screen happy-path UI with best-effort full-screen video/background presentation using `SurfaceView` with `setZOrderMediaOverlay(true)` and not `TextureView`, static-background fallback when video/background fails, top metadata intro strip at `SingingTopIntroStripHeight` (72dp), active minimal strip at `SingingTopMinimalStripHeight` (40dp), lane region, full-width bottom lyrics band at `SingingBottomLyricsBandHeight` (160dp) with `SurfaceLyricsBand` / `LyricsBandAlpha`, one centered neutral full-width lane band at `SingingSingleLaneHeight` (192dp), score box `144dp × 88dp` inset 16dp from lane right edge, sentence rating below the score box, exactly two lyrics lines with current `LyricsCurrent` stronger and next `LyricsNext` muted, progressive clipped-reveal highlight, no third line, no pre-page/blank during instrumental gaps, constant `00000` score with an Iteration 2 scoring comment, elapsed time bottom-right in zero-padded `MM:SS`, allowed active motion limited to lyric highlight and static note lane rendering, no background animation/blur/bloom/multi-panel entrance/particles/full-lane pulses/layout-affecting animation, and countdown overlay using a centered `DisplayHeroNumber` numeral over a dimmed static background at 1 Hz with only numeral scale-pop in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingScreen.kt`
- [ ] T037 [US1] Wire `MainActivity`, `CouchraokeTheme`, and `AppNavHost` for Song List, Select Players modal, Singing, no Settings route/menu/screen/submenu, and inert Results route declaration; implement global Back behavior where Song List clears filter before exit, modal dialogs/overlays close to the underlying screen, Singing opens Pause overlay, Results returns to Song List, and include code comments that the Settings header no-op must be wired in Iteration 3 and Results must be wired in Iteration 2 in `app/src/main/kotlin/com/couchraoke/tv/MainActivity.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt`
- [ ] T038 [US1] Run scoped US1 `testBranch` for the changed files under `app/src/main/kotlin/com/couchraoke/tv/data/network/`, `app/src/main/kotlin/com/couchraoke/tv/data/library/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/songlist/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

**Checkpoint**: User Story 1 is independently functional and is the MVP stopping point.

---

## Phase 4: User Story 2 - Control solo playback from the TV remote (Priority: P2)

**Goal**: Allow Back during singing to open Pause, Resume playback, Restart with confirmation and a new `songInstanceSeq`, and Quit to Song List with confirmation.

**Independent Test**: Start a solo song, press Back, test Resume, Restart Song, and Quit to Song List on separate runs, and verify playback/session/navigation outcomes.

### Validation for User Story 2

- [ ] T039 [P] [US2] Add pause, resume, restart-from-`startSec`, quit, score reset, new `songInstanceSeq`, and `assignSinger` resend coordinator tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorControlsTest.kt`
- [ ] T040 [P] [US2] Add pause, resume, seek-to-`startSec` on restart, stop, audio-focus release, pause on transient audio-focus loss, and resume on `AUDIOFOCUS_GAIN` controller tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerControlsTest.kt`
- [ ] T041 [P] [US2] Add Back, Pause, Resume, Restart Confirm, Quit Confirm, Cancel-default-focus, modal Back-close, and Results Back-to-Song-List state tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModelControlsTest.kt`

### Implementation for User Story 2

- [ ] T042 [US2] Extend pause, resume, restart-from-`startSec`, quit-to-song-list, new `songInstanceSeq`, `assignSinger` resend, playback-state broadcast, score reset placeholder state, and session-open behavior in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [ ] T043 [US2] Extend pause, resume, stop, seek-to-`startSec` on restart, audio-focus abandonment, pause on transient audio-focus loss, and resume on `AUDIOFOCUS_GAIN` behavior in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [ ] T044 [US2] Implement Back, Pause, Resume, Restart Confirm, Quit Confirm, Cancel-default-focus, modal Back-close, and Results Back-to-Song-List UI state in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`
- [ ] T045 [US2] Implement Pause overlay, Restart confirmation, and Quit confirmation Compose UI in `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingOverlays.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingScreen.kt`
- [ ] T046 [US2] Run scoped US2 `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingViewModel.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/singing/SingingOverlays.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

**Checkpoint**: User Stories 1 and 2 work independently and together.

---

## Phase 5: User Story 3 - Recover from startup and playback interruptions (Priority: P3)

**Goal**: Provide blocking recovery UI for no connected phones, countdown disconnect, start failure, playback failure, and audio-focus failure while returning the session to a clear open state.

**Independent Test**: Force no-phone Select Players, countdown disconnect, unreachable audio URL, LibVLC error, and audio-focus denial paths, then verify blocking modal copy, destination, and session-open state.

### Validation for User Story 3

- [ ] T047 [P] [US3] Add Select Players no-phone recovery tests for blocking message `No phones connected`, `Open Join QR` action opening the same Join QR overlay as the Song List Join button, and no Settings route/menu/screen/submenu appearing in `app/src/test/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersRecoveryTest.kt`
- [ ] T048 [P] [US3] Add countdown disconnect, spectator disconnect, start failure, playback error, and session-open tests in `app/src/test/kotlin/com/couchraoke/tv/domain/playback/PlaybackCoordinatorRecoveryTest.kt`
- [ ] T049 [P] [US3] Add LibVLC error, warning-line truncation, audio-focus denial, transient loss pause, `AUDIOFOCUS_GAIN` resume, permanent loss, and focus abandonment tests in `app/src/test/kotlin/com/couchraoke/tv/presentation/playback/PlaybackControllerErrorTest.kt`
- [ ] T050 [P] [US3] Add interruption shell layout and modal-copy tests for Pause, Countdown disconnect, Start-failure / Playback error, and Select-Players no-phone state in `app/src/test/kotlin/com/couchraoke/tv/presentation/common/InterruptionShellTest.kt`; verify the shell contract is reusable for future Song-Library refresh errors without implementing Song-Library refresh behavior or any Settings screen/menu/submenu

### Implementation for User Story 3

- [ ] T051 [US3] Implement no-phone Select Players state and `Open Join QR` action that opens the same Join QR overlay as the Song List Join button, with no Settings route/menu/screen/submenu in `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt` and `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersModal.kt`
- [ ] T052 [US3] Implement required-singer countdown disconnect, spectator disconnect no-op, and reconnect event handling in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [ ] T053 [US3] Implement start-failure and unreachable-audio recovery state with specified `ERROR` modal copy in `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [ ] T054 [US3] Implement LibVLC warning/error log capture, 120-character truncation, encountered-error event handling for required audio, non-fatal video/background failure fallback, audio-focus denial, transient loss, gain resume, permanent loss, focus abandonment, and no-op medley-only `PrebufferNext`, `FadeOut`, and `Crossfade` error-path safety with comments that they must be wired in Iteration 4 in `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`
- [ ] T055 [US3] Implement shared centered elevated interruption shell with `SurfaceLevel2`, 32dp padding, dark scrim, no runtime blur, and entry focus lock for Pause, Countdown disconnect, Start-failure / Playback error, and Select-Players no-phone state in `app/src/main/kotlin/com/couchraoke/tv/presentation/common/InterruptionShell.kt`; keep the shell reusable for future Song-Library refresh errors without implementing Song-Library refresh behavior or any Settings screen/menu/submenu
- [ ] T056 [US3] Wire blocking no-phone, disconnected, and error modal routing while reopening the session in `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt` and `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`
- [ ] T057 [US3] Run scoped US3 `testBranch` for `app/src/main/kotlin/com/couchraoke/tv/presentation/selectplayers/SelectPlayersViewModel.kt`, `app/src/main/kotlin/com/couchraoke/tv/domain/playback/DefaultPlaybackCoordinator.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/playback/DefaultPlaybackController.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/common/InterruptionShell.kt`, `app/src/main/kotlin/com/couchraoke/tv/presentation/navigation/AppNavHost.kt`, and their tests under `app/src/test/kotlin/com/couchraoke/tv/`

**Checkpoint**: All user stories are independently functional and recover from defined interruption paths.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate UI fidelity, lyrics behavior constraints, and final feature completion without expanding Iteration 1 scope.

- [ ] T058 [P] Add Compose previews for Song List, Join overlay, Select Players, no-phone, Singing single-singer layout, countdown, pause, restart confirm, quit confirm, disconnect, and error states in `app/src/main/kotlin/com/couchraoke/tv/presentation/previews/SoloSingPreviews.kt`, excluding medley, live pitch, scoring, Results, Settings, and Song-Library refresh error screens
- [ ] T059 Compare rendered UI previews or screenshots against wireframes in `specs/002-solo-sing-playback/spec.md` using preview definitions in `app/src/main/kotlin/com/couchraoke/tv/presentation/previews/SoloSingPreviews.kt`
- [ ] T060 Run final feature-scoped `testBranch` for all changed feature files under `app/src/main/kotlin/com/couchraoke/tv/` and `app/src/test/kotlin/com/couchraoke/tv/` using validation selectors derived from `specs/002-solo-sing-playback/quickstart.md`; the passing selector set MUST explicitly include F15-style session lifecycle coverage, minimal song-start clock-sync gate coverage, F22 GamePhase FSM coverage, actual Join QR renderer short/full payload sizing coverage, and the cumulative TV-owned end-to-end flow through the TV UI
- [ ] T061 Run emulator solo-sing quickstart verification from `specs/002-solo-sing-playback/quickstart.md` against the runtime entry point in `app/src/main/kotlin/com/couchraoke/tv/MainActivity.kt`, capturing evidence for launch app → pair one phone → load manifest → select one non-duet song → select P1 → start playback with lyrics/static lane/`00000` score → return to Song List

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
- Implement state/ViewModels before Compose UI when both are needed.
- Run the scoped story `testBranch` gate after the story implementation is complete.
- Audit changed files against scope before moving to the next story.

---

## Parallel Opportunities

- T002 and T003 can run in parallel after T001.
- T004 through T007 can run in parallel because they write separate test files.
- T008 through T014 are mostly contract files but should be integrated sequentially unless assigned to isolated worktrees.
- T016 through T022 can run in parallel after Phase 2 because they write separate test files.
- T039 through T041 can run in parallel after Phase 2 and relevant US1 interfaces are stable.
- T047 through T050 can run in parallel after Phase 2 and relevant recovery seams are stable.
- T059 depends on T058 because screenshot comparison uses the preview definitions created there.

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
