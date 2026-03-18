package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectPlayersGatingTest {

    private val dispatcher = StandardTestDispatcher()
    private val library = FakeSongLibrary()

    private fun testSong(
        songId: String,
        isDuet: Boolean = false,
        audioUrl: String? = "http://audio",
    ) = SongEntry(
        songId = songId,
        phoneClientId = "c1",
        relativeTxtPath = "test.txt",
        modifiedTimeMs = 0L,
        isValid = true,
        txtUrl = "http://example.com/test.txt",
        title = "Title $songId",
        artist = "Artist",
        isDuet = isDuet,
        audioUrl = audioUrl,
    )

    private val soloSong = testSong("s1", isDuet = false)
    private val duetSong = testSong("d1", isDuet = true)
    private val songNoAudio = testSong("n1", audioUrl = null)

    @Test
    fun `given non-duet song selected, when onSongSelected, then dialog opened with SingleSong mode`() =
        runTest(dispatcher) {
            val session = FakeSession(
                connectedClientIds = setOf("c1"),
                displayNames = mapOf("c1" to "Phone 1"),
            )
            val viewModel = SongListViewModel(library, session, dispatcher)

            viewModel.onSongSelected(soloSong)

            val dialog = viewModel.uiState.value.selectPlayersDialog
            assertNotNull(dialog)
            assertEquals(SelectPlayersMode.SingleSong(soloSong), dialog?.mode)
            assertEquals(false, dialog?.song?.isDuet)
        }

    @Test
    fun `given duet song, when onSongSelected, then dialog opened with isDuet true`() =
        runTest(dispatcher) {
            val session = FakeSession(
                connectedClientIds = setOf("c1"),
                displayNames = mapOf("c1" to "Phone 1"),
            )
            val viewModel = SongListViewModel(library, session, dispatcher)

            viewModel.onSongSelected(duetSong)

            val dialog = viewModel.uiState.value.selectPlayersDialog
            assertNotNull(dialog)
            assertEquals(true, dialog?.song?.isDuet)
        }

    @Test
    fun `given no phones connected, when onSongSelected, then dialog shows empty availablePhones`() =
        runTest(dispatcher) {
            val session = FakeSession(connectedClientIds = emptySet(), displayNames = emptyMap())
            val viewModel = SongListViewModel(library, session, dispatcher)

            viewModel.onSongSelected(soloSong)

            val dialog = viewModel.uiState.value.selectPlayersDialog
            assertNotNull(dialog)
            assertEquals(0, dialog?.availablePhones?.size)
        }

    @Test
    fun `given song with audioUrl=null, when onSelectPlayersStart, then dialog cleared and errorModal set`() =
        runTest(dispatcher) {
            val session = FakeSession(
                connectedClientIds = setOf("c1"),
                displayNames = mapOf("c1" to "Phone 1"),
            )
            val viewModel = SongListViewModel(library, session, dispatcher)

            viewModel.onSongSelected(songNoAudio)
            assertNotNull(viewModel.uiState.value.selectPlayersDialog)

            viewModel.onSelectPlayersStart()

            assertNull(viewModel.uiState.value.selectPlayersDialog)
            val error = viewModel.uiState.value.errorModal
            assertNotNull(error)
            assertEquals("Song unavailable", error?.title)
        }
}
