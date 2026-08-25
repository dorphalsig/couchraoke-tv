package com.couchraoke.tv.domain.platform

/**
 * Acquires and releases the multicast lock required for mDNS announcement/discovery.
 * Implemented by `com.couchraoke.tv.data.platform.WifiMulticastLease` (contracts/ports.md).
 *
 * Acquired before the announcement is published and released when the session ends
 * (FR-005). Both calls are idempotent, so a double release during teardown cannot throw.
 *
 * The lease is session-scoped, not activity-scoped: `MainActivity` currently ties the
 * multicast lock to `onStart`/`onStop`, which would drop discovery whenever the user
 * switches inputs while the session is still live. Phase D moves ownership to the
 * session.
 */
interface MulticastLease {
    fun acquire()
    fun release()
}
