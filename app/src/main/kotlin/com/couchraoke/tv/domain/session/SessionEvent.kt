package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId

/**
 * Emitted on [SessionCoordinator.events] as roster membership changes (FR-019,
 * contracts/domain-api.md). `Reconnected.previous` is the [ConnectionId] the device held
 * before this reconnect superseded it.
 *
 * Nothing emits these yet in this phase: T035 emits `Connected`, T042 emits
 * `Disconnected`, and T058 emits `Reconnected`.
 */
sealed interface SessionEvent {
    data class Connected(val deviceId: DeviceId, val connectionId: ConnectionId) : SessionEvent
    data class Disconnected(val deviceId: DeviceId, val connectionId: ConnectionId) : SessionEvent
    data class Reconnected(
        val deviceId: DeviceId,
        val connectionId: ConnectionId,
        val previous: ConnectionId,
    ) : SessionEvent
}
