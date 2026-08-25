package com.couchraoke.tv.domain.session

/**
 * The nine-state playback machine. Distinct from [SessionLifecycle], which governs
 * whether the session accepts new members — see data-model.md.
 */
enum class GamePhase {
    Open,
    Preparing,
    Countdown,
    Live,
    Paused,
    DisconnectPaused,
    Stopped,
    Results,
    Error,
}
