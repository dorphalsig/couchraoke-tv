package com.couchraoke.tv.data.network

import android.util.Log
import com.couchraoke.tv.domain.library.ManifestEntry
import com.couchraoke.tv.domain.library.SongIndexer
import com.couchraoke.tv.domain.library.SongLibrary
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class ManifestFetcher(
    private val library: SongLibrary,
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches /manifest.json from phone's HTTP server, parses entries, updates library.
     * On failure: retains prior catalog for this phone (does NOT call addPhone) and returns Result.failure.
     */
    fun fetch(phoneIp: String, httpPort: Int, clientId: String): Result<Unit> {
        val url = "http://$phoneIp:$httpPort/manifest.json"
        return try {
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Manifest fetch failed: HTTP ${response.code} for $clientId")
                return Result.failure(Exception("HTTP ${response.code}"))
            }
            val body = response.body?.string()
                ?: return Result.failure(Exception("Empty body"))
            val entries = json.decodeFromString<List<ManifestEntry>>(body)
            val songEntries = entries.map { SongIndexer.fromManifestEntry(it, clientId) }
            library.addPhone(clientId, songEntries)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Manifest fetch error for $clientId: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ManifestFetcher"
    }
}
