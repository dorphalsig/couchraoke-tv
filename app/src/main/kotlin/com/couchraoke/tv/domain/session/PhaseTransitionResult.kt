package com.couchraoke.tv.domain.session

/**
 * The outcome of a requested [GamePhase] transition. `Rejected` leaves the machine's
 * current phase unchanged (FR-026).
 */
sealed interface PhaseTransitionResult {
    data class Accepted(val from: GamePhase, val to: GamePhase) : PhaseTransitionResult
    data class Rejected(val from: GamePhase, val to: GamePhase) : PhaseTransitionResult
}
