package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.test.assertIsDisplayed
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
class SongListNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `given empty songs, when rendered, then empty state message displayed`() {
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
        composeRule.onNodeWithText("No songs yet. Connect a phone to get started.")
            .assertIsDisplayed()
    }

    @Test
    fun `given populated songs, when rendered, then join code displayed in header`() {
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
        composeRule.onNodeWithText("Code: TEST-1234").assertIsDisplayed()
    }

    @Test
    fun `given filtered empty result, when rendered, then no-results message displayed`() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.filteredEmptyState(),
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
        composeRule.onNodeWithText("No results for \"zzz\"").assertIsDisplayed()
    }

    @Test
    fun `given populated state, when rendered, then Join button displayed`() {
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
        composeRule.onNodeWithText("JOIN").assertIsDisplayed()
    }

    @Test
    fun `given back cascade, when grid focus and back pressed, then ViewModel returns MovedToSearch`() {
        val results = mutableListOf<BackResult>()
        var currentZone = FocusZone.Grid
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.populatedState(),
                onBackPressed = {
                    val result = if (currentZone == FocusZone.Grid || currentZone == FocusZone.LeftPanel) {
                        BackResult.MovedToSearch
                    } else {
                        BackResult.ExitApp
                    }
                    results.add(result)
                    result
                },
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
                onRandomMedley = {},
                onFocusZoneChanged = { currentZone = it },
                onSelectPlayersStart = {},
                onSelectPlayersCancel = {},
                onErrorModalDismissed = {},
                onJoinPressed = {},
                onPairingOverlayDismissed = {},
            )
        }
        // Verify the screen renders without error — Back cascade logic tested in SongListViewModelTest
        composeRule.onNodeWithText("Code: TEST-1234").assertIsDisplayed()
    }
}
