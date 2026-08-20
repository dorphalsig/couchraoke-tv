package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.session.model.ConnectionId

/**
 * The outcome of [com.couchraoke.tv.domain.session.SessionCoordinator.authorize] and
 * `.admit` (contracts/domain-api.md).
 *
 * **Spec gap**: no shape for `AdmissionDecision` is normative anywhere in
 * `specs/003-phone-joins/contracts/`. This shape is inferred from `authorize`/`admit`
 * returning it and from T050's refusal vocabulary ("reject … with `invalid_token`",
 * "refusal branches for `protocol_mismatch`, `invalid_message` and `session_full`"):
 * `Admitted` carries only the freshly allocated [ConnectionId]; `Refused` carries the
 * [RefusalReason] plus the human-readable message that becomes the `error` frame's
 * `message` field. The wire-level `sessionState` reply (T035 calls it "the sessionState
 * reply with connectionId set") is deliberately **not** built here — that would mix a wire
 * DTO into a pure domain type. Building it from `Admitted.connectionId` plus
 * `SessionCoordinator.snapshot` is left to the transport-facing caller. Reported as an
 * open spec gap in this unit's report; not fixed here per scope.
 *
 * T049 places this file "alongside" `HandshakeValidator`; T035 (accept) and T050 (refusal
 * branches) own the code that produces each case.
 */
sealed interface AdmissionDecision {
    data class Admitted(val connectionId: ConnectionId) : AdmissionDecision
    data class Refused(val reason: RefusalReason, val message: String) : AdmissionDecision
}
