package com.couchraoke.tv.domain.session.model

/**
 * The phone-reported asset streaming port. Recorded only — never contacted in this slice.
 */
@JvmInline
value class AssetPort(val value: Int) {
    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "AssetPort must be between $MIN_VALUE and $MAX_VALUE, was $value"
        }
    }

    private companion object {
        const val MIN_VALUE = 1024
        const val MAX_VALUE = 65_535
    }
}
