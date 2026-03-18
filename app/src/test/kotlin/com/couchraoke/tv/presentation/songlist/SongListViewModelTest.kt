package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.session.SessionEvent
import com.couchraoke.tv.domain.session.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SongListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `given empty library, when ViewModel created, then allSongs is empty`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        assertEquals(emptyList<Any>(), vm.uiState.value.allSongs)
    }

    @Test
    fun `given empty library, when ViewModel created, then filteredSongs is empty`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        assertEquals(emptyList<Any>(), vm.uiState.value.filteredSongs)
    }

    @Test
    fun `given empty library, when ViewModel created, then joinToken is non-empty`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        assertTrue(vm.uiState.value.joinToken.isNotEmpty())
    }

    @Test
    fun `given empty library, when ViewModel created, then sessionState is Open`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        assertEquals(SessionState.Open, vm.uiState.value.sessionState)
    }

    @Test
    fun `given Session emits PhoneConnected, when observed, then allSongs updated`() = runTest(testDispatcher) {
        val song = testSongEntry("s1", "phone1")
        val library = FakeSongLibrary(listOf(song))
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        session.emit(SessionEvent.PhoneConnected("phone1", "Phone 1"))
        testScheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.allSongs.size)
    }

    @Test
    fun `given searchQuery non-empty, when onBackPressed called, then searchQuery cleared`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        vm.onSearchQueryChanged("test")
        advanceUntilIdle()
        assertEquals("test", vm.uiState.value.searchQuery)

        val consumed = vm.onBackPressed()
        assertTrue(consumed)
        assertEquals("", vm.uiState.value.searchQuery)
    }

    @Test
    fun `given searchQuery empty, when onBackPressed called, then returns false`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        val consumed = vm.onBackPressed()
        assertFalse(consumed)
    }

    @Test
    fun `given Session with token, when ViewModel created, then joinToken formatted`() = runTest(testDispatcher) {
        val token = "ABCDEFGHIJ"
        val session = FakeSession(token = token)
        val library = FakeSongLibrary()
        val vm = SongListViewModel(library, session, testDispatcher)
        assertTrue(vm.uiState.value.joinToken.contains("-"))
        assertTrue(vm.uiState.value.joinToken.isNotEmpty())
    }

    @Test
    fun `given song focused, when 500ms advance, then preview starts`() = runTest(testDispatcher) {
        val song = testSongEntry("s1", "p1")
        val library = FakeSongLibrary(listOf(song))
        val session = FakeSession()
        val controller = SongPreviewControllerTest.FakePreviewController()
        val vm = SongListViewModel(library, session, testDispatcher, controller)

        vm.onSongFocused("s1")
        testScheduler.advanceTimeBy(400)
        assertEquals(0, controller.startPreviewCalledCount)

        testScheduler.advanceTimeBy(101)
        assertEquals(1, controller.startPreviewCalledCount)
        assertEquals("s1", controller.lastSong?.songId)
    }

    @Test
    fun `given song focused then different song focused before 500ms, when advanced, then second starts`() =
        runTest(testDispatcher) {
            val s1 = testSongEntry("s1", "p1")
            val s2 = testSongEntry("s2", "p1")
            val library = FakeSongLibrary(listOf(s1, s2))
            val session = FakeSession()
            val controller = SongPreviewControllerTest.FakePreviewController()
            val vm = SongListViewModel(library, session, testDispatcher, controller)

            vm.onSongFocused("s1")
            testScheduler.advanceTimeBy(300)
            vm.onSongFocused("s2")
            testScheduler.advanceTimeBy(300)
            assertEquals(0, controller.startPreviewCalledCount)

            testScheduler.advanceTimeBy(201)
            assertEquals(1, controller.startPreviewCalledCount)
            assertEquals("s2", controller.lastSong?.songId)
        }

    @Test
    fun `given onScreenVisible false, when called, then preview stops`() = runTest(testDispatcher) {
        val library = FakeSongLibrary()
        val session = FakeSession()
        val controller = SongPreviewControllerTest.FakePreviewController()
        val vm = SongListViewModel(library, session, testDispatcher, controller)

        vm.onScreenVisible(false)
        assertEquals(1, controller.stopPreviewCalledCount)
    }

    @Test
    fun `given filteredSongs has 5 songs, when onRandomSong, then selectPlayersDialog opened`() =
        runTest(testDispatcher) {
            val s1 = testSongEntry("s1", "p1")
            val library = FakeSongLibrary(listOf(s1))
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            session.emit(SessionEvent.PhoneConnected("p1", "Phone 1"))
            testScheduler.advanceUntilIdle()

            vm.onRandomSong()

            assertNotNull(vm.uiState.value.selectPlayersDialog)
            assertEquals(s1.songId, vm.uiState.value.selectPlayersDialog?.song?.songId)
        }

    @Test
    fun `given filteredSongs has duets, when onRandomDuet, then dialog song isDuet true`() =
        runTest(testDispatcher) {
            val s1 = testSongEntry("s1", "p1", isDuet = true)
            val library = FakeSongLibrary(listOf(s1))
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            session.emit(SessionEvent.PhoneConnected("p1", "Phone 1"))
            testScheduler.advanceUntilIdle()

            vm.onRandomDuet()

            assertNotNull(vm.uiState.value.selectPlayersDialog)
            assertTrue(vm.uiState.value.selectPlayersDialog?.song?.isDuet == true)
        }

    @Test
    fun `given filteredSongs has no duets, when onRandomDuet, then errorModal set`() = runTest(testDispatcher) {
        val s1 = testSongEntry("s1", "p1", isDuet = false)
        val library = FakeSongLibrary(listOf(s1))
        val session = FakeSession()
        val vm = SongListViewModel(library, session, testDispatcher)
        session.emit(SessionEvent.PhoneConnected("p1", "Phone 1"))
        testScheduler.advanceUntilIdle()

        vm.onRandomDuet()

        assertNull(vm.uiState.value.selectPlayersDialog)
        assertNotNull(vm.uiState.value.errorModal)
        assertEquals("No duets available", vm.uiState.value.errorModal?.title)
    }

    @Test
    fun `T3-4-1 given no phones connected, when ViewModel created, then allSongs and filteredSongs are empty`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            assertTrue(vm.uiState.value.allSongs.isEmpty())
            assertTrue(vm.uiState.value.filteredSongs.isEmpty())
        }

    @Test
    fun `T3-4-2 given phone connected but library empty, when PhoneConnected emitted, then filteredSongs remains empty`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            session.emit(SessionEvent.PhoneConnected("p1", "Phone 1"))
            advanceUntilIdle()
            assertTrue(vm.uiState.value.filteredSongs.isEmpty())
        }

    @Test
    fun `T3-4-3 given canMedley=false song, when onSongLongPressed, then errorModal bodyLine1 matches spec text`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val song = com.couchraoke.tv.domain.library.SongEntry(
                songId = "s1",
                phoneClientId = "p1",
                relativeTxtPath = "test.txt",
                modifiedTimeMs = 0L,
                isValid = true,
                txtUrl = "http://example.com/test.txt",
                artist = "Test Artist",
                title = "Test Title",
                canMedley = false,
            )
            vm.onSongLongPressed(song)
            assertEquals("This song can't be used in a medley.", vm.uiState.value.errorModal?.bodyLine1)
            assertEquals(
                "Look for songs with an M tag in the lower right corner",
                vm.uiState.value.errorModal?.bodyLine2,
            )
        }

    @Test
    fun `T3-4-4 given empty medley playlist, when onPlayMedley called, then selectPlayersDialog remains null`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            vm.onPlayMedley()
            assertNull(vm.uiState.value.selectPlayersDialog)
        }

    @Test
    fun `T3-4-5 given filter active, when onBackPressed called, then searchQuery cleared and Back consumed`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            vm.onSearchQueryChanged("test")
            advanceUntilIdle()
            val consumed = vm.onBackPressed()
            assertTrue(consumed)
            assertEquals("", vm.uiState.value.searchQuery)
        }

    @Test
    fun `T3-4-6 given no filter active, when onBackPressed called, then Back not consumed`() =
        runTest(testDispatcher) {
            val library = FakeSongLibrary()
            val session = FakeSession()
            val vm = SongListViewModel(library, session, testDispatcher)
            val consumed = vm.onBackPressed()
            assertFalse(consumed)
        }

    private fun testSongEntry(songId: String, phoneClientId: String, isDuet: Boolean = false) =
        com.couchraoke.tv.domain.library.SongEntry(
            songId = songId,
            phoneClientId = phoneClientId,
            relativeTxtPath = "test.txt",
            modifiedTimeMs = 0L,
            isValid = true,
            txtUrl = "http://example.com/test.txt",
            artist = "Test Artist",
            title = "Test Title",
            isDuet = isDuet
        )
}
