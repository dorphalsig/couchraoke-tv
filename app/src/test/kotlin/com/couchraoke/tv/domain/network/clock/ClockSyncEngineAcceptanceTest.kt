package com.couchraoke.tv.domain.network.clock

import com.couchraoke.tv.domain.network.protocol.PongMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ClockSyncEngineAcceptanceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ClockSyncEntry(
        val pingId: String,
        val t1: Long,
        val t2: Long,
        val t3: Long,
        val t4: Long,
    )

    @Test
    fun `verify clock sync samples from F14v2 fixture`() {
        // Path to fixture relative to project root
        val fixtureFile = File("/home/paavum/Couchraoke/tv/original_spec/fixtures/F14v2_clock_sync_phone_side/clockSync.jsonl")

        // Read lines and parse each as ClockSyncEntry
        val entries = fixtureFile.readLines().map { line ->
            json.decodeFromString<ClockSyncEntry>(line)
        }

        entries.forEach { entry ->
            var currentClock = 0L
            val engine = ClockSyncEngine(
                clock = { currentClock },
                pingSender = { _, _ -> }, // Not used in onPong
                scope = kotlinx.coroutines.MainScope() // Not used in onPong
            )

            // Inject t4 as the clock value when onPong is called
            currentClock = entry.t4

            val pong = PongMessage(
                pingId = entry.pingId,
                tTvSendMs = entry.t1,
                tPhoneRecvMs = entry.t2,
                tPhoneSendMs = entry.t3
            )

            val ack = engine.onPong(pong)

            assertEquals("pingId mismatch for ${entry.pingId}", entry.pingId, ack.pingId)
            assertEquals("tTvRecvMs mismatch for ${entry.pingId}", entry.t4, ack.tTvRecvMs)
        }
    }
}
