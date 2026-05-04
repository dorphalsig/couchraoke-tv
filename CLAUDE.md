# CouchRaoke TV: Agent Runtime Policy
Mandatory for all agents in this repo.

## Startup
Before any work, read in order:
1. `.specify/memory/constitution.md`
2. `CLAUDE.md`
Then load `orchestration`.

## Skill loading
Load the governing skill before acting:
| Action | Skill |
|---|---|
| manage tasks, phases, delegation, branches, worktrees | `orchestration` |
| edit code or tests | `implementation` |
| navigate files, symbols, references, call paths | `navigation` |
| run Gradle, tests, screenshots, or completion validation | `gradle-validation` |
If multiple apply, load all.

## Invariants
- Stay within declared task scope.
- Do not edit out-of-scope issues; report them only.
- Do not edit `quality-conventions/` without explicit approval.
- Do not navigate into third-party source; use docs/search tools.
- Route dependency changes through `gradle/libs.versions.toml`.
- Keep business logic out of UI.
- Use ViewModels as the UI state source.
- Do not claim completion unless required validation is fresh and green.
- Use `context7` to obtain up to date API info. If there are no results use `tavily search`
- Use `tavily search` to search the web, for example when you find an error and need to find how to solve it
- Use `Brave Search`as a backup of `tavily search`
- Loaded skill workflows are mandatory and override task-level autonomy wording, unless the user's is more restrictive, or the user explicitly states that the instruction overrides the workflows

## Active Technologies
- Kotlin 2.2.10, Java 11 + AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle Runtime, Kotlin Coroutines, Kotlinx Serialization JSON, JUnit 4, quality-conventions `testBranch` (001-tv-host-foundation)
- N/A for Phase 0 core logic; fixture files only at test harness boundary (001-tv-host-foundation)
- Kotlin 2.2.10, Java 11 + AndroidX Core/AppCompat, Jetpack Compose + Compose for TV, Lifecycle Runtime, Kotlin Coroutines, Kotlinx Serialization JSON, Ktor server/websockets/client, jmDNS, LibVLC, Coil, JUnit 4, quality-conventions `testBranch` (002-solo-sing-playback)
- DataStore preferences only if needed for existing settings defaults; no persistence for remote song assets; manifest/chart/audio/video assets are streamed from phone-provided LAN URLs (002-solo-sing-playback)

## Recent Changes
- 002-solo-sing-playback: Planned Iteration 1 solo-sing playback with Ktor LAN session, jmDNS discovery, LibVLC streaming, static note lanes, and scoring/results out of scope