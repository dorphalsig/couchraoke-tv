package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.internal.NoteHitEvaluator
import com.couchraoke.tv.domain.scoring.internal.OctaveNormalizer
import com.couchraoke.tv.domain.scoring.internal.Phase0ScoringEngine
import com.couchraoke.tv.domain.scoring.internal.PitchSampleSource
import com.couchraoke.tv.domain.scoring.internal.QualifyingSampleSelector
import com.couchraoke.tv.domain.scoring.internal.ScoreBonusAndRounding
import com.couchraoke.tv.domain.scoring.internal.ScoringSample
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.model.CustomHeaderTag
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.SongHeader
import com.couchraoke.tv.domain.usdx.model.SongTiming
import com.couchraoke.tv.domain.usdx.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringMathUnitTest {
    @Test(timeout = 30_000)
    fun respectsDifficultyThresholds() {
        val target = 0
        val sample = ScoringSample(tvTimeMs = 0L, toneValid = true, toneSemitone = 2)

        assertTrue(NoteHitEvaluator.isHit(NoteType.Normal, target, Difficulty.Easy, sample))
        assertFalse(NoteHitEvaluator.isHit(NoteType.Normal, target, Difficulty.Medium, sample))
        assertFalse(NoteHitEvaluator.isHit(NoteType.Normal, target, Difficulty.Hard, sample))
    }

    @Test(timeout = 30_000)
    fun normalizesAcrossOctavesWithoutModuloReduction() {
        assertEquals(12, OctaveNormalizer.normalize(0, 12))
        assertEquals(0, OctaveNormalizer.normalize(24, 0))
    }

    @Test(timeout = 30_000)
    fun rapRequiresToneValidButIgnoresPitchDifference() {
        assertTrue(
            NoteHitEvaluator.isHit(
                NoteType.Rap,
                targetToneSemitone = 0,
                difficulty = Difficulty.Medium,
                sample = ScoringSample(tvTimeMs = 0L, toneValid = true, toneSemitone = 48),
            ),
        )
        assertFalse(
            NoteHitEvaluator.isHit(
                NoteType.Rap,
                targetToneSemitone = 0,
                difficulty = Difficulty.Medium,
                sample = ScoringSample(tvTimeMs = 0L, toneValid = false, toneSemitone = null),
            ),
        )
    }

    @Test(timeout = 30_000)
    fun selectsQualifyingBeatsWithExclusiveEndAndMedleyFiltering() {
        val note = NoteEvent(
            noteType = NoteType.Normal,
            startBeatFile = 4,
            durationBeats = 4,
            toneSemitone = 0,
            lyric = "la",
        )

        assertEquals(listOf(4, 5, 6, 7), QualifyingSampleSelector.qualifyingBeats(note, 3, 7, null))
        assertEquals(listOf(5, 6), QualifyingSampleSelector.qualifyingBeats(note, 3, 7, BeatRange(5, 7)))
    }

    @Test(timeout = 30_000)
    fun phase0EngineUsesExplicitPitchSampleSourceWithoutPublicSampleLoading() {
        val chart = singleNoteChart(durationBeats = 4)
        val engine = Phase0ScoringEngine(
            PitchSampleSource {
                listOf(
                    PitchSample(PlayerId.P1, midiNote = 36, tvTimeMs = 0L),
                    PitchSample(PlayerId.P1, midiNote = 255, tvTimeMs = 125L),
                )
            },
        )
        engine.loadChart(
            chart = chart,
            micDelayMs = 0,
            medleyWindow = null,
            config = ScoringConfig(
                playerDifficulties = mapOf(PlayerId.P1 to Difficulty.Hard),
                lineBonusEnabled = true,
            ),
        )
        engine.setSongStart(0L)

        val score = runBlocking { engine.finalizeAll() }.getValue(PlayerId.P1)

        assertEquals(4_500.0, score.score, 1e-6)
        assertEquals(500.1111358, score.scoreLine, 1e-6)
        assertEquals(5_000, score.scoreTotalInt)
    }

    @Test(timeout = 30_000)
    fun keepsAsymmetricRoundingAndCapsTotal() {
        val score = ScoreBonusAndRounding.toPlayerScore(
            score = 2_995.1,
            scoreGolden = 6_004.9,
            scoreLine = 995.0,
            scoreLast = 0.0,
        )

        assertEquals(3_000, score.scoreInt)
        assertEquals(6_000, score.scoreGoldenInt)
        assertEquals(990, score.scoreLineInt)
        assertEquals(9_990, score.scoreTotalInt)
    }

    private fun singleNoteChart(durationBeats: Int): ParsedSong = ParsedSong(
        songId = "test::single-note",
        header = SongHeader(
            title = "Single Note",
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
            customTags = emptyList<CustomHeaderTag>(),
        ),
        timing = SongTiming(bpmFile = 120f),
        tracks = listOf(
            Track(
                playerId = PlayerId.P1,
                lines = listOf(
                    Line(
                        lineIndex = 0,
                        notes = listOf(
                            NoteEvent(
                                noteType = NoteType.Normal,
                                startBeatFile = 0,
                                durationBeats = durationBeats,
                                toneSemitone = 0,
                                lyric = "la",
                            ),
                        ),
                        lineScoreValue = durationBeats.toLong(),
                    ),
                ),
                trackScoreValue = durationBeats.toLong(),
            ),
        ),
        diagnostics = emptyList<DiagnosticEntry>(),
    )
}
