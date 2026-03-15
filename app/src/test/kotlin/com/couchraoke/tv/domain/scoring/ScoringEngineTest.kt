package com.couchraoke.tv.domain.scoring

import com.couchraoke.tv.domain.parser.DerivedSongSummary
import com.couchraoke.tv.domain.parser.Line
import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.ParsedSong
import com.couchraoke.tv.domain.parser.SongHeader
import com.couchraoke.tv.domain.parser.Track
import com.couchraoke.tv.domain.parser.TrackId
import com.couchraoke.tv.domain.timing.NoteTimingWindow
import org.junit.Assert.*
import org.junit.Test

class ScoringEngineTest {

    private val engine = DefaultScoringEngine()

    // ──── Helpers ────

    private fun note(type: NoteType, duration: Int, start: Int = 0, tone: Int = 0): NoteEvent =
        NoteEvent.Note(noteType = type, startBeat = start, durationBeats = duration, tone = tone, lyricText = "x")

    private fun song(notes: List<NoteEvent>, header: SongHeader = SongHeader()): ParsedSong =
        ParsedSong(
            songIdentifier = "test",
            isValid = true,
            header = header,
            tracks = listOf(Track(trackId = TrackId.P1, lines = listOf(Line(startBeat = 0, notes = notes)))),
            derivedSummary = DerivedSongSummary(),
        )

    private fun songWithLines(lines: List<Line>, header: SongHeader = SongHeader()): ParsedSong =
        ParsedSong(
            songIdentifier = "test",
            isValid = true,
            header = header,
            tracks = listOf(Track(trackId = TrackId.P1, lines = lines)),
            derivedSummary = DerivedSongSummary(),
        )

    private fun config(lineBonusEnabled: Boolean = false, maxLineBonusPool: Int = 1000, maxSongPoints: Int = 10000): ScoringConfig =
        ScoringConfig(lineBonusEnabled = lineBonusEnabled, maxSongPoints = maxSongPoints, maxLineBonusPool = maxLineBonusPool)

    private fun window(type: NoteType = NoteType.NORMAL, duration: Int = 4): NoteTimingWindow =
        NoteTimingWindow(
            trackId = TrackId.P1,
            lineStartBeat = 0,
            noteType = type,
            startBeat = 0,
            durationBeats = duration,
            noteStartTvMs = 0L,
            noteEndTvMs = 1000L,
        )

    private fun profile(
        trackScoreValue: Double = 4.0,
        maxSongPoints: Int = 10000,
        lineBonusPerLine: Double = 0.0,
        nonEmptyLineCount: Int = 1,
    ): TrackScoringProfile = TrackScoringProfile(
        trackScoreValue = trackScoreValue,
        nonEmptyLineCount = nonEmptyLineCount,
        lineBonusPerLine = lineBonusPerLine,
        medleyStartBeat = null,
        medleyEndBeat = null,
        maxSongPoints = maxSongPoints,
    )

    private fun noteResult(noteScore: Double, accumulator: ScoreAccumulator): NoteResult =
        NoteResult(
            noteTimingWindow = window(),
            hits = 0,
            n = 0,
            noteScore = noteScore,
            maxNoteScore = 0.0,
            accumulator = accumulator,
        )

    // ──── T008 — isPitchMatch: octave normalization ────

    @Test
    fun `T008a octave 12 semitones above target normalizes to hit`() {
        // tone=48-36=12, |12-0|=12>6 → -12 → 0; MEDIUM tol=1; 0<=1 → true
        assertTrue(engine.isPitchMatch(48, true, NoteType.NORMAL, 0, Difficulty.MEDIUM))
    }

    @Test
    fun `T008b octave 24 semitones above target normalizes twice to hit`() {
        // tone=60-36=24, 24>6 → 12>6 → 0; MEDIUM tol=1; 0<=1 → true
        assertTrue(engine.isPitchMatch(60, true, NoteType.NORMAL, 0, Difficulty.MEDIUM))
    }

    @Test
    fun `T008c delta exactly 6 is NOT greater-than-6 — no shift — EASY miss`() {
        // tone=42-36=6; |6-0|=6 NOT>6; EASY tol=2; 6>2 → false
        assertFalse(engine.isPitchMatch(42, true, NoteType.NORMAL, 0, Difficulty.EASY))
    }

    @Test
    fun `T008d delta 7 shifts by 12 then re-evaluates — EASY miss`() {
        // tone=43-36=7; |7-0|=7>6 → 7-12=-5; |-5|=5 NOT>6; EASY tol=2; 5>2 → false
        assertFalse(engine.isPitchMatch(43, true, NoteType.NORMAL, 0, Difficulty.EASY))
    }

    // ──── T009 — isPitchMatch: per-type and per-difficulty ────

    @Test fun `T009 NORMAL MEDIUM within 1 semitone is hit`() = assertTrue(engine.isPitchMatch(37, true, NoteType.NORMAL, 0, Difficulty.MEDIUM))
    @Test fun `T009 NORMAL MEDIUM 2 semitones is miss`() = assertFalse(engine.isPitchMatch(38, true, NoteType.NORMAL, 0, Difficulty.MEDIUM))
    @Test fun `T009 NORMAL EASY within 2 semitones is hit`() = assertTrue(engine.isPitchMatch(38, true, NoteType.NORMAL, 0, Difficulty.EASY))
    @Test fun `T009 NORMAL EASY 3 semitones is miss`() = assertFalse(engine.isPitchMatch(39, true, NoteType.NORMAL, 0, Difficulty.EASY))
    @Test fun `T009 NORMAL HARD exact match is hit`() = assertTrue(engine.isPitchMatch(36, true, NoteType.NORMAL, 0, Difficulty.HARD))
    @Test fun `T009 NORMAL HARD 1 semitone off is miss`() = assertFalse(engine.isPitchMatch(37, true, NoteType.NORMAL, 0, Difficulty.HARD))
    @Test fun `T009 GOLDEN behaves like NORMAL for MEDIUM`() = assertTrue(engine.isPitchMatch(36, true, NoteType.GOLDEN, 0, Difficulty.MEDIUM))
    @Test fun `T009 RAP toneValid true is always hit`() = assertTrue(engine.isPitchMatch(99, true, NoteType.RAP, 0, Difficulty.MEDIUM))
    @Test fun `T009 RAP toneValid false is always miss`() = assertFalse(engine.isPitchMatch(36, false, NoteType.RAP, 0, Difficulty.MEDIUM))
    @Test fun `T009 RAP_GOLDEN toneValid true is always hit`() = assertTrue(engine.isPitchMatch(99, true, NoteType.RAP_GOLDEN, 0, Difficulty.MEDIUM))
    @Test fun `T009 FREESTYLE is always false`() = assertFalse(engine.isPitchMatch(36, true, NoteType.FREESTYLE, 0, Difficulty.MEDIUM))
    @Test fun `T009 NORMAL toneValid false is always miss`() = assertFalse(engine.isPitchMatch(36, false, NoteType.NORMAL, 0, Difficulty.MEDIUM))

    @Test
    fun `T009 default difficulty resolves to MEDIUM when playerIndex absent from config`() {
        val cfg = config(lineBonusEnabled = false)
        assertEquals(Difficulty.MEDIUM, cfg.difficulties.getOrDefault(0, DEFAULT_DIFFICULTY))
    }

    // ──── T015 — buildProfile ────

    @Test
    fun `T015a single NORMAL note trackScoreValue equals durationBeats`() {
        val s = song(listOf(note(NoteType.NORMAL, 4)))
        val p = engine.buildProfile(s, 0, config())
        assertEquals(4.0, p.trackScoreValue, 0.0)
    }

    @Test
    fun `T015b mixed types weighted sum is correct`() {
        // NORMAL(4)*1 + GOLDEN(2)*2 + FREESTYLE(3)*0 = 4+4+0 = 8
        val s = song(listOf(
            note(NoteType.NORMAL, 4),
            note(NoteType.GOLDEN, 2, start = 4),
            note(NoteType.FREESTYLE, 3, start = 6),
        ))
        val p = engine.buildProfile(s, 0, config())
        assertEquals(8.0, p.trackScoreValue, 0.0)
    }

    @Test
    fun `T015c freestyle-only song has trackScoreValue 0`() {
        val s = song(listOf(note(NoteType.FREESTYLE, 4)))
        val p = engine.buildProfile(s, 0, config())
        assertEquals(0.0, p.trackScoreValue, 0.0)
    }

    @Test
    fun `T015d medley filter includes only notes within medley range`() {
        // notes at beats 0,5,10; medley=[4,8); only beat 5 counts; NORMAL dur=1 → tsv=1.0
        val header = SongHeader(medleyStartBeat = 4, medleyEndBeat = 8)
        val s = song(listOf(
            note(NoteType.NORMAL, 1, 0),
            note(NoteType.NORMAL, 1, 5),
            note(NoteType.NORMAL, 1, 10),
        ), header)
        val p = engine.buildProfile(s, 0, config())
        assertEquals(1.0, p.trackScoreValue, 0.0)
    }

    @Test
    fun `T015e nonEmptyLineCount counts only lines with score gt 0`() {
        // line1: NORMAL(2)+GOLDEN(1) → score=4 non-empty; line2: FREESTYLE(3) → score=0 empty
        val lines = listOf(
            Line(0, listOf(note(NoteType.NORMAL, 2), note(NoteType.GOLDEN, 1, start = 2))),
            Line(10, listOf(note(NoteType.FREESTYLE, 3, start = 10))),
        )
        val p = engine.buildProfile(songWithLines(lines), 0, config(lineBonusEnabled = true))
        assertEquals(1, p.nonEmptyLineCount)
        assertEquals(1000.0, p.lineBonusPerLine, 0.0)
    }

    @Test
    fun `T015f nonEmptyLineCount 0 yields lineBonusPerLine 0`() {
        val s = song(listOf(note(NoteType.FREESTYLE, 4)))
        val p = engine.buildProfile(s, 0, config(lineBonusEnabled = true))
        assertEquals(0.0, p.lineBonusPerLine, 0.0)
    }

    @Test
    fun `T015g lineBonusEnabled false yields lineBonusPerLine 0`() {
        val s = song(listOf(note(NoteType.NORMAL, 4)))
        val p = engine.buildProfile(s, 0, config(lineBonusEnabled = false))
        assertEquals(0.0, p.lineBonusPerLine, 0.0)
    }

    // ──── T016 — evaluateNote ────

    @Test
    fun `T016a all frames hit returns maxNoteScore`() {
        // NORMAL dur=4 tsv=4 max=10000; maxNoteScore=(10000/4)*1*4=10000; 3/3 hits
        val frames = List(3) { PitchFrame(36, true) }
        val result = engine.evaluateNote(window(NoteType.NORMAL, 4), note(NoteType.NORMAL, 4), frames, profile(4.0, 10000))
        assertEquals(10000.0, result.noteScore, 0.01)
        assertEquals(3, result.n)
        assertEquals(ScoreAccumulator.SCORE, result.accumulator)
    }

    @Test
    fun `T016b partial hits returns fractional noteScore`() {
        // frame 3 misses: tone=99-36=63→3 (after normalization), >MEDIUM(1) → miss; 2/3 → 6666.67
        val frames = listOf(PitchFrame(36, true), PitchFrame(36, true), PitchFrame(99, true))
        val result = engine.evaluateNote(window(NoteType.NORMAL, 4), note(NoteType.NORMAL, 4), frames, profile(4.0, 10000))
        assertEquals(6666.67, result.noteScore, 1.0)
    }

    @Test
    fun `T016c n equals 0 returns noteScore 0`() {
        val result = engine.evaluateNote(window(NoteType.NORMAL, 4), note(NoteType.NORMAL, 4), emptyList(), profile(4.0, 10000))
        assertEquals(0.0, result.noteScore, 0.0)
        assertEquals(0, result.n)
    }

    @Test
    fun `T016d FREESTYLE noteScore is 0 regardless of frames`() {
        val frames = listOf(PitchFrame(36, true), PitchFrame(36, true))
        val result = engine.evaluateNote(window(NoteType.FREESTYLE, 4), note(NoteType.FREESTYLE, 4), frames, profile())
        assertEquals(0.0, result.noteScore, 0.0)
        assertEquals(ScoreAccumulator.NONE, result.accumulator)
    }

    @Test fun `T016e NORMAL accumulator is SCORE`() {
        val result = engine.evaluateNote(window(NoteType.NORMAL, 1), note(NoteType.NORMAL, 1), emptyList(), profile())
        assertEquals(ScoreAccumulator.SCORE, result.accumulator)
    }

    @Test fun `T016f GOLDEN accumulator is SCORE_GOLDEN`() {
        val result = engine.evaluateNote(window(NoteType.GOLDEN, 1), note(NoteType.GOLDEN, 1), emptyList(), profile())
        assertEquals(ScoreAccumulator.SCORE_GOLDEN, result.accumulator)
    }

    @Test fun `T016g RAP accumulator is SCORE`() {
        val result = engine.evaluateNote(window(NoteType.RAP, 1), note(NoteType.RAP, 1), emptyList(), profile())
        assertEquals(ScoreAccumulator.SCORE, result.accumulator)
    }

    @Test fun `T016h RAP_GOLDEN accumulator is SCORE_GOLDEN`() {
        val result = engine.evaluateNote(window(NoteType.RAP_GOLDEN, 1), note(NoteType.RAP_GOLDEN, 1), emptyList(), profile())
        assertEquals(ScoreAccumulator.SCORE_GOLDEN, result.accumulator)
    }

    @Test
    fun `T016i trackScoreValue 0 yields maxNoteScore 0 and noteScore 0`() {
        val frames = listOf(PitchFrame(36, true))
        val result = engine.evaluateNote(window(NoteType.NORMAL, 4), note(NoteType.NORMAL, 4), frames, profile(trackScoreValue = 0.0))
        assertEquals(0.0, result.noteScore, 0.0)
        assertEquals(0.0, result.maxNoteScore, 0.0)
    }

    // ──── T017 — accumulateNote ────

    @Test
    fun `T017a SCORE accumulator adds to score only`() {
        val base = PlayerScore(score = 100.0, scoreGolden = 50.0)
        val r = engine.accumulateNote(base, noteResult(200.0, ScoreAccumulator.SCORE))
        assertEquals(300.0, r.score, 0.0)
        assertEquals(50.0, r.scoreGolden, 0.0)
        assertEquals(200.0, r.scoreLast, 0.0)
    }

    @Test
    fun `T017b SCORE_GOLDEN accumulator adds to scoreGolden only`() {
        val base = PlayerScore(score = 100.0, scoreGolden = 50.0)
        val r = engine.accumulateNote(base, noteResult(75.0, ScoreAccumulator.SCORE_GOLDEN))
        assertEquals(100.0, r.score, 0.0)
        assertEquals(125.0, r.scoreGolden, 0.0)
        assertEquals(75.0, r.scoreLast, 0.0)
    }

    @Test
    fun `T017c NONE accumulator changes neither score nor scoreGolden`() {
        val base = PlayerScore(score = 100.0, scoreGolden = 50.0)
        val r = engine.accumulateNote(base, noteResult(0.0, ScoreAccumulator.NONE))
        assertEquals(100.0, r.score, 0.0)
        assertEquals(50.0, r.scoreGolden, 0.0)
        assertEquals(0.0, r.scoreLast, 0.0)
    }

    @Test
    fun `T017d scoreLast always updated even for zero FREESTYLE`() {
        val base = PlayerScore(scoreLast = 999.0)
        val r = engine.accumulateNote(base, noteResult(0.0, ScoreAccumulator.NONE))
        assertEquals(0.0, r.scoreLast, 0.0)
    }

    @Test
    fun `T017e accumulation is additive across multiple calls`() {
        var ps = PlayerScore()
        ps = engine.accumulateNote(ps, noteResult(100.0, ScoreAccumulator.SCORE))
        ps = engine.accumulateNote(ps, noteResult(200.0, ScoreAccumulator.SCORE))
        ps = engine.accumulateNote(ps, noteResult(50.0, ScoreAccumulator.SCORE_GOLDEN))
        assertEquals(300.0, ps.score, 0.0)
        assertEquals(50.0, ps.scoreGolden, 0.0)
    }

    // ──── T025 — evaluateLine ────

    @Test
    fun `T025a full hit returns linePerfection 1 and full lineBonusPerLine`() {
        // lineScore=10, maxLineScore=10, (10/(10-2))=1.25 coerced → 1.0; awarded=100
        val r = engine.evaluateLine(10.0, 10.0, profile(lineBonusPerLine = 100.0))
        assertEquals(1.0, r.linePerfection, 0.0)
        assertEquals(100.0, r.lineBonusAwarded, 0.0)
    }

    @Test
    fun `T025b maxLineScore le 2 forgiveness gives linePerfection 1`() {
        val r = engine.evaluateLine(5.0, 2.0, profile(lineBonusPerLine = 50.0))
        assertEquals(1.0, r.linePerfection, 0.0)
        assertEquals(50.0, r.lineBonusAwarded, 0.0)
    }

    @Test
    fun `T025c partial lineScore gives fractional linePerfection`() {
        // 4/(10-2)=0.5
        val r = engine.evaluateLine(4.0, 10.0, profile(lineBonusPerLine = 100.0))
        assertEquals(0.5, r.linePerfection, 0.001)
        assertEquals(50.0, r.lineBonusAwarded, 0.01)
    }

    @Test
    fun `T025d lineScore 0 gives linePerfection 0 and no bonus`() {
        val r = engine.evaluateLine(0.0, 10.0, profile(lineBonusPerLine = 100.0))
        assertEquals(0.0, r.linePerfection, 0.0)
        assertEquals(0.0, r.lineBonusAwarded, 0.0)
    }

    @Test
    fun `T025e lineBonusPerLine 0 gives no bonus regardless of performance`() {
        val r = engine.evaluateLine(10.0, 10.0, profile(lineBonusPerLine = 0.0))
        assertEquals(0.0, r.lineBonusAwarded, 0.0)
    }

    // ──── T031 — computeDisplayScores ────

    @Test
    fun `T031a round score to nearest 10`() {
        // round(9500/10)*10=9500; scoreInt(9500) NOT < score(9500) → floor golden
        val r = engine.computeDisplayScores(PlayerScore(score = 9500.0, scoreGolden = 500.0))
        assertEquals(9500, r.scoreInt)
        assertEquals(500, r.scoreGoldenInt)
        assertEquals(10000, r.scoreTotalInt)
    }

    @Test
    fun `T031b 9505 rounds up to 9510`() {
        // round(950.5)*10=9510
        val r = engine.computeDisplayScores(PlayerScore(score = 9505.0))
        assertEquals(9510, r.scoreInt)
    }

    @Test
    fun `T031c scoreLineInt uses floor-of-round-div-10 — different from scoreInt formula`() {
        // scoreLine=9505 → scoreLineInt=floor(round(9505)/10)*10=floor(950.5)*10=9500
        // vs scoreInt formula: round(9505/10)*10=9510 — asymmetry confirmed
        val r = engine.computeDisplayScores(PlayerScore(scoreLine = 9505.0))
        assertEquals(9500, r.scoreLineInt)
    }

    @Test
    fun `T031d perfect score sums to 10000`() {
        val r = engine.computeDisplayScores(PlayerScore(score = 9000.0, scoreLine = 1000.0))
        assertEquals(10000, r.scoreTotalInt)
    }

    @Test
    fun `T031e scoreTotalInt equals sum of three components`() {
        val r = engine.computeDisplayScores(PlayerScore(score = 5000.0, scoreGolden = 1000.0, scoreLine = 500.0))
        assertEquals(r.scoreInt + r.scoreGoldenInt + r.scoreLineInt, r.scoreTotalInt)
    }
}