package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.data.network.AssignSingerMessage
import com.couchraoke.tv.data.network.ClockAckMessage
import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.PhoneEvent
import com.couchraoke.tv.data.network.PlaybackNetworkState
import com.couchraoke.tv.data.network.PlaybackStateMessage
import com.couchraoke.tv.data.network.PlaybackStateReason
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = scoringEngine,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        assertEquals(listOf(song.txtUrl), network.fetchedTxtUrls)
        val prepareIntent = coordinator.intents.value.filterIsInstance<PlaybackIntent.Prepare>().single()
        assertEquals(song.audioUrl, prepareIntent.audioUrl)
        assertTrue(coordinator.intents.value.none { it is PlaybackIntent.Play })
        assertTrue(network.assignSingerCalls.isEmpty())

        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

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
        assertEquals(SoloSingFixtures.ShortPreparedDurationMs, message.stopAtLyricsTimeMs)
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
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(SoloSingFixtures.PhoneClientId, network.pingedPhoneIds.single())
        val message = network.assignSingerCalls.single().second
        assertEquals(StartMode.Live, message.startMode)
        assertNull(message.countdownMs)
        val json = Json { encodeDefaults = false }
            .encodeToJsonElement(AssignSingerMessage.serializer(), message)
            .jsonObject
        assertFalse(json.containsKey("countdownMs"))
        assertFalse(json.containsKey("connectionId"))
    }

    @Test(timeout = 30_000)
    fun startSongWaitsForValidClockSampleBeforeAssignStateAndPlay() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(listOf("ping", "clockAck", "assignSinger", "playbackState"), network.actionLog)
        assertEquals(SoloSingFixtures.PhoneClientId, network.pingedPhoneIds.single())
        assertEquals(SoloSingFixtures.PhoneClientId, network.clockAckCalls.single().first)
        assertEquals(network.pongResponses.single().pingId, network.clockAckCalls.single().second.pingId)
        assertEquals(network.pongResponses.single().tvReceiveTimeMs, network.clockAckCalls.single().second.tTvRecvMs)
        assertTrue(coordinator.intents.value.last() is PlaybackIntent.Play)
    }

    @Test(timeout = 30_000)
    fun invalidClockSampleAbortsBeforeAssignStateAndPlay() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController(
            pongResponses = listOf(invalidPong(isValidSample = false)),
        )
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(listOf("ping"), network.actionLog)
        assertTrue(network.assignSingerCalls.isEmpty())
        assertTrue(network.playbackStateCalls.isEmpty())
        assertTrue(coordinator.intents.value.none { it is PlaybackIntent.Play })
        assertEquals(GamePhase.Open, coordinator.state.value.phase)
        assertEquals(
            PlaybackModal.Error(listOf("Network too unstable for accurate sync. Check WiFi connection and try again.")),
            coordinator.state.value.modal,
        )
    }

    @Test(timeout = 30_000)
    fun mismatchedClockSampleAbortsBeforeAssignStateAndPlay() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController(
            pongResponses = listOf(invalidPong(phoneId = "other-phone")),
        )
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(listOf("ping"), network.actionLog)
        assertTrue(network.assignSingerCalls.isEmpty())
        assertTrue(network.playbackStateCalls.isEmpty())
        assertTrue(coordinator.intents.value.none { it is PlaybackIntent.Play })
        assertEquals(GamePhase.Open, coordinator.state.value.phase)
    }

    @Test(timeout = 30_000)
    fun timedOutClockSampleAbortsBeforeAssignStateAndPlay() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController(
            pongResponses = listOf(invalidPong(pingId = "")),
        )
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(listOf("ping"), network.actionLog)
        assertTrue(network.assignSingerCalls.isEmpty())
        assertTrue(network.playbackStateCalls.isEmpty())
        assertTrue(coordinator.intents.value.none { it is PlaybackIntent.Play })
        assertEquals(GamePhase.Open, coordinator.state.value.phase)
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
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = FakeScoringEngine(),
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        assertTrue(network.playbackStateCalls.isEmpty())

        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        val message = network.playbackStateCalls.single()
        assertEquals("playbackState", message.type)
        assertEquals(1, message.protocolVersion)
        assertEquals(SoloSingFixtures.SessionId, message.sessionId)
        assertEquals(SoloSingFixtures.SongInstanceSeq, message.songInstanceSeq)
        assertEquals(SoloSingFixtures.ShortPreparedDurationMs, message.stopAtLyricsTimeMs)
        assertEquals(3_000, message.countdownRemainingMs)
        assertEquals(PlaybackNetworkState.Countdown, message.state)
        assertTrue(coordinator.intents.value.last() is PlaybackIntent.Play)
    }

    @Test(timeout = 30_000)
    fun readyTransitionsToLiveAndEndedReturnsToSongListWithoutResults() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val network = FakeNetworkController()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = network,
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        coordinator.onPlaybackEvent(
            PlaybackEvent.Ready(songStartTvMs = SoloSingFixtures.ReadySongStartTvMs),
        )
        assertTrue(coordinator.state.value.phase is GamePhase.Live)

        coordinator.onPlaybackEvent(PlaybackEvent.Ended)
        assertEquals(GamePhase.Open, coordinator.state.value.phase)
        assertFalse(network.sessionLocked)
        val stoppedMessage = network.playbackStateCalls.last()
        assertEquals(PlaybackNetworkState.Stopped, stoppedMessage.state)
        assertEquals(PlaybackStateReason.SongEnd, stoppedMessage.reason)
    }

    @Test(timeout = 30_000)
    fun readyCallsScoringSongStartSeam() = runBlocking {
        val scoringEngine = FakeScoringEngine()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
            scoringEngine = scoringEngine,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        coordinator.onPlaybackEvent(
            PlaybackEvent.Ready(songStartTvMs = SoloSingFixtures.ReadySongStartTvMs),
        )

        assertEquals(listOf(SoloSingFixtures.ReadySongStartTvMs), scoringEngine.songStartCalls)
    }

    @Test(timeout = 30_000)
    fun preparedEventDoesNotChangeCoordinatorState() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
        )
        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.PreparedStateDurationMs),
        )

        val phase = coordinator.state.value.phase as GamePhase.Countdown
        assertEquals(SoloSingFixtures.PreparedStateDurationMs, phase.plan.stopAtLyricsTimeMs)
    }

    @Test(timeout = 30_000)
    fun errorEventMovesCoordinatorToBlockingErrorState() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
        )

        coordinator.onPlaybackEvent(
            PlaybackEvent.Error(
                com.couchraoke.tv.presentation.playback.PlaybackErrorCause.AudioUnavailable(),
            ),
        )

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
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
            sessionId = SoloSingFixtures.SessionId,
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
            sessionId = SoloSingFixtures.SessionId,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

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
            sessionId = SoloSingFixtures.SessionId,
        )

        coordinator.startSong(SoloSingFixtures.songStartSelection().toSelection())

        val prepareIntent = coordinator.intents.value.filterIsInstance<PlaybackIntent.Prepare>().single()
        assertEquals(song.audioUrl, prepareIntent.audioUrl)

        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.ShortPreparedDurationMs),
        )

        assertEquals(
            SoloSingFixtures.ShortPreparedDurationMs,
            network.assignSingerCalls.single().second.stopAtLyricsTimeMs,
        )
    }

    @Test(timeout = 30_000)
    fun readyBeforeSongStartIsIgnored() = runBlocking {
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(SoloSingFixtures.indexedSong()),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
        )

        coordinator.onPlaybackEvent(PlaybackEvent.Ready(songStartTvMs = 123L))

        assertEquals(GamePhase.Open, coordinator.state.value.phase)
    }

    @Test(timeout = 30_000)
    fun startSongMissingSongShowsErrorModal() = runBlocking {
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = MissingSongLibraryManager(),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
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
    }

    @Test(timeout = 30_000)
    fun startSongTxtFetchFailureShowsErrorModal() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(fetchTxtFailure = IllegalStateException("txt failed")),
            usdxParser = FakeUsdxParser(),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
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
    }

    @Test(timeout = 30_000)
    fun startSongParsingFailureShowsErrorModal() = runBlocking {
        val song = SoloSingFixtures.indexedSong()
        val coordinator = DefaultPlaybackCoordinator(
            libraryManager = FakeLibraryManager(song),
            networkController = FakeNetworkController(),
            usdxParser = FakeUsdxParser(parseFailure = IllegalStateException("parse failed")),
            udpPort = SoloSingFixtures.UdpPort,
            sessionId = SoloSingFixtures.SessionId,
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
        override suspend fun onPhoneConnected(phone: com.couchraoke.tv.data.network.ConnectedPhone) = Unit
        override fun onPhoneDisconnected(clientId: String) = Unit
        override suspend fun refreshPhone(clientId: String) = Unit
        override suspend fun refreshAll() = Unit
        override fun getSong(songId: String): IndexedSong? = song.takeIf { it.songId == songId }
    }

    private class MissingSongLibraryManager : LibraryManager {
        override val songs = MutableStateFlow<List<IndexedSong>>(emptyList())
        override suspend fun onPhoneConnected(phone: com.couchraoke.tv.data.network.ConnectedPhone) = Unit
        override fun onPhoneDisconnected(clientId: String) = Unit
        override suspend fun refreshPhone(clientId: String) = Unit
        override suspend fun refreshAll() = Unit
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
        val songStartCalls = mutableListOf<Long>()
        override fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig) {
            loadChartNoOpCalls++
        }

        override fun setSongStart(songStartTvMs: Long) {
            songStartCalls += songStartTvMs
        }

        override suspend fun finalizeAll(): Map<PlayerId, PlayerScore> = emptyMap()
        override fun reset() = Unit
    }

    private class FakeNetworkController(
        private val fetchTxtFailure: Throwable? = null,
        val pongResponses: List<PongResponse> = listOf(validPong()),
    ) : NetworkController {
        override val connectedPhones = MutableStateFlow(
            listOf(
                ConnectedPhone(
                    SoloSingFixtures.PhoneClientId,
                    SoloSingFixtures.PhoneConnectionId,
                    SoloSingFixtures.PhoneDeviceName,
                    SoloSingFixtures.PhoneHttpPort,
                    SoloSingFixtures.LoopbackHost,
                ),
            ),
        )
        override val phoneEvents = MutableSharedFlow<PhoneEvent>()
        val fetchedTxtUrls = mutableListOf<String>()
        val pingedPhoneIds = mutableListOf<String>()
        val clockAckCalls = mutableListOf<Pair<String, ClockAckMessage>>()
        val assignSingerCalls = mutableListOf<Pair<String, AssignSingerMessage>>()
        val playbackStateCalls = mutableListOf<PlaybackStateMessage>()
        val actionLog = mutableListOf<String>()
        var sessionLocked = false
            private set
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> = Result.success(emptyList())
        override suspend fun fetchTxt(url: String): Result<ByteArray> {
            fetchedTxtUrls += url
            return fetchTxtFailure?.let { Result.failure(it) }
                ?: Result.success(SoloSingUsdxFixtures.StaticSoloChartBytes)
        }
        override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) {
            actionLog += "assignSinger"
            assignSingerCalls += phoneId to message
        }
        override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) {
            actionLog += "playbackState"
            sessionLocked = message.state != PlaybackNetworkState.Stopped
            playbackStateCalls += message
        }
        override suspend fun sendSessionState(phoneId: String) = Unit
        override suspend fun sendPing(phoneId: String): PongResponse {
            actionLog += "ping"
            pingedPhoneIds += phoneId
            return pongResponses.getOrElse(pingedPhoneIds.lastIndex) { invalidPong() }
        }
        override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) {
            actionLog += "clockAck"
            clockAckCalls += phoneId to ack
        }
    }

    companion object {
        private fun validPong(): PongResponse = PongResponse(
            phoneId = SoloSingFixtures.PhoneClientId,
            pingId = "ping-1",
            tTvSendMs = 1_000L,
            tPhoneRecvMs = 1_020L,
            tPhoneSendMs = 1_040L,
            tvReceiveTimeMs = 1_060L,
            isValidSample = true,
        )

        private fun invalidPong(
            pingId: String = "ping-1",
            isValidSample: Boolean = true,
            phoneId: String = SoloSingFixtures.PhoneClientId,
        ): PongResponse = PongResponse(
            phoneId = phoneId,
            pingId = pingId,
            tTvSendMs = 1_000L,
            tPhoneRecvMs = 1_020L,
            tPhoneSendMs = 1_040L,
            tvReceiveTimeMs = 1_060L,
            isValidSample = isValidSample,
        )
    }
}
