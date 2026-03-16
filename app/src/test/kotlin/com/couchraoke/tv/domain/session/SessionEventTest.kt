package com.couchraoke.tv.domain.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEventTest {

    private fun collectEvents(session: Session, block: Session.() -> Unit): List<SessionEvent> {
        var events = listOf<SessionEvent>()
        kotlinx.coroutines.test.runTest {
            val collected = mutableListOf<SessionEvent>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                session.events.collect { collected.add(it) }
            }
            session.block()
            advanceUntilIdle()
            job.cancel()
            events = collected.toList()
        }
        return events
    }

    @Test
    fun `given locked session and assigned singer disconnects, when onPhoneDisconnected called, then RequiredSingerDisconnected event emitted with correct slot`() {
        val session = Session()
        session.onPhoneConnected("singer1", "SingerPhone", 1u)
        session.lockForSong(p1ClientId = "singer1")

        val events = collectEvents(session) { onPhoneDisconnected("singer1") }

        val event = events.filterIsInstance<SessionEvent.RequiredSingerDisconnected>()
            .find { it.clientId == "singer1" }
        assertNotNull("RequiredSingerDisconnected should be emitted", event)
        assertTrue("slot should be P1", event!!.slot == "P1")
    }

    @Test
    fun `given locked session and spectator disconnects, when onPhoneDisconnected called, then SpectatorDisconnected emitted`() {
        val session = Session()
        session.onPhoneConnected("spectator1", "SpectatorPhone", 1u)
        session.onPhoneConnected("singer1", "SingerPhone", 2u)
        session.lockForSong(p1ClientId = "singer1")

        val events = collectEvents(session) { onPhoneDisconnected("spectator1") }

        val event = events.filterIsInstance<SessionEvent.SpectatorDisconnected>()
            .find { it.clientId == "spectator1" }
        assertNotNull("SpectatorDisconnected should be emitted", event)
    }

    @Test
    fun `given activeSourceClientId set and that phone disconnects, when onPhoneDisconnected called, then PlaybackSourceLost emitted in addition to disconnect event`() {
        val session = Session()
        session.onPhoneConnected("source1", "SourcePhone", 1u)
        session.lockForSong(p1ClientId = "source1", sourceClientId = "source1")

        val events = collectEvents(session) { onPhoneDisconnected("source1") }

        assertTrue(
            "PlaybackSourceLost should be emitted",
            events.any { it is SessionEvent.PlaybackSourceLost && it.clientId == "source1" }
        )
    }

    @Test
    fun `given roster-full session and reconnecting phone clientId not in displayNames (was forgotten), when reconnect attempted, then treated as new device`() {
        val session = Session()
        // Connect and then forget the client
        session.onPhoneConnected("client1", "Phone", 1u)
        session.forget("client1")

        // Reconnect - since displayNames no longer has clientId, treated as new join
        // onPhoneReconnected checks if clientId is in displayNames; if not, wasSinger=false and PhoneReconnected emitted
        val events = collectEvents(session) { onPhoneReconnected("client1", 2u) }

        // After forget, client is not in displayNames, so wasSinger=false
        val event = events.filterIsInstance<SessionEvent.PhoneReconnected>()
            .find { it.clientId == "client1" }
        assertNotNull("PhoneReconnected event should still be emitted", event)
        assertTrue("wasSinger should be false for forgotten client", !event!!.wasSinger)
    }
}
