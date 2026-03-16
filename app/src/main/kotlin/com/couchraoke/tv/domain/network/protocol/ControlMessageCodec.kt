package com.couchraoke.tv.domain.network.protocol

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ControlMessageCodec {

    private const val TAG = "ControlMessageCodec"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun decodeType(jsonString: String): String? = try {
        json.parseToJsonElement(jsonString).jsonObject["type"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        Log.w(TAG, "Failed to peek type field: ${e.message}")
        null
    }

    fun decode(jsonString: String): Any? {
        val type = decodeType(jsonString) ?: run {
            Log.w(TAG, "Message missing 'type' field: $jsonString")
            return null
        }
        return try {
            when (type) {
                "hello"        -> json.decodeFromString<HelloMessage>(jsonString)
                "sessionState" -> json.decodeFromString<SessionStateMessage>(jsonString)
                "assignSinger" -> json.decodeFromString<AssignSingerMessage>(jsonString)
                "error"        -> json.decodeFromString<ErrorMessage>(jsonString)
                "ping"         -> json.decodeFromString<PingMessage>(jsonString)
                "pong"         -> json.decodeFromString<PongMessage>(jsonString)
                "clockAck"     -> json.decodeFromString<ClockAckMessage>(jsonString)
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode message type '$type': ${e.message}")
            null
        }
    }
}
