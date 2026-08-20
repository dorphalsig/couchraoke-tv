package com.couchraoke.tv.domain.session.model

/**
 * The projection of a [RosterEntry] that currently holds a live connection — data-model.md's
 * "`connected` is the subset of `roster` with a live connection." Unlike `RosterEntry`,
 * [connectionId] is never null: an entry is only projected into this shape while one is live.
 *
 * Not restated in any task of `tasks.md`; declared here because
 * [com.couchraoke.tv.domain.session.SessionCoordinator]'s binding surface
 * (contracts/domain-api.md) requires `StateFlow<List<ConnectedDevice>>` to compile. Flagged as
 * an out-of-scope observation in this unit's report.
 */
data class ConnectedDevice(
    val deviceId: DeviceId,
    val displayName: String,
    val appVersion: String,
    val assetPort: AssetPort,
    val connectionId: ConnectionId,
)
