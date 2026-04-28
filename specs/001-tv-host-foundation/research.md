# Phase 0 Research

## Decision 1: Keep Phase 0 core as pure JVM domain logic
- **Decision**: Implement parser, beat-time conversion, and scoring math as pure Kotlin domain code with fixture/file access only in the test harness.
- **Rationale**: The specification requires host-independent validation with no Android runtime, LAN, playback, sockets, UI, or filesystem responsibilities inside the parser/scoring core, and the authoritative gate is JVM-based `testBranch` validation.
- **Alternatives considered**:
  - Put parsing/scoring behind Android-facing classes in `:app` only — rejected because it would weaken pure-JVM validation.
  - Build a phone/mock runtime harness first — rejected because Phase 0 explicitly excludes peer/runtime behavior from the delivery gate.

## Decision 2: Use deterministic parser-internal decode rules and treat `#ENCODING` as unknown metadata
- **Decision**: Use deterministic internal decoding rules for raw TXT bytes and preserve `#ENCODING` only as an unknown/custom tag with no semantic processing.
- **Rationale**: The clarified Phase 0 rules require deterministic parsing, preserve unknown tags in encounter order, and explicitly forbid semantic handling of `#ENCODING`.
- **Alternatives considered**:
  - Charset auto-detection — rejected because it would introduce nondeterminism across fixtures.
  - Semantic handling of `#ENCODING` — rejected because the spec explicitly keeps it as unknown metadata.

## Decision 3: Lock header/version/audio resolution exactly to the Phase 0 contract
- **Decision**: Header parsing ends at the first non-`#` line, matches tags case-insensitively, keeps last successfully parsed value for duplicates, treats malformed required headers as invalid, treats malformed optional headers as warnings, and preserves unknown tags. For audio resolution, `#AUDIO` takes precedence over `#MP3` only for version `>= 1.0.0`; legacy songs require `#MP3` and ignore `#AUDIO`.
- **Rationale**: This keeps fixture expectations stable across F01 and F02 and prevents later playback-oriented concerns from leaking into parsing.
- **Alternatives considered**:
  - Resolve audio tags uniformly across all versions — rejected because legacy compatibility rules differ.
  - Discard unknown tags — rejected because ordered custom-tag preservation is a fixture requirement.

## Decision 4: Preserve preview/start semantics exactly
- **Decision**: Parsed `SongHeader.previewStartSec` preserves only a valid source `#PREVIEWSTART` value and remains nullable. Library/index projection materializes non-null `previewStartSec`: use `#PREVIEWSTART` when present and positive; otherwise, for valid solo medley tags use `BeatCalculator.beatInternalToTimeSec(medleyStartBeat.toDouble(), bpmFile * 4).toFloat()`; otherwise use `0.0`. `#START` affects only `startSec`.
- **Rationale**: This resolves a clarified ambiguity and keeps parser/header state distinct from the deterministic library-facing fallback value.
- **Alternatives considered**:
  - Reuse `#START` as a preview fallback — rejected because the clarified contract forbids it.
  - Leave preview unset when tags are absent — rejected because the library-facing contract requires deterministic defaults.

## Decision 5: Reject RELATIVE body syntax but preserve `#RELATIVE` header lines as custom tags
- **Decision**: Preserve header `#RELATIVE` as unknown metadata, but reject RELATIVE body sentence syntax with `ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE`.
- **Rationale**: Phase 0 must document legacy behavior explicitly and F05 is part of the required gate.
- **Alternatives considered**:
  - Support RELATIVE mode — rejected by user clarification and fixture scope.
  - Reject the header tag itself — rejected because the header rule preserves unknown/custom tags.

## Decision 6: Make parser-computed line and track score values canonical
- **Decision**: The parser computes `lineScoreValue` and `trackScoreValue` once from authored note data and score factors; scoring consumers must use those values rather than recomputing their own totals.
- **Rationale**: This gives F03, F08, and F11 one authoritative chart-value source and prevents drift between parsing and scoring.
- **Alternatives considered**:
  - Recompute score values inside scoring — rejected because it duplicates chart logic and risks divergence.
  - Precompute only at track level — rejected because line-bonus math also depends on canonical line totals.

## Decision 7: Keep beat/time math on authored file beats with BPM × 4
- **Decision**: Use authored file beats as the stored/internal beat numbers with no beat scaling; convert time using `bpmInternal = bpmFile * 4`; use start-inclusive/end-exclusive intervals for both note activity and scoring sample inclusion.
- **Rationale**: This matches F06 expectations and keeps parser, highlighting, and scoring on the same beat grid.
- **Alternatives considered**:
  - Scale stored beats by 4 internally — rejected because the Phase 0 contract stores authored file beats as-is.
  - Use inclusive end boundaries — rejected because the fixtures define exclusive end semantics.

## Decision 8: Separate lyric timing from scoring timing through mic-delay rules
- **Decision**: Lyric highlighting always uses `micDelayMs = 0`; scoring windows and any later lane-beat consumer use the configured `micDelayMs` in `0..400` and shift later by that amount.
- **Rationale**: The shared math must support both consumers without letting one timing model contaminate the other.
- **Alternatives considered**:
  - Apply mic delay to all consumers — rejected because lyrics must follow heard audio, not singer compensation.
  - Ignore mic delay until later phases — rejected because F06 already asserts scoring-window behavior.

## Decision 9: Keep scoring configuration explicit and deterministic
- **Decision**: All song-specific scoring inputs flow through `ScoringConfig(playerDifficulties, lineBonusEnabled)` and the `loadChart` call. Default difficulty is Medium for newly assigned singers.
- **Rationale**: This keeps scoring pure, reproducible, and independent of UI/global mutable state.
- **Alternatives considered**:
  - Read settings directly from UI or app state — rejected because the Phase 0 contract forbids implicit inputs.
  - Hardcode one difficulty for all players — rejected because F09 requires per-player tolerance rules.

## Decision 10: Preserve exact hit detection, octave normalization, and note-bucket rules
- **Decision**: Normal/Golden notes require tone-valid samples and octave-normalized pitch within tolerance; Rap/RapGolden use tone-valid presence only; Freestyle always scores zero. Octave normalization uses the full-semitone while-loop and never pitch-class modulo 12.
- **Rationale**: These rules are the core of F08-F10 and must remain exact for deterministic fixture math.
- **Alternatives considered**:
  - Normalize with modulo 12 — rejected because it breaks the specified octave-distance behavior.
  - Give Freestyle partial score for voiced samples — rejected because score factor is fixed at zero.

## Decision 11: Preserve line-bonus and display-rounding asymmetry exactly
- **Decision**: Use the forgiveness rule for very small line maxima, skip empty-line bonuses, update `scoreLast` after each line-bonus application, and keep the asymmetric rounding formulas for normal, golden, and line score display totals.
- **Rationale**: F11 depends on these details, including the intentional opposite-direction golden rounding that prevents totals above `10000`.
- **Alternatives considered**:
  - Normalize all buckets to one rounding formula — rejected because it would change fixture-visible totals.
  - Apply bonus to empty lines — rejected because the line-value contract explicitly excludes them.

## Decision 12: Keep the Phase 0 fixture harness at the pure unit/fixture boundary
- **Decision**: Use parser/scoring fixture tests and inline pitch samples where practical; do not build a mock-phone harness, WebSocket harness, UDP sender, or Android runtime test layer for Phase 0 delivery.
- **Rationale**: The Phase 0 gate is parser, beat-time, and scoring math only. Runtime peer-boundary utilities are for later phases.
- **Alternatives considered**:
  - Build reusable phone simulation now — rejected because it is broader than the slice and not needed for F01-F06/F08-F11.
  - Skip fixture discovery outputs and assert only ad hoc values — rejected because deterministic JSON/fixture comparisons are part of the feature contract.

## Decision 13: Define the authoritative completion gate as scoped `:app:testBranch`
- **Decision**: Completion requires a fresh scoped `:app:testBranch` run over the Phase 0 parser, beat-time, scoring, and fixture test classes, plus the built-in Detekt and JaCoCo checks attached to that task.
- **Rationale**: The constitution names `testBranch` as the authoritative completion gate, and the quality-conventions plugin already wires selective tests, Detekt, and coverage verification into `:app:testBranch`.
- **Alternatives considered**:
  - Use plain `testDebugUnitTest` only — rejected because it omits the authoritative scoped quality gate.
  - Use whole-module `check` — rejected because it is broader than the scoped Phase 0 slice.

## Decision 14: Standardize app Kotlin source roots now
- **Decision**: Keep app Kotlin source in `app/src/main/kotlin` and tests in `app/src/test/kotlin`, and move the existing app Kotlin files into that layout immediately before further implementation.
- **Rationale**: The selective `testBranch` Detekt and source-directory lookup resolve `src/main/kotlin` and `src/test/kotlin` directly, so starting with the standard layout avoids mixed-root drift.
- **Alternatives considered**:
  - Leave current files in `src/main/java` and place only new code in `src/main/kotlin` — rejected because it would preserve an unnecessary inconsistency.
  - Keep all Kotlin under `src/main/java` — rejected because the scoped validation tooling would not track those files cleanly.

## Decision 15: Preserve the observable library catalog seam in Phase 0
- **Decision**: `LibraryManager` keeps both `val songs: StateFlow<List<IndexedSong>>` and `fun getSong(songId: String): IndexedSong?`, but Phase 0 may back `songs` with fixture/static data only.
- **Rationale**: Downstream TV consumers depend on the observable catalog seam even though runtime refresh behavior is out of the Phase 0 gate.
- **Alternatives considered**:
  - Reduce the contract to lookup-only — rejected because it narrows the later consumer seam.
  - Require live refresh now — rejected because that belongs to later runtime phases.

## Decision 16: Keep parser diagnostics structured and machine-facing
- **Decision**: Core parser diagnostics carry severity, stable code, TXT identifier, and deterministic line number when present; human-readable diagnostic text is produced by logging or presentation mapping, not stored in the core parse result.
- **Rationale**: This keeps fixture assertions stable and avoids coupling the core parser contract to UI-facing or log-facing text.
- **Alternatives considered**:
  - Carry human-readable detail strings in `DiagnosticEntry` — rejected because it weakens the machine-facing contract and invites brittle fixture expectations.
  - Require UI to display parser diagnostic detail directly — rejected because generic invalid-song messaging is sufficient at the presentation boundary.

## Decision 17: Treat `#INSTRUMENTAL` and `#VOCALS` as custom metadata only
- **Decision**: `#INSTRUMENTAL` and `#VOCALS` are preserved only in `customTags` and have no TV-side playback semantics. The TV always consumes a single phone-provided premixed `audioUrl`.
- **Rationale**: Source-stem handling belongs to the phone-side asset contract, not the TV-side Phase 0 parser/index contract.
- **Alternatives considered**:
  - Model separate stem fields in `SongHeader` or `IndexedSong` — rejected because the TV no longer consumes separate instrumental/vocal assets.
  - Keep `hasInstrumental` as behavior-driving metadata — rejected because it would incorrectly leak playback strategy into the TV contract.
