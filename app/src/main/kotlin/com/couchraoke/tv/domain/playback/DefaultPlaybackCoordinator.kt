package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.data.network.AssignSingerMessage
import com.couchraoke.tv.data.network.ClockAckMessage
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.PhoneEvent
import com.couchraoke.tv.data.network.PlaybackNetworkState
import com.couchraoke.tv.data.network.PlaybackStateMessage
import com.couchraoke.tv.data.network.PlaybackStateReason
import com.couchraoke.tv.data.network.StartMode
import com.couchraoke.tv.domain.library.LibraryManager
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.ScoringEngine
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.UsdxParser
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.presentation.playback.PlaybackErrorCause
import com.couchraoke.tv.presentation.playback.PlaybackEvent
import com.couchraoke.tv.presentation.playback.PlaybackIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DefaultPlaybackCoordinator(
    private val libraryManager: LibraryManager,
    private val networkController: NetworkController,
    private val usdxParser: UsdxParser,
    private val udpPort: Int,
    private val sessionId: String,
    private val scoringEngine: ScoringEngine = Iteration1ScoringNoOp,
) : PlaybackCoordinator {
    private val mutableState = MutableStateFlow(PlaybackCoordinatorState())
    private val mutableIntents = MutableStateFlow<List<PlaybackIntent>>(emptyList())
    private var songInstanceSeq = 0L
    private var activePlan: PlaybackPlan? = null
    private var pendingStart: PendingPlaybackStart? = null

    override val state: StateFlow<PlaybackCoordinatorState> = mutableState
    val intents: StateFlow<List<PlaybackIntent>> = mutableIntents

    override suspend fun startSong(selection: SongStartSelection) {
        val song = libraryManager.getSong(selection.songId)
        if (song == null) {
            recoverWithError(null, playbackErrorBody(IllegalArgumentException("Song not found")))
        } else {
            mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Preparing(selection), selectedSong = song)
            val txtResult = networkController.fetchTxt(song.txtUrl)
            val parsedResult = txtResult.mapCatching { txtBytes ->
                usdxParser.parse(song.songId, txtBytes).getOrThrow()
            }
            parsedResult.fold(
                onSuccess = { parsedSong ->
                    loadChartNoOp(scoringEngine, parsedSong, selection.difficulty)
                    val countdownMs = if (selection.countdownEnabled) selection.countdownSeconds * 1_000 else null
                    val startMode = if (selection.countdownEnabled) {
                        PlaybackStartMode.Countdown
                    } else {
                        PlaybackStartMode.Live
                    }
                    val chartEndLyricsTimeMs = parsedSong.header.endMs?.toLong()?.takeIf { it > 0L }
                    pendingStart = PendingPlaybackStart(
                        song = song,
                        parsedSong = parsedSong,
                        selection = selection,
                        startMode = startMode,
                        countdownMs = countdownMs,
                        chartEndLyricsTimeMs = chartEndLyricsTimeMs,
                        songInstanceSeq = ++songInstanceSeq,
                    )
                    mutableIntents.value = listOf(
                        PlaybackIntent.Prepare(
                            audioUrl = song.audioUrl,
                            videoUrl = song.videoUrl,
                            videoGapSec = parsedSong.header.videoGapSec,
                            seekToSec = song.startSec,
                        ),
                    )
                },
                onFailure = { cause -> recoverWithError(song, playbackErrorBody(cause)) },
            )
        }
    }

    override suspend fun pause() {
        val plan = activePlan ?: return
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Pause
        val pausedPlan = plan.broadcastPlaybackState(
            networkController = networkController,
            sessionId = sessionId,
            state = PlaybackNetworkState.Paused,
            reason = PlaybackStateReason.UserPause,
        ).also { activePlan = it }
        mutableState.value = PlaybackCoordinatorState(
            phase = GamePhase.Paused(pausedPlan, positionMs = 0L),
            selectedSong = pausedPlan.song,
            modal = PlaybackModal.Pause,
        )
    }

    override suspend fun resume() {
        val plan = activePlan ?: return
        val resumedPlan = plan.broadcastPlaybackState(
            networkController = networkController,
            sessionId = sessionId,
            state = PlaybackNetworkState.Playing,
            reason = PlaybackStateReason.Unspecified,
        ).also { activePlan = it }
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Play
        mutableState.value = PlaybackCoordinatorState(
            phase = GamePhase.Live(resumedPlan, songStartTvMs = 0L),
            selectedSong = resumedPlan.song,
        )
    }

    override suspend fun restart() {
        val current = activePlan ?: return
        scoringEngine.reset()
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Stop
        val restarted = current.copy(songInstanceSeq = ++songInstanceSeq, nextRevision = 1L)
        pendingStart = PendingPlaybackStart(
            song = restarted.song,
            parsedSong = restarted.parsedSong,
            selection = SongStartSelection(
                songId = restarted.song.songId,
                playerPhoneId = restarted.assignedSingers.single().phoneId,
                playerId = restarted.assignedSingers.single().playerId,
                difficulty = restarted.assignedSingers.single().difficulty,
                countdownEnabled = restarted.startMode == PlaybackStartMode.Countdown,
                countdownSeconds = (restarted.countdownMs ?: 0) / 1_000,
            ),
            startMode = restarted.startMode,
            countdownMs = restarted.countdownMs,
            chartEndLyricsTimeMs = restarted.stopAtLyricsTimeMs,
            songInstanceSeq = restarted.songInstanceSeq,
        )
        activePlan = restarted
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Prepare(
            audioUrl = restarted.song.audioUrl,
            videoUrl = restarted.song.videoUrl,
            videoGapSec = restarted.parsedSong.header.videoGapSec,
            seekToSec = restarted.song.startSec,
        )
        mutableState.value = PlaybackCoordinatorState(
            phase = GamePhase.Preparing(
                SongStartSelection(
                    songId = restarted.song.songId,
                    playerPhoneId = restarted.assignedSingers.single().phoneId,
                    playerId = restarted.assignedSingers.single().playerId,
                    difficulty = restarted.assignedSingers.single().difficulty,
                    countdownEnabled = restarted.startMode == PlaybackStartMode.Countdown,
                    countdownSeconds = (restarted.countdownMs ?: 0) / 1_000,
                ),
            ),
            selectedSong = restarted.song,
        )
    }

    override suspend fun quitToSongList() {
        val plan = activePlan
        scoringEngine.reset()
        activePlan = null
        pendingStart = null
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Stop
        if (plan != null) {
            val stoppedPlan = plan.broadcastPlaybackState(
                networkController = networkController,
                sessionId = sessionId,
                state = PlaybackNetworkState.Stopped,
                reason = PlaybackStateReason.UserQuit,
            )
            stoppedPlan.assignedSingers.forEach { networkController.sendSessionState(it.phoneId) }
        }
        mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Open)
    }

    suspend fun onPhoneEvent(event: PhoneEvent) {
        when (event) {
            is PhoneEvent.Disconnected -> handlePhoneDisconnected(event)
            is PhoneEvent.Connected,
            is PhoneEvent.Reconnected -> Unit
        }
    }

    override suspend fun onPlaybackEvent(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.Ready -> {
                val plan = activePlan ?: return
                scoringEngine.setSongStart(event.songStartTvMs)
                mutableState.value = PlaybackCoordinatorState(
                    phase = GamePhase.Live(plan, event.songStartTvMs),
                    selectedSong = plan.song,
                )
            }
            PlaybackEvent.Ended -> {
                activePlan?.stopForOpenSession(networkController, sessionId, PlaybackStateReason.SongEnd)
                activePlan = null
                pendingStart = null
                // Iteration 2 wires Results; Iteration 1 returns to Song List.
                mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Open)
            }
            is PlaybackEvent.Error -> recoverWithError(activePlan?.song, playbackErrorBody(event.cause))
            is PlaybackEvent.Prepared -> finalizePreparedStart(event.effectivePlaybackDurationMs)
        }
    }

    private suspend fun handlePhoneDisconnected(event: PhoneEvent.Disconnected) {
        val plan = activePlan
        val assignedSinger = plan?.assignedSingers?.firstOrNull { it.phoneId == event.clientId }
        if (plan != null && assignedSinger != null && event.wasAssignedSinger) {
            if (mutableState.value.phase is GamePhase.Countdown) {
                mutableIntents.value = mutableIntents.value + PlaybackIntent.Stop
                plan.stopForOpenSession(networkController, sessionId, PlaybackStateReason.SingerDisconnected)
                activePlan = null
                pendingStart = null
                mutableState.value = PlaybackCoordinatorState(
                    phase = GamePhase.Open,
                    selectedSong = plan.song,
                    modal = PlaybackModal.Disconnected(),
                )
            }
            // Iteration 3 wires Live-phase disconnect auto-pause (DisconnectPaused); no-op in Iter 1.
        }
    }

    private suspend fun recoverWithError(song: com.couchraoke.tv.domain.library.IndexedSong?, bodyLines: List<String>) {
        val plan = activePlan
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Stop
        plan?.stopForOpenSession(networkController, sessionId, PlaybackStateReason.Unspecified)
        activePlan = null
        pendingStart = null
        mutableState.value = PlaybackCoordinatorState(
            phase = GamePhase.Open,
            selectedSong = song,
            modal = PlaybackModal.Error(bodyLines),
        )
    }

    private suspend fun finalizePreparedStart(effectivePlaybackDurationMs: Long) {
        val start = pendingStart ?: return
        val stopAtLyricsTimeMs = start.chartEndLyricsTimeMs ?: effectivePlaybackDurationMs
        val plan = PlaybackPlan(
            song = start.song,
            parsedSong = start.parsedSong,
            assignedSingers = listOf(
                AssignedSinger(
                    phoneId = start.selection.playerPhoneId,
                    playerId = start.selection.playerId,
                    difficulty = start.selection.difficulty,
                ),
            ),
            songInstanceSeq = start.songInstanceSeq,
            startMode = start.startMode,
            countdownMs = start.countdownMs,
            stopAtLyricsTimeMs = stopAtLyricsTimeMs,
            udpPort = udpPort,
        )
        pendingStart = null
        val clockSample = networkController.sendPing(start.selection.playerPhoneId)
        if (!clockSample.isUsableClockSample(start.selection.playerPhoneId)) {
            activePlan = null
            mutableState.value = PlaybackCoordinatorState(
                phase = GamePhase.Open,
                selectedSong = start.song,
                modal = PlaybackModal.Error(
                    listOf("Network too unstable for accurate sync. Check WiFi connection and try again."),
                ),
            )
            return
        }
        networkController.sendClockAck(
            start.selection.playerPhoneId,
            ClockAckMessage(
                pingId = clockSample.pingId,
                tTvRecvMs = clockSample.tvReceiveTimeMs,
            ),
        )
        activePlan = plan
        networkController.sendAssignSinger(start.selection.playerPhoneId, plan.toAssignSingerMessage(sessionId))
        val broadcastPlan = plan.broadcastPlaybackState(
            networkController = networkController,
            sessionId = sessionId,
        ).also { activePlan = it }
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Play
        val phase = if (start.selection.countdownEnabled) {
            GamePhase.Countdown(broadcastPlan)
        } else {
            GamePhase.Live(broadcastPlan, songStartTvMs = 0L)
        }
        mutableState.value = PlaybackCoordinatorState(phase = phase, selectedSong = start.song)
    }
}

private fun loadChartNoOp(scoringEngine: ScoringEngine, parsedSong: ParsedSong, difficulty: Difficulty) {
    scoringEngine.loadChart(
        chart = parsedSong,
        micDelayMs = 0,
        medleyWindow = null,
        config = ScoringConfig(playerDifficulties = mapOf(PlayerId.P1 to difficulty), lineBonusEnabled = true),
    )
}

private fun PlaybackPlan.toAssignSingerMessage(sessionId: String): AssignSingerMessage = AssignSingerMessage(
    sessionId = sessionId,
    songInstanceSeq = songInstanceSeq,
    playerId = assignedSingers.single().playerId,
    difficulty = assignedSingers.single().difficulty,
    startMode = if (startMode == PlaybackStartMode.Countdown) StartMode.Countdown else StartMode.Live,
    countdownMs = countdownMs,
    stopAtLyricsTimeMs = stopAtLyricsTimeMs,
    udpPort = udpPort,
    songTitle = song.title,
    songArtist = song.artist,
)

private suspend fun PlaybackPlan.stopForOpenSession(
    networkController: NetworkController,
    sessionId: String,
    reason: PlaybackStateReason,
) {
    val stoppedPlan = broadcastPlaybackState(
        networkController = networkController,
        sessionId = sessionId,
        state = PlaybackNetworkState.Stopped,
        reason = reason,
    )
    stoppedPlan.assignedSingers.forEach { networkController.sendSessionState(it.phoneId) }
}

private suspend fun PlaybackPlan.broadcastPlaybackState(
    networkController: NetworkController,
    sessionId: String,
    state: PlaybackNetworkState = if (startMode == PlaybackStartMode.Countdown) {
        PlaybackNetworkState.Countdown
    } else {
        PlaybackNetworkState.Playing
    },
    reason: PlaybackStateReason = PlaybackStateReason.Unspecified,
): PlaybackPlan {
    networkController.broadcastPlaybackState(
        PlaybackStateMessage(
            sessionId = sessionId,
            songInstanceSeq = songInstanceSeq,
            revision = nextRevision,
            state = state,
            lyricsTimeMs = 0L,
            stopAtLyricsTimeMs = stopAtLyricsTimeMs,
            countdownRemainingMs = if (state == PlaybackNetworkState.Countdown) countdownMs else null,
            reason = reason,
        ),
    )
    return copy(nextRevision = nextRevision + 1L)
}

private fun playbackErrorBody(@Suppress("UNUSED_PARAMETER") cause: Throwable): List<String> = listOf(
    "This song can't be played.",
    "Check Settings > Song Library — the song's phone may be disconnected.",
)

private fun playbackErrorBody(cause: PlaybackErrorCause): List<String> = buildList {
    add("This song can't be played.")
    val detail = when (cause) {
        is PlaybackErrorCause.AudioUnavailable -> cause.message
        is PlaybackErrorCause.AudioFocusDenied -> cause.message
        is PlaybackErrorCause.PlayerError -> cause.lastWarningOrError
    }
    if (!detail.isNullOrBlank()) add(detail)
}

private fun com.couchraoke.tv.data.network.PongResponse.isUsableClockSample(expectedPhoneId: String): Boolean =
    isValidSample && phoneId == expectedPhoneId && pingId.isNotBlank()

private data class PendingPlaybackStart(
    val song: com.couchraoke.tv.domain.library.IndexedSong,
    val parsedSong: ParsedSong,
    val selection: SongStartSelection,
    val startMode: PlaybackStartMode,
    val countdownMs: Int?,
    val chartEndLyricsTimeMs: Long?,
    val songInstanceSeq: Long,
)

private object Iteration1ScoringNoOp : ScoringEngine {
    override fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig) = Unit
    override fun setSongStart(songStartTvMs: Long) = Unit
    override suspend fun finalizeAll(): Map<PlayerId, PlayerScore> = emptyMap()
    override fun reset() = Unit
}
