package com.couchraoke.tv.presentation.selectplayers

import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.presentation.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectPlayersRecoveryTest {
    @Test(timeout = 30_000)
    fun noPhonesShowsBlockingRecoveryMessage() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = emptyList(),
        )

        val recovery = viewModel.state.value.noPhoneRecovery

        assertEquals("No phones connected", recovery?.title)
        assertEquals("Open Join and scan the QR code with the phone app.", recovery?.body)
        assertEquals("Open Join QR", recovery?.primaryActionLabel)
        assertFalse(viewModel.state.value.canStart)
        assertNull(viewModel.startSelection())
    }

    @Test(timeout = 30_000)
    fun openJoinQrRequestsTheSharedJoinOverlay() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = emptyList(),
        )

        assertNull(viewModel.consumeRecoveryRequest())

        viewModel.openJoinQr()

        assertEquals(SelectPlayersRecoveryRequest.OpenJoinQrOverlay, viewModel.consumeRecoveryRequest())
        assertNull(viewModel.consumeRecoveryRequest())
    }

    @Test(timeout = 30_000)
    fun noPhoneRecoveryDoesNotExposeSettingsRouteMenuScreenOrSubmenu() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = emptyList(),
        )

        val recovery = viewModel.state.value.noPhoneRecovery
        val routes = listOf(AppRoute.SongList.route, AppRoute.Singing.route, AppRoute.Results.route)

        assertEquals(listOf("Open Join QR"), recovery?.actionLabels)
        assertTrue(routes.none { it.contains("settings", ignoreCase = true) })
    }

    @Test(timeout = 30_000)
    fun connectedPhonesDoNotShowNoPhoneRecovery() {
        val viewModel = SelectPlayersViewModel(
            song = SoloSingFixtures.indexedSong(),
            connectedPhones = listOf(phone()),
        )

        assertNull(viewModel.state.value.noPhoneRecovery)
        assertNull(viewModel.consumeRecoveryRequest())
    }

    private fun phone(): ConnectedPhone = ConnectedPhone(
        clientId = SoloSingFixtures.PhoneClientId,
        connectionId = SoloSingFixtures.PhoneConnectionId,
        deviceName = SoloSingFixtures.PhoneDeviceName,
        httpPort = SoloSingFixtures.PhoneHttpPort,
        ipAddress = SoloSingFixtures.PhoneIpAddress,
    )
}
