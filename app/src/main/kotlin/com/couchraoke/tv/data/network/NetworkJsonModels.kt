package com.couchraoke.tv.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val networkJson = Json { ignoreUnknownKeys = true }

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
    val isDuet: Boolean = false,
    val hasRap: Boolean = false,
    val hasVideo: Boolean = false,
    val hasInstrumental: Boolean = false,
    val canMedley: Boolean = false,
    val medleySource: String? = null,
    val medleyStartBeat: Int? = null,
    val medleyEndBeat: Int? = null,
    val startSec: Float = 0f,
    val previewStartSec: Float = 0f,
    @SerialName("txtUrl") val txtUrl: String,
    @SerialName("audioUrl") val audioUrl: String,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("backgroundUrl") val backgroundUrl: String? = null,
)

fun parseManifestJson(json: String): List<SongEntry> = networkJson
    .decodeFromString<ManifestJson>(json)
    .songs
    .map { entry ->
        SongEntry(
            relativeTxtPath = entry.relativeTxtPath,
            modifiedTimeMs = entry.modifiedTimeMs,
            title = entry.title,
            artist = entry.artist,
            album = entry.album,
            year = entry.year,
            genre = entry.genre,
            isDuet = entry.isDuet,
            hasRap = entry.hasRap,
            hasVideo = entry.hasVideo,
            hasInstrumental = entry.hasInstrumental,
            canMedley = entry.canMedley,
            medleySource = entry.medleySource,
            medleyStartBeat = entry.medleyStartBeat,
            medleyEndBeat = entry.medleyEndBeat,
            startSec = entry.startSec,
            previewStartSec = entry.previewStartSec,
            txtUrl = entry.txtUrl,
            audioUrl = entry.audioUrl,
            videoUrl = entry.videoUrl,
            coverUrl = entry.coverUrl,
            backgroundUrl = entry.backgroundUrl,
        )
    }
