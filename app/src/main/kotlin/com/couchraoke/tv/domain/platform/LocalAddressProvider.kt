package com.couchraoke.tv.domain.platform

import java.net.Inet4Address

/**
 * Resolves the single IPv4 address of the network the TV is currently using for local
 * traffic. Implemented by `com.couchraoke.tv.data.platform.ConnectivityLocalAddressProvider`
 * (contracts/ports.md).
 *
 * Returns `null` if there is none usable (FR-008) — never a list. `null` is a
 * session-start failure via the FR-028 path.
 */
fun interface LocalAddressProvider {
    fun activeLocalIpv4(): Inet4Address?
}
