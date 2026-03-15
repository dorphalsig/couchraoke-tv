# Implementation Plan: Beat Timing Engine

**Branch**: `002-beat-timing-engine` | **Date**: 2026-03-15 | **Spec**: `/home/paavum/Couchraoke/tv/.worktree/002-beat-timing-engine/specs/002-beat-timing-engine/spec.md`
**Input**: Feature specification from `/specs/002-beat-timing-engine/spec.md`

## Summary

Build a pure Kotlin beat timing engine inside the Android TV app that consumes valid parsed songs and deterministically derives beat cursors, playback bounds, note timing windows, and note finalization timing for later singing-screen and scoring features. The implementation preserves a single playback-timeline model with two derived views: a UI highlight beat cursor derived from `lyricsTimeSec` and the song gap, and scoring note windows derived from `songStartTvMs`, authored beats, `BPM_file`, the song gap, and `micDelayMs`.

## Technical Context

**Language/Version**: Kotlin 2.3.10 on Java 11
**Primary Dependencies**: Kotlin standard library, existing parser domain model, existing Gradle/JUnit4 stack
**Storage**: N/A for feature runtime; timing logic consumes in-memory `ParsedSong` data and runtime playback timestamps
**Testing**: JUnit 4 JVM tests plus fixture-driven acceptance tests under `app/src/test`
**Target Platform**: Android TV app, but timing itself is pure JVM/domain logic
**Project Type**: Mobile app feature with pure domain timing component
**Performance Goals**: Deterministic beat and note-window calculations for a single parsed song, suitable for later 100 Hz scoring polling without UI coupling
**Constraints**: Must remain pure domain logic, fixed-BPM only, preserve start-inclusive/end-exclusive note windows, preserve the 450 ms late-frame/finalization rule, use JUnit4-only Android tests, and keep parser-specific Gradle tasks isolated from new timing tasks
**Scale/Scope**: One parsed song at a time; one or two tracks mixed into one playback timeline; beat cursor, playback bounds, note windows, mic-delay adjustment, and late-frame eligibility only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **LAN-only / authoritative host**: PASS — the timing engine is an internal TV-host domain component and does not change LAN-only operation or host authority.
- **Approved stack**: PASS — the plan stays within Kotlin, the existing Gradle/JUnit4 toolchain, and the approved TV-host stack; no new library is introduced.
- **Architecture boundaries**: PASS — timing logic is planned as a new pure `domain/timing` package that depends on parser models but keeps framework/UI types out of the domain layer.
- **Networking / streaming / scoring contracts**: PASS — no transport or asset-streaming behavior changes; the plan preserves fixed timing rules, playback bounds, and later scoring-loop decoupling from UI rendering.
- **Tests and quality gates**: PASS — plan requires JUnit4-only JVM unit and acceptance tests, Gradle verification, and JaCoCo coverage gates that remain aligned with the constitution thresholds.
- **Branch cleanup**: PASS — after merge, feature branch/worktree `002-beat-timing-engine` must be marked closed by renaming it to `[✓] 002-beat-timing-engine` per constitution branch hygiene.

## Project Structure

### Documentation (this feature)

```text
specs/002-beat-timing-engine/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── beat-timing-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── kotlin/com/couchraoke/tv/domain/
    │       ├── parser/
    │       │   ├── ParsedSong.kt
    │       │   ├── SongHeader.kt
    │       │   ├── Track.kt
    │       │   ├── Line.kt
    │       │   └── NoteEvent.kt
    │       └── timing/
    │           ├── BeatTimingEngine.kt
    │           └── BeatTimingModels.kt
    └── test/
        ├── kotlin/com/couchraoke/tv/domain/
        │   ├── parser/
        │   │   └── UsdxParserAcceptanceTest.kt
        │   └── timing/
        │       ├── BeatTimingEngineTest.kt
        │       └── BeatTimingEngineAcceptanceTest.kt
        └── resources/fixtures/parser/
            ├── derived/
            │   ├── 18_beat_timing_basic/
            │   └── 19_beat_timing_gap_and_start/
            └── edge/
                └── 20_beat_timing_boundary_case/
```

**Structure Decision**: Keep beat timing separate from parsing by introducing a new pure `com.couchraoke.tv.domain.timing` package that depends on the existing parser model types. Preserve the current parser package as the upstream source of truth, keep timing fixtures parser-compatible under `app/src/test/resources/fixtures/parser/`, and add timing-specific Gradle test selectors instead of broadening the existing parser-only test tasks.

## Complexity Tracking

No constitution violations or justified exceptions are required for this feature.
