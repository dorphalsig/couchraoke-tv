package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.RosterEntry

/**
 * The outcome of [SessionRoster.admit] (contracts/domain-api.md). `Reclaimed` carries the
 * device's previous [ConnectionId] (or `null`) so [SessionCoordinator] can emit
 * [SessionEvent.Reconnected] (FR-019); `AtCapacity` is returned only for a previously-unseen
 * device when the roster is full (FR-015, FR-020, FR-021).
 *
 * T034 owns `Admitted`, T051 owns `AtCapacity`, T057 owns `Reclaimed`.
 */
sealed interface RosterAdmission {
    data class Admitted(val entry: RosterEntry) : RosterAdmission
    data class Reclaimed(val entry: RosterEntry, val previous: ConnectionId?) : RosterAdmission
    data object AtCapacity : RosterAdmission
}
