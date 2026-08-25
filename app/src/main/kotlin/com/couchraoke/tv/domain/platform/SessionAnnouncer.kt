package com.couchraoke.tv.domain.platform

import java.net.Inet4Address

/**
 * Publishes and withdraws the mDNS/DNS-SD session announcement. Implemented by
 * `com.couchraoke.tv.data.discovery.JmdnsSessionAnnouncer` (contracts/ports.md).
 *
 * Service type `_karaoke._tcp.` and the TXT keys `code` and `v` are the adapter's
 * concern; this port passes values, not encodings. [Inet4Address] is `java.net`, not
 * Android, so the domain stays JVM-pure.
 */
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

/**
 * A live announcement. [registeredInstanceName] exists because jmDNS renames on LAN
 * collision — FR-004 requires the instance name to be `KaraokeTV-<noun>`, so the caller
 * compares the readback against what it asked for and treats a mismatch as a
 * session-start failure rather than accepting a silent rename.
 */
interface AnnouncementHandle {
    val registeredInstanceName: String
}
