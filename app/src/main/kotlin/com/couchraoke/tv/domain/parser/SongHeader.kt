package com.couchraoke.tv.domain.parser

data class SongVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
)

data class CustomTag(
    val tagName: String,
    val content: String,
    val lineNumber: Int,
)

data class SongHeader(
    val version: SongVersion? = null,
    val title: String? = null,
    val artist: String? = null,
    val bpm: Double? = null,
    val gapMs: Int? = null,
    val startSec: Double? = null,
    val endMs: Int? = null,
    val previewStartSec: Double? = null,
    val audioReference: String? = null,
    val videoReference: String? = null,
    val videoGapSec: Double? = null,
    val coverReference: String? = null,
    val backgroundReference: String? = null,
    val instrumentalReference: String? = null,
    val vocalsReference: String? = null,
    val medleyStartBeat: Int? = null,
    val medleyEndBeat: Int? = null,
    val calcMedleyEnabled: Boolean = false,
    val p1Name: String? = null,
    val p2Name: String? = null,
    val customTags: List<CustomTag> = emptyList(),
)
