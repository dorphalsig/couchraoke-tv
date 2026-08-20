package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.AssetPort
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.RosterEntry

/**
 * The durable session membership list, keyed by [DeviceId], capacity 10 by default
 * (data-model.md).
 *
 * T034 implements the admit path (new devices only -- a repeat `deviceId` is T057's
 * reclaim branch) and the `connected`/`entries` derivations. T051 adds `admit`'s
 * capacity check (FR-015): a previously-unseen `deviceId` is refused with `AtCapacity`
 * once `size == capacity`; a known `deviceId` never is, because that check only runs on
 * the new-entry branch below, after the reclaim branch's `TODO` would already have been
 * reached. T042 implements `detach`'s FR-023 retention: the entry survives with its
 * connection cleared, keeping its capacity slot. `detach` does not yet check whether the
 * given [ConnectionId] is still the entry's *active* one before clearing it (FR-022) --
 * that guard is unreachable before T057, since without reclaim a device can never hold
 * more than one `ConnectionId` in its lifetime, so there is nothing yet for a stale
 * connectionId to be stale against. T057 adds that guard alongside reclaim.
 */
class SessionRoster(private val capacity: Int = 10) {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    private val byDevice = LinkedHashMap<DeviceId, RosterEntry>()

    val entries: List<RosterEntry> get() = byDevice.values.toList()
    val size: Int get() = entries.size
    val connected: List<RosterEntry> get() = entries.filter { it.connection != null }

    fun admit(
        deviceId: DeviceId,
        displayName: String,
        appVersion: String,
        assetPort: AssetPort,
        connectionId: ConnectionId,
    ): RosterAdmission {
        val existing = byDevice[deviceId]
        if (existing != null) {
            TODO(
                "SessionRoster.admit's reclaim branch is completed by T057: deviceId=$deviceId " +
                    "existing=$existing connectionId=$connectionId capacity=$capacity",
            )
        }
        if (size == capacity) {
            return RosterAdmission.AtCapacity
        }
        val entry = RosterEntry(
            deviceId = deviceId,
            displayName = displayName,
            appVersion = appVersion,
            assetPort = assetPort,
            connection = connectionId,
        )
        byDevice[deviceId] = entry
        return RosterAdmission.Admitted(entry)
    }

    /**
     * `connectionId` is currently unused: FR-022's guard (a no-op unless it is still the
     * entry's *active* connection) is provably unreachable before T057 adds reclaim -- a
     * device can only ever hold one `ConnectionId` in its lifetime without it, so there is
     * nothing yet for a stale value to be stale against. `@Suppress`ed rather than dropped
     * because the contract's binding signature (contracts/domain-api.md) requires it now, and
     * T057 completes the guard using it.
     */
    @Suppress("UnusedParameter")
    fun detach(deviceId: DeviceId, connectionId: ConnectionId): Boolean {
        val entry = byDevice[deviceId] ?: return false
        byDevice[deviceId] = entry.copy(connection = null)
        return true
    }

    fun release(deviceId: DeviceId): Unit = TODO("SessionRoster.release is completed by T057: deviceId=$deviceId")

    fun releaseDisconnected(): Unit = TODO("SessionRoster.releaseDisconnected is completed by T057")

    /**
     * No task in tasks.md owns `clear`'s behaviour — flagged as an out-of-scope
     * observation in this unit's report. Left as a `TODO` rather than a guessed
     * implementation.
     */
    fun clear(): Unit = TODO("SessionRoster.clear has no owning task in tasks.md; see this unit's spec-gap report")
}
