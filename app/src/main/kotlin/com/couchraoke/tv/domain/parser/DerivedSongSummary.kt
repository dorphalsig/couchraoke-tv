package com.couchraoke.tv.domain.parser

enum class MedleySource {
    NONE,
    EXPLICIT,
    FALLBACK,
}

data class DerivedSongSummary(
    val isDuet: Boolean = false,
    val hasRap: Boolean = false,
    val hasVideo: Boolean = false,
    val hasInstrumental: Boolean = false,
    val previewStartSec: Double? = null,
    val medleySource: MedleySource = MedleySource.NONE,
    val medleyStartBeat: Int? = null,
    val medleyEndBeat: Int? = null,
    val calcMedleyEnabled: Boolean = false,
)
