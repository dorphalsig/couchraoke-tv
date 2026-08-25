package com.couchraoke.tv.domain.session.model

/**
 * Opaque session identity. No format is normative — phones must not parse it.
 * Unique among the sessions this TV creates (FR-001).
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotEmpty()) { "SessionId must not be empty" }
    }
}
