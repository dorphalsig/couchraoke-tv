package com.couchraoke.tv.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

private val networkJson = Json { ignoreUnknownKeys = false }

@Serializable
private data class ManifestJson(
    val songs: List<SongEntryJson> = emptyList(),
)

@Serializable
private data class SongEntryJson(
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val isDuet: Boolean? = null,
    val hasRap: Boolean? = null,
    val hasVideo: Boolean? = null,
    val hasInstrumental: Boolean? = null,
    val canMedley: Boolean? = null,
    val medleySource: String? = null,
    val medleyStartBeat: Int? = null,
    val medleyEndBeat: Int? = null,
    val startSec: Float? = null,
    val previewStartSec: Float? = null,
    @SerialName("txtUrl") val txtUrl: String,
    @SerialName("audioUrl") val audioUrl: String,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("backgroundUrl") val backgroundUrl: String? = null,
)

fun parseManifestJson(json: String): List<SongEntry> {
    val element = networkJson.parseToJsonElement(json)
    val entries = if (element is JsonArray) {
        networkJson.decodeFromString<List<SongEntryJson>>(json)
    } else {
        networkJson.decodeFromString<ManifestJson>(json).songs
    }
    return entries.mapNotNull { entry ->
        val isDuet = entry.isDuet ?: return@mapNotNull null
        val hasRap = entry.hasRap ?: return@mapNotNull null
        val hasVideo = entry.hasVideo ?: return@mapNotNull null
        val hasInstrumental = entry.hasInstrumental ?: return@mapNotNull null
        val canMedley = entry.canMedley ?: return@mapNotNull null
        val startSec = entry.startSec ?: return@mapNotNull null
        val previewStartSec = entry.previewStartSec ?: return@mapNotNull null
        SongEntry(
            relativeTxtPath = entry.relativeTxtPath,
            modifiedTimeMs = entry.modifiedTimeMs,
            title = entry.title,
            artist = entry.artist,
            album = entry.album,
            year = entry.year,
            genre = entry.genre,
            isDuet = isDuet,
            hasRap = hasRap,
            hasVideo = hasVideo,
            hasInstrumental = hasInstrumental,
            canMedley = canMedley,
            medleySource = entry.medleySource,
            medleyStartBeat = entry.medleyStartBeat,
            medleyEndBeat = entry.medleyEndBeat,
            startSec = startSec,
            previewStartSec = previewStartSec,
            txtUrl = entry.txtUrl,
            audioUrl = entry.audioUrl,
            videoUrl = entry.videoUrl,
            coverUrl = entry.coverUrl,
            backgroundUrl = entry.backgroundUrl,
        )
    }
}
