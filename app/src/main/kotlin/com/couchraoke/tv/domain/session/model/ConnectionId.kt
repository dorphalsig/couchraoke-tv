package com.couchraoke.tv.domain.session.model

/**
 * A uint16 connection identity. Never reused within a session (FR-013).
 */
@JvmInline
value class ConnectionId(val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "ConnectionId must be between $MIN_VALUE and $MAX_VALUE, was $value"
        }
    }

    private companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 65_535
    }
}
