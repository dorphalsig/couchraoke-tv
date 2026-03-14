# Phase 0 Research — USDX Parser

## Decision 1: Keep the parser inside the `app` module as a pure domain package
- **Decision**: Implement the parser under a pure Kotlin domain package in the existing Android TV app module rather than creating a new Gradle module.
- **Rationale**: The repository currently has a single `app` module and no Kotlin source tree yet. A package-level boundary satisfies the constitution's clean-architecture requirement without adding module-management overhead before any feature code exists.
- **Alternatives considered**:
  - Create a separate `:domain` module now — rejected because it adds build complexity without immediate value.

## Decision 2: Use a custom parse result and diagnostic model
- **Decision**: Model parser output as a custom result containing parsed song data, validity, and accumulated diagnostics.
- **Rationale**: The feature requires warning and invalid diagnostics to accumulate in one pass. Standard `Result` is exception-oriented and does not fit a multi-diagnostic parser workflow.
- **Alternatives considered**:
  - Kotlin `Result` — rejected because it short-circuits and centers on exceptions rather than structured diagnostics.
  - Arrow `Either`/`Validated` — rejected because it introduces an unapproved dependency.

## Decision 3: Use a single-pass line-oriented parser with explicit phases
- **Decision**: Parse the TXT file in one pass using phase transitions for header parsing, body parsing, and post-parse cleanup.
- **Rationale**: The USDX TXT format is line-oriented and token-driven. A single-pass parser keeps behavior deterministic, maps naturally to the file format, and avoids unnecessary grammar-library complexity.
- **Alternatives considered**:
  - Two-pass tokenizer/combinator parser — rejected because the format does not need nested parsing or lookahead.

## Decision 4: Accumulate diagnostics and decide validity after parsing
- **Decision**: Continue collecting diagnostics throughout parsing and determine final validity after post-parse cleanup.
- **Rationale**: The spec requires best-effort parsing for unknown tags, optional malformed fields, and non-fatal issues while still rejecting songs that violate fatal rules. Accumulation also supports fixture-driven acceptance testing.
- **Alternatives considered**:
  - Throw and stop on first fatal error — rejected because it loses useful diagnostics and weakens troubleshooting.

## Decision 5: Split the data model into raw parsed structures and derived summary fields
- **Decision**: Represent the output as `ParsedSong` containing a normalized header, tracks, lines, notes, diagnostics, and a derived summary for downstream consumers.
- **Rationale**: Later features should consume parser output without reinterpreting USDX rules. Separating raw parsed data from derived fields keeps responsibilities explicit.
- **Alternatives considered**:
  - Expose only raw parsed fields and leave all derivation to later features — rejected because the spec explicitly places derived metadata in this feature.

## Decision 6: Inject file existence checks through an interface
- **Decision**: Perform required asset existence checks in the parser through an injected file-resolution interface.
- **Rationale**: The parser must be authoritative for single-song validity while remaining testable without touching the real filesystem in unit tests.
- **Alternatives considered**:
  - Call filesystem APIs directly in parser logic — rejected because it couples domain logic to I/O and weakens test isolation.
  - Push all existence checks into `song-library` — rejected because the spec assigns these checks to parser scope.

## Decision 7: Use fixed duet track routing during parsing
- **Decision**: Maintain an active track pointer and route note and sentence content into track 1 or track 2 based on `P1` and `P2` markers.
- **Rationale**: This is the simplest model that directly matches the file format and the feature's duet requirements.
- **Alternatives considered**:
  - Post-process one unified stream into duet tracks — rejected because it complicates line ownership and error handling.

## Decision 8: Parse and validate version explicitly
- **Decision**: Treat absent version as legacy, reject malformed versions, and reject unsupported major versions.
- **Rationale**: Version determines validity rules for required audio tags and avoids ambiguous legacy behavior.
- **Alternatives considered**:
  - Infer format only from presence of tags — rejected because it would blur legacy and modern compatibility rules.

## Decision 9: Convert zero-duration notes into freestyle notes with warnings
- **Decision**: Preserve the note as a valid parsed event by converting zero-duration notes to freestyle and emitting a warning.
- **Rationale**: This matches the source spec while keeping parsing deterministic and downstream-friendly.
- **Alternatives considered**:
  - Reject songs containing zero-duration notes — rejected because the spec defines a non-fatal conversion path.

## Decision 10: Use fixture-driven acceptance tests with expected snapshots
- **Decision**: Implement parser acceptance tests from input TXT fixtures and expected structured outputs plus expected validity/diagnostic assertions.
- **Rationale**: The feature spec and constitution both emphasize fixture-driven acceptance tests. Snapshot-like expected outputs also make parser regressions visible.
- **Alternatives considered**:
  - Inline-only test data — rejected because it does not scale as well to mandatory acceptance fixtures.

## Decision 11: Add a separate acceptance-oriented JVM test path
- **Decision**: Keep parser tests in JVM test scope and introduce a clear separation between unit-style parser tests and fixture-driven acceptance tests.
- **Rationale**: The parser is pure Kotlin and does not require Android runtime resources. JVM execution keeps tests fast and aligns with constitution requirements for isolated tests.
- **Alternatives considered**:
  - Android instrumented tests — rejected because the feature has no Android dependency.
