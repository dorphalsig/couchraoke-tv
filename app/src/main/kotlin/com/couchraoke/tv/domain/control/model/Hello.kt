package com.couchraoke.tv.domain.control.model

import kotlinx.serialization.Serializable

/**
 * Inbound `hello` handshake frame from a phone. Every property is non-null with no
 * default, so kotlinx-serialization raises `MissingFieldException` for an absent field
 * (contracts/wire-protocol.md). Property names are the wire names verbatim — no
 * `@SerialName` renaming is permitted here.
 */
@Serializable
data class Hello(
    val type: String,
    val protocolVersion: Int,
    val clientId: String,
    val deviceName: String,
    val appVersion: String,
    val httpPort: Int,
)
