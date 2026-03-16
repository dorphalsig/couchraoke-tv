# Research: Network Protocol (005)

## Technology Decisions

### WebSocket Server
**Decision**: Ktor server CIO with `ktor-server-websockets` (already declared in `build.gradle.kts`)
**Rationale**: Constitution-mandated. Already in the classpath at 3.4.1.
**Alternatives considered**: Netty, raw Java NIO — both violate the approved stack.

### JSON Serialization
**Decision**: `kotlinx-serialization-json` with `@Serializable` data classes; `Json { ignoreUnknownKeys = true }` instance
**Rationale**: Constitution-mandated. Required for forward-compat — unknown message types must be ignored with a warning, not crash.
**Alternatives considered**: Gson, Moshi — forbidden by constitution (reflection-based).

### Manifest HTTP Fetch
**Decision**: OkHttp (already transitive via `media3-datasource-okhttp` and `coil-network-okhttp`). Add explicit `implementation` declaration using the transitive version.
**Rationale**: Avoids new dep; already on classpath; ergonomic for synchronous LAN call. Ktor client would require an additional artifact.
**Alternatives considered**: `HttpURLConnection` (zero dep but verbose), Ktor client CIO (new dep).

### UDP Pitch Receiver
**Decision**: `java.net.DatagramSocket` — standard JVM, no new dependency.
**Rationale**: Fixed 16-byte datagrams with no batching; standard socket is sufficient. Android API 28+ supports it without restriction.
**Alternatives considered**: NIO `DatagramChannel` — overkill for a single fixed-size frame loop.

### mDNS
**Decision**: JmDNS 3.6.3 (declared; constitution specifies 3.5.9 minimum — 3.6.3 is compatible and newer)
**Rationale**: Constitution-mandated. NSD Manager on Android TV has known OEM firmware issues.
**Alternatives considered**: Android NSD Manager — explicitly rejected by constitution.

### Cleartext HTTP (RFC-1918)
**Decision**: `res/xml/network_security_config.xml` with `<domain-config cleartextTrafficPermitted="true">` for RFC-1918 address ranges.
**Rationale**: Required by Android API 28+ for HTTP to phone IPs. Spec §8.7.3.1 provides exact XML.
**Alternatives considered**: `<base-config cleartextTrafficPermitted="true">` — simpler but overly broad; the spec recommends the domain-config approach for MVP.

### Session State Interface
**Decision**: Define a minimal `ISessionGate` interface in `domain/session/` that feature 006 will implement. Feature 005 depends on this interface, not a concrete class.
**Fields required by 005**: `isLocked: Boolean`, `registerConnection(clientId, ...) : UShort`, `deregisterConnection(clientId)`, `getConnectionId(clientId): UShort?`
**Rationale**: Keeps network layer stateless about session lifecycle. Feature 006 owns all state transitions.

### OkHttp version in catalog
**Decision**: Add `okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "..." }` using the version already pulled transitively (check via `./gradlew dependencies`). Or use the existing transitive dep without re-declaring if it resolves cleanly.
**Note**: Implementation task should verify exact version at dependency resolution time.
