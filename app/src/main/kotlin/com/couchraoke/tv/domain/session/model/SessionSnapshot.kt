package com.couchraoke.tv.domain.session.model

import com.couchraoke.tv.domain.session.GamePhase
import com.couchraoke.tv.domain.session.SessionLifecycle

/**
 * The coordinator's observable session state (data-model.md). `roster` is ordered by
 * admission, size at most 10; `connected` is the subset with a live connection.
 *
 * Not restated in any task of `tasks.md`; declared here because
 * [com.couchraoke.tv.domain.session.SessionCoordinator]'s binding surface
 * (contracts/domain-api.md) requires `StateFlow<SessionSnapshot>` to compile. Flagged as an
 * out-of-scope observation in this unit's report.
 */
data class SessionSnapshot(
    val sessionId: SessionId,
    val joinCode: JoinCode,
    val lifecycle: SessionLifecycle,
    val phase: GamePhase,
    val roster: List<RosterEntry>,
    val connected: List<ConnectedDevice>,
)
