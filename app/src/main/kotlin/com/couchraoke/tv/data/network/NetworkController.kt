package com.couchraoke.tv.data.network

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface NetworkController {
    val connectedPhones: StateFlow<List<ConnectedPhone>>
    val phoneEvents: SharedFlow<PhoneEvent>

    suspend fun start(udpPort: Int, wsPort: Int)

    suspend fun stop()

    suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>>

    suspend fun fetchTxt(url: String): Result<ByteArray>

    suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage)

    suspend fun broadcastPlaybackState(message: PlaybackStateMessage)

    suspend fun sendSessionState(phoneId: String)

    suspend fun sendPing(phoneId: String): PongResponse

    suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage)
}
