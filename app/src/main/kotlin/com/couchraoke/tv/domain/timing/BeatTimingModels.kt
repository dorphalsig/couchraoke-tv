package com.couchraoke.tv.domain.timing

import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.TrackId

// ---------------------------------------------------------------------------
// State enums
// ---------------------------------------------------------------------------

enum class SongPlaybackState {
    PRE_ROLL,
    ACTIVE,
    ENDED,
}

enum class NoteWindowState {
    PENDING,
    COLLECTING,
    WAITING_FOR_FINALIZATION,
    FINALIZED,
}

// ---------------------------------------------------------------------------
// TimingContext
// ---------------------------------------------------------------------------

data class TimingContext(
    val songIdentifier: String,
    val bpmFile: Double,
    val gapMs: Int,
    val startSec: Double? = null,
    val endMs: Int? = null,
    val songStartTvMs: Long? = null,
    val micDelayMs: Int = 0,
    val mediaDurationSec: Double? = null,
) {
    val bpmInternal: Double = bpmFile * 4.0

    init {
        require(bpmFile > 0.0) { "bpmFile must be positive, was $bpmFile" }
        require(micDelayMs in 0..400) {
            "micDelayMs must be in 0..400 inclusive, was $micDelayMs"
        }
    }
}

// ---------------------------------------------------------------------------
// PlaybackBounds
// ---------------------------------------------------------------------------

data class PlaybackBounds(
    val initialSongTimeSec: Double,
    val effectiveSongEndSec: Double?,
    val endsFromHeader: Boolean,
) {
    companion object {
        fun from(context: TimingContext): PlaybackBounds {
            val initialSongTimeSec = context.startSec ?: 0.0
            val endsFromHeader = context.endMs != null && context.endMs > 0
            val effectiveSongEndSec = if (endsFromHeader) {
                context.endMs!! / 1000.0
            } else {
                context.mediaDurationSec
            }
            return PlaybackBounds(
                initialSongTimeSec = initialSongTimeSec,
                effectiveSongEndSec = effectiveSongEndSec,
                endsFromHeader = endsFromHeader,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// BeatCursor
// ---------------------------------------------------------------------------

data class BeatCursor(
    val lyricsTimeSec: Double,
    val highlightTimeSec: Double,
    val midBeat: Double,
    val currentBeat: Int,
)

// ---------------------------------------------------------------------------
// NoteTimingWindow
// ---------------------------------------------------------------------------

data class NoteTimingWindow(
    val trackId: TrackId,
    val lineStartBeat: Int,
    val noteType: NoteType,
    val startBeat: Int,
    val durationBeats: Int,
    val noteStartTvMs: Long,
    val noteEndTvMs: Long,
) {
    val finalizationTvMs: Long = noteEndTvMs + 450

    init {
        require(noteEndTvMs >= noteStartTvMs) {
            "noteEndTvMs ($noteEndTvMs) must be >= noteStartTvMs ($noteStartTvMs)"
        }
    }
}

// ---------------------------------------------------------------------------
// PitchFrameTiming
// ---------------------------------------------------------------------------

data class PitchFrameTiming(
    val frameTimestampTvMs: Long,
    val arrivalTimeTvMs: Long,
    val eligibleForCollection: Boolean,
) {
    val latenessMs: Long = arrivalTimeTvMs - frameTimestampTvMs
}
