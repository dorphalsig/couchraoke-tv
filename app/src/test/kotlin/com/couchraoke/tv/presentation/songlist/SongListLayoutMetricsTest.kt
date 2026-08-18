package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongListLayoutMetricsTest {
    @Test(timeout = 30_000)
    fun compactModeActivatesBelowCompactMaxWidthOnly() {
        assertTrue(isCompactSongListLayout(960.dp))
        assertTrue(isCompactSongListLayout(1199.dp))
        assertFalse(isCompactSongListLayout(1200.dp))
    }

    @Test(timeout = 30_000)
    fun compactColumnsFollowBreakpointRule() {
        assertEquals(3, songListGridColumns(960.dp))
        assertEquals(3, songListGridColumns(1199.dp))
    }

    @Test(timeout = 30_000)
    fun wideColumnsApplyAtCompactBreakpointAndAbove() {
        assertEquals(4, songListGridColumns(1200.dp))
        assertEquals(4, songListGridColumns(1920.dp))
    }
}
