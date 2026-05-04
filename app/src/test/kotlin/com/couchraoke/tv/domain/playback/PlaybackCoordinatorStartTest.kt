package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.data.network.AssignSingerMessage
import com.couchraoke.tv.data.network.ClockAckMessage
import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.PhoneEvent
import com.couchraoke.tv.data.network.PlaybackNetworkState
import com.couchraoke.tv.data.network.PlaybackStateMessage
import com.couchraoke.tv.data.network.PongResponse
import com.couchraoke.tv.data.network.SongEntry
import com.couchraoke.tv.data.network.StartMode
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.library.LibraryManager
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.ScoringEngine
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.UsdxParser
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.fixtures.SoloSingUsdxFixtures
import com.couchraoke.tv.presentation.playback.PlaybackEvent
import com.couchraoke.tv.presentation.playback.PlaybackIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCoordinatorStartTest {
    @Test(timeout = 30_000)
    fun startSongFetchesTxtParsesPreparesAndSendsCountdownAssignSingerToSelectedPhoneOnly() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val scoringEngine = FakeScoringEngine()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            scoringEngine = scoringEngine,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        assertEquals(listOf(song.txtUrl), network.fetchedTxtUrls)
        val prepareIntent = coordinator.intents.value.filterIsInstance<PlaybackIntent.Prepare>().single()
        assertEquals(song.audioUrl, prepareIntent.audioUrl)
        assertTrue(coordinator.intents.value.none { it is PlaybackIntent.Play })
        assertTrue(network.assignSingerCalls.isEmpty())

        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        assertEquals(SoloSingFixtures.PhoneClientId, network.assignSingerCalls.single().first)
        val message = network.assignSingerCalls.single().second
        assertEquals("assignSinger", message.type)
        assertEquals(1, message.protocolVersion)
        assertEquals(SoloSingFixtures.SessionId, message.sessionId)
        assertEquals(PlayerId.P1, message.playerId)
        assertEquals(StartMode.Countdown, message.startMode)
        assertEquals(3_000, message.countdownMs)
        assertEquals(song.title, message.songTitle)
        assertEquals(song.artist, message.songArtist)
        assertEquals(12_000L, message.stopAtLyricsTimeMs)
        assertEquals(SoloSingFixtures.UdpPort, message.udpPort)
        assertEquals(1, scoringEngine.loadChartNoOpCalls)
    }

    @Test(timeout = 30_000)
    fun liveStartOmitsCountdownMsAndStillRequiresClockSyncGate() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        assertEquals(SoloSingFixtures.PhoneClientId, network.pingedPhoneIds.single())
        assertEquals(StartMode.Live, network.assignSingerCalls.single().second.startMode)
        assertNull(network.assignSingerCalls.single().second.countdownMs)
    }

    @Test(timeout = 30_000)
    fun startSongBroadcastsPlaybackStateWithFinalizedStopBoundaryBeforePlay() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        assertTrue(network.playbackStateCalls.isEmpty())

        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        val message = network.playbackStateCalls.single()
        assertEquals("playbackState", message.type)
        assertEquals(1, message.protocolVersion)
        assertEquals(SoloSingFixtures.SessionId, message.sessionId)
        assertEquals(SoloSingFixtures.SongInstanceSeq, message.songInstanceSeq)
        assertEquals(12_000L, message.stopAtLyricsTimeMs)
        assertEquals(3_000, message.countdownRemainingMs)
        assertEquals(PlaybackNetworkState.Countdown, message.state)
        assertTrue(coordinator.intents.value.last() is PlaybackIntent.Play)
    }

    @Test(timeout = 30_000)
    fun readyTransitionsToLiveAndEndedReturnsToSongListWithoutResults() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        coordinator.onPlaybackEvent(PlaybackEvent.Ready(songStartTvMs = 123_456L))
        assertTrue(coordinator.state.value.phase is GamePhase.Live)

        coordinator.onPlaybackEvent(PlaybackEvent.Ended)
        assertEquals(GamePhase.Open, coordinator.state.value.phase)
    }

    @Test(timeout = 30_000)
    fun preparedEventDoesNotChangeCoordinatorState() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 10_000L))

        val phase = coordinator.state.value.phase as GamePhase.Countdown
        assertEquals(10_000L, phase.plan.stopAtLyricsTimeMs)
    }

    @Test(timeout = 30_000)
    fun errorEventMovesCoordinatorToBlockingErrorState() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        coordinator.onPlaybackEvent(
            PlaybackEvent.Error(
                com.couchraoke.tv.presentation.playback.PlaybackErrorCause.AudioUnavailable(),
            ),
        )

        val phase = coordinator.state.value.phase as GamePhase.Error
        assertEquals("ERROR", phase.title)
        assertEquals(listOf("This song can't be played."), phase.bodyLines)
        assertEquals(PlaybackModal.Error(listOf("This song can't be played.")), coordinator.state.value.modal)
    }

    @Test(timeout = 30_000)
    fun quitToSongListReturnsCoordinatorToOpenState() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        coordinator.quitToSongList()

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
    }

    @Test(timeout = 30_000)
    fun liveStartTransitionsCoordinatorStateToLiveImmediately() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        val phase = coordinator.state.value.phase as GamePhase.Live
        assertEquals(0L, phase.songStartTvMs)
        assertEquals(song.songId, phase.plan.song.songId)
    }

    @Test(timeout = 30_000)
    fun startSongUsesNullChartBoundaryWhenParsedSongHasNoEndMs() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(
                parsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart().copy(
                    header = SoloSingUsdxFixtures.parsedStaticSoloChart().header.copy(endMs = null),
                ),
            ),
            udpPort = SoloSingFixtures.UdpPort,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        val prepareIntent = coordinator.intents.value.filterIsInstance<PlaybackIntent.Prepare>().single()
        assertNull(prepareIntent.chartEndLyricsTimeMs)

        coordinator.onPlaybackEvent(PlaybackEvent.Prepared(effectivePlaybackDurationMs = 12_000L))

        val playIntent = coordinator.intents.value.filterIsInstance<PlaybackIntent.Play>().single()
        assertEquals(12_000L, playIntent.stopAtLyricsTimeMs)
        assertEquals(12_000L, network.assignSingerCalls.single().second.stopAtLyricsTimeMs)
    }

    @Test(timeout = 30_000)
    fun readyBeforeSongStartIsIgnored() = runBlocking {
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        coordinator.onPlaybackEvent(PlaybackEvent.Ready(songStartTvMs = 123L))

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
    }

    @Test(timeout = 30_000)
    fun pauseResumeAndRestartRemainNoOpsInThisSlice() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        val before = coordinator.state.value

        coordinator.pause()
        coordinator.resume()
        coordinator.restart()

        assertEquals(before, coordinator.state.value)
    }

    @Test(timeout = 30_000)
    fun startSongThrowsWhenSongIsMissing() = runBlocking {
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = MissingSongLibraryManager(),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        try {
            coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
            throw AssertionError("Expected missing song failure")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message?.contains("Required value was null") == true)
        }
    }

    @Test(timeout = 30_000)
    fun startSongThrowsWhenTxtFetchFails() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(fetchTxtFailure = IllegalStateException("txt failed")),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
        )

        try {
            coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
            throw AssertionError("Expected txt fetch failure")
        } catch (expected: IllegalStateException) {
            assertEquals("txt failed", expected.message)
        }
    }

    @Test(timeout = 30_000)
    fun startSongThrowsWhenParsingFails() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(parseFailure = IllegalStateException("parse failed")),
            udpPort = SoloSingFixtures.UdpPort,
        )

        try {
            coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
            throw AssertionError("Expected parse failure")
        } catch (expected: IllegalStateException) {
            assertEquals("parse failed", expected.message)
        }
    }

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

    private class MissingSongLibraryManager : LibraryManager {
        override val songs = MutableStateFlow<List<IndexedSong>>(emptyList())
        override fun getSong(songId: String): IndexedSong? = null
    }

    private class FakeUsdxParser(
        private val parsedSong: ParsedSong = SoloSingUsdxFixtures.parsedStaticSoloChart(),
        private val parseFailure: Throwable? = null,
    ) : UsdxParser {
        override fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong> =
            parseFailure?.let { Result.failure(it) } ?: Result.success(parsedSong)
    }

    private class FakeScoringEngine : ScoringEngine {
        var loadChartNoOpCalls = 0
        override fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig) {
            loadChartNoOpCalls++
        }

        override fun setSongStart(songStartTvMs: Long) = Unit
        override suspend fun finalizeAll(): Map<PlayerId, PlayerScore> = emptyMap()
        override fun reset() = Unit
    }

    private class FakeNetworkController(
        private val fetchTxtFailure: Throwable? = null,
    ) : NetworkController {
        override val connectedPhones = MutableStateFlow(
            listOf(ConnectedPhone(SoloSingFixtures.PhoneClientId, 7u, "P1", 1, "127.0.0.1")),
        )
        override val phoneEvents = MutableSharedFlow<PhoneEvent>()
        val fetchedTxtUrls = mutableListOf<String>()
        val pingedPhoneIds = mutableListOf<String>()
        val assignSingerCalls = mutableListOf<Pair<String, AssignSingerMessage>>()
        val playbackStateCalls = mutableListOf<PlaybackStateMessage>()
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> = Result.success(emptyList())
        override suspend fun fetchTxt(url: String): Result<ByteArray> {
            fetchedTxtUrls += url
            return fetchTxtFailure?.let { Result.failure(it) }
                ?: Result.success(SoloSingUsdxFixtures.StaticSoloChartBytes)
        }
        override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) {
            assignSingerCalls += phoneId to message
        }
        override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) {
            playbackStateCalls += message
        }
        override suspend fun sendSessionState(phoneId: String) = Unit
        override suspend fun sendPing(phoneId: String): PongResponse {
            pingedPhoneIds += phoneId
            return PongResponse(phoneId, phoneTimeMs = 1L, tvReceiveTimeMs = 2L, isValidSample = true)
        }
        override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) = Unit
    }
}
