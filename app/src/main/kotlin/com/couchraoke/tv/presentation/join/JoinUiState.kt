package com.couchraoke.tv.presentation.join

/**
 * Presentation state for the join surface (T040, contracts/domain-api.md). `connectedCount` is
 * derived from [com.couchraoke.tv.domain.session.SessionCoordinator.connectedDevices]`.size` --
 * the live connections, never
 * [com.couchraoke.tv.domain.session.model.SessionSnapshot.roster]`.size` -- so a
 * retained-but-disconnected roster entry never keeps counting (FR-025, SC-007).
 *
 * `contracts/domain-api.md` also lists a fourth field, `startFailure: SessionStartFailure?`.
 * That type is domain code owned by T060 (Phase 6, out of scope for this unit per the
 * orchestrator's boundary), so it is deliberately omitted here rather than forward-declared in
 * `domain/`. T060 is expected to add it to this class alongside `SessionStartFailure` itself;
 * flagged as an out-of-scope observation in this unit's report.
 */
data class JoinUiState(
    val joinCodeDisplay: String,
    val qrPayload: String,
    val connectedCount: Int,
)
