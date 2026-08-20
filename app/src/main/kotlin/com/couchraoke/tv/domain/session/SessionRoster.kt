package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.AssetPort
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.RosterEntry

/**
 * The durable session membership list, keyed by [DeviceId], capacity 10 by default
 * (data-model.md).
 *
 * Forward-declared here with its binding signature (contracts/domain-api.md) so
 * [SessionCoordinator] can be constructed ahead of the tasks that complete each behaviour:
 * T034 implements the admit path and the `connected` derivation, T051 adds capacity, and
 * T057 completes reclaim, `detach`'s identity guard, `release` and `releaseDisconnected`.
 */
class SessionRoster(private val capacity: Int = 10) {

    init {
        require(capacity > 0) { "capacity must be positive, was $capacity" }
    }

    val entries: List<RosterEntry> = emptyList()
    val size: Int get() = entries.size
    val connected: List<RosterEntry> = emptyList()

    fun admit(
        deviceId: DeviceId,
        displayName: String,
        appVersion: String,
        assetPort: AssetPort,
        connectionId: ConnectionId,
    ): RosterAdmission = TODO(
        "SessionRoster.admit is completed by T034/T051/T057: deviceId=$deviceId " +
            "displayName=$displayName appVersion=$appVersion assetPort=$assetPort " +
            "connectionId=$connectionId capacity=$capacity",
    )

    fun detach(deviceId: DeviceId, connectionId: ConnectionId): Boolean = TODO(
        "SessionRoster.detach's identity guard is completed by T057: deviceId=$deviceId connectionId=$connectionId",
    )

    fun release(deviceId: DeviceId): Unit = TODO("SessionRoster.release is completed by T057: deviceId=$deviceId")

    fun releaseDisconnected(): Unit = TODO("SessionRoster.releaseDisconnected is completed by T057")

    /**
     * No task in tasks.md owns `clear`'s behaviour — flagged as an out-of-scope
     * observation in this unit's report. Left as a `TODO` rather than a guessed
     * implementation.
     */
    fun clear(): Unit = TODO("SessionRoster.clear has no owning task in tasks.md; see this unit's spec-gap report")
}
