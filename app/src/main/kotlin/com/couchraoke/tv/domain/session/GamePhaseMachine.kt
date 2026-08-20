package com.couchraoke.tv.domain.session

/**
 * The nine-state playback machine's transition rules. The allow-list below matches
 * `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json` exactly — see
 * [GamePhaseMachineFixtureTest] in the test sources, which drives every fixture entry
 * against this class rather than restating the table.
 *
 * There is deliberately no `Open -> Error` edge (FR-028): a session-start failure is a
 * blocking modal, not a phase change.
 */
class GamePhaseMachine(initial: GamePhase = GamePhase.Open) {

    var current: GamePhase = initial
        private set

    fun canTransition(from: GamePhase, to: GamePhase): Boolean =
        Pair(from, to) in ALLOWED_TRANSITIONS

    /**
     * Never throws. A rejected transition leaves [current] untouched (FR-026).
     */
    fun transition(to: GamePhase): PhaseTransitionResult {
        val from = current
        if (!canTransition(from, to)) {
            return PhaseTransitionResult.Rejected(from, to)
        }
        current = to
        return PhaseTransitionResult.Accepted(from, to)
    }

    private companion object {
        val ALLOWED_TRANSITIONS: Set<Pair<GamePhase, GamePhase>> = setOf(
            Pair(GamePhase.Open, GamePhase.Preparing),
            Pair(GamePhase.Preparing, GamePhase.Countdown),
            Pair(GamePhase.Preparing, GamePhase.Live),
            Pair(GamePhase.Preparing, GamePhase.Error),
            Pair(GamePhase.Countdown, GamePhase.Live),
            Pair(GamePhase.Countdown, GamePhase.Open),
            Pair(GamePhase.Countdown, GamePhase.Error),
            Pair(GamePhase.Live, GamePhase.Error),
            Pair(GamePhase.Error, GamePhase.Open),
            Pair(GamePhase.Live, GamePhase.Paused),
            Pair(GamePhase.Live, GamePhase.DisconnectPaused),
            Pair(GamePhase.Live, GamePhase.Stopped),
            Pair(GamePhase.Paused, GamePhase.Live),
            Pair(GamePhase.Paused, GamePhase.Preparing),
            Pair(GamePhase.Paused, GamePhase.Open),
            Pair(GamePhase.DisconnectPaused, GamePhase.Live),
            Pair(GamePhase.DisconnectPaused, GamePhase.Open),
            Pair(GamePhase.Stopped, GamePhase.Results),
            Pair(GamePhase.Results, GamePhase.Open),
        )
    }
}
