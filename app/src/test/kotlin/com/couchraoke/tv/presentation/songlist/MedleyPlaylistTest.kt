package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MedleyPlaylistTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `given canMedley=true song, when onSongLongPressed, then medleyPlaylist contains song`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val song = testSong("s1", canMedley = true)

            vm.onSongLongPressed(song)

            assertEquals(listOf(song), vm.uiState.value.medleyPlaylist)
        }

    @Test
    fun `given canMedley=false song, when onSongLongPressed, then errorModal set`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val song = testSong("s1", canMedley = false)

            vm.onSongLongPressed(song)

            assertTrue(vm.uiState.value.medleyPlaylist.isEmpty())
            assertNotNull(vm.uiState.value.errorModal)
            assertEquals("Cannot add to medley", vm.uiState.value.errorModal?.title)
        }

    @Test
    fun `given playlist has 3 songs, when onPlaylistRowLongPressed(1), then playlist has 2 songs`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val s1 = testSong("s1")
            val s2 = testSong("s2")
            val s3 = testSong("s3")
            vm.onSongLongPressed(s1)
            vm.onSongLongPressed(s2)
            vm.onSongLongPressed(s3)

            vm.onPlaylistRowLongPressed(1)

            assertEquals(listOf(s1, s3), vm.uiState.value.medleyPlaylist)
        }

    @Test
    fun `given reorder started at index 2, when onReorderConfirm(2,0), then song moved to top`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val s1 = testSong("s1")
            val s2 = testSong("s2")
            val s3 = testSong("s3")
            vm.onSongLongPressed(s1)
            vm.onSongLongPressed(s2)
            vm.onSongLongPressed(s3)

            vm.onPlaylistRowSelected(2)
            assertEquals(2, vm.uiState.value.isReorderingMedleyIndex)

            vm.onReorderConfirm(2, 0)

            assertEquals(listOf(s3, s1, s2), vm.uiState.value.medleyPlaylist)
            assertNull(vm.uiState.value.isReorderingMedleyIndex)
        }

    @Test
    fun `given reorder started, when onReorderCancel, then playlist unchanged`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val s1 = testSong("s1")
            val s2 = testSong("s2")
            vm.onSongLongPressed(s1)
            vm.onSongLongPressed(s2)

            vm.onPlaylistRowSelected(0)
            vm.onReorderCancel()

            assertEquals(listOf(s1, s2), vm.uiState.value.medleyPlaylist)
            assertNull(vm.uiState.value.isReorderingMedleyIndex)
        }

    @Test
    fun `given playlist non-empty, when clearMedleyPlaylist called, then playlist empty`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            vm.onSongLongPressed(testSong("s1"))

            vm.clearMedleyPlaylist()

            assertTrue(vm.uiState.value.medleyPlaylist.isEmpty())
        }

    private fun testSong(songId: String, canMedley: Boolean = true, isDuet: Boolean = false) = SongEntry(
        songId = songId, phoneClientId = "p1", relativeTxtPath = "t.txt",
        modifiedTimeMs = 0L, isValid = true, txtUrl = "http://x",
        title = "Song $songId", artist = "Artist", canMedley = canMedley, isDuet = isDuet,
        audioUrl = "http://audio",
    )
}
