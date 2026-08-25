package com.couchraoke.tv.domain.control.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Outbound `sessionState` frame, TV -> phone. `type` and `protocolVersion` carry
 * defaults but are schema-*required*, so [com.couchraoke.tv.domain.control.ControlMessageCodec]
 * serializes this class with `encodeDefaults = true` while `explicitNulls = false` keeps
 * the nullable optionals (`tsTvMs`, `songTimeSec`, `connectionId`) absent rather than
 * `null` when unset — all three schemas set `additionalProperties: false`
 * (contracts/wire-protocol.md).
 *
 * `connectionId` is populated only on the direct reply to a `hello` and omitted
 * everywhere else (FR-014).
 */
@Serializable
data class SessionState(
    val type: String = "sessionState",
    val protocolVersion: Int = 1,
    val tsTvMs: Double? = null,
    val sessionId: String,
    val slots: Slots,
    val connectedDevices: List<ConnectedDeviceDto>,
    val inSong: Boolean,
    val songTimeSec: Double? = null,
    val connectionId: Int? = null,
)

/**
 * Singer assignment slots. The wire schema names these fields `P1` / `P2`
 * (contracts/wire-protocol.md). `@SerialName` pins the wire spelling while the Kotlin
 * properties keep the conventional casing, so the exact wire shape is preserved without
 * a naming suppression.
 */
@Serializable
data class Slots(
    @SerialName("P1") val p1: SlotDto,
    @SerialName("P2") val p2: SlotDto,
)

@Serializable
data class SlotDto(val connected: Boolean, val deviceName: String)

@Serializable
data class ConnectedDeviceDto(
    val clientId: String,
    val displayName: String,
    val state: String,
    val slot: String? = null,
)
