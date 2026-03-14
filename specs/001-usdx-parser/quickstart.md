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

## Coverage gate
- `jacocoTestCoverageVerification` enforces the constitution coverage gate for JVM unit tests.
- Bundle-level line coverage must remain at or above 80%.
- Class-level line coverage must remain at or above 60%.
- Android-generated classes such as `R`, `BuildConfig`, and `Manifest` are excluded from class-level enforcement.

## Branch/worktree closure
- After this feature is integrated, rename the completed feature branch/worktree to `[✓] 001-usdx-parser` to mark it closed per branch hygiene.

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

## Fixture coverage and execution notes
- `fixtures/parser/valid/01_valid_minimal_v1` covers the minimal valid single-track path.
- `fixtures/parser/valid/metadata_rich` covers optional timing/media metadata and local optional assets.
- `fixtures/parser/duet/11_duet_valid` covers `P1`/`P2` routing and duet singer labels.
- `fixtures/parser/invalid/05_missing_required_artist`, `06_missing_required_audio`, and `07_missing_required_audio_file` cover required header and audio validity.
- `fixtures/parser/invalid/08_malformed_body_numeric`, `12_duet_invalid_marker`, `14_relative_in_v1_invalid`, and `15_bpm_change_non_relative` cover fatal body and unsupported-format failures.
- `fixtures/parser/invalid/09_no_breaks_allowed_usdx` and `10_no_notes_invalid` cover post-parse line creation and empty-track cleanup.
- `fixtures/parser/derived/06_duration_zero_converts_to_freestyle` and `rap_preview_medley` cover zero-duration conversion plus derived rap/preview/medley values.
- `fixtures/parser/edge/02_header_duplicates_last_wins`, `03_unknown_header_tags`, `07_unknown_body_token_ignored`, and `external_video` cover duplicate tags, preserved custom tags, ignored unknown body tokens, and non-local video handling.
- Use `./gradlew :app:parserUnitTest`, `./gradlew :app:parserAcceptanceTest`, or the targeted `:app:testDebugUnitTest --tests ...` commands in `tasks.md` while iterating.

## Completion check
The feature is ready for task generation when:
- Parser behavior is fully represented in tests
- Acceptance fixtures cover the mandatory scenarios from the feature spec
- The parser returns deterministic parsed-song and diagnostics results
- `./gradlew test`, `./gradlew lint`, and `./gradlew jacocoTestCoverageVerification` pass
