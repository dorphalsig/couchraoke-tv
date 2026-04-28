package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.usdx.model.NoteEvent

internal object QualifyingSampleSelector {
    fun qualifyingBeats(
        note: NoteEvent,
        oldBeatD: Int,
        currentBeatD: Int,
        medleyWindow: BeatRange?,
    ): List<Int> {
        if (currentBeatD <= oldBeatD) {
            return emptyList()
        }

        return (oldBeatD + 1..currentBeatD)
            .filter { beat -> beat >= note.startBeatFile && beat < note.endBeatFileExclusive }
            .filter { beat -> medleyWindow == null || (beat >= medleyWindow.startBeat && beat < medleyWindow.endBeat) }
    }
}
