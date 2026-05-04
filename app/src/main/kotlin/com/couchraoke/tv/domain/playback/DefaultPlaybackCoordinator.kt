package com.couchraoke.tv.domain.playback

import com.couchraoke.tv.data.network.AssignSingerMessage
import com.couchraoke.tv.data.network.NetworkController
import com.couchraoke.tv.data.network.PlaybackNetworkState
import com.couchraoke.tv.data.network.PlaybackStateMessage
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
import com.couchraoke.tv.presentation.playback.PlaybackEvent
import com.couchraoke.tv.presentation.playback.PlaybackIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DefaultPlaybackCoordinator(
    private val libraryManager: LibraryManager,
    private val networkController: NetworkController,
    private val usdxParser: UsdxParser,
    private val udpPort: Int,
    private val scoringEngine: ScoringEngine = Iteration1ScoringNoOp,
    private val sessionId: String = "tv-session-001",
) : PlaybackCoordinator {
    private val mutableState = MutableStateFlow(PlaybackCoordinatorState())
    private val mutableIntents = MutableStateFlow<List<PlaybackIntent>>(emptyList())
    private var songInstanceSeq = 0L
    private var activePlan: PlaybackPlan? = null
    private var pendingStart: PendingPlaybackStart? = null

    override val state: StateFlow<PlaybackCoordinatorState> = mutableState
    val intents: StateFlow<List<PlaybackIntent>> = mutableIntents

    override suspend fun startSong(selection: SongStartSelection) {
        val song = requireNotNull(libraryManager.getSong(selection.songId))
        mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Preparing(selection), selectedSong = song)
        val txtBytes = networkController.fetchTxt(song.txtUrl).getOrThrow()
        val parsedSong = usdxParser.parse(song.songId, txtBytes).getOrThrow()
        loadChartNoOp(parsedSong, selection.difficulty)
        val countdownMs = if (selection.countdownEnabled) selection.countdownSeconds * 1_000 else null
        val startMode = if (selection.countdownEnabled) PlaybackStartMode.Countdown else PlaybackStartMode.Live
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
                videoGapSec = null,
                seekToSec = song.startSec,
                chartEndLyricsTimeMs = chartEndLyricsTimeMs,
            ),
        )
    }

    override suspend fun pause() = Unit

    override suspend fun resume() = Unit

    override suspend fun restart() = Unit

    override suspend fun quitToSongList() {
        mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Open)
    }

    override suspend fun onPlaybackEvent(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.Ready -> {
                val plan = activePlan ?: return
                // Iteration 2 wires ScoringEngine.setSongStart() and ScoringEngine.start() here.
                mutableState.value = PlaybackCoordinatorState(
                    phase = GamePhase.Live(plan, event.songStartTvMs),
                    selectedSong = plan.song,
                )
            }
            PlaybackEvent.Ended -> {
                activePlan = null
                // Iteration 2 wires Results; Iteration 1 returns to Song List.
                mutableState.value = PlaybackCoordinatorState(phase = GamePhase.Open)
            }
            is PlaybackEvent.Error -> {
                pendingStart = null
                mutableState.value = PlaybackCoordinatorState(
                    phase = GamePhase.Error("ERROR", listOf("This song can't be played.")),
                    modal = PlaybackModal.Error(listOf("This song can't be played.")),
                )
            }
            is PlaybackEvent.Prepared -> finalizePreparedStart(event.effectivePlaybackDurationMs)
        }
    }

    /** Structural placeholder: calls `loadChart` to exercise the interface contract, but
     *  `Iteration1ScoringNoOp.loadChart` is a no-op and performs no real chart loading.
     *  Iteration 2 replaces this with a real `ScoringEngine` implementation and wires scoring start. */
    private fun loadChartNoOp(parsedSong: ParsedSong, difficulty: Difficulty) {
        scoringEngine.loadChart(
            chart = parsedSong,
            micDelayMs = 0,
            medleyWindow = null,
            config = ScoringConfig(playerDifficulties = mapOf(PlayerId.P1 to difficulty), lineBonusEnabled = true),
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
        activePlan = plan
        networkController.sendPing(start.selection.playerPhoneId)
        networkController.sendAssignSinger(start.selection.playerPhoneId, plan.toAssignSingerMessage())
        networkController.broadcastPlaybackState(plan.toPlaybackStateMessage())
        mutableIntents.value = mutableIntents.value + PlaybackIntent.Play(stopAtLyricsTimeMs)
        val phase = if (start.selection.countdownEnabled) {
            GamePhase.Countdown(plan)
        } else {
            GamePhase.Live(plan, songStartTvMs = 0L)
        }
        mutableState.value = PlaybackCoordinatorState(phase = phase, selectedSong = start.song)
    }

    private fun PlaybackPlan.toAssignSingerMessage(): AssignSingerMessage = AssignSingerMessage(
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

    private fun PlaybackPlan.toPlaybackStateMessage(): PlaybackStateMessage = PlaybackStateMessage(
        sessionId = sessionId,
        songInstanceSeq = songInstanceSeq,
        revision = 1L,
        state = if (startMode == PlaybackStartMode.Countdown) {
            PlaybackNetworkState.Countdown
        } else {
            PlaybackNetworkState.Playing
        },
        lyricsTimeMs = 0L,
        stopAtLyricsTimeMs = stopAtLyricsTimeMs,
        countdownRemainingMs = countdownMs,
        reason = "song_start",
    )
}

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
