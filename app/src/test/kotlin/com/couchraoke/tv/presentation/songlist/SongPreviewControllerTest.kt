package com.couchraoke.tv.presentation.songlist

import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.presentation.songlist.preview.ISongPreviewController
import org.junit.Assert.assertEquals
import org.junit.Test

class SongPreviewControllerTest {

    class FakePreviewController : ISongPreviewController {
        var startPreviewCalledCount = 0
        var stopPreviewCalledCount = 0
        var releaseCalledCount = 0
        var lastSong: SongEntry? = null

        override fun startPreview(song: SongEntry) {
            startPreviewCalledCount++
            lastSong = song
        }

        override fun stopPreview() {
            stopPreviewCalledCount++
        }

        override fun release() {
            releaseCalledCount++
        }
    }

    @Test
    fun `given fake controller, when startPreview, then record call`() {
        val controller = FakePreviewController()
        val song = testSongEntry("s1")

        controller.startPreview(song)

        assertEquals(1, controller.startPreviewCalledCount)
        assertEquals(song, controller.lastSong)
    }

    @Test
    fun `given fake controller, when stopPreview, then record call`() {
        val controller = FakePreviewController()

        controller.stopPreview()

        assertEquals(1, controller.stopPreviewCalledCount)
    }

    @Test
    fun `given fake controller, when release, then record call`() {
        val controller = FakePreviewController()

        controller.release()

        assertEquals(1, controller.releaseCalledCount)
    }

    private fun testSongEntry(songId: String) =
        com.couchraoke.tv.domain.library.SongEntry(
            songId = songId,
            phoneClientId = "p1",
            relativeTxtPath = "test.txt",
            modifiedTimeMs = 0L,
            isValid = true,
            txtUrl = "http://example.com/test.txt",
            artist = "Test Artist",
            title = "Test Title"
        )
}
