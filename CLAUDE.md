# tv Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-14

## Active Technologies
- Kotlin 2.3.10 / Java 11 + Kotlin standard library, kotlinx-serialization-json (test support only), UsdxParser + FileResolver (domain.parser — feature 001), BeatTimingEngine formula (domain.timing — feature 002) (004-song-library)
- In-memory only (LinkedHashMap); no persistence (004-song-library)
- Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, JmDNS 3.6.3, kotlinx-serialization-json, OkHttp (transitive via media3/coil), `java.net.DatagramSocket` (stdlib) (005-network-protocol)
- In-memory only (`ConnectionRegistry`, `SongLibrary`) — no persistence (005-network-protocol)
- Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + None new — `ISessionGate`, `SessionToken`, `ConnectionRegistry`, `SlotMap`/`SlotInfo` are already on the classpath from features 004–005 (006-session-pairing)
- In-memory only; no persistence (006-session-pairing)
- Kotlin 2.3.10 / Java 11 (Android minSdk 28, compileSdk 36) + Jetpack Compose for TV (`androidx.tv:tv-material` 1.0.0, `tv-foundation` 1.0.0), Compose BOM `2025.05.01`, `lifecycle-viewmodel-compose`, Hilt 2.56.1, `hilt-navigation-compose` 1.2.0, Media3 ExoPlayer 1.9.2 (already on classpath), Coil 3.4.0 (already on classpath), ZXing `zxing-android-embedded` 4.3.0 (already on classpath) (007-song-list-ui)
- In-memory only — `SongLibrary` (session-scoped), `Session` (session-scoped) (007-song-list-ui)

- Kotlin 2.3.10 on Java 11 + Kotlin standard library, kotlinx-serialization-json (test/support use only if needed), existing Gradle/JUnit stack (001-usdx-parser)

## Project Structure

```text
src/
tests/
```

## Commands

# Add commands for Kotlin 2.3.10 on Java 11

## Code Style

Kotlin 2.3.10 on Java 11: Follow standard conventions

## Recent Changes
- 007-song-list-ui: Added Kotlin 2.3.10 / Java 11 (Android minSdk 28, compileSdk 36) + Jetpack Compose for TV (`androidx.tv:tv-material` 1.0.0, `tv-foundation` 1.0.0), Compose BOM `2025.05.01`, `lifecycle-viewmodel-compose`, Hilt 2.56.1, `hilt-navigation-compose` 1.2.0, Media3 ExoPlayer 1.9.2 (already on classpath), Coil 3.4.0 (already on classpath), ZXing `zxing-android-embedded` 4.3.0 (already on classpath)
- 006-session-pairing: Added Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + None new — `ISessionGate`, `SessionToken`, `ConnectionRegistry`, `SlotMap`/`SlotInfo` are already on the classpath from features 004–005
- 005-network-protocol: Added Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, JmDNS 3.6.3, kotlinx-serialization-json, OkHttp (transitive via media3/coil), `java.net.DatagramSocket` (stdlib)


<!-- MANUAL ADDITIONS START -->
## TOOLS
**IMPORTANT:** Avoid full file reads, prefer Serena `get_symbols_overview` and `find_symbol` tools. `search_for_pattern` and `bash` tools should be used as a last resort only. Prefer serena's tools for reading, searching and high precision edits. Use `read_file` and claude code's own `read` sparingly and only when needed.

##AGENTS
- BEFORE sending agents to 
- HAIKU Agents for coding: They are mostly independent and can use tools. They need strict ground rules, like if they find errors / inconsistencies outside of their scope, they should not touch it, but report it back to you. And the agents should be focused on their scope, and are allowed max 1 degree of freedom (follow max 1 link from their scope if needed). This is tolerated but not encouraged. When hitting cooldown on these agents, dont fall back to another model. Stop and let the user know. The agents should be encouraged to follow the tools guideline above. Encourage agents to find solutions and suggest them. They MUST NOT commit. They should create a stash with their changes so you can check them independently.

**Checking Results**
Since agents are not to be trusted too much, you will verify their tasks by:
- Using git to check file level changes
- Use serena's `get_symbols_overview` before popping stash + after to check for new methods that needed to be implemented 
- Use git diff to do spot checks on implementation. Check 3-5 changes randomly to make sure they make sense
- run tests: check pass + coverage
- if all of this passes -> green

**Dealing With Errors**
 - When agents have problems, or find issues: Research briefly their suggestion and assess if it aligns with the project context + consitution. If yes, check if its valid. If you dont get suggestions or they dont apply, research them yourself.
 - **IMPORTANT:** Consider: Is the fix larger than the 1.5*prompt length for the fix? If so -> subagents do it. Else you do it 

## INSTRUCTIONS - EXECUTE WHEN READING
If you have not done so yet:
 - Read constitution.md
 - Read relevant memories from serena. Esp. important look for handovers f the current feature we are working on
 - Always carry out the **IMPORTANT** instructions in this file
<!-- MANUAL ADDITIONS END -->
