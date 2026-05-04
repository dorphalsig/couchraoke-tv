package com.couchraoke.tv.presentation.selectplayers

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectPlayersViewModelTest {
    @Test(timeout = 30_000)
    fun nonDuetSongDefaultsToPlayerOneMediumAndDisabledPlayerTwo() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = listOf(phone()),
        )

        val state = viewModel.state.value
        assertEquals(PlayerId.P1, state.playerOne.playerId)
        assertEquals(SoloSingFixtures.PhoneClientId, state.playerOne.selectedPhoneId)
        assertEquals(Difficulty.Medium, state.playerOne.difficulty)
        assertFalse(state.playerTwo.enabled)
        assertNull(state.playerTwo.selectedPhoneId)
        assertFalse(state.showPlayerTwoDifficulty)
    }

    @Test(timeout = 30_000)
    fun startHandoffRequiresPlayerOneAndReturnsSoloSelection() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = listOf(phone()),
        )

        val selection = viewModel.startSelection()

        assertEquals(SoloSingFixtures.SongId, selection?.songId)
        assertEquals(SoloSingFixtures.PhoneClientId, selection?.playerPhoneId)
        assertEquals(PlayerId.P1, selection?.playerId)
        assertEquals(Difficulty.Medium, selection?.difficulty)
        assertTrue(selection?.countdownEnabled == true)
    }

    @Test(timeout = 30_000)
    fun startIsUnavailableWhenNoPlayerOnePhoneExists() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = emptyList(),
        )

        assertFalse(viewModel.state.value.canStart)
        assertNull(viewModel.startSelection())
    }

    private fun phone(): ConnectedPhone = ConnectedPhone(
        clientId = SoloSingFixtures.PhoneClientId,
        connectionId = SoloSingFixtures.PhoneConnectionId,
        deviceName = SoloSingFixtures.PhoneDeviceName,
        httpPort = SoloSingFixtures.PhoneHttpPort,
        ipAddress = SoloSingFixtures.PhoneIpAddress,
    )
}
