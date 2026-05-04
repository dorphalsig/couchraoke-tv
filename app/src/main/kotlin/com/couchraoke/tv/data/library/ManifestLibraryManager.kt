package com.couchraoke.tv.data.library

import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.data.network.toIndexedSong
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.library.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ManifestLibraryManager(
    private val networkController: NetworkController,
) : LibraryManager {
    private val mutableSongs = MutableStateFlow<List<IndexedSong>>(emptyList())

    override val songs: StateFlow<List<IndexedSong>> = mutableSongs

    override fun getSong(songId: String): IndexedSong? = songs.value.firstOrNull { it.songId == songId }

    suspend fun refreshFromConnectedPhones() {
        mutableSongs.value = networkController.connectedPhones.value
            .flatMap { phone ->
                networkController.fetchManifest(phone).getOrElse { emptyList() }
                    .filter { entry -> entry.hasRequiredFields() }
                    .map { entry -> entry.toIndexedSong(phone.clientId) }
            }
            .sortedWith(
                compareBy<IndexedSong, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album.orEmpty() }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
            )
    }

    private fun SongEntry.hasRequiredFields(): Boolean = relativeTxtPath.isNotBlank() &&
        title.isNotBlank() &&
        artist.isNotBlank() &&
        txtUrl.isNotBlank() &&
        audioUrl.isNotBlank()
}
