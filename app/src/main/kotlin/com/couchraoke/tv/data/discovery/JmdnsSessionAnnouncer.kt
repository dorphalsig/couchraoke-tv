package com.couchraoke.tv.data.discovery

import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

private const val SERVICE_TYPE = "_karaoke._tcp.local."

/**
 * jmDNS adapter for [SessionAnnouncer] (contracts/ports.md, research.md R4).
 *
 * Binds jmDNS to the [address] the caller supplies — the same address
 * `ConnectivityLocalAddressProvider` resolved for the QR/announcement — rather than a
 * wildcard, so the two can never disagree. Reads the registered instance name back into
 * [AnnouncementHandle.registeredInstanceName] because jmDNS silently renames on LAN
 * collision; whether a mismatch is a session-start failure is the caller's decision
 * (FR-004), not this adapter's.
 */
class JmdnsSessionAnnouncer : SessionAnnouncer {

    override suspend fun publish(
        address: Inet4Address,
        instanceName: String,
        port: Int,
        joinCode: String,
        protocolVersion: Int,
    ): AnnouncementHandle = withContext(Dispatchers.IO) {
        val jmdns = JmDNS.create(address, instanceName)
        val serviceInfo = ServiceInfo.create(
            SERVICE_TYPE,
            instanceName,
            port,
            0,
            0,
            mapOf("code" to joinCode, "v" to protocolVersion.toString()),
        )
        jmdns.registerService(serviceInfo)
        JmdnsAnnouncementHandle(jmdns, serviceInfo)
    }

    override suspend fun withdraw(handle: AnnouncementHandle) {
        if (handle !is JmdnsAnnouncementHandle) return
        withContext(Dispatchers.IO) {
            handle.jmdns.unregisterService(handle.serviceInfo)
            handle.jmdns.close()
        }
    }

    private class JmdnsAnnouncementHandle(
        val jmdns: JmDNS,
        val serviceInfo: ServiceInfo,
    ) : AnnouncementHandle {
        override val registeredInstanceName: String = serviceInfo.name
    }
}
