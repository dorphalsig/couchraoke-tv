# Implementation Plan: USDX Parser

**Branch**: `001-usdx-parser` | **Date**: 2026-03-14 | **Spec**: [`/specs/001-usdx-parser/spec.md`](/home/paavum/Couchraoke/tv/specs/001-usdx-parser/spec.md)
**Input**: Feature specification from `/specs/001-usdx-parser/spec.md`

## Summary

Build a pure Kotlin USDX TXT parser inside the Android TV app that parses one song at a time, validates required song rules, accumulates diagnostics, and emits downstream-ready derived song metadata for later library, timing, and scoring features.

## Technical Context

**Language/Version**: Kotlin 2.3.10 on Java 11
**Primary Dependencies**: Kotlin standard library, kotlinx-serialization-json (test/support use only if needed), existing Gradle/JUnit stack
**Storage**: N/A for feature runtime; parser consumes in-memory song TXT content and asset existence lookups
**Testing**: JUnit 4 JVM tests plus fixture-driven acceptance tests under `app/src/test`
**Target Platform**: Android TV app, but parser itself is pure JVM/domain logic
**Project Type**: Mobile app feature with pure domain parser component
**Performance Goals**: Deterministic single-pass parsing for one song file, fast enough for library scanning and test fixtures without Android runtime dependency
**Constraints**: Must stay inside approved stack, keep parser logic outside UI/framework layers, avoid real filesystem/network access in unit tests, satisfy constitution TDD and coverage gates
**Scale/Scope**: Single-song parsing only; one or two tracks per song; valid, invalid, duet, edge-case, and derived-metadata fixture coverage

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **LAN-only / authoritative host**: PASS — the parser is an internal TV-host domain component and does not alter the LAN-only operating model.
- **Approved stack**: PASS — plan stays within Kotlin, Gradle, and existing approved dependencies; no unapproved parser library is introduced.
- **Architecture boundaries**: PASS — parser code is planned as pure domain logic with I/O abstracted behind an interface.
- **Networking / streaming / scoring contracts**: PASS — no transport, streaming, or scoring behavior is changed in this feature.
- **Tests and quality gates**: PASS — plan requires TDD, JVM unit tests, fixture-driven acceptance tests, JaCoCo coverage tooling, documented enforcement of constitution coverage thresholds/exemptions, and Gradle verification including `jacocoTestCoverageVerification`.
- **Branch cleanup**: PASS — feature branch `001-usdx-parser` is temporary and must be removed after merge per constitution branch hygiene.

## Project Structure

### Documentation (this feature)

```text
specs/001-usdx-parser/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── parser-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/couchraoke/tv/
    │   │   ├── domain/parser/
    │   │   │   ├── ParsedSong.kt
    │   │   │   ├── SongHeader.kt
    │   │   │   ├── Track.kt
    │   │   │   ├── Line.kt
    │   │   │   ├── NoteEvent.kt
    │   │   │   ├── DiagnosticEntry.kt
    │   │   │   ├── DerivedSongSummary.kt
    │   │   │   ├── ParseResult.kt
    │   │   │   ├── FileResolver.kt
    │   │   │   ├── UsdxParser.kt
    │   │   │   ├── HeaderParser.kt
    │   │   │   ├── BodyParser.kt
    │   │   │   └── PostParseValidator.kt
    │   │   └──
    │   ├── kotlin/com/couchraoke/tv/data/files/
    │   │   └── LocalFileResolver.kt
    │   └── res/
    └── test/
        ├── kotlin/com/couchraoke/tv/domain/parser/
        │   ├── ParsedSongModelTest.kt
        │   ├── HeaderParserTest.kt
        │   ├── BodyParserTest.kt
        │   ├── PostParseValidatorTest.kt
        │   └── UsdxParserAcceptanceTest.kt
        └── resources/fixtures/parser/
            ├── valid/
            ├── invalid/
            ├── duet/
            ├── derived/
            └── edge/
```

**Structure Decision**: Keep the feature inside the existing `app` module, but isolate it under a domain parser package so business rules remain framework-free and later features can depend on a stable parser boundary. The production file resolver adapter lives under `app/src/main/kotlin/com/couchraoke/tv/data/files/` to preserve the domain/data boundary. Parser acceptance fixtures should be sourced from `original_spec/fixtures/song_txt_variants/` and copied or adapted into `app/src/test/resources/fixtures/parser/` as needed for JVM tests.

## Phase 0: Research Output

Research decisions are documented in [`research.md`](./research.md). All technical unknowns from this feature are resolved with the following outcomes:
- parser remains in the current module as a pure domain package
- custom parse result and diagnostic model is used
- parsing is single-pass and line-oriented
- diagnostics are accumulated and validity is decided after parsing
- asset existence checks are injected via an interface
- acceptance testing is fixture-driven in JVM test scope

## Phase 1: Design & Contracts Output

### Data Model
See [`data-model.md`](./data-model.md).

### Contracts
See [`contracts/parser-contract.md`](./contracts/parser-contract.md).

### Quickstart
See [`quickstart.md`](./quickstart.md).

## Post-Design Constitution Check

- **LAN-only / authoritative host**: PASS — unchanged.
- **Approved stack**: PASS — design still avoids new non-approved libraries.
- **Architecture boundaries**: PASS — parser/domain, file resolver abstraction, and later data-layer adapter keep boundaries clean.
- **Networking / streaming / scoring contracts**: PASS — untouched by this design.
- **Tests and quality gates**: PASS — design includes TDD, unit tests, acceptance fixtures, JaCoCo coverage tooling, documented enforcement of constitution coverage thresholds/exemptions, and Gradle verification including coverage verification gate.
- **Branch cleanup**: PASS — feature branch remains disposable after merge.

## Complexity Tracking

No constitution violations or justified exceptions are required for this feature.
