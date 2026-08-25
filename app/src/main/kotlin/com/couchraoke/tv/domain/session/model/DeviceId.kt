package com.couchraoke.tv.domain.session.model

/**
 * Phone-supplied device identity. Never authoritative — it only keys the roster.
 */
@JvmInline
value class DeviceId(val value: String) {
    init {
        require(value.length >= MIN_LENGTH) {
            "DeviceId must be at least $MIN_LENGTH characters, was ${value.length}"
        }
    }

    private companion object {
        const val MIN_LENGTH = 8
    }
}
