package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId

/**
 * Emitted on [SessionCoordinator.events] as roster membership changes (FR-019,
 * contracts/domain-api.md). `Reconnected.previous` is the [ConnectionId] the device held
 * before this reconnect, or `null` if no live connection existed to supersede -- the
 * ordinary case of a phone dropping and later returning (FR-021, FR-023). A non-null
 * `previous` means a still-live connection was displaced (FR-022). See spec.md
 * Observation 22.
 */
sealed interface SessionEvent {
    data class Connected(val deviceId: DeviceId, val connectionId: ConnectionId) : SessionEvent
    data class Disconnected(val deviceId: DeviceId, val connectionId: ConnectionId) : SessionEvent
    data class Reconnected(
        val deviceId: DeviceId,
        val connectionId: ConnectionId,
        val previous: ConnectionId?,
    ) : SessionEvent
}
