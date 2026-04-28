package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.internal.BeatWindowCalculator
import com.couchraoke.tv.domain.scoring.internal.Phase0ScoringEngine
import com.couchraoke.tv.domain.scoring.internal.PitchSampleSource
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import com.couchraoke.tv.fixtures.ScoreSnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.io.path.readBytes

class ScoringEngineFixtureTest {
    private val parser = DefaultUsdxParser()

    @Test(timeout = 30_000)
    fun matchesF08ScoringFixture() {
        assertFixtureCase(
            fixtureId = "F08_scoring_beat_stepping_interval_semantics",
            chartRelativePath = "song.txt",
            expectedPath = "expected.score.json",
            difficulty = Difficulty.Medium,
            pitchSamples = readPitchSamples(
                fixtureId = "F08_scoring_beat_stepping_interval_semantics",
                relativePath = "pitchFrames.jsonl",
            ),
        )
    }

    @Test(timeout = 30_000)
    fun matchesF09ScoringFixtures() {
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "easy_hit_diff1",
            Difficulty.Easy,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "easy_miss_diff3",
            Difficulty.Easy,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "hard_hit_diff0",
            Difficulty.Hard,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "hard_hit_diff0_double_octave",
            Difficulty.Hard,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "hard_miss_diff1",
            Difficulty.Hard,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "medium_hit_diff1",
            Difficulty.Medium,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "medium_hit_diff1_lower_octave",
            Difficulty.Medium,
        )
        assertSubcase(
            "F09_pitch_tolerance_octave_normalization",
            "medium_miss_diff2",
            Difficulty.Medium,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF10ScoringFixture() {
        assertSubcase(
            "F10_rap_scoring_tonevalid_gate",
            null,
            Difficulty.Medium,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF11ScoringFixture() {
        assertSubcase(
            "F11_line_bonus_and_rounding",
            null,
            Difficulty.Medium,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF03FreestyleOnlyScoringFixture() {
        val fixtureId = "F03_body_grammar_token_recognition"
        val pitchSamples = readPitchSamples(
            fixtureId = fixtureId,
            relativePath = "scoring/freestyle_only/pitchFrames.jsonl",
        )

        assertFixtureCase(
            fixtureId = fixtureId,
            chartRelativePath = "scoring/freestyle_only/song.txt",
            expectedPath = "scoring/freestyle_only/expected.score.json",
            difficulty = Difficulty.Medium,
            pitchSamples = pitchSamples,
        )
    }

    private fun assertSubcase(
        fixtureId: String,
        subcase: String?,
        difficulty: Difficulty,
    ) {
        val relative = if (subcase == null) "" else "$subcase/"
        val pitchSamples = if (fixtureId == "F10_rap_scoring_tonevalid_gate") {
            emptyList()
        } else {
            readPitchSamples(
                fixtureId = fixtureId,
                relativePath = "${relative}pitchFrames.jsonl",
            )
        }
        assertFixtureCase(
            fixtureId = fixtureId,
            chartRelativePath = "${relative}song.txt",
            expectedPath = "${relative}expected.score.json",
            difficulty = difficulty,
            pitchSamples = pitchSamples,
        )
    }

    private fun assertFixtureCase(
        fixtureId: String,
        chartRelativePath: String,
        expectedPath: String,
        difficulty: Difficulty,
        pitchSamples: List<PitchSample>,
    ) {
        val chartPath = FixturePaths.fixtureFile(fixtureId, chartRelativePath)
        val expectedScorePath = FixturePaths.fixtureFile(fixtureId, expectedPath)
        val chart = parser.parse(
            "$fixtureId::$chartRelativePath",
            chartPath.readBytes(),
        ).getOrThrow()
        val expected = FixtureJson.decode<ScoreSnapshot>(expectedScorePath)
        val fixturePitchSamples = pitchSamples.ifEmpty {
            expected.perBeat.map { perBeat ->
                PitchSample(
                    playerId = PlayerId.P1,
                    midiNote = if (perBeat.toneValid) 36 else 255,
                    tvTimeMs = beatStartTvMs(
                        bpmFile = chart.header.bpmFile,
                        gapMs = chart.header.gapMs,
                        beat = perBeat.beat,
                    ),
                )
            }
        }
        val normalizedPitchSamples = expandFixtureBeatSamples(
            bpmFile = chart.header.bpmFile,
            gapMs = chart.header.gapMs,
            pitchSamples = fixturePitchSamples,
        )
        val engine = Phase0ScoringEngine(PitchSampleSource { normalizedPitchSamples })
        engine.loadChart(
            chart = chart,
            micDelayMs = 0,
            medleyWindow = null,
            config = ScoringConfig(
                playerDifficulties = mapOf(PlayerId.P1 to difficulty),
                lineBonusEnabled = true,
            ),
        )
        engine.setSongStart(0L)

        val actual = runBlocking { engine.finalizeAll() }.getValue(PlayerId.P1)

        expected.expectedTotals.score?.let {
            assertEquals(it.toDouble(), actual.score, 1e-6)
        }
        expected.expectedTotals.scoreGolden?.let {
            assertEquals(it.toDouble(), actual.scoreGolden, 1e-6)
        }
        expected.expectedTotals.scoreLine?.let {
            assertEquals(it.toDouble(), actual.scoreLine, 1e-3)
        }
        expected.expectedTotals.scoreInt?.let { assertEquals(it, actual.scoreInt) }
        expected.expectedTotals.scoreGoldenInt?.let {
            assertEquals(it, actual.scoreGoldenInt)
        }
        expected.expectedTotals.scoreLineInt?.let {
            assertEquals(it, actual.scoreLineInt)
        }
        assertEquals(expected.expectedTotals.scoreTotalInt, actual.scoreTotalInt)
    }

    private fun expandFixtureBeatSamples(
        bpmFile: Float,
        gapMs: Float,
        pitchSamples: List<PitchSample>,
    ): List<PitchSample> = pitchSamples.flatMap { sample ->
        val tvTimeMs = sample.tvTimeMs ?: return@flatMap emptyList()
        val beat = BeatWindowCalculator.currentBeatDAtTvTime(
            sampleTvTimeMs = tvTimeMs,
            songStartTvMs = 0L,
            gapMs = gapMs,
            bpmFile = bpmFile,
            micDelayMs = 0,
        )
        val beatStartMs = beatStartTvMs(bpmFile, gapMs, beat)
        val beatEndMs = BeatWindowCalculator.noteEndTvMs(
            songStartTvMs = 0L,
            startBeatFile = beat,
            durationBeats = 1,
            bpmFile = bpmFile,
            gapMs = gapMs,
            micDelayMs = 0,
        )
        (beatStartMs until beatEndMs step 20L).map { expandedTvTimeMs ->
            sample.copy(tvTimeMs = expandedTvTimeMs)
        }
    }

    private fun beatStartTvMs(bpmFile: Float, gapMs: Float, beat: Int): Long =
        BeatWindowCalculator.noteStartTvMs(
            songStartTvMs = 0L,
            startBeatFile = beat,
            bpmFile = bpmFile,
            gapMs = gapMs,
            micDelayMs = 0,
        )

    private fun readPitchSamples(
        fixtureId: String,
        relativePath: String,
    ): List<PitchSample> {
        val path = FixturePaths.fixtureFile(fixtureId, relativePath)
        return if (path.toFile().exists()) {
            FixtureJson.readJsonLines(path).map(::toPitchSample)
        } else {
            emptyList()
        }
    }

    private fun toPitchSample(frame: JsonObject): PitchSample = PitchSample(
        playerId = PlayerId.valueOf(
            frame["playerId"]?.jsonPrimitive?.content ?: PlayerId.P1.name,
        ),
        midiNote = frame["midiNote"]?.jsonPrimitive?.intOrNull ?: 255,
        tvTimeMs = frame["tvTimeMs"]?.jsonPrimitive?.longOrNull
            ?: frame["tCaptureMs"]?.jsonPrimitive?.longOrNull,
    )
}
