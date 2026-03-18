package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.session.ISession
import com.couchraoke.tv.domain.session.SessionEvent
import com.couchraoke.tv.domain.session.SessionState
import com.couchraoke.tv.domain.session.SessionToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeSession(
    token: String = SessionToken.generate(),
    override val connectedClientIds: Set<String> = emptySet(),
    override val displayNames: Map<String, String> = emptyMap(),
) : ISession {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<SessionEvent> = _events.asSharedFlow()
    override val sessionId: String = token
    override val state: SessionState = SessionState.Open

    suspend fun emit(event: SessionEvent) {
        _events.emit(event)
    }
}
