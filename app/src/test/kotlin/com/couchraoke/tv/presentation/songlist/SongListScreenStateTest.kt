package com.couchraoke.tv.presentation.songlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.couchraoke.tv.presentation.songlist.fixtures.SongListScreenFixtures
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28], qualifiers = "w1280dp-h720dp-land-television-hdpi")
class SongListScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_emptyState_when_rendered_then_matchesBaseline() {
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_noSongsState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.noSongsState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_populatedState_when_rendered_then_matchesBaseline() {
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_filteredHitState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.filteredHitState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_filteredEmptyState_when_rendered_then_matchesBaseline() {
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_selectPlayersNonDuetState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.selectPlayersNonDuetState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_selectPlayersDuetBothState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.selectPlayersDuetBothState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_selectPlayersDuetSoloState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.selectPlayersDuetSoloState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_noPhonesBlockingState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.noPhonesBlockingState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_errorModalState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.errorModalState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_medleyVisibleState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.medleyVisibleState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_medleyReorderState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.medleyReorderState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_pairingOverlayState_when_rendered_then_matchesBaseline() {
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun given_previewPaneFocusedSongState_when_rendered_then_matchesBaseline() {
        composeRule.setContent {
            SongListScreenContent(
                state = SongListScreenFixtures.previewPaneFocusedSongState(),
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
                onErrorModalDismissed = {}
            )
        }
        composeRule.onRoot().captureRoboImage()
    }
}
