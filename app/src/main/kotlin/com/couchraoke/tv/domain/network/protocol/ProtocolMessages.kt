package com.couchraoke.tv.domain.network.protocol

import kotlinx.serialization.Serializable

@Serializable
data class HelloMessage(
    val type: String = "hello",
    val protocolVersion: Int,
    val clientId: String,
    val deviceName: String,
    val appVersion: String,
    val httpPort: Int,
    val capabilities: Capabilities,
    val tsTvMs: Long? = null,
)

@Serializable
data class Capabilities(val pitchFps: Int? = null)

@Serializable
data class SessionStateMessage(
    val type: String = "sessionState",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val slots: SlotMap,
    val inSong: Boolean,
    val songTimeSec: Double? = null,
    val connectionId: Int? = null,
    val tsTvMs: Long? = null,
)

@Serializable
data class SlotMap(val P1: SlotInfo, val P2: SlotInfo)

@Serializable
data class SlotInfo(val connected: Boolean, val deviceName: String)

@Serializable
data class AssignSingerMessage(
    val type: String = "assignSinger",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val songInstanceSeq: Long,
    val playerId: String,
    val difficulty: String,
    val thresholdIndex: Int,
    val effectiveMicDelayMs: Int,
    val expectedPitchFps: Int,
    val startMode: String,
    val countdownMs: Int? = null,
    val endTimeTvMs: Long,
    val udpPort: Int,
    val songTitle: String? = null,
    val songArtist: String? = null,
    val tsTvMs: Long? = null,
)

@Serializable
data class ErrorMessage(
    val type: String = "error",
    val protocolVersion: Int = 1,
    val code: String,
    val message: String,
    val tsTvMs: Long? = null,
)

@Serializable
data class PingMessage(
    val type: String = "ping",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvSendMs: Long,
    val tsTvMs: Long? = null,
)

@Serializable
data class PongMessage(
    val type: String = "pong",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvSendMs: Long,
    val tPhoneRecvMs: Long,
    val tPhoneSendMs: Long,
    val tsTvMs: Long? = null,
)

@Serializable
data class ClockAckMessage(
    val type: String = "clockAck",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvRecvMs: Long,
    val tsTvMs: Long? = null,
)
