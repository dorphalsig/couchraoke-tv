<!--
Sync Impact Report
- Version change: unversioned template -> 1.0.0
- Modified principles:
  - Template principle slot 1 -> I. Host Role & Authority
  - Template principle slot 2 -> II. Clean Architecture Boundaries
  - Template principle slot 3 -> III. Approved Dependency Source
  - Template principle slot 4 -> IV. Testing & Honest Quality Gates
  - Template principle slot 5 -> V. Contract-First Planning
- Added sections:
  - Workflow Units
  - Delivery & Compliance Workflow
- Removed sections:
  - None
- Templates requiring updates:
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/spec-template.md
  - ✅ .specify/templates/tasks-template.md
  - ⚠ .specify/templates/commands/*.md (directory not present)
  - ✅ CLAUDE.md (verified aligned; no edit required)
- Follow-up TODOs:
  - None
-->
# Couchraoke Constitution

## Core Principles

### I. Host Role & Authority

The Android TV app MUST remain the authoritative Host and game engine. It MUST own the shared
session, game flow, and scoring outcomes. Couchraoke is a LAN-only ecosystem. The TV app MUST NOT
persist remote song assets; companion devices MUST stream those assets on demand. Pitch transport
between devices MUST use fixed-size UDP frames. Companion devices are subordinate clients and MUST
not redefine or override host-managed session state.

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

`testBranch` MUST be the authoritative task gate and the single source of truth for task
completion. A task is not complete until its scoped `testBranch` passes fresh. Failing checks MUST
NOT be bypassed through skipped tests, disabled rules, unjustified suppressions, or equivalent
workarounds. Any skip, suppression, or similar exception MUST be explicitly justified in the
current task context, including why the exception is necessary and why it does not invalidate
confidence in the outcome.

Rationale: green results are only meaningful when they represent genuine compliance with the agreed
scope and quality standards.

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
- Validation MUST use the scoped `testBranch` as the completion gate, and the validation evidence
  MUST be fresh when work is reported complete.
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
contracts, and `testBranch` expectations remain satisfied.

**Version**: 1.0.0 | **Ratified**: 2026-04-12 | **Last Amended**: 2026-04-26
