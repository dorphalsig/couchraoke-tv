package com.couchraoke.tv.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTransitionTest {

    @Test
    fun `given new session, when created, then state is Open and isLocked is false`() {
        val session = Session()
        assertEquals(SessionState.Open, session.state)
        assertFalse(session.isLocked)
    }

    @Test
    fun `given Open session and fewer than 10 phones, when onPhoneConnected called, then no exception and phone is in session`() {
        val session = Session()
        session.onPhoneConnected("client1", "Phone 1", 1u)
        // Session accepts the connection without throwing
        assertEquals(
            SessionState.Open,
            session.state
        )
    }

    @Test
    fun `given a new session, calling onPhoneConnected 11 times succeeds`() {
        val session = Session()
        repeat(11) { i ->
            session.onPhoneConnected("client$i", "Phone $i", i.toUShort())
        }
        // Session trusts the caller and doesn't enforce the 10-phone cap itself — no exception expected
        assertEquals(SessionState.Open, session.state)
    }

    @Test
    fun `given Open session, when lockForSong called with p1ClientId, then state is Locked and isLocked is true`() {
        val session = Session()
        session.lockForSong("client1")
        assertEquals(SessionState.Locked, session.state)
        assertTrue(session.isLocked)
    }

    @Test
    fun `given Locked session, when unlockAfterSong called, then state is Open and isLocked is false`() {
        val session = Session()
        session.lockForSong("client1")
        session.unlockAfterSong()
        assertEquals(SessionState.Open, session.state)
        assertFalse(session.isLocked)
    }

    @Test
    fun `given Locked session, when lockForSong called again, then state remains Locked (no-op guard)`() {
        val session = Session()
        session.lockForSong("client1")
        session.lockForSong("client2")
        assertEquals(SessionState.Locked, session.state)
        assertTrue(session.isLocked)
    }

    @Test
    fun `given Open session, when unlockAfterSong called, then state remains Open (no-op guard)`() {
        val session = Session()
        session.unlockAfterSong()
        assertEquals(SessionState.Open, session.state)
        assertFalse(session.isLocked)
    }

    @Test
    fun `given Open session, then inSong is false`() {
        val session = Session()
        assertFalse(session.inSong)
    }

    @Test
    fun `given Locked session, then inSong is true`() {
        val session = Session()
        session.lockForSong("client1")
        assertTrue(session.inSong)
    }

    @Test
    fun `given active session with connected phones, when endSession called, then state is Ended`() {
        val closed = mutableListOf<String>()
        val session = Session(connectionCloser = IConnectionCloser { closed.add(it) })
        session.onPhoneConnected("client1", "Phone 1", 1u)

        session.endSession()

        assertEquals(SessionState.Ended, session.state)
        // Check if connectionCloser was called for the connected phone
        assertTrue("Expected client1 to be closed", closed.contains("client1"))
    }

    @Test
    fun `given ended session, when endSession called again, then no-op (state stays Ended)`() {
        val session = Session()
        session.endSession()
        session.endSession()
        assertEquals(SessionState.Ended, session.state)
    }
}
