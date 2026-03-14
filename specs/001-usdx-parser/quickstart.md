# Quickstart — USDX Parser

## Goal
Validate the parser feature from failing tests to passing acceptance fixtures.

## Prerequisites
- Repository checked out on branch `001-usdx-parser`
- JDK available for Gradle builds
- Gradle wrapper executable available

## Expected implementation areas
- Pure Kotlin parser code under the TV app's domain package
- JVM unit tests for parser behavior
- Fixture-driven acceptance tests for valid, invalid, duet, derived-metadata, and edge-case songs
- Existing source fixtures under `original_spec/fixtures/song_txt_variants/` to be reused or adapted into parser test resources

## Mandatory acceptance fixture set
The mandatory acceptance fixture set for this feature must cover at minimum:
- valid minimal single-track song
- valid metadata-rich single-track song with optional media tags
- valid duet song with `P1`/`P2` routing
- invalid missing required header song
- invalid missing required audio file song
- invalid malformed body numeric field song
- invalid variable BPM song
- invalid relative sentence format song
- invalid duet marker song
- invalid empty-track-after-cleanup song
- derived metadata song covering rap, duet, preview fallback, and medley inputs
- edge-case song covering duplicate known tags, unknown tags, empty-value tags, tag-like lines without separators, zero-duration notes, and optional missing assets
- edge-case song with external video reference that must not count as a local optional video asset

## Suggested workflow
1. Write a failing JVM test for the smallest parser behavior.
2. Run the targeted test and confirm it fails.
3. Implement the minimum parser/domain code to satisfy the test.
4. Re-run the targeted test until it passes.
5. Add or extend fixture-driven acceptance tests for the same rule.
6. Repeat until all parser requirements are covered.

## Key verification commands
```bash
./gradlew test
./gradlew lint
./gradlew jacocoTestReport  # Generate coverage report
./gradlew jacocoTestCoverageVerification  # Verify coverage thresholds (CI gate)
```

## Acceptance focus
Verify at minimum:
- Valid single-track song parsing
- Valid metadata-rich song parsing with optional media tags
- Valid duet parsing and track routing
- Invalid missing-required-header handling
- Invalid missing-required-audio-file handling
- Invalid malformed-body handling
- Variable BPM rejection
- Relative sentence format rejection
- Invalid duet marker rejection
- Empty-track-after-cleanup rejection
- Duplicate tag last-wins behavior
- Unknown tag preservation
- Zero-duration note conversion
- Derived duet/rap/preview/medley/asset metadata
- External video references that must not count as local optional video assets

## Completion check
The feature is ready for task generation when:
- Parser behavior is fully represented in tests
- Acceptance fixtures cover the mandatory scenarios from the feature spec
- The parser returns deterministic parsed-song and diagnostics results
- `./gradlew test`, `./gradlew lint`, and `./gradlew jacocoTestCoverageVerification` pass
