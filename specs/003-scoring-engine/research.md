# Research — Scoring Engine

## Decision 1: No new library dependencies
**Decision**: The scoring engine requires no new Gradle dependencies.
**Rationale**: All required operations (IEEE 754 arithmetic, rounding, clamping) are available in the Kotlin stdlib (`kotlin.math.*`). Parser and timing domain types are already on the classpath.
**Alternatives considered**: None warranted.

## Decision 2: Octave normalization algorithm
**Decision**: Implement octave normalization as an iterative loop. Derive `tone = midiNote - 36`, then while `abs(tone - targetTone) > 6`, shift tone by +12 or -12 toward the target. Stop when within 6 semitones.
**Rationale**: FR-006 and FR-007 require shifting by ±12 without reducing to pitch class first. The iterative form correctly handles pitches more than one octave away. Exact boundary: 6 semitones away counts as within tolerance (the loop stops at ≤ 6).
**Alternatives considered**: Modulo pitch-class reduction — rejected because it loses octave information before the tolerance check, violating FR-006.

## Decision 3: Rounding functions
**Decision**: Use `kotlin.math.round(x)` for `ScoreInt` (half-away-from-zero for positive values, matching normative `round()` semantics). Use `ceil` or `floor` for directional golden rounding. Use `floor(round(x) / 10.0) * 10` for `ScoreLineInt`.
**Rationale**: FR-017 mandates exact formula matching including the intentional asymmetry between `ScoreLineInt` and `ScoreInt`. The direction of golden rounding (`ceil` vs `floor`) depends on whether `ScoreInt < Player.Score`.
**Alternatives considered**: `roundToInt()` from `kotlin.math` — same semantics for positive values; valid alias.

## Decision 4: ScoreFactor constants
**Decision**: Define as a `Map<NoteType, Int>` constant in `ScoringConstants.kt`: FREESTYLE→0, NORMAL→1, GOLDEN→2, RAP→1, RAP_GOLDEN→2.
**Rationale**: The spec (Assumptions) marks these as normative. Keying by the existing parser `NoteType` enum makes the lookup direct and exhaustive.
**Alternatives considered**: Inline when-expression — valid but a named constant is safer if `NoteType` gains new entries.

## Decision 5: Difficulty tolerance
**Decision**: Encode tolerance as `Map<Difficulty, Int>` in `ScoringConstants.kt`: EASY→2, MEDIUM→1, HARD→0. Hit condition: `abs(normalizedTone - targetTone) <= tolerance`.
**Rationale**: FR-008 defines the three levels. After octave normalization, the distance check is a simple absolute comparison against the per-difficulty bound.
**Alternatives considered**: None.

## Decision 6: Threading and coroutine model
**Decision**: The scoring engine is a stateless pure-function interface with no internal coroutines or threading. Callers (e.g., the future singing-screen scoring loop) wrap invocations in coroutine contexts as needed.
**Rationale**: Keeps the domain layer pure and testable without `TestCoroutineScheduler`. Consistent with the timing engine's approach.
**Alternatives considered**: Flow-based accumulator — deferred to the singing-screen feature where reactive state is required.

## Decision 7: Fixture format for acceptance tests
**Decision**: Scoring fixtures are self-contained JSON files (no USDX song files). Each fixture directory under `app/src/test/resources/fixtures/scoring/<scenario>/` contains an `input.json` and `expected.json`.
**Rationale**: Scoring inputs (note type, duration, frame counts, MIDI note values) are simpler to express directly in JSON than embedded in USDX songs. The parser acceptance tests use USDX files because the parser reads them; the scoring engine does not.
**Alternatives considered**: Programmatic Kotlin data defined inline in test files — valid but not fixture-driven per constitution §V acceptance test definition.

## Decision 8: TrackScoreValue medley filtering
**Decision**: When the song's header contains non-null medley bounds, filter notes to `startBeat >= medleyStartBeat && startBeat < medleyEndBeat` when computing `TrackScoreValue`. When medley bounds are absent, all notes in the track are included.
**Rationale**: FR-016 requires this filter. The `ParsedSong` / `SongHeader` types already carry these fields from the parser feature.
**Alternatives considered**: None.

## Decision 9: NoteType reuse from parser
**Decision**: Scoring uses the existing `NoteType` enum from `com.couchraoke.tv.domain.parser.NoteEvent` directly. No duplicate type is introduced in the scoring package.
**Rationale**: `NoteType` (NORMAL, GOLDEN, FREESTYLE, RAP, RAP_GOLDEN) is already authoritative in the parser. Duplicating it would require synchronisation and adds no value.
**Alternatives considered**: None.
