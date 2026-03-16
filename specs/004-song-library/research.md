# Research: Song Library (004)

**Branch**: `004-song-library` | **Date**: 2026-03-16

No blocking unknowns — all decisions are derived from existing codebase patterns and the normative spec.

---

## Decision 1: Validation Pipeline — TV Re-parses .txt via UsdxParser

**Decision**: The TV uses the existing `UsdxParser` to parse and validate song `.txt` content. `SongIndexer` converts a `ParseResult` (from `UsdxParser`) into a `SongEntry`.

**Rationale**: `UsdxParser` + `PostParseValidator` + `FileResolver` already implement all validation rules from §3.2 (required headers, BPM check, body parse, track non-empty). Building a separate validator would duplicate logic. The abstraction fits naturally: `SongDiscovery` reads `.txt` content, calls `UsdxParser`, and hands the `ParseResult` to `SongIndexer`.

**Alternatives considered**:
- *Trust phone's pre-parsed manifest fields*: Simpler at ingest time, but leaves TV unable to validate independently. Deferred to feature 005 which owns the manifest wire format and HTTP transport.
- *Re-implement validation in library domain*: Code duplication, divergence risk.

**Impact**: `SongDiscovery` orchestrates `UsdxParser` + `FileResolver` + `SongIndexer`. In production (feature 005), the HTTP `FileResolver` checks asset existence via HEAD requests or URL availability.

---

## Decision 2: SongDiscovery is a Domain Component

**Decision**: `SongDiscovery` lives in `com.couchraoke.tv.domain.library` and provides a `discoverFromDirectory(root: Path, fileResolver: FileResolver): List<SongEntry>` method used by the F01 acceptance test. It scans `.txt` files recursively, calls `UsdxParser`, then `SongIndexer`.

**Rationale**: The F01 fixture tests recursive `.txt` scanning against expected discovery output. This logic must be testable in isolation from networking. The same behavior is what the phone-side scanner would do; the TV tests it for parity validation.

**Alternatives considered**:
- *Only test via manifest ingest path*: Can't test T3.2.9 (recursive scan) without direct scan logic.

---

## Decision 3: SongLibrary is a Simple In-Memory Map, Not Reactive

**Decision**: `DefaultSongLibrary` uses a `LinkedHashMap<String, List<SongEntry>>` (keyed by `phoneClientId`). No Kotlin `Flow` or `StateFlow` in this feature — reactive wrapping is feature 005's responsibility.

**Rationale**: This is a pure domain model. Feature 005 (network/session) wraps it in a `StateFlow` for UI reactivity. Keeping it simple keeps the domain testable without coroutine scaffolding.

**Alternatives considered**:
- *StateFlow-based*: Premature coupling to coroutines in domain layer.

---

## Decision 4: previewStartSec Uses BeatTimingEngine Formula Inline

**Decision**: `SongIndexer` computes `previewStartSec` using the formula derived from `BeatTimingEngine.beatsToMs`: `beatSec = beat * 60.0 / (bpmFile * 4.0)`, `previewStartSec = beatSec + gapMs / 1000.0`. This mirrors `BeatTimingEngine` without creating a `TimingContext` dependency.

**Rationale**: `previewStartSec` needs only BPM and gap, both available on `SongHeader`. Constructing a full `TimingContext` just for this computation would be over-engineering. The formula is a single line.

**Alternatives considered**:
- *Call BeatTimingEngine.beatsToMs()*: Requires a TimingContext; unnecessary overhead for a preview offset.

---

## Decision 5: ManifestEntry is a Pure Data Class (No JSON Annotation)

**Decision**: `ManifestEntry` in the library domain is a plain Kotlin data class. JSON deserialization annotations (`@Serializable`) are added in feature 005 (network/data layer). The library domain only sees a plain `ManifestEntry`.

**Rationale**: Keeps the domain layer free of serialization concerns per Clean Architecture / constitution III.

---

## Decision 6: Null Album Sorts as Empty String

**Decision**: Songs with no `album` field sort as if `album = ""`, placing them before songs with albums in Artist → Album → Title order.

**Rationale**: Standard behavior in most music apps; avoids NullPointerException; consistent with USDX where missing `#ALBUM` is treated as absent/empty.

---

## Decision 7: Gradle Tasks Mirror Features 001–003

**Decision**: Add `libraryUnitTest`, `libraryAcceptanceTest`, `libraryTest` tasks to `app/build.gradle.kts` mirroring the existing `scoringUnitTest/scoringAcceptanceTest/scoringTest` pattern.

- `libraryUnitTest`: excludes `*AcceptanceTest.class`, includes `**/domain/library/**/*Test.class`
- `libraryAcceptanceTest`: includes `**/domain/library/**/*AcceptanceTest.class`
- `libraryTest`: both

---

## Existing Reuse Summary

| What | Where | Used By |
|------|-------|---------|
| `UsdxParser` | `domain.parser.UsdxParser` | `SongDiscovery` |
| `FileResolver` | `domain.parser.FileResolver` | `SongDiscovery`, `SongIndexer` |
| `ParseResult` / `ParsedSong` | `domain.parser.*` | `SongIndexer` |
| `DerivedSongSummary` / `MedleySource` | `domain.parser.DerivedSongSummary` | `SongIndexer` |
| `DiagnosticCode` | `domain.parser.DiagnosticCode` | `SongIndexer` (for `invalidReasonCode`) |
| F01 fixture | `original_spec/fixtures/F01_*` | `SongDiscoveryAcceptanceTest` |
