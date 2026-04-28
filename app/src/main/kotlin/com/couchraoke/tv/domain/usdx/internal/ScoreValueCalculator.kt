package com.couchraoke.tv.domain.usdx.internal

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.Track

internal object ScoreValueCalculator {
    fun buildTrack(playerId: PlayerId, lineNotes: List<List<NoteEvent>>): Track {
        val lines = lineNotes.mapIndexed { index, notes ->
            Line(
                lineIndex = index,
                notes = notes,
                lineScoreValue = lineScoreValue(notes),
            )
        }

        return Track(
            playerId = playerId,
            lines = lines,
            trackScoreValue = lines.sumOf(Line::lineScoreValue),
        )
    }

    private fun lineScoreValue(notes: List<NoteEvent>): Long =
        notes.sumOf { note -> note.durationBeats.toLong() * scoreFactor(note.noteType) }

    private fun scoreFactor(noteType: NoteType): Long =
        when (noteType) {
            NoteType.Freestyle -> 0L
            NoteType.Normal, NoteType.Rap -> 1L
            NoteType.Golden, NoteType.RapGolden -> 2L
        }
}
