package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.presentation.playback.PlaybackEvent
import kotlinx.coroutines.flow.StateFlow

interface PlaybackCoordinator {
    val state: StateFlow<PlaybackCoordinatorState>

    suspend fun startSong(selection: SongStartSelection)

    suspend fun pause()

    suspend fun resume()

    suspend fun restart()

    suspend fun quitToSongList()

    suspend fun onPlaybackEvent(event: PlaybackEvent)
}
