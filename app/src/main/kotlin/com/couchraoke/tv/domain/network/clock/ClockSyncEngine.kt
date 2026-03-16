package com.couchraoke.tv.domain.network.clock

import com.couchraoke.tv.domain.network.protocol.ClockAckMessage
import com.couchraoke.tv.domain.network.protocol.PingMessage
import com.couchraoke.tv.domain.network.protocol.PongMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

fun interface PingSender {
    fun send(clientId: String, ping: PingMessage)
}

class ClockSyncEngine(
    private val clock: () -> Long,
    private val pingSender: PingSender,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private var suspended = false
    private val jobs = mutableMapOf<String, Job>()

    /** Schedules 5 pings 100ms apart for the given client. */
    fun startInitialSync(clientId: String) {
        jobs[clientId]?.cancel()
        jobs[clientId] = scope.launch(dispatcher) {
            repeat(5) {
                if (!suspended) sendPing(clientId)
                delay(100L)
            }
        }
    }

    /** Called when a pong arrives. Returns a clockAck immediately. */
    fun onPong(pong: PongMessage): ClockAckMessage =
        ClockAckMessage(
            pingId = pong.pingId,
            tTvRecvMs = clock(),
        )

    /** Suspends clock sync (e.g., during active singing). */
    fun suspendSync() {
        suspended = true
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    /** Resumes with a single ping exchange (e.g., after song end). */
    fun resumeSingle(clientId: String) {
        suspended = false
        jobs[clientId]?.cancel()
        jobs[clientId] = scope.launch(dispatcher) {
            sendPing(clientId)
        }
    }

    private fun sendPing(clientId: String) {
        pingSender.send(
            clientId,
            PingMessage(pingId = UUID.randomUUID().toString(), tTvSendMs = clock()),
        )
    }
}
