package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.ScoringEngine
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.model.ParsedSong

internal class Phase0ScoringEngine(
    private val pitchSampleSource: PitchSampleSource = PitchSampleSource { emptyList() },
) : ScoringEngine {
    private var chart: ParsedSong? = null
    private var micDelayMs: Int = 0
    private var medleyWindow: BeatRange? = null
    private var config: ScoringConfig = ScoringConfig(emptyMap(), lineBonusEnabled = true)
    private var songStartTvMs: Long = 0L

    override fun loadChart(
        chart: ParsedSong,
        micDelayMs: Int,
        medleyWindow: BeatRange?,
        config: ScoringConfig,
    ) {
        this.chart = chart
        this.micDelayMs = micDelayMs.coerceIn(0, 400)
        this.medleyWindow = medleyWindow
        this.config = config
    }

    override fun setSongStart(songStartTvMs: Long) {
        this.songStartTvMs = songStartTvMs
    }

    override suspend fun finalizeAll(): Map<PlayerId, PlayerScore> {
        val loadedChart = checkNotNull(chart)
        val samplesByPlayer = pitchSampleSource.samples()
            .mapNotNull(::normalizeSample)
            .groupBy(NormalizedPitchSample::playerId)

        return loadedChart.tracks.associate { track ->
            track.playerId to scoreTrack(loadedChart, track.playerId, samplesByPlayer)
        }
    }

    override fun reset() {
        chart = null
        micDelayMs = 0
        medleyWindow = null
        config = ScoringConfig(emptyMap(), lineBonusEnabled = true)
        songStartTvMs = 0L
    }

    private fun scoreTrack(
        chart: ParsedSong,
        playerId: PlayerId,
        samplesByPlayer: Map<PlayerId, List<NormalizedPitchSample>>,
    ): PlayerScore {
        val track = chart.tracks.first { it.playerId == playerId }
        val context = TrackScoringContext(
            track = track,
            samples = samplesByPlayer[playerId].orEmpty().map(::toScoringSample),
            difficulty = config.playerDifficulties[playerId] ?: Difficulty.Medium,
            medleyWindow = medleyWindow,
            lineBonusEnabled = config.lineBonusEnabled,
            bpmFile = chart.timing.bpmFile,
            gapMs = chart.header.gapMs,
            micDelayMs = micDelayMs,
            songStartTvMs = songStartTvMs,
        )
        val breakdown = NoteScoreCalculator.scoreTrack(context)
        return ScoreBonusAndRounding.toPlayerScore(
            score = breakdown.score,
            scoreGolden = breakdown.scoreGolden,
            scoreLine = breakdown.scoreLine,
            scoreLast = breakdown.scoreLast,
        )
    }

    private fun toScoringSample(sample: NormalizedPitchSample): ScoringSample =
        ScoringSample(
            tvTimeMs = sample.tvTimeMs,
            toneValid = sample.toneValid,
            toneSemitone = sample.toneSemitone,
        )

    private fun normalizeSample(sample: PitchSample): NormalizedPitchSample? {
        val tvTimeMs = sample.tvTimeMs ?: return null
        val toneValid = sample.midiNote != 255
        return NormalizedPitchSample(
            playerId = sample.playerId,
            tvTimeMs = tvTimeMs,
            toneValid = toneValid,
            toneSemitone = if (toneValid) sample.midiNote - 36 else null,
        )
    }

    internal data class NormalizedPitchSample(
        val playerId: PlayerId,
        val tvTimeMs: Long,
        val toneValid: Boolean,
        val toneSemitone: Int?,
    )
}
