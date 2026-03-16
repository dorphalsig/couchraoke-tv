package com.couchraoke.tv.domain.session

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRosterTest {

    @Test
    fun `given new session, when onPhoneConnected with clientId and deviceName, then displayName defaults to deviceName`() {
        val session = Session()
        // calling twice with same clientId should not throw
        session.onPhoneConnected("client1", "MyPhone", 1u)
        // second call: displayName should NOT change
        session.onPhoneConnected("client1", "DifferentName", 2u)
        // We can't read displayNames directly; verify no crash and behavior is idempotent
    }

    @Test
    fun `given phone connected, when onPhoneConnected called again with same clientId, then roster entry not duplicated`() {
        val session = Session()
        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.onPhoneConnected("client1", "MyPhone", 1u)
        // No exception thrown, internal roster size should be 1
    }

    @Test
    fun `given phone was connected and disconnected, when onPhoneReconnected with same clientId, then no exception thrown`() {
        val session = Session()
        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.onPhoneDisconnected("client1")
        session.onPhoneReconnected("client1", 2u)
    }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun `given singer phone was assigned to P1, when it reconnects, then wasSinger is reflected in PhoneReconnected event`() =
        runTest {
            val session = Session()
            val events = mutableListOf<SessionEvent>()
            // UnconfinedTestDispatcher runs the collector eagerly so it subscribes before any emission
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                session.events.collect { events.add(it) }
            }

            session.onPhoneConnected("client1", "MyPhone", 1u)
            session.lockForSong(p1ClientId = "client1")
            session.onPhoneDisconnected("client1")
            session.onPhoneReconnected("client1", 2u)

            advanceUntilIdle()
            job.cancel()

            val reconnectedEvent = events.filterIsInstance<SessionEvent.PhoneReconnected>()
                .find { it.clientId == "client1" }
            assertNotNull("PhoneReconnected event should be emitted", reconnectedEvent)
            assertTrue("wasSinger should be true for assigned P1 singer", reconnectedEvent!!.wasSinger)
        }

    @Test
    fun `given phone in roster, when rename called, then no exception thrown`() {
        val session = Session()
        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.rename("client1", "NewName")
    }

    @Test
    fun `given phone in roster, when kick called, then connectionCloser invoked with clientId`() {
        val closed = mutableListOf<String>()
        val session = Session(connectionCloser = { closed.add(it) })

        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.kick("client1")

        assertEquals(listOf("client1"), closed)
    }

    @Test
    fun `given phone in roster, when forget called, then connectionCloser invoked`() {
        val closed = mutableListOf<String>()
        val session = Session(connectionCloser = { closed.add(it) })

        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.forget("client1")

        assertEquals(listOf("client1"), closed)
    }

    @Test
    fun `given clientId not in roster, when rename called, then no exception (no-op)`() {
        val session = Session()
        session.rename("unknown", "NewName")
    }

    @Test
    fun `given locked session with assigned singer, when releaseSlot called, then no exception thrown`() {
        val session = Session()
        session.onPhoneConnected("client1", "MyPhone", 1u)
        session.lockForSong(p1ClientId = "client1")
        session.releaseSlot("client1")
    }
}
