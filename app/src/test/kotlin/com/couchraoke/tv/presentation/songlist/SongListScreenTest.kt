package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun rendersSongListHeaderRailActionsAndSongCard() {
        composeRule.setContent {
            CouchraokeTheme {
                SongListScreen(
                    state = SongListState(
                        visibleSongs = listOf(SoloSingFixtures.indexedSong()),
                        randomDuetEnabled = false,
                        randomMedleyEnabled = false,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("songlist-background").assertIsDisplayed()
        composeRule.onNodeWithText("Join").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Random Song").assertIsDisplayed()
        composeRule.onNodeWithTag("song-card-${SoloSingFixtures.SongId}").assertIsDisplayed()
        composeRule.onNodeWithTag("songlist-preview-pane").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun rendersJoinOverlayWhenStateRequestsIt() {
        composeRule.setContent {
            CouchraokeTheme {
                SongListScreen(
                    state = SongListState(
                        visibleSongs = listOf(SoloSingFixtures.indexedSong()),
                        randomDuetEnabled = false,
                        randomMedleyEnabled = false,
                        joinOverlay = JoinOverlayState(
                            qrPayload = SoloSingFixtures.joinQrPayload(),
                            joinCode = SoloSingFixtures.JoinCode,
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("join-overlay-scrim").assertIsDisplayed()
        composeRule.onNodeWithTag("join-overlay-qr-panel").assertIsDisplayed()
        composeRule.onNodeWithText(SoloSingFixtures.JoinCode).assertIsDisplayed()
    }
}
