package com.couchraoke.tv.data.platform

import android.net.wifi.WifiManager
import com.couchraoke.tv.domain.platform.MulticastLease

private const val LOCK_TAG = "com.couchraoke.tv.session"

/**
 * [WifiManager]-backed adapter for [MulticastLease] (contracts/ports.md, research.md R5).
 *
 * `WifiManager.MulticastLock` is reference-counted and throws if `release()` is called
 * without a matching outstanding `acquire()`. [held] tracks whether this lease currently
 * holds the underlying lock so both [acquire] and [release] are idempotent — a double
 * release during teardown cannot throw. This is state-presence handling, the same kind
 * of guard as the null checks elsewhere in these adapters, not a session decision.
 */
class WifiMulticastLease(wifiManager: WifiManager) : MulticastLease {

    private val lock = wifiManager.createMulticastLock(LOCK_TAG)
    private var held = false

    @Synchronized
    override fun acquire() {
        if (held) return
        lock.acquire()
        held = true
    }

    @Synchronized
    override fun release() {
        if (!held) return
        lock.release()
        held = false
    }
}
