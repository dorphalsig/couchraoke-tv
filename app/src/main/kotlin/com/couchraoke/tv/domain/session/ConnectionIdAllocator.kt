package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.ConnectionId

/**
 * Issues [ConnectionId]s starting at 1, incrementing per call and never repeating within
 * a session, including across reconnects (FR-013). Wraps within uint16.
 */
class ConnectionIdAllocator {

    private var nextValue: Int = MIN_VALUE

    fun next(): ConnectionId {
        val allocated = ConnectionId(nextValue)
        nextValue = if (nextValue == MAX_VALUE) MIN_VALUE else nextValue + 1
        return allocated
    }

    private companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 65_535
    }
}
