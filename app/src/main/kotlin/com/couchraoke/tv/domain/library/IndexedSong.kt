package com.couchraoke.tv.domain.library

data class IndexedSong(
    val songId: String,
    val phoneClientId: String,
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val genre: String?,
    val txtUrl: String,
    val audioUrl: String,
    val videoUrl: String?,
    val coverUrl: String?,
    val backgroundUrl: String?,
    val isDuet: Boolean,
    val hasRap: Boolean,
    val hasVideo: Boolean,
    val canMedley: Boolean,
    val medleySource: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val startSec: Float,
    val previewStartSec: Float
)
