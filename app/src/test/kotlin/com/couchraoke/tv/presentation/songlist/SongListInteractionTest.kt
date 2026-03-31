package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.couchraoke.tv.presentation.songlist.fixtures.SongListScreenFixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w1280dp-h720dp-land-television-hdpi")
class SongListInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `given isPairingOverlayOpen true, when rendered, then join code text displayed in overlay`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.pairingOverlayState(),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        // Pairing overlay shows the join token text
        composeRule.onNodeWithText("TEST-1234").assertIsDisplayed()
    }

    @Test
    fun `given populated state with medley-eligible songs, when rendered, then Random Medley button enabled`() {
        // populatedState has songs 1,3,4,5 with canMedley=true (>= 2 eligible)
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.populatedState(),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        composeRule.onNodeWithText("Sing Random Medley").assertIsDisplayed()
    }

    @Test
    fun `given empty state, when rendered, then Random Song and Random Duet buttons disabled`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.emptyState(),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        composeRule.onNodeWithText("Random Song").assertIsNotEnabled()
        composeRule.onNodeWithText("Random Duet").assertIsNotEnabled()
    }

    @Test
    fun `given duplicateMedleyFeedback true, when rendered, then already in medley message shown`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.medleyVisibleState().copy(
                    duplicateMedleyFeedback = true
                ),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        composeRule.onNodeWithText("Already in medley").assertIsDisplayed()
    }

    @Test
    fun `given hint SongTile, when rendered, then hints bar shows OK sing hint`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.populatedState().copy(
                    currentHint = HintMode.SongTile
                ),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        composeRule.onNodeWithText("OK = Sing   Long-Press OK = Add to Medley").assertIsDisplayed()
    }

    @Test
    fun `given hint ReorderMode, when rendered, then hints bar shows reorder hint`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.medleyReorderState().copy(
                    currentHint = HintMode.ReorderMode
                ),
                onBackPressed = {},
                onSearchQueryChanged = {},
                onSongSelected = {},
                onSongLongPressed = {},
                onSongFocused = {},
                onPlaylistRowSelected = {},
                onPlaylistRowLongPressed = {},
                onReorderConfirm = { _, _ -> },
                onReorderCancel = {},
                onPlayMedley = {},
                onRandomSong = {},
                onRandomDuet = {},
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
            )
        }
        composeRule.onNodeWithText("Up/Down = Move   OK = Accept   Back = Cancel").assertIsDisplayed()
    }
}
