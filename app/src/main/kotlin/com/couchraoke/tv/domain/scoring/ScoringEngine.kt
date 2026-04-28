package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.model.ParsedSong

interface ScoringEngine {
    fun loadChart(chart: ParsedSong, micDelayMs: Int, medleyWindow: BeatRange?, config: ScoringConfig)

    fun setSongStart(songStartTvMs: Long)

    suspend fun finalizeAll(): Map<PlayerId, PlayerScore>

    fun reset()
}
