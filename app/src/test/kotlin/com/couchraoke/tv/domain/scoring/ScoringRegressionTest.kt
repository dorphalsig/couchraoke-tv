package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.internal.BeatWindowCalculator
import com.couchraoke.tv.domain.scoring.internal.Phase0ScoringEngine
import com.couchraoke.tv.domain.scoring.internal.PitchSampleSource
import com.couchraoke.tv.domain.scoring.internal.ScoreBonusAndRounding
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.SongHeader
import com.couchraoke.tv.domain.usdx.model.SongTiming
import com.couchraoke.tv.domain.usdx.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringRegressionTest {
    @Test(timeout = 30_000)
    fun perfectPerformanceTotalsExactly10000() {
        val chart = chart(
            lines = listOf(
                line(0, note(NoteType.Normal, startBeat = 0, durationBeats = 4, toneSemitone = 0)),
                line(1, note(NoteType.Golden, startBeat = 4, durationBeats = 2, toneSemitone = 0)),
            ),
            trackScoreValue = 8,
        )
        val score = score(
            chart = chart,
            samples = samplesForNotes(chart.tracks.single().lines.flatMap(Line::notes)),
            lineBonusEnabled = true,
        )

        assertEquals(4_500, score.scoreInt)
        assertEquals(4_500, score.scoreGoldenInt)
        assertEquals(1_000, score.scoreLineInt)
        assertEquals(10_000, score.scoreTotalInt)
    }

    @Test(timeout = 30_000)
    fun emptyLineDoesNotReceiveLineBonus() {
        val chart = chart(
            lines = listOf(
                line(0, note(NoteType.Freestyle, startBeat = 0, durationBeats = 4, toneSemitone = 0)),
                line(1, note(NoteType.Normal, startBeat = 4, durationBeats = 4, toneSemitone = 0)),
            ),
            trackScoreValue = 4,
        )
        val score = score(
            chart = chart,
            samples = samplesForNotes(listOf(chart.tracks.single().lines[1].notes.single())),
            lineBonusEnabled = true,
        )

        assertEquals(9_000, score.scoreInt)
        assertEquals(0, score.scoreGoldenInt)
        assertEquals(1_000, score.scoreLineInt)
        assertEquals(10_000, score.scoreTotalInt)
    }

    @Test(timeout = 30_000)
    fun medleyWindowFiltersScoreValueAndSamples() {
        val notes = listOf(
            note(NoteType.Normal, startBeat = 0, durationBeats = 4, toneSemitone = 0),
            note(NoteType.Normal, startBeat = 4, durationBeats = 4, toneSemitone = 0),
        )
        val chart = chart(
            lines = listOf(line(0, notes[0]), line(1, notes[1])),
            trackScoreValue = 8,
        )
        val score = score(
            chart = chart,
            samples = samplesForNotes(notes),
            medleyWindow = BeatRange(startBeat = 4, endBeat = 8),
            lineBonusEnabled = false,
        )

        assertEquals(10_000, score.scoreInt)
        assertEquals(0, score.scoreGoldenInt)
        assertEquals(0, score.scoreLineInt)
        assertEquals(10_000, score.scoreTotalInt)
    }

    @Test(timeout = 30_000)
    fun displayTotalNeverExceeds10000() {
        val score = ScoreBonusAndRounding.toPlayerScore(
            score = 3_004.9,
            scoreGolden = 6_995.1,
            scoreLine = 0.0,
            scoreLast = 0.0,
        )

        assertEquals(3_000, score.scoreInt)
        assertEquals(7_000, score.scoreGoldenInt)
        assertEquals(10_000, score.scoreTotalInt)
        assertTrue(score.scoreTotalInt <= 10_000)
    }

    private fun score(
        chart: ParsedSong,
        samples: List<PitchSample>,
        medleyWindow: BeatRange? = null,
        lineBonusEnabled: Boolean,
    ) = Phase0ScoringEngine(PitchSampleSource { samples }).run {
        loadChart(
            chart = chart,
            micDelayMs = 0,
            medleyWindow = medleyWindow,
            config = ScoringConfig(
                playerDifficulties = mapOf(PlayerId.P1 to Difficulty.Hard),
                lineBonusEnabled = lineBonusEnabled,
            ),
        )
        setSongStart(0L)
        runBlocking { finalizeAll() }.getValue(PlayerId.P1)
    }

    private fun samplesForNotes(notes: List<NoteEvent>): List<PitchSample> = notes.map { note ->
        PitchSample(
            playerId = PlayerId.P1,
            midiNote = note.toneSemitone + 36,
            tvTimeMs = BeatWindowCalculator.noteStartTvMs(
                songStartTvMs = 0L,
                startBeatFile = note.startBeatFile,
                bpmFile = 120f,
                gapMs = 0f,
                micDelayMs = 0,
            ),
        )
    }

    private fun chart(
        lines: List<Line>,
        trackScoreValue: Long,
    ): ParsedSong = ParsedSong(
        songId = "test::scoring-regression",
        header = SongHeader(
            title = "Scoring Regression",
            artist = "Fixture",
            bpmFile = 120f,
            gapMs = 0f,
            audio = "audio.ogg",
            startSec = null,
            endMs = null,
            videoGapSec = null,
            previewStartSec = null,
            video = null,
            cover = null,
            background = null,
            instrumental = null,
            vocals = null,
            version = "1.0.0",
            year = null,
            genre = null,
            album = null,
            isDuet = false,
            p1Name = null,
            p2Name = null,
            medleyStartBeat = null,
            medleyEndBeat = null,
            customTags = emptyList(),
        ),
        timing = SongTiming(bpmFile = 120f),
        tracks = listOf(
            Track(
                playerId = PlayerId.P1,
                lines = lines,
                trackScoreValue = trackScoreValue,
            ),
        ),
        diagnostics = emptyList(),
    )

    private fun line(lineIndex: Int, vararg notes: NoteEvent): Line = Line(
        lineIndex = lineIndex,
        notes = notes.toList(),
        lineScoreValue = notes.sumOf { note -> note.durationBeats.toLong() * scoreFactor(note.noteType) },
    )

    private fun note(
        noteType: NoteType,
        startBeat: Int,
        durationBeats: Int,
        toneSemitone: Int,
    ): NoteEvent = NoteEvent(
        noteType = noteType,
        startBeatFile = startBeat,
        durationBeats = durationBeats,
        toneSemitone = toneSemitone,
        lyric = "la",
    )

    private fun scoreFactor(noteType: NoteType): Long = when (noteType) {
        NoteType.Freestyle -> 0L
        NoteType.Normal, NoteType.Rap -> 1L
        NoteType.Golden, NoteType.RapGolden -> 2L
    }
}
