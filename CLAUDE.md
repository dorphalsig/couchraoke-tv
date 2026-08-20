# Couchraoke TV: Agent Runtime Policy
Mandatory runtime policy for all LLM agents in this repository.
---
## §1 Startup
MUST read the following before any work — in this order:
```
Read .specify/memory/constitution.md
Read CLAUDE.md  ← this file
Load skill `orchestration`
```
All three are mandatory. Treat them as invariants for the session.
---

## §2 Scope & Edit Boundaries
- MUST NOT edit files outside the task's explicit scope
- MAY also read or edit any file **in this repository** that directly references,
  or is directly referenced by, a scoped file — including its dependencies,
  callers, data types, and test counterparts
- MUST NOT navigate into third-party library source; use Context7 for their APIs
- MUST NOT make out-of-scope changes, even if beneficial
- MUST report detected OOS issues with a concise suggested fix — MUST NOT fix them
- MUST NOT edit anything under quality-conventions/ without explicit previous consent by the user
---

## §3 Skills
Four skills govern specific workflows in this repository. They auto-load based on context.
**If a skill has not auto-loaded when you need it, read it explicitly before proceeding.**

Skills live in `.github/skills/<name>/SKILL.md`, resolved **relative to the main checkout**, not
`$HOME`. `.github/` is currently untracked, so it is absent from every git worktree. When you
delegate to a subagent working in a worktree, pass the main-checkout absolute path or the subagent
cannot load the skill at all.

| Skill | Read when you are about to... | Explicit load |
|---|---|---|
| `navigation` | navigate code, look up a symbol, discover files, decide how much to read | `Read <main-checkout>/.github/skills/navigation/SKILL.md` |
| `gradle-validation` | run any Gradle command, execute tests, check snapshots, claim completion | `Read <main-checkout>/.github/skills/gradle-validation/SKILL.md` |
| `implementation` | start implementing, writing code | `Read <main-checkout>/.github/skills/implementation/SKILL.md` |
| `orchestration` | manage tasks, make delegation decisions, manage worktrees/branches | `Read <main-checkout>/.github/skills/orchestration/SKILL.md` |
---

## §3a Environment
This repository is developed on **Windows**. Assume PowerShell, not a POSIX shell.

- **There is no bash.** Only `git.exe` is installed; there is no Git Bash. Every script under
  `.specify/scripts/bash/` is unrunnable here. Inspect the repository directly instead of invoking
  them, and do not report a task blocked solely because a `.sh` helper would not run.
- Use `.\gradlew.bat`, not `./gradlew`. There is no `timeout` command; a wrapper like
  `timeout 10m ./gradlew …` fails outright.
- PowerShell splits an argument at a `.`, so `-Proborazzi.record=true` does not reach Gradle. Use
  the camelCase property form instead.
- Any `uv` invocation needs `UV_SYSTEM_CERTS=1` in its environment. Corporate TLS interception
  otherwise fails the build with `invalid peer certificate: UnknownIssuer`. This applies to the
  `mockphone` peer the loopback gate launches as a subprocess — set it in the **subprocess**
  environment, not just your own shell.
---

## §4 Error Protocol
MUST NOT make blind changes after any failure. The `gradle-validation` skill is the
authoritative source for the full error protocol. In all cases:
- Read the failure output first
- Navigate to the reported problem location before touching anything
- Perform Root-Cause Analysis, follow the execution chain 1 step away. DO NOT go in libraries or native code.
- Still failing after three full cycles → STOP and warn the user
---

## §5 Invariants
These apply at all times, no skill exempts them:
- MUST operate only within the task's declared scope and constraints
- MUST preserve TV host authority, LAN-only assumptions, streaming-only remote song assets,
  and fixed-size UDP pitch transport when touching those flows
- MUST keep business logic out of UI, ViewModels as the single source of UI state,
  and Android framework types in platform-facing layers
- MUST plan dependency additions, removals, and version changes in advance and route them through
  `gradle/libs.versions.toml`
- MUST define material producer or consumer contracts during planning as FQCN + method +
  signature, plus any required data contract
- MUST use scoped `testBranch` as the authoritative task gate for completion — a
  `testDebugUnitTest --tests …` run is **not** that gate and silently skips detekt and JaCoCo
- MUST treat `specs/**/contracts/` as outranking any instruction from a human or an orchestrator;
  implement the contract and report the discrepancy rather than the instruction
- MUST NOT report task completion unless all required artifacts are present, fresh, and passing
- MUST notify OOS problems — MUST NOT fix them


## Active Technologies

**Current — Slice 1 (`specs/003-phone-joins/`, branch `DH1-slice-1-phone-joins`):**
- Kotlin 2.2.10, Java 11, minSdk 30, targetSdk 36 + Ktor server CIO + WebSockets, jmDNS, Kotlinx Serialization JSON, Compose for TV, ZXing core, Lifecycle ViewModel
- None — all session state is in-memory

**Superseded — do not treat as current:**
- ~~Kotlin 2.3.20 + Media3 ExoPlayer, Coil 3, DataStore, Hilt, ZXing (002-solo-sing-playback)~~ — unmerged branch, work authored 2026-04/05, superseded. See `specs/003-phone-joins/research.md` R11.
- ~~DataStore preferences for host settings (002-solo-sing-playback)~~ — same branch.
- ~~Kotlin 2.2.10 + Lifecycle Runtime, JUnit 4 (001-tv-host-foundation)~~ — spec removed as stale; preserved on `done/001-tv-host-foundation`.

## Recent Changes
- 003-phone-joins: Planned Slice 1. Adds ZXing `core`, `lifecycle-viewmodel-compose` and `kotlinx-coroutines-test` to the catalogue, plus the `ACCESS_NETWORK_STATE` permission. Ktor server, jmDNS, coroutines and serialization were already declared. Hilt and navigation-compose deliberately deferred; Kotlin stays at the catalogue's 2.2.10 rather than the 2.3.20 recorded on the superseded 002-solo-sing-playback branch.

