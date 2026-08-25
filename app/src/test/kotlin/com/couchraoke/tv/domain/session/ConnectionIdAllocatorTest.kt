package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.ConnectionId
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionIdAllocatorTest {

    @Test(timeout = 30_000)
    fun startsAtOne() {
        val allocator = ConnectionIdAllocator()

        assertEquals(ConnectionId(1), allocator.next())
    }

    @Test(timeout = 30_000)
    fun incrementsPerCall() {
        val allocator = ConnectionIdAllocator()

        assertEquals(ConnectionId(1), allocator.next())
        assertEquals(ConnectionId(2), allocator.next())
        assertEquals(ConnectionId(3), allocator.next())
    }

    @Test(timeout = 30_000)
    fun neverRepeatsWithinASession() {
        val allocator = ConnectionIdAllocator()

        val allocated = (1..1000).map { allocator.next() }

        assertEquals(allocated.size, allocated.toSet().size)
    }

    @Test(timeout = 30_000)
    fun wrapsWithinUint16() {
        val allocator = ConnectionIdAllocator()

        repeat(65_535) { allocator.next() }

        assertEquals(ConnectionId(1), allocator.next())
    }
}
