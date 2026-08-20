package com.couchraoke.tv.domain.session.model

/**
 * A durable session-membership record, keyed by [deviceId] (data-model.md). Survives its
 * connection ending — [connection] becomes `null` while the entry keeps its capacity slot
 * (FR-023).
 *
 * Declared now with its full binding shape so [com.couchraoke.tv.domain.session.SessionRoster]
 * and [com.couchraoke.tv.domain.session.SessionCoordinator] compile; T034 (admit), T051
 * (capacity) and T057 (reclaim/detach) own the behaviour that creates and mutates entries.
 */
data class RosterEntry(
    val deviceId: DeviceId,
    val displayName: String,
    val appVersion: String,
    val assetPort: AssetPort,
    val connection: ConnectionId?,
)
