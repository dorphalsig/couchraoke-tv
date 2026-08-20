package com.couchraoke.tv.domain.control

/**
 * The refusal vocabulary for the `error` frame's `code` field. Each enum constant
 * carries its wire [code] string so the payload code and the WebSocket close reason
 * (FR-016) can never drift apart — both are read from this single source.
 *
 * `SESSION_LOCKED` is unreachable in this slice; it is defined so the vocabulary is
 * complete (contracts/wire-protocol.md).
 */
enum class RefusalReason(val code: String) {
    INVALID_TOKEN("invalid_token"),
    PROTOCOL_MISMATCH("protocol_mismatch"),
    INVALID_MESSAGE("invalid_message"),
    SESSION_FULL("session_full"),
    SESSION_LOCKED("session_locked"),
}
