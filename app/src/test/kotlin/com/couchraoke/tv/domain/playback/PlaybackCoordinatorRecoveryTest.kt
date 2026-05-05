package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.data.network.AssignSingerMessage
import com.couchraoke.tv.data.network.ClockAckMessage
import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.PhoneEvent
import com.couchraoke.tv.data.network.PlaybackStateMessage
import com.couchraoke.tv.data.network.PongResponse
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.library.LibraryManager
import com.couchraoke.tv.domain.usdx.UsdxParser
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import com.couchraoke.tv.presentation.playback.PlaybackErrorCause
import com.couchraoke.tv.presentation.playback.PlaybackEvent
import com.couchraoke.tv.presentation.playback.PlaybackIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCoordinatorRecoveryTest {
    @Test(timeout = 30_000)
    fun requiredSingerCountdownDisconnectStopsPlaybackShowsDisconnectedModalAndOpensSession() = runBlocking {
        val harness = preparedCountdownHarness()

        harness.coordinator.onPhoneEvent(
            PhoneEvent.Disconnected(clientId = SoloSingFixtures.PhoneClientId, wasAssignedSinger = true),
        )

        assertEquals(GamePhase.Open, harness.coordinator.state.value.phase)
        assertEquals(PlaybackModal.Disconnected(), harness.coordinator.state.value.modal)
        assertTrue(harness.coordinator.intents.value.last() is PlaybackIntent.Stop)
        assertEquals(listOf(SoloSingFixtures.PhoneClientId), harness.network.sessionStatePhoneIds)
    }

    @Test(timeout = 30_000)
    fun spectatorDisconnectDoesNotInterruptLivePlayback() = runBlocking {
        val harness = liveHarness()
        val beforeState = harness.coordinator.state.value
        val beforeIntents = harness.coordinator.intents.value
        val beforePlaybackStateCalls = harness.network.playbackStateCalls.toList()

        harness.coordinator.onPhoneEvent(
            PhoneEvent.Disconnected(clientId = "spectator", wasAssignedSinger = false),
        )

        assertEquals(beforeState, harness.coordinator.state.value)
        assertEquals(beforeIntents, harness.coordinator.intents.value)
        assertEquals(beforePlaybackStateCalls, harness.network.playbackStateCalls)
    }

    @Test(timeout = 30_000)
    fun assignedSingerLiveDisconnectIsNoOpInIteration1() = runBlocking {
        // Iteration 3 wires Live-phase disconnect auto-pause; Iteration 1 must not produce
        // DisconnectPaused or any state/intent change on this event.
        val harness = liveHarness()
        val beforeState = harness.coordinator.state.value
        val beforeIntents = harness.coordinator.intents.value

        harness.coordinator.onPhoneEvent(
            PhoneEvent.Disconnected(clientId = SoloSingFixtures.PhoneClientId, wasAssignedSinger = true),
        )

        assertEquals(beforeState, harness.coordinator.state.value)
        assertEquals(beforeIntents, harness.coordinator.intents.value)
    }

    @Test(timeout = 30_000)
    fun txtFetchStartFailureShowsErrorModalAndOpensSession() = runBlocking {
        val network = FakeNetworkController(fetchTxtFailure = IllegalStateException("txt failed"))
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
        assertEquals(
            PlaybackModal.Error(
                listOf(
                    "This song can't be played.",
                    "Check Settings > Song Library — the song's phone may be disconnected.",
                ),
            ),
            coordinator.state.value.modal,
        )
        assertTrue(coordinator.intents.value.last() is PlaybackIntent.Stop)
    }

    @Test(timeout = 30_000)
    fun songNotFoundStartFailureShowsSpecErrorBodyAndOpensSession() = runBlocking {
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        val missingSelection = SoloSingFixtures.songStartSelection()
            .copy(songId = "missing::song")
            .toSelection()

        coordinator.startSong(missingSelection)

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
        assertEquals(
            PlaybackModal.Error(
                listOf(
                    "This song can't be played.",
                    "Check Settings > Song Library — the song's phone may be disconnected.",
                ),
            ),
            coordinator.state.value.modal,
        )
    }

    @Test(timeout = 30_000)
    fun playbackErrorShowsErrorModalDiagnosticAndOpensSession() = runBlocking {
        val harness = liveHarness()

        harness.coordinator.onPlaybackEvent(
            PlaybackEvent.Error(PlaybackErrorCause.PlayerError(lastWarningOrError = "latest libvlc warning")),
        )

        assertEquals(GamePhase.Open, harness.coordinator.state.value.phase)
        assertEquals(
            PlaybackModal.Error(listOf("This song can't be played.", "latest libvlc warning")),
            harness.coordinator.state.value.modal,
        )
        assertTrue(harness.coordinator.intents.value.last() is PlaybackIntent.Stop)
        assertEquals(listOf(SoloSingFixtures.PhoneClientId), harness.network.sessionStatePhoneIds)
    }

    @Test(timeout = 30_000)
    fun reconnectAfterRecoveryDoesNotLeaveSessionLocked() = runBlocking {
        val harness = preparedCountdownHarness()
        harness.coordinator.onPhoneEvent(
            PhoneEvent.Disconnected(clientId = SoloSingFixtures.PhoneClientId, wasAssignedSinger = true),
        )

        harness.coordinator.onPhoneEvent(
            PhoneEvent.Reconnected(
                phone = ConnectedPhone(SoloSingFixtures.PhoneClientId, 9u, "P1", 1, "127.0.0.1"),
                wasAssignedSinger = true,
            ),
        )

        assertEquals(GamePhase.Open, harness.coordinator.state.value.phase)
    }

    private suspend fun preparedCountdownHarness(): Harness {
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = true).toSelection())
        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 18_000L))
        return Harness(coordinator = coordinator, network = network)
    }

    private suspend fun liveHarness(): Harness {
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 18_000L))
        coordinator.onPlaybackEvent(PlaybackEvent.Ready(songStartTvMs = 123_456L))
        return Harness(coordinator = coordinator, network = network)
    }

    private data class Harness(
        val coordinator: DefaultPlaybackCoordinator,
        val network: FakeNetworkController,
    )

    private fun com.couchraoke.tv.fixtures.SongStartSelectionFixture.toSelection(): SongStartSelection =
        SongStartSelection(
            songId = songId,
            playerPhoneId = playerPhoneId,
            playerId = playerId,
            difficulty = difficulty,
            countdownEnabled = countdownEnabled,
            countdownSeconds = countdownSeconds,
        )

    private class FakeLibraryManager(private val song: IndexedSong) : LibraryManager {
        override val songs = MutableStateFlow(listOf(song))
        override fun getSong(songId: String): IndexedSong? = song.takeIf { it.songId == songId }
    }

    private class FakeUsdxParser : UsdxParser {
        override fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong> =
            Result.success(SoloSingUsdxFixtures.parsedStaticSoloChart())
    }

    private class FakeNetworkController(
        private val fetchTxtFailure: Throwable? = null,
    ) : NetworkController {
        override val connectedPhones = MutableStateFlow(
            listOf(ConnectedPhone(SoloSingFixtures.PhoneClientId, 7u, "P1", 1, "127.0.0.1")),
        )
        override val phoneEvents = MutableSharedFlow<PhoneEvent>()
        val playbackStateCalls = mutableListOf<PlaybackStateMessage>()
        val sessionStatePhoneIds = mutableListOf<String>()
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> = Result.success(emptyList())
        override suspend fun fetchTxt(url: String): Result<ByteArray> =
            fetchTxtFailure?.let { Result.failure(it) } ?: Result.success(SoloSingUsdxFixtures.StaticSoloChartBytes)
        override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) = Unit
        override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) {
            playbackStateCalls += message
        }
        override suspend fun sendSessionState(phoneId: String) {
            sessionStatePhoneIds += phoneId
        }
        override suspend fun sendPing(phoneId: String): PongResponse =
            PongResponse(phoneId, phoneTimeMs = 1L, tvReceiveTimeMs = 2L, isValidSample = true)
        override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) = Unit
    }
}
