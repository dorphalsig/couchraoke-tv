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
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCoordinatorControlsTest {
    @Test(timeout = 30_000)
    fun pauseBroadcastsPausedStateAndEmitsPauseIntent() = runBlocking {
        val harness = startedHarness()

        harness.coordinator.pause()

        val phase = harness.coordinator.state.value.phase as GamePhase.Paused
        assertEquals(0L, phase.positionMs)
        assertTrue(harness.coordinator.intents.value.last() is PlaybackIntent.Pause)
        val paused = harness.network.playbackStateCalls.last()
        assertEquals(PlaybackNetworkState.Paused, paused.state)
        assertEquals(PlaybackStateReason.UserPause, paused.reason)
        assertEquals(harness.plan.stopAtLyricsTimeMs, paused.stopAtLyricsTimeMs)
        assertTrue(paused.revision > harness.network.playbackStateCalls.first().revision)
    }

    @Test(timeout = 30_000)
    fun resumeBroadcastsPlayingStateAndEmitsPlayIntent() = runBlocking {
        val harness = startedHarness()
        harness.coordinator.pause()

        harness.coordinator.resume()

        assertTrue(harness.coordinator.state.value.phase is GamePhase.Live)
        assertEquals(PlaybackIntent.Play, harness.coordinator.intents.value.last())
        val playing = harness.network.playbackStateCalls.last()
        assertEquals(PlaybackNetworkState.Playing, playing.state)
        assertEquals(PlaybackStateReason.Unspecified, playing.reason)
        assertTrue(playing.revision > harness.network.playbackStateCalls.first().revision)
    }

    @Test(timeout = 30_000)
    fun restartIncrementsSongInstanceSeqResendsAssignSingerBeforePlayAndSeeksToStartSec() = runBlocking {
        val harness = startedHarness()
        harness.coordinator.pause()

        harness.coordinator.restart()

        val prepare = harness.coordinator.intents.value.last() as PlaybackIntent.Prepare
        assertEquals(1.5f, prepare.seekToSec)
        harness.coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.StopAtLyricsTimeMs),
        )

        val restartPlan = (harness.coordinator.state.value.phase as GamePhase.Live).plan
        assertEquals(harness.plan.songInstanceSeq + 1L, restartPlan.songInstanceSeq)
        val assignSinger = harness.network.assignSingerCalls.last().second
        assertEquals(restartPlan.songInstanceSeq, assignSinger.songInstanceSeq)
        assertEquals(restartPlan.stopAtLyricsTimeMs, assignSinger.stopAtLyricsTimeMs)
        val prepareIndex = harness.coordinator.intents.value.indexOfLast { it is PlaybackIntent.Prepare }
        val playIndex = harness.coordinator.intents.value.indexOfLast { it is PlaybackIntent.Play }
        assertTrue(prepareIndex >= 0)
        assertTrue(playIndex > prepareIndex)
        val restartState = harness.network.playbackStateCalls.last()
        assertEquals(PlaybackNetworkState.Playing, restartState.state)
        assertEquals(PlaybackStateReason.Unspecified, restartState.reason)
        assertEquals(restartPlan.songInstanceSeq, restartState.songInstanceSeq)
        assertEquals(1L, restartState.revision)
        assertEquals(1, harness.scoringEngine.resetCalls)
    }

    @Test(timeout = 30_000)
    fun quitStopsPlaybackBroadcastsStoppedStateSendsSessionStateAndOpensSession() = runBlocking {
        val harness = startedHarness()
        harness.coordinator.pause()

        harness.coordinator.quitToSongList()

        assertEquals(GamePhase.Open, harness.coordinator.state.value.phase)
        assertTrue(harness.coordinator.intents.value.last() is PlaybackIntent.Stop)
        val stopped = harness.network.playbackStateCalls.last()
        assertEquals(PlaybackNetworkState.Stopped, stopped.state)
        assertEquals(PlaybackStateReason.UserQuit, stopped.reason)
        assertTrue(stopped.revision > harness.network.playbackStateCalls.first().revision)
        assertEquals(listOf(SoloSingFixtures.PhoneClientId), harness.network.sessionStatePhoneIds)
        assertEquals(1, harness.scoringEngine.resetCalls)
    }

    private suspend fun startedHarness(): Harness {
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
        coordinator.startSong(SoloSingFixtures.songStartSelection(countdownEnabled = false).toSelection())
        coordinator.onPlaybackEvent(
            PlaybackEvent.Prepared(effectivePlaybackDurationMs = SoloSingFixtures.StopAtLyricsTimeMs),
        )
        coordinator.onPlaybackEvent(
            PlaybackEvent.Ready(songStartTvMs = SoloSingFixtures.ReadySongStartTvMs),
        )
        return Harness(
            coordinator = coordinator,
            network = network,
            scoringEngine = scoringEngine,
            plan = (coordinator.state.value.phase as GamePhase.Live).plan,
        )
    }

    private data class Harness(
        val coordinator: DefaultPlaybackCoordinator,
        val network: FakeNetworkController,
        val scoringEngine: FakeScoringEngine,
        val plan: PlaybackPlan,
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
        override suspend fun onPhoneConnected(phone: com.couchraoke.tv.data.network.ConnectedPhone) = Unit
        override fun onPhoneDisconnected(clientId: String) = Unit
        override suspend fun refreshPhone(clientId: String) = Unit
        override suspend fun refreshAll() = Unit
        override fun getSong(songId: String): IndexedSong? = song.takeIf { it.songId == songId }
    }

    private class FakeUsdxParser : UsdxParser {
        override fun parse(songId: String, txtBytes: ByteArray): Result<ParsedSong> =
            Result.success(SoloSingUsdxFixtures.parsedStaticSoloChart())
    }

    private class FakeScoringEngine : ScoringEngine {
        var resetCalls = 0
        override fun loadChart(
            chart: ParsedSong,
            micDelayMs: Int,
            medleyWindow: BeatRange?,
            config: ScoringConfig,
        ) = Unit
        override fun setSongStart(songStartTvMs: Long) = Unit
        override suspend fun finalizeAll(): Map<PlayerId, PlayerScore> = emptyMap()
        override fun reset() {
            resetCalls++
        }
    }

    private class FakeNetworkController : NetworkController {
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
        val assignSingerCalls = mutableListOf<Pair<String, AssignSingerMessage>>()
        val playbackStateCalls = mutableListOf<PlaybackStateMessage>()
        val sessionStatePhoneIds = mutableListOf<String>()
        override suspend fun start(udpPort: Int, wsPort: Int) = Unit
        override suspend fun stop() = Unit
        override suspend fun fetchManifest(
            phone: ConnectedPhone,
        ): Result<List<SongEntry>> = Result.success(emptyList())

        override suspend fun fetchTxt(
            url: String,
        ): Result<ByteArray> = Result.success(SoloSingUsdxFixtures.StaticSoloChartBytes)
        override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) {
            assignSingerCalls += phoneId to message
        }
        override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) {
            playbackStateCalls += message
        }
        override suspend fun sendSessionState(phoneId: String) {
            sessionStatePhoneIds += phoneId
        }
        override suspend fun sendPing(phoneId: String): PongResponse =
            PongResponse(
                phoneId = phoneId,
                pingId = "ping-1",
                phoneTimeMs = SoloSingFixtures.PongPhoneTimeMs,
                tvReceiveTimeMs = SoloSingFixtures.PongTvReceiveTimeMs,
                isValidSample = true,
            )
        override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) = Unit
    }
}
