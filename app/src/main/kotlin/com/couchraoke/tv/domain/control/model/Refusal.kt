package com.couchraoke.tv.domain.control.model

import kotlinx.serialization.Serializable

/**
 * Outbound `error` frame, TV -> phone. `type` and `protocolVersion` carry defaults but
 * are schema-required — see [SessionState] for why the codec's outbound `Json` differs
 * from its inbound one. `code` is also the WebSocket close reason (FR-016); see
 * [com.couchraoke.tv.domain.control.RefusalReason].
 */
@Serializable
data class Refusal(
    val type: String = "error",
    val protocolVersion: Int = 1,
    val tsTvMs: Double? = null,
    val code: String,
    val message: String,
)
