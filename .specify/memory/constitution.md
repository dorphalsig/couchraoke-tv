<!--
Sync Impact Report
- Version change: 1.0.0 -> 2.0.0 (MAJOR: Principle IV redefined)
- Modified principles:
  - I. Host Role & Authority -> pinned the UDP pitch frame at 20 bytes, `<IqIBBH`, little-endian.
    Previously said only "fixed-size", which let a 16-byte layout appear in mockphone's readme.
  - IV. Testing & Honest Quality Gates -> `testBranch` is no longer the single source of truth.
    Adopts the plan.md §2 gate vocabulary (U/S/L/D) and requires at least one L or D gate per
    slice. Screenshot gates must verify against a committed baseline, not record. A skipped test
    inside a gate is a failure.
- Modified sections:
  - Delivery & Compliance Workflow -> validation runs the slice's declared gates, not `testBranch`
    alone.
  - Governance -> compliance review checks gate expectations rather than `testBranch` expectations.
- Rationale for the MAJOR bump: three implementations passed every gate while the app played no
  video, left the lyrics frozen, and rendered at twice the target size. Principle IV as written
  made that outcome compliant.
- Templates requiring updates:
  - ⚠ dev skills/gradle-validation/SKILL.md (encodes the withdrawn single-gate rule)
  - ⚠ dev skills/orchestration/SKILL.md (re-runs the same gate as "independent verification")
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/spec-template.md
  - ✅ .specify/templates/tasks-template.md
- Follow-up TODOs:
  - Fix mockphone/readme.md: documents 16-byte frames; the code implements 20.
-->
# Couchraoke Constitution

## Core Principles

### I. Host Role & Authority

The Android TV app MUST remain the authoritative Host and game engine. It MUST own the shared
session, game flow, and scoring outcomes. Couchraoke is a LAN-only ecosystem. The TV app MUST NOT
persist remote song assets; companion devices MUST stream those assets on demand. Pitch transport
between devices MUST use fixed-size 20-byte UDP frames, little-endian, packed as `<IqIBBH`
(`seq` uint32, `tvTimeMs` int64, `songInstanceSeq` uint32, `playerId` uint8, `midiNote` uint8,
`connectionId` uint16). Companion devices are subordinate clients and MUST not redefine or override
host-managed session state.

Rationale: a single authoritative host prevents split-brain game state, keeps scoring
deterministic, and preserves the network and media assumptions the product is built on.

### II. Clean Architecture Boundaries

Business logic MUST remain outside UI code. ViewModels MUST be the single source of UI state.
Android framework types MUST stay confined to platform-facing layers. Repository structure and
changes MUST preserve clear Domain, Data, Presentation, and DI boundaries. When behavior spans
layers, responsibilities MUST be assigned to the narrowest layer that can own them without leaking
framework concerns upward.

Rationale: these boundaries protect testability, keep UI state predictable, and prevent platform
details from contaminating core game logic.

### III. Approved Dependency Source

Dependencies and versions MUST come from `gradle/libs.versions.toml`. Any dependency addition,
removal, or version change MUST be decided during planning, documented in the feature plan, and
applied through that catalog. Implementers MUST NOT introduce ad hoc dependency or version changes
during task execution unless the constitution or approved plan is amended first.

Rationale: centralized dependency governance keeps builds reproducible, reviewable, and consistent
across modules and automation.

### IV. Testing & Honest Quality Gates

A task is not complete until its scoped validation passes fresh. Validation MUST use the gate
vocabulary defined in `plan.md` §2: **U** (JVM unit test), **S** (screenshot verified against a
committed baseline), **L** (loopback against the real `mockphone` process over `127.0.0.1`), and
**D** (device).

`testBranch` MUST pass, but it MUST NOT be the only gate. It runs entirely in the JVM and therefore
cannot observe a native decoder, a real socket, or a real clock. Every slice MUST carry at least one
L or D gate. A slice proved only by U and S is not proved.

Screenshot gates MUST verify against a committed baseline. Record-only mode never fails and MUST NOT
be accepted as evidence. A gate MUST NOT be satisfied by a skipped test; `assumeTrue` and equivalent
runtime skips inside a gate are failures, not passes.

Failing checks MUST NOT be bypassed through skipped tests, disabled rules, unjustified suppressions,
or equivalent workarounds. Any skip, suppression, or similar exception MUST be explicitly justified
in the current task context, including why the exception is necessary and why it does not invalidate
confidence in the outcome.

Rationale: three prior implementations passed every gate while the app played no video, left the
lyrics frozen, and rendered at twice the target size. Green results mean something only when the
gate can observe the thing it claims to prove.

### V. Contract-First Planning

Before implementation begins, every material cross-feature or cross-layer boundary MUST be defined
during planning. Each contract MUST identify the producer, consumer, and callable interface as
FQCN + method + signature. Any return value or exchanged payload that crosses that boundary MUST be
documented as a data contract, including field-level details whenever downstream or upstream
expectations depend on them. Planning and execution artifacts MUST preserve the workflow units
Feature, Phase, and Task as defined below.

Rationale: explicit contracts reduce integration drift, make dependencies testable, and keep work
packages reviewable.

## Workflow Units

- **Feature**: One feature MUST map to exactly one `spec.md`, one `plan.md`, and one `tasks.md`
  unit.
- **Phase**: A phase MUST represent either a delivery slice or a dependency bucket. Phases MUST NOT
  be used as arbitrary chronology without a clear delivery or dependency purpose.
- **Task**: A task MUST be a bounded implementation or validation step with one clear outcome. It
  SHOULD usually fit in a single focused session and MUST be small enough to validate and review
  independently.

## Delivery & Compliance Workflow

- Planning artifacts MUST capture host authority, architecture boundaries, dependency decisions,
  and any material producer/consumer contracts before implementation starts.
- Implementation MUST stay within the declared scope and preserve Domain, Data, Presentation, and
  DI boundaries.
- Validation MUST run every gate the slice declares, including at least one L or D gate, and the
  validation evidence MUST be fresh when work is reported complete.
- Out-of-scope issues MAY be reported with a concise suggested fix, but they MUST NOT be addressed
  as part of the current task unless the scope is amended first.

## Governance

This constitution is the authoritative process document for Couchraoke TV work. When other local
guidance conflicts with it, this constitution takes precedence unless the conflicting instruction is
an explicit user directive for the current task.

Amendments MUST:
- be documented in the relevant feature and planning artifacts when they affect delivery behavior;
- include a clear rationale and a semantic version bump:
  - MAJOR for backward-incompatible governance changes or principle removals or redefinitions;
  - MINOR for new principles, sections, or materially expanded guidance;
  - PATCH for clarifications, wording fixes, or non-semantic refinements;
- update dependent templates and runtime guidance in the same change when those artifacts encode
  the amended rule.

Compliance review MUST occur during planning, implementation review, and pre-completion validation.
Each review MUST confirm that host authority, architecture boundaries, dependency governance,
contracts, and gate expectations remain satisfied.

**Version**: 2.0.0 | **Ratified**: 2026-04-12 | **Last Amended**: 2026-08-18
