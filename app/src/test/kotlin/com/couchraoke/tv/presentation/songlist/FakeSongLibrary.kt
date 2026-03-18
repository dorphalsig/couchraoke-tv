package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.domain.library.SongLibrary

class FakeSongLibrary(private var songs: List<SongEntry> = emptyList()) : SongLibrary {
    override fun addPhone(clientId: String, entries: List<SongEntry>) { songs = songs + entries }
    override fun removePhone(clientId: String) { songs = songs.filter { it.phoneClientId != clientId } }
    override fun getSortedSongs(): List<SongEntry> = songs.sortedWith(
        compareBy({ it.artist?.lowercase() }, { it.album?.lowercase() }, { it.title?.lowercase() })
    )
    override fun getSongById(songId: String): SongEntry? = songs.find { it.songId == songId }
    override fun getSongsByPhone(clientId: String): List<SongEntry> = songs.filter { it.phoneClientId == clientId }
}
