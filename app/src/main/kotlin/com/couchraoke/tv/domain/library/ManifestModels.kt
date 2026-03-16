package com.couchraoke.tv.domain.library

data class ManifestEntry(
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val txtUrl: String,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val instrumentalUrl: String? = null,
    val vocalsUrl: String? = null,
)