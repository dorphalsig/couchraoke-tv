package com.couchraoke.tv.presentation.selectplayers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectPlayersModalTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun rendersRequiredSinglePlayerModalContent() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = listOf(phone()),
        )

        composeRule.setContent {
            CouchraokeTheme {
                SelectPlayersModal(
                    state = viewModel.state.value,
                    onStart = {},
                    onCancel = {},
                )
            }
        }

        composeRule.onNodeWithText("SELECT PLAYERS").assertIsDisplayed()
        composeRule.onNodeWithText("${SoloSingFixtures.SongArtist} — ${SoloSingFixtures.SongTitle}").assertIsDisplayed()
        composeRule.onNodeWithText("Player 1 (required)").assertIsDisplayed()
        composeRule.onNodeWithText("Player 2").assertIsDisplayed()
        composeRule.onNodeWithText(SoloSingFixtures.PhoneClientId).assertIsDisplayed()
        composeRule.onNodeWithText("Medium").assertIsDisplayed()
    }

    private fun phone() = com.couchraoke.tv.data.network.ConnectedPhone(
        clientId = SoloSingFixtures.PhoneClientId,
        connectionId = SoloSingFixtures.PhoneConnectionId,
        deviceName = SoloSingFixtures.PhoneDeviceName,
        httpPort = SoloSingFixtures.PhoneHttpPort,
        ipAddress = SoloSingFixtures.PhoneIpAddress,
    )
}
