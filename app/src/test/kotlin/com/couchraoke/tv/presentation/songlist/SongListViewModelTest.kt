package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongListViewModelTest {
    @Test(timeout = 30_000)
    fun searchesArtistAlbumAndTitleCaseInsensitiveAfterDebounce() = runBlocking {
        val songs = listOf(
            SoloSingFixtures.indexedSong(
                entry = SoloSingFixtures.songEntry(title = "Blue Sky", artist = "Demo Artist", album = "Clouds"),
            ),
            SoloSingFixtures.indexedSong(
                songId = "2",
                entry = SoloSingFixtures.songEntry(title = "Red Sun", artist = "Other", album = "Fire"),
            ),
        )
        val viewModel = SongListViewModel(songs = songs)

        viewModel.updateSearch("cloud")
        viewModel.advanceSearchDebounce(149)
        assertEquals(2, viewModel.state.value.visibleSongs.size)

        viewModel.advanceSearchDebounce(1)
        assertEquals(listOf("Blue Sky"), viewModel.state.value.visibleSongs.map { it.title })
    }

    @Test(timeout = 30_000)
    fun exposesTextInputLaunchRequestOnSearchOkWithoutAndroidTypes() {
        val viewModel = SongListViewModel(songs = listOf(SoloSingFixtures.indexedSong()))

        viewModel.onEvent(SongListEvent.SearchOk)

        assertEquals("", viewModel.state.value.textInputRequest?.initialValue)
    }

    @Test(timeout = 30_000)
    fun showsRequiredEmptyStateCopy() {
        val noPhones = SongListViewModel(songs = emptyList(), connectedPhoneCount = 0)
        val noSongs = SongListViewModel(songs = emptyList(), connectedPhoneCount = 1)

        assertEquals("No phones connected.", noPhones.state.value.emptyState?.title)
        assertEquals(
            "Connect a phone to see songs. Open the karaoke app on your phone and scan the QR code.",
            noPhones.state.value.emptyState?.body,
        )
        assertEquals("No songs found.", noSongs.state.value.emptyState?.title)
        assertEquals(
            "Open the karaoke app on your phone and make sure the songs folder is set.",
            noSongs.state.value.emptyState?.body,
        )
    }

    @Test(timeout = 30_000)
    fun keepsOutOfScopeDuetMedleyAndSettingsAffordancesVisibleButInert() {
        val viewModel = SongListViewModel(songs = listOf(SoloSingFixtures.indexedSong()))

        viewModel.onEvent(SongListEvent.Inert(SongListInertAction.Settings))
        viewModel.onEvent(SongListEvent.Inert(SongListInertAction.RandomDuet))
        viewModel.onEvent(SongListEvent.Inert(SongListInertAction.RandomMedley))

        assertFalse(viewModel.state.value.randomDuetEnabled)
        assertFalse(viewModel.state.value.randomMedleyEnabled)
        assertNull(viewModel.state.value.openedRoute)
    }

    @Test(timeout = 30_000)
    fun backClearsFilterBeforeExit() = runBlocking {
        val viewModel = SongListViewModel(songs = listOf(SoloSingFixtures.indexedSong()))
        viewModel.updateSearch("demo")
        viewModel.advanceSearchDebounce(150)

        assertFalse(viewModel.onBack())
        assertEquals("", viewModel.state.value.searchQuery)
        assertTrue(viewModel.onBack())
    }

    @Test(timeout = 30_000)
    fun previewDebouncesFocusedTileAndStopsOnTransitions() {
        val song = SoloSingFixtures.indexedSong(entry = SoloSingFixtures.songEntry(previewStartSec = 0f))
        val viewModel = SongListViewModel(songs = listOf(song))

        viewModel.onSongFocused(song.songId)
        viewModel.tick(499)
        assertNull(viewModel.state.value.preview)

        viewModel.tick(1)
        assertEquals(song.audioUrl, viewModel.state.value.preview?.audioUrl)
        assertEquals(0L, viewModel.state.value.preview?.startPositionMs)

        viewModel.onEvent(SongListEvent.OverlayOpened)
        assertNull(viewModel.state.value.preview)
    }

    @Test(timeout = 30_000)
    fun previewUsesPositivePreviewStartAndSilentFailureState() {
        val song = SoloSingFixtures.indexedSong(entry = SoloSingFixtures.songEntry(previewStartSec = 12f))
        val viewModel = SongListViewModel(songs = listOf(song))

        viewModel.onSongFocused(song.songId)
        viewModel.tick(500)
        viewModel.onEvent(SongListEvent.PreviewFailure)

        assertEquals(12_000L, viewModel.state.value.preview?.startPositionMs)
        assertNull(viewModel.state.value.previewError)
    }

    @Test(timeout = 30_000)
    fun exposesFocusedSongPreviewMetadataUntilGridFocusLeaves() {
        val song = SoloSingFixtures.indexedSong()
        val viewModel = SongListViewModel(songs = listOf(song))

        viewModel.onSongFocused(song.songId)
        assertEquals(song, viewModel.state.value.focusedSong)

        viewModel.onEvent(SongListEvent.GridFocusLost)
        assertNull(viewModel.state.value.focusedSong)
    }

    @Test(timeout = 30_000)
    fun focusLeavingGridAndScreenExitStopPreviewAndReleaseScreenScopedPreview() {
        val song = SoloSingFixtures.indexedSong()
        val viewModel = SongListViewModel(songs = listOf(song))

        viewModel.onSongFocused(song.songId)
        viewModel.tick(500)
        viewModel.onEvent(SongListEvent.GridFocusLost)
        assertNull(viewModel.state.value.preview)

        viewModel.onSongFocused(song.songId)
        viewModel.tick(500)
        viewModel.onEvent(SongListEvent.ScreenExit)
        assertNull(viewModel.state.value.preview)
        assertTrue(viewModel.state.value.previewReleased)
    }

    @Test(timeout = 30_000)
    fun longPressRejectsNonMedleySongWithRequiredBlockingCopy() {
        val song = SoloSingFixtures.indexedSong(entry = SoloSingFixtures.songEntry(canMedley = false))
        val viewModel = SongListViewModel(songs = listOf(song))

        viewModel.onEvent(SongListEvent.SongLongPressed(song.songId))

        assertEquals(
            "This song can't be used in a medley. Look for songs with an M tag in the lower right corner",
            viewModel.state.value.blockingModal?.body,
        )
    }

    @Test(timeout = 30_000)
    fun backFromGridOrLeftPanelMovesFocusToSearchBeforeExit() {
        val viewModel = SongListViewModel(songs = listOf(SoloSingFixtures.indexedSong()))

        viewModel.onEvent(SongListEvent.FocusAreaChanged(SongListFocusArea.Grid))

        assertFalse(viewModel.onBack())
        assertEquals(SongListFocusArea.Search, viewModel.state.value.focusArea)
    }

    @Test(timeout = 30_000)
    fun joinStateUsesFullWebSocketEndpointPayload() {
        val viewModel = SongListViewModel(
            songs = listOf(SoloSingFixtures.indexedSong()),
            joinEndpointUrl = SoloSingFixtures.joinQrPayload(),
            joinCode = SoloSingFixtures.JoinCode,
        )

        viewModel.onEvent(SongListEvent.Join)

        assertEquals(SoloSingFixtures.joinQrPayload(), viewModel.state.value.joinOverlay?.qrPayload)
        assertEquals(SoloSingFixtures.JoinCode, viewModel.state.value.joinOverlay?.joinCode)
    }
}
