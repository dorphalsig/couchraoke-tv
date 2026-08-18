package com.couchraoke.tv.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val wireJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

internal inline fun <reified T> encodeNetworkMessage(message: T): String = wireJson.encodeToString(message)

@Serializable
internal data class HelloWireMessage(
    val type: String,
    val protocolVersion: Int,
    val clientId: String,
    val deviceName: String,
    val appVersion: String,
    val httpPort: Int,
)

@Serializable
internal data class SessionStateWireMessage(
    val type: String = "sessionState",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val slots: SessionSlotsWireMessage = SessionSlotsWireMessage(),
    val inSong: Boolean,
    val songTimeSec: Double? = null,
    val connectionId: Int? = null,
)

@Serializable
internal data class SessionSlotsWireMessage(
    @SerialName("P1") val p1: SessionSlotWireMessage = SessionSlotWireMessage(),
    @SerialName("P2") val p2: SessionSlotWireMessage = SessionSlotWireMessage(),
)

@Serializable
internal data class SessionSlotWireMessage(
    val connected: Boolean = false,
    val deviceName: String = "",
)

@Serializable
internal data class ErrorWireMessage(
    val type: String = "error",
    val protocolVersion: Int = 1,
    val code: String,
    val message: String,
)

@Serializable
internal data class PingWireMessage(
    val type: String = "ping",
    val protocolVersion: Int = 1,
    val pingId: String,
    val tTvSendMs: Long,
)

internal fun errorMessageFor(code: String): String = when (code) {
    "invalid_token" -> "Invalid session token."
    "protocol_mismatch" -> "Protocol version mismatch."
    "session_locked" -> "Session is locked."
    "session_full" -> "Session is full."
    else -> "Network protocol error."
}
