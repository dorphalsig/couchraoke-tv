package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.Track

internal data class ScoringSample(
    val tvTimeMs: Long,
    val toneValid: Boolean,
    val toneSemitone: Int?,
)

internal data class BeatDelta(
    val beat: Int,
    val activeNoteType: NoteType?,
    val toneValid: Boolean,
    val scoreDelta: Int,
    val scoreGoldenDelta: Int,
)

internal data class LineBonusBreakdown(
    val lineIndex: Int,
    val lineType: NoteType,
    val maxLineScore: Double,
    val lineScore: Double,
    val linePerfection: Double,
    val lineBonus: Double,
)

internal data class TrackScoreBreakdown(
    val score: Double,
    val scoreGolden: Double,
    val scoreLine: Double,
    val scoreLast: Double,
    val perBeat: List<BeatDelta>,
    val lineBonuses: List<LineBonusBreakdown>,
    val effectiveTrackScoreValue: Long,
    val maxSongPoints: Int,
    val maxLineBonusPool: Int,
    val lineBonusPerLine: Double,
)

internal data class TrackScoringContext(
    val track: Track,
    val samples: List<ScoringSample>,
    val difficulty: Difficulty,
    val medleyWindow: BeatRange?,
    val lineBonusEnabled: Boolean,
    val bpmFile: Float,
    val gapMs: Float,
    val micDelayMs: Int,
    val songStartTvMs: Long,
)

internal object NoteScoreCalculator {
    fun scoreTrack(context: TrackScoringContext): TrackScoreBreakdown {
        val session = ScoringSession(context)
        session.processSamples()
        session.finalizeRemainingLines()
        return session.toBreakdown()
    }

    private class ScoringSession(
        private val context: TrackScoringContext,
    ) {
        private val maxSongPoints = if (context.lineBonusEnabled) 9_000 else 10_000
        private val maxLineBonusPool = if (context.lineBonusEnabled) 1_000 else 0
        private val lineStates = context.track.lines.map(::buildLineState)
        private val effectiveTrackScoreValue = lineStates.sumOf(LineState::effectiveLineScoreValue)
        private val nonEmptyLines = lineStates.count { it.effectiveLineScoreValue > 0 }
        private val lineBonusPerLine =
            ScoreBonusAndRounding.lineBonusPerLine(maxLineBonusPool, nonEmptyLines)

        private var score = 0.0
        private var scoreGolden = 0.0
        private var scoreLine = 0.0
        private var scoreLast = 0.0

        private val perBeat = mutableListOf<BeatDelta>()
        private val lineBonuses = mutableListOf<LineBonusBreakdown>()

        fun processSamples() {
            lineStates.forEach(::scoreLine)
        }

        fun finalizeRemainingLines() = Unit

        fun toBreakdown(): TrackScoreBreakdown =
            TrackScoreBreakdown(
                score = score,
                scoreGolden = scoreGolden,
                scoreLine = scoreLine,
                scoreLast = scoreLast,
                perBeat = perBeat,
                lineBonuses = lineBonuses,
                effectiveTrackScoreValue = effectiveTrackScoreValue,
                maxSongPoints = maxSongPoints,
                maxLineBonusPool = maxLineBonusPool,
                lineBonusPerLine = lineBonusPerLine,
            )

        private fun scoreLine(lineState: LineState) {
            lineState.line.notes.forEach { note ->
                val noteScore = scoreNote(note)
                when (note.noteType) {
                    NoteType.Normal, NoteType.Rap -> score += noteScore
                    NoteType.Golden, NoteType.RapGolden -> scoreGolden += noteScore
                    NoteType.Freestyle -> Unit
                }
            }

            if (lineState.effectiveLineScoreValue > 0) {
                finalizeLine(lineState)
            }
        }

        private fun scoreNote(note: NoteEvent): Double {
            val effectiveWindow = effectiveNoteWindow(note, context.medleyWindow)
            val qualifyingSamples = effectiveWindow?.let(::qualifyingSamples).orEmpty()
            return if (effectiveTrackScoreValue == 0L || effectiveWindow == null || qualifyingSamples.isEmpty()) {
                0.0
            } else {
                val hits = qualifyingSamples.count { sample ->
                    NoteHitEvaluator.isHit(
                        noteType = note.noteType,
                        targetToneSemitone = note.toneSemitone,
                        difficulty = context.difficulty,
                        sample = sample,
                    )
                }
                val noteMaxScore = maxSongPoints.toDouble() / effectiveTrackScoreValue *
                    scoreFactor(note.noteType) * effectiveWindow.durationBeats
                noteMaxScore * hits.toDouble() / qualifyingSamples.size
            }
        }

        private fun qualifyingSamples(effectiveWindow: EffectiveNoteWindow): List<ScoringSample> {
            val noteStartTvMs = BeatWindowCalculator.noteStartTvMs(
                songStartTvMs = context.songStartTvMs,
                startBeatFile = effectiveWindow.startBeatFile,
                bpmFile = context.bpmFile,
                gapMs = context.gapMs,
                micDelayMs = context.micDelayMs,
            )
            val noteEndTvMs = BeatWindowCalculator.noteEndTvMs(
                songStartTvMs = context.songStartTvMs,
                startBeatFile = effectiveWindow.startBeatFile,
                durationBeats = effectiveWindow.durationBeats,
                bpmFile = context.bpmFile,
                gapMs = context.gapMs,
                micDelayMs = context.micDelayMs,
            )
            return context.samples.filter { sample ->
                sample.tvTimeMs >= noteStartTvMs && sample.tvTimeMs < noteEndTvMs
            }
        }

        private fun finalizeLine(lineState: LineState) {
            val maxLineScore = maxLineScore(lineState)
            val currentLineScore = (score + scoreGolden) - scoreLast
            val perfection =
                ScoreBonusAndRounding.linePerfection(currentLineScore, maxLineScore)
            val bonus = lineBonusPerLine * perfection

            scoreLine += bonus
            scoreLast = score + scoreGolden
            lineState.finalized = true

            lineBonuses += LineBonusBreakdown(
                lineIndex = lineState.line.lineIndex,
                lineType = lineState.lineType,
                maxLineScore = maxLineScore,
                lineScore = currentLineScore,
                linePerfection = perfection,
                lineBonus = bonus,
            )
        }

        private fun maxLineScore(lineState: LineState): Double {
            if (effectiveTrackScoreValue == 0L) {
                return 0.0
            }
            return maxSongPoints.toDouble() *
                lineState.effectiveLineScoreValue / effectiveTrackScoreValue
        }

        private fun buildLineState(line: Line): LineState {
            val scorableNotes = line.notes.filter { note ->
                scoreFactor(note.noteType) > 0 &&
                    effectiveDuration(note, context.medleyWindow) > 0
            }
            return LineState(
                line = line,
                effectiveLineScoreValue = effectiveLineScoreValue(
                    line,
                    context.medleyWindow,
                ),
                lineType = scorableNotes.firstOrNull()?.noteType ?: NoteType.Freestyle,
            )
        }
    }

    private fun effectiveLineScoreValue(
        line: Line,
        medleyWindow: BeatRange?,
    ): Long = line.notes.sumOf { note ->
        effectiveDuration(note, medleyWindow).toLong() * scoreFactor(note.noteType)
    }

    private fun effectiveDuration(
        note: NoteEvent,
        medleyWindow: BeatRange?,
    ): Int {
        if (scoreFactor(note.noteType) == 0L) {
            return 0
        }
        val start = maxOf(
            note.startBeatFile,
            medleyWindow?.startBeat ?: note.startBeatFile,
        )
        val end = minOf(
            note.endBeatFileExclusive,
            medleyWindow?.endBeat ?: note.endBeatFileExclusive,
        )
        return (end - start).coerceAtLeast(0)
    }

    private fun effectiveNoteWindow(
        note: NoteEvent,
        medleyWindow: BeatRange?,
    ): EffectiveNoteWindow? {
        if (scoreFactor(note.noteType) == 0L) {
            return null
        }
        val startBeatFile = maxOf(
            note.startBeatFile,
            medleyWindow?.startBeat ?: note.startBeatFile,
        )
        val endBeatExclusive = minOf(
            note.endBeatFileExclusive,
            medleyWindow?.endBeat ?: note.endBeatFileExclusive,
        )
        val durationBeats = (endBeatExclusive - startBeatFile).coerceAtLeast(0)
        return if (durationBeats == 0) {
            null
        } else {
            EffectiveNoteWindow(
                startBeatFile = startBeatFile,
                endBeatExclusive = endBeatExclusive,
                durationBeats = durationBeats,
            )
        }
    }

    private fun scoreFactor(noteType: NoteType): Long =
        when (noteType) {
            NoteType.Freestyle -> 0L
            NoteType.Normal, NoteType.Rap -> 1L
            NoteType.Golden, NoteType.RapGolden -> 2L
        }

    private data class EffectiveNoteWindow(
        val startBeatFile: Int,
        val endBeatExclusive: Int,
        val durationBeats: Int,
    )

    private data class LineState(
        val line: Line,
        val effectiveLineScoreValue: Long,
        val lineType: NoteType,
        var finalized: Boolean = false,
    )
}
