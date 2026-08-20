package com.couchraoke.tv.domain.session

/**
 * Session membership state. Deliberately distinct from [GamePhase]: `Locked` is what
 * produces the `session_locked` refusal, and neither state derives from the other.
 */
enum class SessionLifecycle {
    Open,
    Locked,
    Ended,
}
