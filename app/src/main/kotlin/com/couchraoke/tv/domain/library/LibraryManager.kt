package com.couchraoke.tv.domain.library

import com.couchraoke.tv.data.network.ConnectedPhone
import kotlinx.coroutines.flow.StateFlow

interface LibraryManager {
    val songs: StateFlow<List<IndexedSong>>

    suspend fun onPhoneConnected(phone: ConnectedPhone)

    fun onPhoneDisconnected(clientId: String)

    suspend fun refreshPhone(clientId: String)

    suspend fun refreshAll()

    fun getSong(songId: String): IndexedSong?
}
