# Implementation Plan: Phase 0 TV Host Foundation

**Branch**: `001-tv-host-foundation` | **Date**: 2026-04-26 | **Spec**: `/home/paavum/Couchraoke/tv/specs/001-tv-host-foundation/spec.md`
**Input**: Feature specification from `/specs/001-tv-host-foundation/spec.md`

## Summary

Deliver a self-contained Phase 0 foundation for the TV host app: pure Kotlin parser, deterministic beat-time conversion, deterministic scoring math, and fixture-based validation for F01-F06 and F08-F11. The implementation will keep all host-owned chart and score authority on TV, isolate pure logic from Android/UI/runtime concerns, and preserve the spec’s detailed parser, scoring, rounding, and fixture contracts without relying on external source documents.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 11
**Primary Dependencies**: AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle Runtime, Kotlin Coroutines, Kotlinx Serialization JSON, JUnit 4, quality-conventions `testBranch`
**Storage**: N/A for Phase 0 core logic; fixture files only at test harness boundary
**Testing**: JVM unit tests, fixture assertions, JaCoCo coverage verification, Detekt via `:app:testBranch`
**Target Platform**: Android TV app repository; Phase 0 core itself must run as pure JVM logic
**Project Type**: Mobile app with host-owned pure domain foundation
**Performance Goals**: Parse a 10KB USDX TXT in <50ms; deterministic beat round-trip precision to `1e-9s`; perfect-scoring fixtures produce `10000`
**Constraints**:
- Preserve TV host authority for parsed chart state and scoring outcomes
- No Android framework types, UI, playback, sockets, filesystem discovery, or network logic in parser/scoring core
- No persistence of remote song assets
- No dependency/version changes unless explicitly added to `gradle/libs.versions.toml`
- Keep F07 and all live runtime seams outside the Phase 0 delivery gate
**Scale/Scope**:
- Single feature slice in `specs/001-tv-host-foundation`
- Single Android app module (`:app`)
- Phase 0 gates only F01-F06 and F08-F11
- New implementation expected under `app/src/main/kotlin/com/couchraoke/tv/...` and `app/src/test/kotlin/com/couchraoke/tv/...`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Host Authority**: PASS. The plan keeps parsed chart state, beat math, and scoring outcomes as TV-owned domain logic. Companion/runtime/network seams remain consumers only and do not redefine host state.
- **Architecture Boundaries**: PASS. Phase 0 work stays in pure domain/test layers. UI, playback, networking, and Android runtime behavior remain out of scope. ViewModels and presentation remain untouched in this slice.
- **Dependency Governance**: PASS. No new dependencies or version changes are required for Phase 0. Existing catalog entries in `gradle/libs.versions.toml` are sufficient.
- **Contracts First**: PASS. Material boundaries are defined below with FQCN + method + signature and corresponding data contracts.
- **Workflow Units**: PASS. This feature remains one `spec.md`, one `plan.md`, one future `tasks.md`; phases are split into research, design/contracts, and later implementation work.
- **Validation Gate**: PASS. Authoritative gate is `:app:testBranch` with scoped production/test selectors for the parser, beat-time, scoring, and fixture harness classes defined by this feature.

### Material Contracts Planned

1. `com.couchraoke.tv.domain.usdx.UsdxParser#parse(songId: String, txtBytes: ByteArray): Result<ParsedSong>`
   - Producer: parser core
   - Consumers: fixture harness, library indexing adapter, scoring chart loader
2. `com.couchraoke.tv.domain.library.LibraryManager#getSong(songId: String): IndexedSong?`
   - Producer: library/index layer
   - Consumers: selection flow and fixture assertions
3. `com.couchraoke.tv.domain.scoring.BeatCalculator#timeSecToMidBeatInternal(tSec: Double, bpmInternal: Float): Double`
4. `com.couchraoke.tv.domain.scoring.BeatCalculator#beatInternalToTimeSec(beatInternal: Double, bpmInternal: Float): Double`
   - Producer: beat-time math
   - Consumers: lyric highlighting, scoring-window math, medley preview derivation
5. `com.couchraoke.tv.domain.scoring.ScoringEngine#loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig): Unit`
6. `com.couchraoke.tv.domain.scoring.ScoringEngine#setSongStart(songStartTvMs: Long): Unit`
7. `com.couchraoke.tv.domain.scoring.ScoringEngine#finalizeAll(): Map<PlayerId, PlayerScore>`
   - Producer: scoring core
   - Consumers: playback/game coordinator in later phases, fixture harness in Phase 0
8. Future runtime seam kept documented but out of delivery gate:
   - `com.couchraoke.tv.data.network.NetworkController#pitchFrames: SharedFlow<PitchFrame>`
   - Consumer: `com.couchraoke.tv.domain.scoring.ScoringEngine`

## Project Structure

```text
specs/001-tv-host-foundation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── foundation-boundaries.md

app/
└── src/
    ├── main/
    │   └── kotlin/com/couchraoke/tv/
    │       ├── domain/usdx/
    │       │   ├── UsdxParser.kt
    │       │   ├── ParseException.kt
    │       │   ├── model/
    │       │   └── internal/
    │       ├── domain/library/
    │       │   └── IndexedSong.kt
    │       └── domain/scoring/
    │           ├── BeatCalculator.kt
    │           ├── ScoringEngine.kt
    │           ├── model/
    │           └── internal/
    └── test/
        └── kotlin/com/couchraoke/tv/
            ├── domain/usdx/
            ├── domain/scoring/
            └── fixtures/
```

**Structure Decision**:
- Keep app Kotlin sources in `src/main/kotlin` and tests in `src/test/kotlin` so the selective `testBranch` Detekt/coverage wiring resolves the intended files directly.
- Keep fixture discovery and file access in test/harness code only; parser and scoring classes remain pure logic.

## Phase 0: Research & Decision Consolidation

1. Convert all resolved clarifications and copied contract details into `research.md`.
2. Lock parser semantics for version handling, audio resolution, custom tags, RELATIVE rejection, diagnostics, and score-value invariants.
3. Lock beat-time formulas, mic-delay rules, and interval semantics.
4. Lock scoring math for note buckets, difficulty tolerance, octave normalization, line bonus, and display rounding.
5. Lock fixture-harness approach to pure unit/fixture testing for this phase.

## Phase 1: Design & Contracts

1. Create `data-model.md` for parser, library-facing index, beat-time, and scoring entities.
2. Create `contracts/foundation-boundaries.md` with producer/consumer boundaries, FQCNs, signatures, and payload rules.
3. Create `quickstart.md` describing implementation order, fixture mapping, and validation commands.
4. Run the agent-context update script after artifacts are written.
5. Re-check constitution compliance after design artifacts are complete.

## Validation Gate

Phase 0 completion must use scoped `testBranch` selectors for the exact production and test classes introduced by this feature. The authoritative command set is:

```bash
./gradlew :app:testBranch \
  --src com.couchraoke.tv.domain.usdx.UsdxParser \
  --src com.couchraoke.tv.domain.usdx.ParseException \
  --src com.couchraoke.tv.domain.usdx.model.ParsedSong \
  --src com.couchraoke.tv.domain.usdx.model.SongHeader \
  --src com.couchraoke.tv.domain.usdx.model.Track \
  --src com.couchraoke.tv.domain.usdx.model.Line \
  --src com.couchraoke.tv.domain.usdx.model.NoteEvent \
  --src com.couchraoke.tv.domain.library.IndexedSong \
  --src com.couchraoke.tv.domain.scoring.BeatCalculator \
  --src com.couchraoke.tv.domain.scoring.ScoringEngine \
  --src com.couchraoke.tv.domain.scoring.model.BeatRange \
  --src com.couchraoke.tv.domain.scoring.model.ScoringConfig \
  --src com.couchraoke.tv.domain.scoring.model.PlayerScore \
  --src com.couchraoke.tv.domain.scoring.model.PitchSample \
  --test com.couchraoke.tv.domain.usdx.UsdxParserFixtureTest \
  --test com.couchraoke.tv.domain.scoring.BeatCalculatorFixtureTest \
  --test com.couchraoke.tv.domain.scoring.ScoringEngineFixtureTest
```

The final task list may add more granular selectors, but it must not widen beyond the Phase 0 parser, beat-time, scoring, and fixture-harness slice.

## Post-Design Constitution Re-Check

- **Host Authority**: Maintained by TV-owned parser and scoring contracts; no companion-controlled overrides.
- **Architecture Boundaries**: Maintained by isolating pure domain logic and test-only fixture I/O.
- **Dependency Governance**: Still no dependency changes required.
- **Contracts First**: Satisfied by the generated contracts artifact and the explicit validation gate.
- **Workflow Units**: Still one feature with bounded design outputs.
- **Validation Gate**: Scoped `testBranch` command defined and kept authoritative.

## Out-of-Scope Issues Noted

1. The current app module has no `app/src/test` tree yet.
   - Suggested fix: create `app/src/test/kotlin` as part of implementation; do not broaden beyond Phase 0 fixture tests.
