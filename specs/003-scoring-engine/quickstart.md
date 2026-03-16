# Quickstart — Scoring Engine

## Goal
Validate the scoring engine from failing tests to passing acceptance fixtures.

## Prerequisites
- Repository on branch `003-scoring-engine`
- JDK available for Gradle builds
- Gradle wrapper executable available
- Parser feature (001) and beat timing engine (002) already present on this branch

## Expected implementation areas
- Pure Kotlin scoring code under `app/src/main/kotlin/com/couchraoke/tv/domain/scoring/`
- Three source files: `ScoringEngine.kt`, `ScoringModels.kt`, `ScoringConstants.kt`
- JVM unit tests for all scoring formulas
- Fixture-driven acceptance tests for all user stories
- Scoring-focused Gradle verification tasks in `app/build.gradle.kts`

## Mandatory acceptance fixture set
The mandatory acceptance fixture set for this feature must cover at minimum:
- perfect note: all frames hit → `noteScore = maxNoteScore`
- partial note: some frames hit → `noteScore = maxNoteScore × (hits / n)`
- zero-frame note: `n = 0` → `noteScore = 0`
- Freestyle note: frames present → `noteScore = 0` regardless
- Normal note accumulated into `Player.Score`; Golden into `Player.ScoreGolden`
- Rap hit when `toneValid = true`; miss when `toneValid = false`
- Octave normalization: pitch 12 semitones away normalizes before tolerance check
- Octave boundary: pitch exactly 6 semitones away → within tolerance (hit)
- All three difficulty levels (Easy/Medium/Hard) for Normal note
- Line perfection = 1.0 when all notes hit
- Line forgiveness when `maxLineScore ≤ 2`
- Empty-line exclusion from line bonus distribution
- `LineBonusEnabled = OFF` → no line bonus computed
- All four rounding formula outputs including `ScoreLineInt` asymmetry
- Perfect performance with `LineBonusEnabled = ON` → `ScoreTotalInt = 10000`

## Concrete scenario walkthrough

### Scenario A — Perfect Normal note
- Song: 1 note, NORMAL, `durationBeats = 4`, `trackScoreValue = 4`, `MaxSongPoints = 10000`
- `maxNoteScore = (10000 / 4) × 1 × 4 = 10000`
- 4 frames, all with `midiNote = 84` (MIDI-36 = tone 48), target `tone = 48`, difficulty MEDIUM
- `normalizedTone = 48`, `|48 - 48| = 0 ≤ 1` → all hits
- Expected: `hits = 4`, `n = 4`, `noteScore = 10000.0`

### Scenario B — Partial note (50% hit rate)
- Same setup as A but only 2 of 4 frames match
- Expected: `noteScore = 5000.0`

### Scenario C — Rap note always hits on toneValid
- Note: RAP, any `durationBeats`
- Frame: `midiNote = 0` (wrong pitch), `toneValid = true`
- Expected: `isPitchMatch = true`
- Frame: `midiNote = 60`, `toneValid = false`
- Expected: `isPitchMatch = false`

### Scenario D — Octave normalization
- Target: `tone = 48` (C3 in USDX). Detected: `midiNote = 108` → `tone = 72`, delta = 24
- Iteration 1: `72 - 12 = 60`, delta = 12 > 6 → continue
- Iteration 2: `60 - 12 = 48`, delta = 0 ≤ 6 → stop; `normalizedTone = 48` → hit (MEDIUM)

### Scenario E — Line bonus with forgiveness
- `maxLineScore = 2`, `lineScore = 1.0`
- Expected: `linePerfection = 1.0` (forgiveness rule), `lineBonusAwarded = lineBonusPerLine × 1.0`

### Scenario F — Perfect performance total
- Song: 2 lines, all notes Normal, `LineBonusEnabled = ON`
- Singer hits every note perfectly
- Expected: `ScoreTotalInt = 10000`

## Suggested workflow
1. Write a failing unit test for the smallest scoring rule (e.g., `n = 0 → noteScore = 0`).
2. Run the targeted test and confirm it fails.
3. Implement the minimum scoring-domain code to satisfy the test.
4. Re-run until it passes; then expand to the next rule.
5. Add fixture-driven acceptance tests once unit coverage is complete per user story.
6. Repeat until all acceptance scenarios pass.

## Key verification commands
```bash
./gradlew test
./gradlew jacocoTestCoverageVerification
./gradlew :app:scoringUnitTest
./gradlew :app:scoringAcceptanceTest
./gradlew :app:scoringTest
```

## Coverage gate
- `jacocoTestCoverageVerification` enforces constitution coverage gates.
- Bundle-level line coverage must remain ≥ 80%.
- Per-file line coverage must remain ≥ 60%.
- Android-generated classes (`R`, `BuildConfig`, `Manifest`) are excluded.

## Branch closure
After this feature is integrated, rename the branch to `[✓] 003-scoring-engine` per branch hygiene.

## Acceptance focus
Verify at minimum:
- Per-note score calculation from hit ratio using IEEE 754 double-precision division
- N = 0 handling (zero-frame notes score 0)
- ScoreFactor per note type (Freestyle = 0)
- Octave normalization before tolerance check
- Difficulty-gated tolerance for Normal/Golden notes
- Rap/RapGolden hit detection from `toneValid` only
- Line perfection formula including forgiveness rule
- Empty-line exclusion from line bonus pool
- `LineBonusEnabled` toggle and derived `MaxSongPoints`
- All four normative rounding formulas and their asymmetry
- `ScoreTotalInt ≤ 10000` for perfect performance

## Completion check
The feature is ready for integration when:
- All unit and acceptance tests pass
- Acceptance fixtures cover all mandatory scenarios above
- Scoring calculations are deterministic for identical inputs
- `./gradlew test`, `./gradlew jacocoTestCoverageVerification`, and scoring Gradle tasks all pass
