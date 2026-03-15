package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.TrackId
import com.couchraoke.tv.domain.timing.NoteTimingWindow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoringEngineAcceptanceTest {

    private fun loadFixture(path: String): String {
        val stream = checkNotNull(javaClass.classLoader).getResourceAsStream(path)
            ?: error("fixture not found: $path")
        return stream.bufferedReader().readText()
    }

    private val engine = DefaultScoringEngine()

    @Test
    fun `T011 fixture pitch_hit_detection isPitchMatch matches expected for all cases`() {
        val inputRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/pitch_hit_detection/input.json")).jsonArray
        val expectedRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/pitch_hit_detection/expected.json")).jsonArray

        assertEquals("Fixtures must have same size", expectedRoot.size, inputRoot.size)

        for (i in inputRoot.indices) {
            val input = inputRoot[i].jsonObject
            val expected = expectedRoot[i].jsonObject

            val noteType = input["noteType"]!!.jsonPrimitive.content
            val targetTone = input["targetTone"]!!.jsonPrimitive.int
            val midiNote = input["midiNote"]!!.jsonPrimitive.int
            val toneValid = input["toneValid"]!!.jsonPrimitive.boolean
            val difficulty = input["difficulty"]!!.jsonPrimitive.content

            val result = engine.isPitchMatch(
                midiNote = midiNote,
                toneValid = toneValid,
                noteType = NoteType.valueOf(noteType),
                targetTone = targetTone,
                difficulty = Difficulty.valueOf(difficulty)
            )

            assertEquals(
                "case $i isPitchMatch",
                expected["isPitchMatch"]!!.jsonPrimitive.boolean,
                result
            )
        }
    }

    @Test
    fun `T019 fixture per_note_scoring evaluateNote matches expected for all cases`() {
        val inputRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/per_note_scoring/input.json")).jsonArray
        val expectedRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/per_note_scoring/expected.json")).jsonArray

        assertEquals("Fixtures must have same size", expectedRoot.size, inputRoot.size)

        for (i in inputRoot.indices) {
            val input = inputRoot[i].jsonObject
            val expected = expectedRoot[i].jsonObject

            val noteTypeStr = input["noteType"]!!.jsonPrimitive.content
            val durationBeats = input["durationBeats"]!!.jsonPrimitive.int
            val trackScoreValue = input["trackScoreValue"]!!.jsonPrimitive.double
            val maxSongPoints = input["maxSongPoints"]!!.jsonPrimitive.int
            val targetTone = input["targetTone"]!!.jsonPrimitive.int
            val difficultyStr = input["difficulty"]!!.jsonPrimitive.content

            val noteType = NoteType.valueOf(noteTypeStr)
            val difficulty = Difficulty.valueOf(difficultyStr)

            val frames = input["frames"]!!.jsonArray.map {
                val f = it.jsonObject
                PitchFrame(
                    midiNote = f["midiNote"]!!.jsonPrimitive.int,
                    toneValid = f["toneValid"]!!.jsonPrimitive.boolean
                )
            }

            val note = NoteEvent.Note(
                noteType = noteType,
                startBeat = 0,
                durationBeats = durationBeats,
                tone = targetTone,
                lyricText = "x"
            )

            val window = NoteTimingWindow(
                trackId = TrackId.P1,
                lineStartBeat = 0,
                noteType = noteType,
                startBeat = 0,
                durationBeats = durationBeats,
                noteStartTvMs = 0L,
                noteEndTvMs = 1000L
            )

            val profile = TrackScoringProfile(
                trackScoreValue = trackScoreValue,
                nonEmptyLineCount = 1,
                lineBonusPerLine = 0.0,
                medleyStartBeat = null,
                medleyEndBeat = null,
                maxSongPoints = maxSongPoints,
                difficulty = difficulty
            )

            val result = engine.evaluateNote(window, note, frames, profile)

            assertEquals("case $i hits", expected["hits"]!!.jsonPrimitive.int, result.hits)
            assertEquals("case $i n", expected["n"]!!.jsonPrimitive.int, result.n)
            assertEquals("case $i noteScore", expected["noteScore"]!!.jsonPrimitive.double, result.noteScore, 1.0)
            assertEquals("case $i accumulator", expected["accumulator"]!!.jsonPrimitive.content, result.accumulator.name)
        }
    }

    @Test
    fun `T027 fixture line_bonus evaluateLine matches expected for all cases`() {
        val inputRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/line_bonus/input.json")).jsonArray
        val expectedRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/line_bonus/expected.json")).jsonArray

        assertEquals("Fixtures must have same size", expectedRoot.size, inputRoot.size)

        for (i in inputRoot.indices) {
            val input = inputRoot[i].jsonObject
            val expected = expectedRoot[i].jsonObject

            val lineScore = input["lineScore"]!!.jsonPrimitive.double
            val maxLineScore = input["maxLineScore"]!!.jsonPrimitive.double
            val lineBonusPerLine = input["lineBonusPerLine"]!!.jsonPrimitive.double

            val profile = TrackScoringProfile(
                trackScoreValue = 1.0,
                nonEmptyLineCount = 1,
                lineBonusPerLine = lineBonusPerLine,
                medleyStartBeat = null,
                medleyEndBeat = null,
                maxSongPoints = 10000
            )

            val result = engine.evaluateLine(lineScore, maxLineScore, profile)

            assertEquals("case $i linePerfection", expected["linePerfection"]!!.jsonPrimitive.double, result.linePerfection, 0.001)
            assertEquals("case $i lineBonusAwarded", expected["lineBonusAwarded"]!!.jsonPrimitive.double, result.lineBonusAwarded, 0.01)
        }
    }

    @Test
    fun `T033 fixture score_rounding computeDisplayScores matches expected for all cases`() {
        val inputRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/score_rounding/input.json")).jsonArray
        val expectedRoot = Json.parseToJsonElement(loadFixture("fixtures/scoring/score_rounding/expected.json")).jsonArray

        assertEquals("Fixtures must have same size", expectedRoot.size, inputRoot.size)

        for (i in inputRoot.indices) {
            val input = inputRoot[i].jsonObject
            val expected = expectedRoot[i].jsonObject

            val score = input["score"]!!.jsonPrimitive.double
            val scoreGolden = input["scoreGolden"]!!.jsonPrimitive.double
            val scoreLine = input["scoreLine"]!!.jsonPrimitive.double

            val playerScore = PlayerScore(
                score = score,
                scoreGolden = scoreGolden,
                scoreLine = scoreLine
            )

            val result = engine.computeDisplayScores(playerScore)

            assertEquals("case $i scoreInt", expected["scoreInt"]!!.jsonPrimitive.int, result.scoreInt)
            assertEquals("case $i scoreGoldenInt", expected["scoreGoldenInt"]!!.jsonPrimitive.int, result.scoreGoldenInt)
            assertEquals("case $i scoreLineInt", expected["scoreLineInt"]!!.jsonPrimitive.int, result.scoreLineInt)
            assertEquals("case $i scoreTotalInt", expected["scoreTotalInt"]!!.jsonPrimitive.int, result.scoreTotalInt)
        }
    }
}
