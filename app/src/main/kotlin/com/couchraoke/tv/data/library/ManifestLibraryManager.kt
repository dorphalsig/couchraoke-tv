package com.couchraoke.tv.data.library

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.data.network.toIndexedSong
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.library.LibraryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManifestLibraryManager(
    private val networkController: NetworkController,
    private val debugLog: (String) -> Unit = { message -> println("CouchraokeLibrary: $message") },
) : LibraryManager {
    private val connectedPhonesById = mutableMapOf<String, ConnectedPhone>()
    private val songsByPhone = mutableMapOf<String, List<IndexedSong>>()
    private val mutableSongs = MutableStateFlow<List<IndexedSong>>(emptyList())

    override val songs: StateFlow<List<IndexedSong>> = mutableSongs

    override fun getSong(songId: String): IndexedSong? = songs.value.firstOrNull { it.songId == songId }

    override suspend fun onPhoneConnected(phone: ConnectedPhone) {
        connectedPhonesById[phone.clientId] = phone
        refreshPhone(phone.clientId)
    }

    override fun onPhoneDisconnected(clientId: String) {
        connectedPhonesById.remove(clientId)
        songsByPhone.remove(clientId)
        publishSongs()
    }

    override suspend fun refreshPhone(clientId: String) {
        val phone = connectedPhonesById[clientId]
            ?: networkController.connectedPhones.value.firstOrNull { it.clientId == clientId }
            ?: return
        connectedPhonesById[clientId] = phone
        fetchPhoneCatalog(phone, stats = null)
        publishSongs()
    }

    override suspend fun refreshAll() {
        val connectedPhones = networkController.connectedPhones.value
        val connectedPhoneIds = connectedPhones.mapTo(mutableSetOf()) { it.clientId }
        connectedPhonesById.keys.retainAll(connectedPhoneIds)
        songsByPhone.keys.retainAll(connectedPhoneIds)
        connectedPhones.forEach { phone -> connectedPhonesById[phone.clientId] = phone }

        val stats = RefreshStats()
        debugLog("Refreshing catalog for ${connectedPhones.size} connected phone(s).")
        connectedPhones.forEach { phone -> fetchPhoneCatalog(phone, stats) }
        publishSongs()
        debugLog(
            "Catalog refresh accepted ${stats.acceptedCount} song(s), " +
                "rejected ${stats.rejectedCount} song(s), total visible ${mutableSongs.value.size}.",
        )
    }

    fun launchConnectedPhoneRefresh(scope: CoroutineScope): Job = scope.launch {
        networkController.connectedPhones.collect { connectedPhones ->
            val incomingPhoneIds = connectedPhones.mapTo(mutableSetOf()) { it.clientId }
            (connectedPhonesById.keys - incomingPhoneIds).forEach(::onPhoneDisconnected)
            connectedPhones.forEach { phone ->
                if (connectedPhonesById[phone.clientId] != phone) {
                    onPhoneConnected(phone)
                }
            }
        }
    }

    private suspend fun fetchPhoneCatalog(phone: ConnectedPhone, stats: RefreshStats?) {
        debugLog("Fetching manifest for ${phone.clientId} at ${phone.ipAddress}:${phone.httpPort}.")
        networkController.fetchManifest(phone)
            .onSuccess { entries ->
                val acceptedSongs = entries.mapNotNull { entry ->
                    if (entry.hasRequiredFields()) {
                        stats?.let { it.acceptedCount++ }
                        entry.toIndexedSong(phone.clientId)
                    } else {
                        stats?.let { it.rejectedCount++ }
                        debugLog(
                            "Rejected manifest entry for ${phone.clientId}: " +
                                "${entry.relativeTxtPath.ifBlank { "<blank>" }}.",
                        )
                        null
                    }
                }
                songsByPhone[phone.clientId] = acceptedSongs
            }
            .onFailure {
                val cachedCount = songsByPhone[phone.clientId].orEmpty().size
                debugLog("Manifest fetch failed for ${phone.clientId}; retaining $cachedCount cached song(s).")
            }
    }

    private fun publishSongs() {
        mutableSongs.value = songsByPhone.values.flatten().sortedForCatalog()
    }

    private fun SongEntry.hasRequiredFields(): Boolean = relativeTxtPath.isNotBlank() &&
        !relativeTxtPath.startsWith("/") &&
        relativeTxtPath.split("/").none { it == "." || it == ".." } &&
        title.isNotBlank() &&
        artist.isNotBlank() &&
        txtUrl.isNotBlank() &&
        audioUrl.isNotBlank() &&
        hasVideo == (videoUrl != null)

    private data class RefreshStats(
        var acceptedCount: Int = 0,
        var rejectedCount: Int = 0,
    )
}

private fun List<IndexedSong>.sortedForCatalog(): List<IndexedSong> = sortedWith(
    compareBy<IndexedSong, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.album.orEmpty() }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
)
