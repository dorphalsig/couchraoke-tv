package com.couchraoke.tv.data.platform

import android.net.ConnectivityManager
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import java.net.Inet4Address

/**
 * [ConnectivityManager]-backed adapter for [LocalAddressProvider] (contracts/ports.md,
 * research.md R3).
 *
 * Resolves the single IPv4 address of the network the TV is currently using for local
 * traffic via `activeNetwork` -> `linkProperties`, excluding loopback and link-local
 * addresses. Returns `null`, never a list, when there is no such network or no such
 * address — the domain treats `null` as a session-start failure via the FR-028 path.
 */
class ConnectivityLocalAddressProvider(
    private val connectivityManager: ConnectivityManager,
) : LocalAddressProvider {

    override fun activeLocalIpv4(): Inet4Address? =
        connectivityManager.activeNetwork
            ?.let { connectivityManager.getLinkProperties(it) }
            ?.linkAddresses
            ?.asSequence()
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
}
