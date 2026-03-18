package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry

/**
 * Returns songs from [songs] whose artist, album (nullable), or title (nullable)
 * contains [query] as a case-insensitive substring.
 * If [query] is blank, returns all songs unchanged.
 * Preserves the input order (caller is responsible for sorting before passing in).
 */
fun filterSongs(songs: List<SongEntry>, query: String): List<SongEntry> {
    if (query.isBlank()) return songs
    val q = query.lowercase()
    return songs.filter { song ->
        song.artist?.lowercase()?.contains(q) == true ||
            song.album?.lowercase()?.contains(q) == true ||
            song.title?.lowercase()?.contains(q) == true
    }
}
