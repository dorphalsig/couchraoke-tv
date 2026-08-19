# Contract: Domain-declared ports

**Feature**: `002-phone-joins` | **Date**: 2026-08-19

Four ports separate framework from logic. Each is declared in `domain`, implemented in `data`, and constructed at the composition root. No adapter holds a decision; every branch beyond null handling belongs on the domain side.

Per Constitution Principle V, each boundary is pinned below as fully-qualified name, method, and full signature. These signatures are binding on implementation.

---

## ControlTransport

**Port**: `com.couchraoke.tv.domain.control.ControlTransport`
**Adapter**: `com.couchraoke.tv.data.control.KtorControlTransport`

```kotlin
package com.couchraoke.tv.domain.control

interface ControlTransport {
    suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport
    suspend fun stop()
}

interface StartedTransport {
    val boundPort: Int
}

interface ControlConnectionHandler {
    suspend fun onConnection(connection: ControlConnection)
}

interface ControlConnection {
    val token: String?
    suspend fun receiveText(): String?
    suspend fun sendText(text: String)
    suspend fun refuse(code: String, message: String)
    suspend fun close()
}
```

`boundPort` is the **actual** bound port, not the requested one. Passing `0` yields an ephemeral port, which is how the loopback gate runs tests concurrently without collisions, and it is the value that must be advertised (FR-004) rather than the configured default.

`token` is the `token` query parameter, or `null` when absent. The transport does not validate it — `JoinCodeMatcher` does.

`refuse(code, message)` is one operation rather than a send followed by a close, because FR-016 requires the reason to arrive **before** the close and the two must not be separable. The adapter sends the `error` frame, flushes, then closes with `1008` and close reason `code`. Splitting this into two calls is what produces the peer's exit 4.

`receiveText()` returns `null` when the peer closes.

---

## SessionAnnouncer

**Port**: `com.couchraoke.tv.domain.platform.SessionAnnouncer`
**Adapter**: `com.couchraoke.tv.data.discovery.JmdnsSessionAnnouncer`

```kotlin
package com.couchraoke.tv.domain.platform

import java.net.Inet4Address

interface SessionAnnouncer {
    suspend fun publish(
        address: Inet4Address,
        instanceName: String,
        port: Int,
        joinCode: String,
        protocolVersion: Int,
    ): AnnouncementHandle

    suspend fun withdraw(handle: AnnouncementHandle)
}

interface AnnouncementHandle {
    val registeredInstanceName: String
}
```

Service type `_karaoke._tcp.` and the TXT keys `code` and `v` are the adapter's concern; the port passes values, not encodings.

`registeredInstanceName` exists because jmDNS renames on LAN collision. FR-004 requires the instance name to be `KaraokeTV-<noun>`, so the caller compares the readback against what it asked for and treats a mismatch as a session-start failure rather than accepting a silent rename that would desynchronise the announcement from the join code.

`Inet4Address` is `java.net`, not Android, so the domain stays JVM-pure.

---

## LocalAddressProvider

**Port**: `com.couchraoke.tv.domain.platform.LocalAddressProvider`
**Adapter**: `com.couchraoke.tv.data.platform.ConnectivityLocalAddressProvider`

```kotlin
package com.couchraoke.tv.domain.platform

import java.net.Inet4Address

fun interface LocalAddressProvider {
    fun activeLocalIpv4(): Inet4Address?
}
```

Returns the single IPv4 address of the network the TV is currently using for local traffic, or `null` if there is none usable (FR-008). Never a list. `null` is a session-start failure via the FR-028 path.

The adapter resolves it through `ConnectivityManager.activeNetwork` → `linkProperties`, excluding loopback and link-local. That requires `ACCESS_NETWORK_STATE`, which this feature adds to the manifest.

---

## MulticastLease

**Port**: `com.couchraoke.tv.domain.platform.MulticastLease`
**Adapter**: `com.couchraoke.tv.data.platform.WifiMulticastLease`

```kotlin
package com.couchraoke.tv.domain.platform

interface MulticastLease {
    fun acquire()
    fun release()
}
```

Acquired before the announcement is published and released when the session ends (FR-005). Both calls are idempotent, so a double release during teardown cannot throw.

The lease is **session-scoped, not activity-scoped**. `MainActivity` currently ties the multicast lock to `onStart`/`onStop`, which would drop discovery whenever the user switches inputs while the session is still live. Phase D moves ownership to the session.

---

## Composition root

**Class**: `com.couchraoke.tv.di.SessionComponent`

```kotlin
package com.couchraoke.tv.di

class SessionComponent(
    private val transport: ControlTransport,
    private val announcer: SessionAnnouncer,
    private val addressProvider: LocalAddressProvider,
    private val multicastLease: MulticastLease,
    private val joinCodeGenerator: JoinCodeGenerator,
    private val clock: () -> Long,
) {
    fun createCoordinator(): SessionCoordinator
}
```

Manual construction, no DI framework. Every adapter is injected, so a JVM test can build a coordinator with test doubles for the *ports* — which is legitimate, since the ports are not the transport. FR-039 bars a fake from proving that the transport works; that claim is proved only by the loopback gate against the real peer.
