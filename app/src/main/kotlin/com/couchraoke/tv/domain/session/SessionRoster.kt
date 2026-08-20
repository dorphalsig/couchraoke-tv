package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.AssetPort
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.RosterEntry

/**
 * The durable session membership list, keyed by [DeviceId], capacity 10 by default
 * (data-model.md).
 *
 * T034 implements the admit path for previously-unseen devices and the
 * `connected`/`entries` derivations. T051 adds `admit`'s capacity check (FR-015): a
 * previously-unseen `deviceId` is refused with `AtCapacity` once `size == capacity`; a
 * known `deviceId` never is, because the capacity check only runs on the new-entry
 * branch, after the reclaim branch below has already returned. T042 implements
 * `detach`'s FR-023 retention: the entry survives with its connection cleared, keeping
 * its capacity slot. T057 completes `admit`'s reclaim branch (FR-020, FR-021): a known
 * `deviceId` reuses its entry in place, refreshing `displayName`/`appVersion`/
 * `assetPort` from the new hello and taking a fresh [ConnectionId], regardless of
 * capacity or whether a live connection currently exists. T057 also completes
 * `detach`'s FR-022 guard -- it is a no-op unless the supplied [ConnectionId] is still
 * the entry's *active* one -- and adds `release`/`releaseDisconnected` (FR-024),
 * unreachable in this slice but required so capacity semantics are complete and
 * testable now.
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
        val admission = when {
            existing != null -> {
                val reclaimed = existing.copy(
                    displayName = displayName,
                    appVersion = appVersion,
                    assetPort = assetPort,
                    connection = connectionId,
                )
                byDevice[deviceId] = reclaimed
                RosterAdmission.Reclaimed(reclaimed, existing.connection)
            }
            size == capacity -> RosterAdmission.AtCapacity
            else -> {
                val entry = RosterEntry(
                    deviceId = deviceId,
                    displayName = displayName,
                    appVersion = appVersion,
                    assetPort = assetPort,
                    connection = connectionId,
                )
                byDevice[deviceId] = entry
                RosterAdmission.Admitted(entry)
            }
        }
        return admission
    }

    /**
     * A no-op unless [connectionId] is still the entry's *active* connection (FR-022):
     * this is what stops a superseded connection's late close from evicting the
     * replacement that reclaimed it. On success the entry is retained with
     * `connection = null`, keeping its capacity slot (FR-023).
     */
    fun detach(deviceId: DeviceId, connectionId: ConnectionId): Boolean {
        val entry = byDevice[deviceId]
        if (entry == null || entry.connection != connectionId) {
            return false
        }
        byDevice[deviceId] = entry.copy(connection = null)
        return true
    }

    /** Kick: drops [deviceId]'s entry entirely, freeing its capacity slot. Unreachable this slice. */
    fun release(deviceId: DeviceId) {
        byDevice.remove(deviceId)
    }

    /** Song-end sweep: drops every entry with no live connection (FR-024). Unreachable this slice. */
    fun releaseDisconnected() {
        byDevice.entries.removeAll { it.value.connection == null }
    }

    /**
     * No task in tasks.md owns `clear`'s behaviour — flagged as an out-of-scope
     * observation in this unit's report. Left as a `TODO` rather than a guessed
     * implementation.
     */
    fun clear(): Unit = TODO("SessionRoster.clear has no owning task in tasks.md; see this unit's spec-gap report")
}
