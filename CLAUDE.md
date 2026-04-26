# Couchraoke TV: Agent Runtime Policy
Mandatory runtime policy for all LLM agents in this repository.
---
## §1 Startup
MUST read the following before any work — in this order:
```
Read .specify/memory/constitution.md
Read CLAUDE.md  ← this file
Read .claude/skills/implementation-orchestration/SKILL.md
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
---

## §3 Skills
Three skills govern specific workflows in this repository. They auto-load based on context.
**If a skill has not auto-loaded when you need it, read it explicitly before proceeding.**
| Skill | Read when you are about to... | Explicit load |
|---|---|---|
| `navigation` | navigate code, look up a symbol, discover files, decide how much to read | `Read $HOME/.claude/skills/navigation/SKILL.md` |
| `gradle-validation` | run any Gradle command, execute tests, check snapshots, claim completion | `Read $HOME/.claude/skills/gradle-validation/SKILL.md` |
| `implementation` | start implementing, writing code | `Read $HOME/.claude/skills/implementation/SKILL.md` |
| `orchestration` | manage tasks, make delegation decisions, manage worktrees/branches | `Read $HOME/.claude/skills/orchestration/SKILL.md` |
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
- MUST use scoped `testBranch` as the authoritative task gate for completion
- MUST NOT report task completion unless all required artifacts are present, fresh, and passing
- MUST notify OOS problems — MUST NOT fix them


## Active Technologies
- Kotlin 2.3.20, Java 11 + AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle ViewModel, Kotlin Coroutines, Kotlinx Serialization JSON, Ktor server/websockets, Media3 ExoPlayer + OkHttp datasource, Coil 3, DataStore Preferences, Hilt, jmDNS, ZXing, JUnit/Robolectric/AndroidX test (002-solo-sing-playback)
- DataStore preferences for host settings; no persistent storage for remote song assets (002-solo-sing-playback)
- Kotlin 2.2.10, Java 11 + AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle Runtime, Kotlin Coroutines, Kotlinx Serialization JSON, JUnit 4, quality-conventions `testBranch` (001-tv-host-foundation)
- N/A for Phase 0 core logic; fixture files only at test harness boundary (001-tv-host-foundation)

## Recent Changes
- 002-solo-sing-playback: Added Kotlin 2.3.20, Java 11 + AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle ViewModel, Kotlin Coroutines, Kotlinx Serialization JSON, Ktor server/websockets, Media3 ExoPlayer + OkHttp datasource, Coil 3, DataStore Preferences, Hilt, jmDNS, ZXing, JUnit/Robolectric/AndroidX test
