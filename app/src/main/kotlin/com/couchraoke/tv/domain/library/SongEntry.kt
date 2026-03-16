package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.MedleySource

data class SongEntry(
    // Identity
    val songId: String,
    val phoneClientId: String,
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    // Validation
    val isValid: Boolean,
    val invalidReasonCode: String? = null,
    val invalidLineNumber: Int? = null,
    // Display
    val artist: String? = null,
    val title: String? = null,
    val album: String? = null,
    // Derived flags
    val isDuet: Boolean = false,
    val hasRap: Boolean = false,
    val hasVideo: Boolean = false,
    val hasInstrumental: Boolean = false,
    val canMedley: Boolean = false,
    val medleySource: MedleySource = MedleySource.NONE,
    val medleyStartBeat: Int? = null,
    val medleyEndBeat: Int? = null,
    val calcMedleyEnabled: Boolean = true,
    // Preview/seek metadata
    val startSec: Double = 0.0,
    val previewStartSec: Double = 0.0,
    // Asset URLs (as received from manifest; null if absent)
    val txtUrl: String,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val instrumentalUrl: String? = null,
    val vocalsUrl: String? = null,
)