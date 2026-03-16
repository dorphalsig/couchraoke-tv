package com.couchraoke.tv.domain.library

interface SongLibrary {
    fun addPhone(clientId: String, entries: List<SongEntry>)
    fun removePhone(clientId: String)
    fun getSortedSongs(): List<SongEntry>
    fun getSongById(songId: String): SongEntry?
    fun getSongsByPhone(clientId: String): List<SongEntry>
}

class DefaultSongLibrary : SongLibrary {
    private val phoneEntries = LinkedHashMap<String, List<SongEntry>>()

    override fun addPhone(clientId: String, entries: List<SongEntry>) {
        phoneEntries[clientId] = entries
    }

    override fun removePhone(clientId: String) {
        phoneEntries.remove(clientId)
    }

    override fun getSortedSongs(): List<SongEntry> =
        phoneEntries.values.flatten().sortedWith(
            compareBy(
                { it.artist.orEmpty().lowercase() },
                { it.album.orEmpty().lowercase() },
                { it.title.orEmpty().lowercase() },
            )
        )

    override fun getSongById(songId: String): SongEntry? =
        phoneEntries.values.flatten().firstOrNull { it.songId == songId }

    override fun getSongsByPhone(clientId: String): List<SongEntry> =
        phoneEntries[clientId] ?: emptyList()
}