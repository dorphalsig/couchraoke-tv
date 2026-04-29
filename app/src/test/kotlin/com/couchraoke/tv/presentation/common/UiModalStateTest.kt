package com.couchraoke.tv.presentation.common

import com.couchraoke.tv.fixtures.SoloSingFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class UiModalStateTest {
    @Test(timeout = 30_000)
    fun modalContractsCoverJoinSelectPauseConfirmAndBlockingStates() {
        val none = UiModalState.None
        val join = UiModalState.JoinOverlay(
            qrPayload = SoloSingFixtures.joinQrPayload(),
            joinCode = SoloSingFixtures.JoinCode,
        )
        val selectPlayers = UiModalState.SelectPlayers(
            songId = SoloSingFixtures.SongId,
            title = "SELECT PLAYERS",
            subtitle = "${SoloSingFixtures.SongArtist} — ${SoloSingFixtures.SongTitle}",
        )
        val pause = UiModalState.Pause()
        val confirm = UiModalState.Confirm(
            title = "CONFIRM",
            body = "Quit to Song List?",
        )
        val blocking = UiModalState.Blocking(
            title = "ERROR",
            bodyLines = listOf("This song can't be played."),
        )

        assertEquals(UiModalState.None, none)
        assertEquals(SoloSingFixtures.joinQrPayload(), join.qrPayload)
        assertEquals(SoloSingFixtures.SongId, selectPlayers.songId)
        assertEquals(ModalAction.Resume, pause.defaultFocusedAction)
        assertEquals(ModalAction.Cancel, confirm.defaultFocusedAction)
        assertEquals(ModalAction.Ok, blocking.action)
    }
}
