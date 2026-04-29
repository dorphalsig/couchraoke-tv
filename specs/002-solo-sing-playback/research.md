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

## Decision: Keep duet/medley controls visible but disabled

**Rationale**: Shared Song List and Select Players sections include duet/medley affordances, but Iteration 1 is one phone/one player solo sing. Keeping controls visible preserves layout and wireframe compatibility while disabling execution prevents scope creep.

**Alternatives considered**:
- Hide all duet/medley controls: rejected because it would diverge more from extracted shared-screen wireframes.
- Leave controls enabled with “Coming soon”: rejected because enabled controls imply interaction and extra modal behavior not needed for Iteration 1.

## Decision: Use existing dependency catalog only

**Rationale**: `gradle/libs.versions.toml` already declares Ktor client/server/websockets, jmDNS, LibVLC, Coil, Compose TV, coroutines, serialization, and tests needed for the planned feature. The constitution requires dependency changes to be planned through the catalog, and no new dependencies are necessary.

**Alternatives considered**:
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
