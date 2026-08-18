package com.couchraoke.tv.presentation.navigation

import com.couchraoke.tv.domain.playback.GamePhase

internal fun GamePhase.stopAtLyricsTimeMsOrZero(): Long = when (this) {
    is GamePhase.Countdown -> plan.stopAtLyricsTimeMs
    is GamePhase.Live -> plan.stopAtLyricsTimeMs
    is GamePhase.Paused -> plan.stopAtLyricsTimeMs
    is GamePhase.Stopped -> plan.stopAtLyricsTimeMs
    is GamePhase.DisconnectPaused -> plan.stopAtLyricsTimeMs
    is GamePhase.Open,
    is GamePhase.Preparing,
    is GamePhase.Error,
    is GamePhase.Results,
    -> 0L
}
