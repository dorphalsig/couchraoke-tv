package com.couchraoke.tv.domain.session

import kotlinx.coroutines.flow.SharedFlow

/**
 * Read-only view of the session needed by the presentation layer.
 * Implemented by [Session]; test doubles implement this interface directly.
 */
interface ISession {
    val sessionId: String
    val state: SessionState
    val events: SharedFlow<SessionEvent>
    val connectedClientIds: Set<String>
    val displayNames: Map<String, String>
}
