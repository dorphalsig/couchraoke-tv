package com.couchraoke.tv.domain.network.clock

import com.couchraoke.tv.domain.network.protocol.PongMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClockSyncEngineTest {

    private fun buildEngine(
        clock: () -> Long = { 1000L },
        pings: MutableList<String> = mutableListOf(),
        scope: TestScope,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ) = ClockSyncEngine(
        clock = clock,
        pingSender = { _, ping -> pings.add(ping.pingId) },
        scope = scope,
        dispatcher = dispatcher,
    )

    @Test
    fun `given pong received, when onPong, then clockAck has correct pingId and tTvRecvMs`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val engine = buildEngine(clock = { 999L }, pings = mutableListOf(), scope = this, dispatcher = dispatcher)
        val pong = PongMessage(pingId = "test-id", tTvSendMs = 100, tPhoneRecvMs = 110, tPhoneSendMs = 115)
        val ack = engine.onPong(pong)
        assertEquals("test-id", ack.pingId)
        assertEquals(999L, ack.tTvRecvMs)
    }

    @Test
    fun `given startInitialSync called, when idle, then exactly 5 pings dispatched`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pings = mutableListOf<String>()
        val engine = buildEngine(pings = pings, scope = this, dispatcher = dispatcher)
        engine.startInitialSync("client1")
        advanceUntilIdle()
        assertEquals(5, pings.size)
    }

    @Test
    fun `given suspendSync called, when startInitialSync called, then no pings dispatched`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pings = mutableListOf<String>()
        val engine = buildEngine(pings = pings, scope = this, dispatcher = dispatcher)
        engine.suspendSync()
        engine.startInitialSync("client1")
        advanceUntilIdle()
        assertEquals(0, pings.size)
    }

    @Test
    fun `given resumeSingle called, when idle, then exactly 1 ping dispatched`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val pings = mutableListOf<String>()
        val engine = buildEngine(pings = pings, scope = this, dispatcher = dispatcher)
        engine.resumeSingle("client1")
        advanceUntilIdle()
        assertEquals(1, pings.size)
    }
}
