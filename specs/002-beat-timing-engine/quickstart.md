# Quickstart — Beat Timing Engine

## Goal
Validate the beat timing feature from failing tests to passing acceptance fixtures.

## Prerequisites
- Repository checked out in the worktree on branch `002-beat-timing-engine`
- JDK available for Gradle builds
- Gradle wrapper executable available
- Parser feature already present in the same worktree

## Expected implementation areas
- Pure Kotlin timing code under `app/src/main/kotlin/com/couchraoke/tv/domain/timing/`
- Existing parser models reused as timing inputs
- JVM unit tests for beat conversion, playback bounds, and note-window rules
- Fixture-driven acceptance tests for gap/start, boundary, late-frame, and mic-delay scenarios
- Timing-focused Gradle verification tasks in `app/build.gradle.kts`

## Mandatory acceptance fixture set
The mandatory acceptance fixture set for this feature must cover at minimum:
- fixed-BPM beat cursor baseline with named playback positions
- gap-aware pre-roll behavior that preserves negative chart-relative time
- start-offset playback initialization
- note-window start and end boundary membership
- late-frame rejection when `latenessMs > 450`
- note finalization after the 450 ms allowance
- manual mic-delay default and non-zero shift cases
- explicit song end boundary from metadata
- media-duration fallback when the song has no explicit end boundary

## Suggested workflow
1. Write a failing JVM unit test for the smallest beat or note-window rule.
2. Run the targeted test and confirm it fails.
3. Implement the minimum timing-domain code to satisfy the test.
4. Re-run the targeted test until it passes.
5. Add or extend fixture-driven acceptance tests for the same rule.
6. Repeat until all timing requirements are covered.

## Key verification commands
```bash
./gradlew test
./gradlew jacocoTestCoverageVerification
./gradlew :app:timingUnitTest
./gradlew :app:timingAcceptanceTest
./gradlew :app:timingTest
```

## Coverage gate
- `jacocoTestCoverageVerification` enforces the constitution coverage gate for JVM unit tests.
- Bundle-level line coverage must remain at or above 80%.
- Class-level line coverage must remain at or above 60%.
- Android-generated classes such as `R`, `BuildConfig`, and `Manifest` are excluded from class-level enforcement.

## Branch/worktree closure
- After this feature is integrated, rename the completed feature branch/worktree to `[✓] 002-beat-timing-engine` to mark it closed per branch hygiene.

## Acceptance focus
Verify at minimum:
- Beat cursor calculation from playback time and gap
- Pre-roll negative chart-relative timing
- Start-offset playback bounds
- Start-inclusive and end-exclusive note membership
- Late-frame cutoff and note finalization timing
- Mic-delay offset application without changing authored beats
- Explicit song end handling and media-duration fallback

## Fixture coverage and execution notes
- `fixtures/parser/derived/18_beat_timing_basic` should cover the baseline fixed-BPM beat cursor path.
- `fixtures/parser/derived/19_beat_timing_gap_and_start` should cover gap-aware pre-roll behavior plus non-zero start offset handling.
- `fixtures/parser/edge/20_beat_timing_boundary_case` should cover note-window boundary membership, late-arrival rejection, and note finalization timing.
- Each timing fixture directory should remain parser-compatible and include any required local media reference used for parser validation.
- Use `./gradlew :app:timingUnitTest`, `./gradlew :app:timingAcceptanceTest`, or targeted `:app:testDebugUnitTest --tests ...` commands from `tasks.md` while iterating.

## Completion check
The feature is ready for task generation when:
- Timing behavior is fully represented in unit and acceptance tests
- Acceptance fixtures cover the mandatory scenarios from the feature spec
- Beat and note-window calculations are deterministic from the same inputs
- `./gradlew test`, `./gradlew jacocoTestCoverageVerification`, and timing-focused Gradle tasks pass
