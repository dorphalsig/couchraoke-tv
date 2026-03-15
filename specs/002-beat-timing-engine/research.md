# Phase 0 Research — Beat Timing Engine

## Decision 1: Keep beat timing in a new pure `domain/timing` package
- **Decision**: Implement beat timing as a new `com.couchraoke.tv.domain.timing` package instead of extending `com.couchraoke.tv.domain.parser`.
- **Rationale**: Parsing and timing are adjacent but distinct responsibilities. Keeping timing downstream from the parsed-song model preserves clean domain boundaries and makes later scoring/UI features depend on a stable timing interface rather than parser internals.
- **Alternatives considered**:
  - Keep timing logic inside `domain/parser` — rejected because it would mix file-format parsing with downstream runtime timing behavior.

## Decision 2: Use `ParsedSong` and existing parser models as the sole upstream input
- **Decision**: Consume `ParsedSong`, `SongHeader`, `Track`, `Line`, and `NoteEvent` directly rather than introducing a second song-description format.
- **Rationale**: The parser feature already established the parsed-song model as the authoritative downstream source. Reusing it avoids duplicate representations and keeps the feature scope tight.
- **Alternatives considered**:
  - Create timing-specific source DTOs — rejected because they would duplicate parsed data and create unnecessary mapping work.

## Decision 3: Represent timing results with immutable timing-specific models
- **Decision**: Return beat cursor, playback-bounds, pitch-frame timing, and note-window results as immutable timing models in `domain/timing`.
- **Rationale**: Timing outputs are derived views over parsed song data and runtime timestamps. Keeping them separate avoids mutating parser models and makes unit tests deterministic.
- **Alternatives considered**:
  - Store derived timing fields directly inside parser entities — rejected because parser entities should remain format/output models, not runtime state holders.

## Decision 4: Keep conversion rules fixed-BPM only and derive internal beat math from header BPM
- **Decision**: Build the engine around the parser-guaranteed fixed-BPM contract, using header `bpm`, `gapMs`, `startSec`, and `endMs` plus the existing fixed-BPM internal-beat conversion rules.
- **Rationale**: Variable-BPM songs are already rejected upstream, so timing can stay deterministic and simple while matching the original product spec’s formulas.
- **Alternatives considered**:
  - Add variable-BPM segment support here — rejected because it contradicts the parser contract and expands scope into unsupported song formats.

## Decision 5: Treat manual mic delay as an explicit bounded runtime input
- **Decision**: Accept manual microphone delay as a timing-engine input with default `0 ms` and an inclusive valid range of `0..400 ms`.
- **Rationale**: The feature spec and original spec both treat mic delay as a user-controlled calibration value, not an inferred or adaptive behavior.
- **Alternatives considered**:
  - Infer or auto-adjust mic delay during scoring — rejected because calibration is explicitly manual and adaptive logic belongs to a different feature, if anywhere.

## Decision 6: Encode note membership and finalization with explicit boundary rules
- **Decision**: Use start-inclusive, end-exclusive note windows and a `450 ms` late-arrival/finalization allowance in the timing contract.
- **Rationale**: These rules are explicitly fixed in the source spec and are required for later scoring features to behave deterministically at note boundaries.
- **Alternatives considered**:
  - End-inclusive windows or configurable lateness — rejected because they would diverge from the product spec and complicate downstream scoring expectations.

## Decision 7: Use fixture-driven acceptance tests with parser-compatible song fixtures and expected timing snapshots
- **Decision**: Keep timing acceptance tests fixture-driven, reusing parser-compatible fixture directories under `app/src/test/resources/fixtures/parser/` and pairing them with expected timing assertions.
- **Rationale**: The repository already uses fixture-driven parser acceptance tests. Extending that pattern keeps acceptance coverage readable and makes regressions visible at the behavior level.
- **Alternatives considered**:
  - Inline-only acceptance data — rejected because it scales poorly for gap/start/boundary scenarios and drifts from existing repository patterns.

## Decision 8: Add dedicated timing-focused Gradle test selectors instead of broadening parser selectors
- **Decision**: Add `timingUnitTest`, `timingAcceptanceTest`, and `timingTest` tasks rather than expanding the existing parser-only class patterns.
- **Rationale**: Parser tasks should remain parser-scoped, while the timing feature needs the same targeted feedback loop without collapsing all domain features into one Gradle selector.
- **Alternatives considered**:
  - Reuse parser task selectors for timing tests — rejected because tests outside `domain/parser` would be skipped.
  - Broaden parser task selectors to all domain tests — rejected because it would blur feature-specific verification paths and make parser tasks less focused.
