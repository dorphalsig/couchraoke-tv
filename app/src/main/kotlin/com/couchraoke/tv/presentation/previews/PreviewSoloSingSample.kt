@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.previews

import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.presentation.songlist.SongListState

@NoCoverageGenerated
internal object PreviewSoloSingSample {
    const val SessionId = "preview-session"
    const val SessionToken = "PREVIEW1"
    const val JoinCode = SessionToken
    const val TvIpAddress = "192.168.1.10"
    const val WebSocketPort = 8080
    const val PhoneClientId = "preview-phone"
    const val PhoneDeviceName = "Living Room Phone"
    const val RelativeTxtPath = "solo/preview-song.txt"
    const val ModifiedTimeMs = 1_700_000_000_000L
    const val SongTitle = "Demo Song"
    const val SongArtist = "Demo Artist"
    const val SongAlbum = "Demo Album"
    const val SongYear = 2026
    const val SongGenre = "Pop"
    const val StartSec = 1.5f
    const val PreviewStartSec = 12.0f
    const val TxtUrl = "http://192.168.1.23:43210/songs/solo/preview-song.txt"
    const val AudioUrl = "http://192.168.1.23:43210/songs/solo/preview-song.mp3"
    const val VideoUrl = "http://192.168.1.23:43210/songs/solo/preview-song.mp4"
}

@NoCoverageGenerated
internal fun previewSongListState(): SongListState {
    val songs = listOf(
        previewIndexedSong(songId = "song-1", title = "Demo Song", artist = "Demo Artist"),
        previewIndexedSong(
            songId = "song-2",
            title = "Duet Demo",
            artist = "Second Artist",
            isDuet = true,
        ),
        previewIndexedSong(
            songId = "song-3",
            title = "Rap Medley",
            artist = "Third Artist",
            hasRap = true,
            canMedley = true,
        ),
    )
    return SongListState(
        visibleSongs = songs,
        focusedSong = songs.first(),
        randomDuetEnabled = false,
        randomMedleyEnabled = false,
    )
}

@NoCoverageGenerated
internal fun previewIndexedSong(
    songId: String,
    title: String,
    artist: String,
    coverUrl: String? = null,
    isDuet: Boolean = false,
    hasRap: Boolean = false,
    canMedley: Boolean = false,
): IndexedSong = IndexedSong(
    songId = songId,
    phoneClientId = PreviewSoloSingSample.PhoneClientId,
    relativeTxtPath = PreviewSoloSingSample.RelativeTxtPath,
    modifiedTimeMs = PreviewSoloSingSample.ModifiedTimeMs,
    title = title,
    artist = artist,
    album = PreviewSoloSingSample.SongAlbum,
    year = PreviewSoloSingSample.SongYear,
    genre = PreviewSoloSingSample.SongGenre,
    txtUrl = PreviewSoloSingSample.TxtUrl,
    audioUrl = PreviewSoloSingSample.AudioUrl,
    videoUrl = PreviewSoloSingSample.VideoUrl,
    coverUrl = coverUrl,
    backgroundUrl = null,
    isDuet = isDuet,
    hasRap = hasRap,
    hasVideo = true,
    hasInstrumental = false,
    canMedley = canMedley,
    medleySource = null,
    medleyStartBeat = null,
    medleyEndBeat = null,
    startSec = PreviewSoloSingSample.StartSec,
    previewStartSec = PreviewSoloSingSample.PreviewStartSec,
)
