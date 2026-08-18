package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongListLayoutTest {
    @Test(timeout = 30_000)
    fun compactModeActivatesBelow1200DpBreakpoint() {
        assertTrue(isCompactSongListLayout(960.dp))
        assertFalse(isCompactSongListLayout(1200.dp))
    }

    @Test(timeout = 30_000)
    fun compactGridUsesThreeColumnsBelowCompactBreakpoint() {
        assertEquals(3, songListGridColumns(960.dp))
        assertEquals(3, songListGridColumns(1199.dp))
    }

    @Test(timeout = 30_000)
    fun gridUsesFourColumnsAtCompactBreakpointAndAbove() {
        assertEquals(4, songListGridColumns(1200.dp))
        assertEquals(4, songListGridColumns(1920.dp))
    }
}
