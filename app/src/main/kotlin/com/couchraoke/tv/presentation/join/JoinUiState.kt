package com.couchraoke.tv.presentation.join

import com.couchraoke.tv.domain.session.SessionStartFailure

/**
 * Presentation state for the join surface (T040, contracts/domain-api.md). `connectedCount` is
 * derived from [com.couchraoke.tv.domain.session.SessionCoordinator.connectedDevices]`.size` --
 * the live connections, never
 * [com.couchraoke.tv.domain.session.model.SessionSnapshot.roster]`.size` -- so a
 * retained-but-disconnected roster entry never keeps counting (FR-025, SC-007).
 *
 * `startFailure` (T060, FR-028/SC-008) is the FR-028 blocking notice: non-null when the session
 * failed to start, naming which of [SessionStartFailure]'s three cases occurred. It is
 * presentation-only state -- clearing it via [JoinViewModel.onStartFailureAcknowledged] never
 * touches the game phase or asks [com.couchraoke.tv.domain.session.GamePhaseMachine] for a
 * transition, since F22 has no `Open -> Error` edge to recover from.
 */
data class JoinUiState(
    val joinCodeDisplay: String,
    val qrPayload: String,
    val connectedCount: Int,
    val startFailure: SessionStartFailure? = null,
)
