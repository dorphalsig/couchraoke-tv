package com.couchraoke.tv.domain.timing

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.TrackId
import kotlin.math.floor

object BeatTimingEngine {

    fun computeBeatCursor(context: TimingContext, lyricsTimeSec: Double): BeatCursor {
        val highlightTimeSec = lyricsTimeSec - context.gapMs / 1000.0
        val midBeat = highlightTimeSec * context.bpmInternal / 60.0
        val currentBeat = floor(midBeat).toInt()
        return BeatCursor(lyricsTimeSec, highlightTimeSec, midBeat, currentBeat)
    }

    fun beatsToMs(beats: Double, context: TimingContext): Double {
        return beats * 60_000.0 / context.bpmInternal
    }

    fun msToBeats(ms: Double, context: TimingContext): Double {
        return ms * context.bpmInternal / 60_000.0
    }

    fun computePlaybackBounds(context: TimingContext): PlaybackBounds {
        return PlaybackBounds.from(context)
    }

    fun computeNoteTimingWindow(
        context: TimingContext,
        trackId: TrackId,
        lineStartBeat: Int,
        note: NoteEvent,
    ): NoteTimingWindow {
        val songStartTvMs = context.songStartTvMs
            ?: throw IllegalStateException("songStartTvMs must be set before computing note windows")

        val noteStartTvMs = songStartTvMs +
            context.gapMs +
            beatsToMs(note.startBeat.toDouble(), context).toLong() +
            context.micDelayMs

        val noteEndTvMs = songStartTvMs +
            context.gapMs +
            beatsToMs((note.startBeat + note.durationBeats).toDouble(), context).toLong() +
            context.micDelayMs

        return NoteTimingWindow(
            trackId = trackId,
            lineStartBeat = lineStartBeat,
            noteType = note.noteType,
            startBeat = note.startBeat,
            durationBeats = note.durationBeats,
            noteStartTvMs = noteStartTvMs,
            noteEndTvMs = noteEndTvMs,
        )
    }

    fun isPitchFrameEligible(frame: PitchFrameTiming, window: NoteTimingWindow): Boolean {
        return frame.latenessMs <= 450 &&
            frame.frameTimestampTvMs >= window.noteStartTvMs &&
            frame.frameTimestampTvMs < window.noteEndTvMs
    }
}
