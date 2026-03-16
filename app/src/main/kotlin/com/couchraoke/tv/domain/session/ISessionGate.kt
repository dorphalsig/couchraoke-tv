package com.couchraoke.tv.domain.session

interface ISessionGate {
    val isLocked: Boolean
    val sessionId: String
    // = 10 for MVP (T8.3.7: session_full fires when >10 connected; only P1/P2 can sing)
    val maxConnections: Int
}
