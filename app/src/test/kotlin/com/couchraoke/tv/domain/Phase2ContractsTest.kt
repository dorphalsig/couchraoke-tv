package com.couchraoke.tv.domain

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.model.CustomHeaderTag
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.Severity
import com.couchraoke.tv.domain.usdx.model.SongTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Phase2ContractsTest {
    @Test(timeout = 30_000)
    fun constructsSharedContracts() {
        val playerId = PlayerId.P1
        val difficulty = Difficulty.Medium
        val beatRange = BeatRange(startBeat = 4, endBeat = 8)
        val scoringConfig = ScoringConfig(
            playerDifficulties = mapOf(playerId to difficulty),
            lineBonusEnabled = true,
        )
        val playerScore = PlayerScore(
            score = 1.0,
            scoreGolden = 2.0,
            scoreLine = 3.0,
            scoreLast = 4.0,
            scoreInt = 10,
            scoreGoldenInt = 20,
            scoreLineInt = 30,
            scoreTotalInt = 60,
        )
        val pitchSample = PitchSample(
            playerId = playerId,
            midiNote = 36,
            tvTimeMs = 1234L,
        )
        val customHeaderTag = CustomHeaderTag(tag = "FOO", content = "bar")
        val diagnosticEntry = DiagnosticEntry(
            severity = Severity.Warn,
            code = "CODE",
            txtUri = "song.txt",
            lineNumber = 7,
        )
        val songTiming = SongTiming(bpmFile = 120f)

        assertEquals(PlayerId.P1, playerId)
        assertEquals(Difficulty.Medium, difficulty)
        assertEquals(4, beatRange.startBeat)
        assertEquals(8, beatRange.endBeat)
        assertEquals(difficulty, scoringConfig.playerDifficulties[playerId])
        assertEquals(true, scoringConfig.lineBonusEnabled)
        assertEquals(60, playerScore.scoreTotalInt)
        assertEquals(playerId, pitchSample.playerId)
        assertEquals(36, pitchSample.midiNote)
        assertEquals(1234L, pitchSample.tvTimeMs)
        assertEquals("FOO", customHeaderTag.tag)
        assertEquals("bar", customHeaderTag.content)
        assertEquals(Severity.Warn, diagnosticEntry.severity)
        assertEquals("CODE", diagnosticEntry.code)
        assertEquals("song.txt", diagnosticEntry.txtUri)
        assertEquals(7, diagnosticEntry.lineNumber)
        assertEquals(120f, songTiming.bpmFile)
        assertEquals(NoteType.RapGolden, NoteType.valueOf("RapGolden"))
        assertEquals(Severity.Invalid, Severity.valueOf("Invalid"))
    }

    @Test(timeout = 30_000)
    fun preservesNullableContractFields() {
        val pitchSample = PitchSample(playerId = PlayerId.P2, midiNote = 255, tvTimeMs = null)
        val diagnosticEntry = DiagnosticEntry(
            severity = Severity.Info,
            code = "INFO",
            txtUri = "fixture.txt",
            lineNumber = null,
        )

        assertNull(pitchSample.tvTimeMs)
        assertNull(diagnosticEntry.lineNumber)
    }
}
