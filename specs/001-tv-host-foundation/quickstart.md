# Quickstart: Phase 0 TV Host Foundation

## Goal

Implement the host-owned Phase 0 foundation: USDX parsing, static-BPM beat-time conversion, deterministic scoring math, and fixture validation for F01-F06 and F08-F11.

## Scope guardrails

- Keep business logic in pure domain code.
- Keep parser and scoring math free of Android runtime, UI, playback, sockets, manifest fetching, and filesystem discovery responsibilities.
- Keep fixture file access in test/harness code only.
- Do not add dependency or version changes unless they are first planned through `gradle/libs.versions.toml`.
- Do not claim completion without a fresh scoped `testBranch` pass.

## Recommended package layout

```text
app/src/main/kotlin/com/couchraoke/tv/
├── domain/usdx/
│   ├── UsdxParser.kt
│   ├── ParseException.kt
│   ├── model/
│   └── internal/
├── domain/library/
│   └── IndexedSong.kt
└── domain/scoring/
    ├── BeatCalculator.kt
    ├── ScoringEngine.kt
    ├── model/
    └── internal/

app/src/test/kotlin/com/couchraoke/tv/
├── domain/usdx/
├── domain/scoring/
└── fixtures/
```

## Implementation order

### 1. Establish data contracts
Create the parser, index, beat-time, and scoring data models exactly as defined in `data-model.md` and `contracts/foundation-boundaries.md`.

### 2. Implement parser core
Cover these rules first:
- required headers and version handling
- `#AUDIO` / `#MP3` precedence by version
- unknown/custom tag preservation
- duet routing with `P1` / `P2`
- unsupported `B` and RELATIVE body rejection
- zero-duration Freestyle conversion
- canonical `lineScoreValue` and `trackScoreValue`

Fixture focus:
- F01 discovery/validation
- F02 header edge cases
- F03 body grammar/token recognition
- F04 duet routing
- F05 legacy RELATIVE rejection

### 3. Implement beat-time math
Cover these rules next:
- stored file beats are unscaled authored beats
- `bpmInternal = bpmFile * 4`
- lyric timing uses `micDelayMs = 0`
- scoring timing uses configured `micDelayMs`
- interval convention is start-inclusive/end-exclusive

Fixture focus:
- F06 static-BPM beat-time conversion

### 4. Implement scoring math
Cover these rules next:
- explicit `ScoringConfig`
- note-window selection and `N == 0` behavior
- note bucket accumulation
- difficulty tolerance
- octave normalization
- Rap tone-valid behavior
- line bonus
- display rounding
- medley-window score-value handling

Fixture focus:
- F08 interval semantics
- F09 tolerance + octave normalization
- F10 Rap gating
- F11 line bonus + rounding
- F03 freestyle-only scoring subcase

### 5. Build the fixture harness
Keep the Phase 0 harness at the pure unit/fixture boundary:
- parser fixtures compare deterministic parsed/discovery outputs
- scoring fixtures compare deterministic score outputs
- inline pitch samples are acceptable for tiny cases
- do not build a mock-phone harness for this phase

## Validation commands

### Authoritative completion gate

```bash
./gradlew :app:testBranch \
  --src com.couchraoke.tv.domain.usdx.UsdxParser \
  --src com.couchraoke.tv.domain.usdx.ParseException \
  --src com.couchraoke.tv.domain.usdx.model.ParsedSong \
  --src com.couchraoke.tv.domain.usdx.model.SongHeader \
  --src com.couchraoke.tv.domain.usdx.model.Track \
  --src com.couchraoke.tv.domain.usdx.model.Line \
  --src com.couchraoke.tv.domain.usdx.model.NoteEvent \
  --src com.couchraoke.tv.domain.library.IndexedSong \
  --src com.couchraoke.tv.domain.scoring.BeatCalculator \
  --src com.couchraoke.tv.domain.scoring.ScoringEngine \
  --src com.couchraoke.tv.domain.scoring.model.BeatRange \
  --src com.couchraoke.tv.domain.scoring.model.ScoringConfig \
  --src com.couchraoke.tv.domain.scoring.model.PlayerScore \
  --src com.couchraoke.tv.domain.scoring.model.PitchSample \
  --test com.couchraoke.tv.domain.usdx.UsdxParserFixtureTest \
  --test com.couchraoke.tv.domain.scoring.BeatCalculatorFixtureTest \
  --test com.couchraoke.tv.domain.scoring.ScoringEngineFixtureTest
```

### Expected completion signals
- F01-F06 and F08-F11 pass fresh
- parser and scoring math coverage reaches at least 80%
- the implementation remains pure JVM logic with no Android runtime dependency in the core

## Known implementation notes

1. The app module does not yet have `app/src/test/kotlin`; create it as part of implementation.
2. Keep runtime seams such as jitter buffering, UDP pitch transport, reconnect handling, and note-finalization scheduling documented but out of the Phase 0 completion gate.
