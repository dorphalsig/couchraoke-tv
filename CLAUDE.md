# tv Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-03-14

## Active Technologies
- Kotlin 2.3.10 / Java 11 + Kotlin standard library, kotlinx-serialization-json (test support only), UsdxParser + FileResolver (domain.parser — feature 001), BeatTimingEngine formula (domain.timing — feature 002) (004-song-library)
- In-memory only (LinkedHashMap); no persistence (004-song-library)
- Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, JmDNS 3.6.3, kotlinx-serialization-json, OkHttp (transitive via media3/coil), `java.net.DatagramSocket` (stdlib) (005-network-protocol)
- In-memory only (`ConnectionRegistry`, `SongLibrary`) — no persistence (005-network-protocol)

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
- 005-network-protocol: Added Kotlin 2.x / Java 11 (Android minSdk 28, compileSdk 36) + Ktor server CIO 3.4.1, ktor-server-websockets 3.4.1, JmDNS 3.6.3, kotlinx-serialization-json, OkHttp (transitive via media3/coil), `java.net.DatagramSocket` (stdlib)
- 004-song-library: Added Kotlin 2.3.10 / Java 11 + Kotlin standard library, kotlinx-serialization-json (test support only), UsdxParser + FileResolver (domain.parser — feature 001), BeatTimingEngine formula (domain.timing — feature 002)

- 001-usdx-parser: Added Kotlin 2.3.10 on Java 11 + Kotlin standard library, kotlinx-serialization-json (test/support use only if needed), existing Gradle/JUnit stack

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
