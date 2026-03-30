<!--
Sync Impact Report
- Version change: 2.0.1 -> 2.1.0
- Modified principles:
    V. Testing, Quality & Branch Hygiene — added "Visual Regression Testing (Roborazzi)"
      subsection; removed "Screenshot / snapshot regression tests" from Explicitly Out of
      Scope; updated CI Job Structure to include roborazzi job.
- Added sections:
    VI. Contract-Gated Inter-Task Development (new top-level Core Principle)
- Removed sections: none
- Templates requiring updates:
  ✅ updated .specify/templates/plan-template.md (Constitution Check now references
     Roborazzi coverage and contract-gating gate)
  ✅ updated .specify/templates/tasks-template.md (tests phase now references Roborazzi
     snapshot tasks)
  ✅ reviewed .specify/templates/spec-template.md (no change required — technology-agnostic)
  ✅ reviewed .specify/templates/constitution-template.md (source template only; no change)
  ✅ no .specify/templates/commands/ directory present; no updates required
  ✅ reviewed CLAUDE.md (no references to the changed sections; no change required)
- Follow-up TODOs: none
-->
# Couchraoke Constitution

## Core Principles

### I. Platform Identity & LAN-Only Operation
Couchraoke is a LAN-only karaoke system with no backend cloud services. The Android TV app
MUST act as the authoritative TV Host and game engine. Companion platforms MUST discover and
pair with the TV Host over mDNS, serve local song assets over HTTP, and exchange real-time
session and scoring data only over the local network. Violations of the LAN-only operating
model MUST be treated as constitution failures, not implementation shortcuts.

### II. Approved Platform Technology Stack
Platform implementations MUST use the approved stack for their target surface.

- TV Host code MUST use Kotlin, Jetpack Compose for TV, coroutines with Flow,
  kotlinx-serialization-json, Media3, Coil, JmDNS, Ktor server CIO with WebSockets,
  DataStore, Hilt, and the declared testing stack.
- Android Companion code MUST keep to the Android-specific networking, storage, and audio
  constraints defined by the platform constitution source.
- iOS Companion code MUST keep to the Network.framework, URLSessionWebSocketTask, Swifter,
  and protocol-driven dependency boundaries defined by the platform constitution source.
- New work MUST NOT introduce forbidden alternatives called out by the governing platform
  rules, including cloud backends, legacy media stacks, reflection-based JSON parsing,
  or unauthorized dependency-injection patterns.

### III. Clean Architecture Boundaries
Business logic MUST live outside UI code and MUST follow the platform boundary rules.
ViewModels are the single source of UI state. Android framework types MUST remain in `data/`
or `di/` layers on Android and TV. iOS hardware and framework dependencies MUST be hidden
behind Domain protocols. Directory structures for each platform MUST continue to reflect the
Domain/Data/Presentation/DI separation established by the governing platform design.

### IV. Streaming, Networking & Performance Contracts
Playback, scoring, and transport behavior are contractual.

- Song assets MUST stream directly from companion devices; hosts MUST NOT persist remote song
  assets locally.
- Control payloads MUST remain small JSON messages on the LAN; pitch transport MUST use fixed
  16-byte UDP packets with invalid frames dropped rather than retried through a different
  transport.
- The TV scoring loop MUST remain decoupled from UI rendering and preserve the required polling
  cadence.
- Implementations MUST honor platform performance targets for startup, discovery, latency,
  memory, and frame rate.

### V. Testing, Quality & Branch Hygiene

#### Test-Driven Development
Tests MUST be written before or alongside the production code they cover. No production code
is merged without corresponding tests. This is a CI gate, not a suggestion.

#### Coverage Requirements
Coverage is measured across the full test suite (unit + integration + acceptance). Running only
unit tests does not satisfy the target.

| Threshold | Value |
|---|---|
| Overall project line coverage | ≥ 80% |
| Per-file minimum | ≥ 60% |
| Tiny file exemption | Files with ≤ 30 non-comment, non-blank lines |
| Generated code exemption | Protobuf stubs, schema-generated types |

CI MUST fail the build if either threshold is not met on any qualifying file.
Coverage tooling: **JaCoCo** for Android (configured in Gradle); **Xcode code coverage** (`LLVM_COV`)
for iOS.

#### Test Categories
Android unit and instrumented tests MUST use JUnit4-only tooling. JUnit5/Jupiter APIs,
engines, tags, and extensions MUST NOT be introduced for Android test execution unless the
constitution is amended again.

| Category | Definition | Annotation |
|---|---|---|
| **Unit [U]** | Single class/function in isolation; all I/O mocked | JUnit4 test class/method conventions |
| **Instrumented [I]** | Requires real OS resource: socket, filesystem, hardware | Android: `@MediumTest` or `@LargeTest`; iOS: on-simulator/device |
| **Acceptance** | Fixture-driven; consumes `fixtures/` and asserts against `expected.*` files | Android: dedicated `*AcceptanceTest` classes or an acceptance source set/task selected by Gradle naming/task configuration; iOS: `XCTestCase` subclass suffixed `AcceptanceTests` |

Instrumented tests MUST NOT run in the unit test CI job. They run in a separate job that
provisions an emulator or simulator.

#### Test Naming Conventions
Android (Kotlin):
```kotlin
fun `given <context>, when <action>, then <expected outcome>`()
// Example:
fun `given toneValid false, when encoding pitchFrame, then midiNote byte is 255`()
```

iOS (Swift):
```swift
func test_<subject>_<action>_<expectedOutcome>()
// Example:
func test_pitchFrameEncoder_toneValidFalse_setsMidiNoteTo255()
```

One assertion focus per test. A test that validates 5 unrelated behaviours is 5 tests.

#### Test Isolation
- Tests MUST NOT share mutable state. No `static` / `companion object` fields that accumulate
  state across tests.
- Tests MUST NOT depend on execution order. Each test sets up and tears down its own state.
- Tests MUST NOT touch the real filesystem, real network, or real clock unless explicitly
  tagged `[I]`.
- **Clock**: inject a `FakeClock` / `TestCoroutineScheduler` — never call
  `System.currentTimeMillis()` or `Date()` directly in testable code.

#### Coroutine Testing (Android)
- Use `StandardTestDispatcher` (not `UnconfinedTestDispatcher`) as the default in all unit
  tests. `UnconfinedTestDispatcher` masks ordering bugs by running coroutines eagerly.
- Wrap test bodies with `runTest { }` from `kotlinx-coroutines-test`.
- Inject dispatchers via constructor — never hardcode `Dispatchers.IO` or `Dispatchers.Main`
  in production classes that need to be tested.

```kotlin
// Production
class SongScanner(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO)
// Test
val scanner = SongScanner(ioDispatcher = StandardTestDispatcher(testScheduler))
```

#### Visual Regression Testing (Roborazzi)
Every screen in the TV app MUST have Roborazzi screenshot baselines committed to the
repository. This is an additive requirement on top of unit and instrumented test coverage —
it does not replace other test categories.

- **Screen coverage**: Every distinct screen state (empty, loading, populated, error) MUST
  have a dedicated Roborazzi snapshot test.
- **Navigation path coverage**: Every path that leads to a screen MUST be tested starting
  from the immediately preceding screen in the navigation graph. All paths to the current
  screen MUST be covered; a screen with multiple entry points requires one test per entry
  point.
- **Interaction coverage**: Every interaction that opens or dismisses an overlay (modal
  dialog, bottom sheet, snackbar, or blocking error) MUST be captured in both the
  pre-interaction state and the post-interaction state.
- **Contract-gated mocking**: Any screen or component that depends on an interface owned by
  a future (unimplemented) feature MUST be driven by a mock that strictly implements the
  published contract interface. If the required contract interface has not been published,
  the Roborazzi test task MUST NOT be started — see Principle VI.
- **Baseline management**: Baseline images MUST be committed to the repository under
  `app/src/test/snapshots/` (or the Roborazzi-configured path). Diffs are reviewed as part
  of the PR.
- **CI placement**: Roborazzi tests run in the instrumented CI job (emulator required).
  They are excluded from JaCoCo coverage calculations.

#### Skip / Disable Policy
A test MAY be skipped only under one of these three conditions:

| Condition | Required action |
|---|---|
| Blocked by a known spec ambiguity | Annotate with `@Ignore("SPEC-<issue>: <reason>")` (Android) or `try XCTSkip("SPEC-<issue>: ...")` (iOS). Must link to a tracked issue. |
| Hardware-only test in unit CI job | Move to the instrumented job instead. Do not skip — fix the job configuration. |
| Demonstrably flaky (≥ 2 failures in 10 runs) | Move to quarantine (see Flaky Test Quarantine below). |

Blanket `@Ignore` on a test class is PROHIBITED. Disable individual test methods only.
Skipped tests count against coverage. A file full of skipped tests will fail the per-file
coverage gate.

#### Flaky Test Quarantine
A test is **flaky** if it fails intermittently without code changes (timing sensitivity,
uncontrolled I/O, etc.).

1. Move the test to a `quarantine` source set / test target separate from the main suite.
2. Open a tracking issue with: test name, failure mode, reproduction rate.
3. The quarantine suite runs on a nightly schedule, not on every PR.
4. A quarantined test MUST be fixed or deleted within **2 sprints**. Indefinite quarantine is
   not permitted.
5. Quarantined tests are excluded from coverage measurement until restored.

#### Static Analysis
Static analysis is a CI gate — the build fails if any rule configured as `error` is violated.

**Android**

| Tool | Role | Config file | CI gate |
|---|---|---|---|
| **Detekt** `1.23.x` | Kotlin code smells, complexity, style | `detekt.yml` in repo root | Yes — error-level findings fail build |
| **ktlint** `1.2.x` | Formatting (via Detekt `detekt-formatting` plugin) | `.editorconfig` | Yes |
| **Android Lint** | Android-specific issues (missing permissions, deprecated APIs) | `lint.xml` | Yes — `abortOnError true` |

Detekt rules in scope:
```yaml
complexity:
  LongMethod:
    threshold: 40        # test methods exempt via @Suppress if genuinely data-driven
  CyclomaticComplexMethod:
    threshold: 10
style:
  MagicNumber:
    active: true
    ignoreTests: true    # fixture numeric literals in tests are fine
naming:
  FunctionNaming:
    active: true
    functionPattern: '^[a-z`][a-zA-Z0-9 ,_`<>()?.]*$'   # allows backtick test names
```

**iOS**

| Tool | Role | Config file | CI gate |
|---|---|---|---|
| **SwiftLint** `0.57.x` | Swift style and code smells | `.swiftlint.yml` in repo root | Yes — errors fail build, warnings do not |

SwiftLint rules enabled beyond defaults:
```yaml
opt_in_rules:
  - force_unwrapping        # error: no force-unwrap in production code
  - explicit_init
  - closure_spacing
disabled_rules:
  - todo                    # TODOs allowed with ticket reference; enforced by PR review
```

Force-unwrap (`!`) in test files is permitted where the test will immediately crash and clearly
indicate the failure. It is NOT permitted in production code.

#### CI Job Structure
```text
PR gate (runs on every commit):
  ├── unit-tests-android      [U] only — JUnit4, no emulator
  ├── unit-tests-ios          [U] only — XCTest, no simulator hardware
  ├── lint-android            Detekt + ktlint + Android Lint
  ├── lint-ios                SwiftLint
  └── coverage-check          Fails build if thresholds not met
Nightly:
  ├── instrumented-android    [I] + Roborazzi snapshots — emulator required
  ├── instrumented-ios        [I] — simulator required
  └── quarantine-suite        Flaky tests; results reported but do not block
```

#### Explicitly Out of Scope
The following are not tested and MUST NOT be added to coverage calculations:

- `AudioRecord` / `AVAudioEngine` hardware latency characterisation
- ExoPlayer streaming latency (only seek accuracy is tested)
- mDNS advertisement timing and multi-device discovery (manual integration test only)
- Advanced Search — POST-MVP
- ISO-8859-1 legacy encoding (`F02 encoding_legacy_honors`) — explicitly skipped

#### Branch Hygiene
After development is complete and a worktree or feature branch has been merged into `master`,
that branch MUST be marked closed by renaming it to `[✓] <original branch name>`. Temporary
isolation is encouraged during development, but post-merge closure is mandatory. Closed branches
MUST NOT be deleted solely as a hygiene step.

### VI. Contract-Gated Inter-Task Development
No implementation task that depends on an interface, protocol, or data contract owned by
another task (current or future) MAY proceed until that contract is formally specified and
committed to the repository.

- **Stop rule**: If a required contract is absent, the dependent task MUST stop immediately
  and report the missing contract. Work on the dependent task MUST NOT resume until the
  contract is defined and merged.
- **No assumptions permitted**: Implementing agents MUST NOT infer, guess, or assume the
  shape of an unspecified contract. The contract MUST be explicitly documented before any
  code that relies on it is written.
- **Mock discipline**: Where a contract is defined but its implementation belongs to a future
  task, the dependent task MUST use a mock or stub that strictly implements the published
  contract interface — no wider, no narrower.
- **Contract location**: Contracts MUST be documented in `specs/<feature>/contracts/` before
  the tasks that depend on them are planned. The planning step (`/speckit.plan`) MUST verify
  that all required contracts exist before generating tasks.
- **Scope boundary**: This principle applies to all task types — unit tests, instrumented
  tests, Roborazzi snapshot tests, and production code alike. A Roborazzi test that renders
  a screen depending on a future feature's navigation target is subject to the same stop rule.

## Additional Constraints

Platform-specific operational rules from the governing Couchraoke platform constitution remain
normative, including:

- TV Host progressive HTTP streaming via Media3, direct remote playback, dedicated scoring
  polling, and cleartext allowlisting only for RFC-1918 LAN addresses.
- Android Companion requirements for SAF-backed file access, HTTP Range support, mDNS browsing,
  and UDP pitch transmission.
- iOS Companion requirements for `NWBrowser`, `NWConnection` UDP transport, Range-serving HTTP,
  security-scoped file access, and active-session device lifecycle handling.
- Platform performance ceilings and memory budgets remain release-blocking requirements.

## Development Workflow

Every implementation, review, and merge decision MUST verify constitutional compliance.

- Before design or implementation begins, plans MUST confirm alignment with LAN-only operation,
  approved stack choices, architecture boundaries, streaming/networking contracts, test
  expectations, and the presence of all required inter-task contracts (Principle VI).
- After every task, teams MUST verify test coverage updates, Roborazzi snapshot baselines,
  absence of unintended adjacent regressions, and any platform-specific checklist items
  relevant to the changed code.
- Before merge, reviewers MUST confirm that quality gates passed and that any worktree or
  temporary development branch has a cleanup path.
- After merge to `master`, the merged worktree branch or feature branch MUST be renamed to
  `[✓] <original branch name>` unless it is still actively carrying unmerged work.

## Governance

This constitution supersedes conflicting local habits and ad hoc workflow choices for the
Couchraoke repository.

- Amendments MUST be documented in `.specify/memory/constitution.md`, include a Sync Impact
  Report, and update any affected templates or runtime guidance in the same change.
- Compliance reviews MUST occur during planning, code review, and pre-merge verification.
  Non-compliance MUST be fixed or explicitly approved as a documented exception before merge.
- Semantic versioning governs this constitution: MAJOR for incompatible governance changes,
  MINOR for new principles or materially expanded obligations, and PATCH for clarifications or
  editorial refinements.
- Repository hygiene is part of compliance. Branches and worktrees that no longer represent
  active work MUST be clearly marked closed after merge by renaming them to
  `[✓] <original branch name>`.

**Version**: 2.1.0 | **Ratified**: 2026-03-13 | **Last Amended**: 2026-03-30
