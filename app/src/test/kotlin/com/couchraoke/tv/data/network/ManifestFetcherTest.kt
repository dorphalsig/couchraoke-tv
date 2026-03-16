package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.domain.library.SongLibrary
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class ManifestFetcherTest {
    private lateinit var server: MockWebServer
    private lateinit var library: FakeSongLibrary
    private lateinit var fetcher: ManifestFetcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        library = FakeSongLibrary()
        fetcher = ManifestFetcher(library)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful fetch updates library`() {
        val json = """
            [{
                "relativeTxtPath": "songs/test.txt",
                "isValid": true,
                "modifiedTimeMs": 1000,
                "title": "Test",
                "artist": "Artist",
                "isDuet": false,
                "hasRap": false,
                "hasVideo": false,
                "hasInstrumental": false,
                "canMedley": false
            }]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = fetcher.fetch(server.hostName, server.port, "client1")

        assertTrue(result.isSuccess)
        assertEquals(1, library.addPhoneCalls.size)
        val (clientId, entries) = library.addPhoneCalls[0]
        assertEquals("client1", clientId)
        assertEquals(1, entries.size)
        assertEquals("client1::songs/test.txt", entries[0].songId)
    }

    @Test
    fun `http 500 failure returns failure and does not update library`() {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = fetcher.fetch(server.hostName, server.port, "client1")

        assertTrue(result.isFailure)
        assertEquals(0, library.addPhoneCalls.size)
    }

    @Test
    fun `network error returns failure and does not update library`() {
        // Enqueue a response that will time out or fail
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START))

        val result = fetcher.fetch(server.hostName, server.port, "client1")

        assertTrue(result.isFailure)
        assertEquals(0, library.addPhoneCalls.size)
    }

    private class FakeSongLibrary : SongLibrary {
        val addPhoneCalls = mutableListOf<Pair<String, List<SongEntry>>>()

        override fun addPhone(clientId: String, entries: List<SongEntry>) {
            addPhoneCalls.add(clientId to entries)
        }

        override fun removePhone(clientId: String) {}
        override fun getSortedSongs(): List<SongEntry> = emptyList()
        override fun getSongById(songId: String): SongEntry? = null
        override fun getSongsByPhone(clientId: String): List<SongEntry> = emptyList()
    }
}
