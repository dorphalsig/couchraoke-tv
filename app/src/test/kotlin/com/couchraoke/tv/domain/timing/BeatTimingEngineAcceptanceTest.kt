package com.couchraoke.tv.domain.timing

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.TrackId
import org.junit.Test
import org.junit.Assert.*

class BeatTimingEngineAcceptanceTest {

    // US1 — Acceptance: baseline and gap/start fixture
    @Test
    fun `fixture 18 baseline beat cursors match expected values`() {
        val stream = javaClass.classLoader.getResourceAsStream(
            "fixtures/parser/derived/18_beat_timing_basic/expected.beat_cursors.json"
        ) ?: error("fixture 18 not found")
        val json = stream.bufferedReader().readText()

        val root = org.json.JSONObject(json)
        val inputs = root.getJSONObject("inputs")
        val bpmFile = inputs.getDouble("BPM_file")
        val gapMs = inputs.getDouble("GAPms").toInt()
        val micDelayMs = inputs.getDouble("micDelayMs").toInt()

        val context = TimingContext(
            songIdentifier = "fixture-18",
            bpmFile = bpmFile,
            gapMs = gapMs,
            micDelayMs = micDelayMs,
        )

        val samples = root.getJSONArray("samples")
        for (i in 0 until samples.length()) {
            val sample = samples.getJSONObject(i)
            val lyricsTimeSec = sample.getDouble("lyricsTimeSec")
            val expectedHighlightTimeSec = sample.getDouble("highlightTimeSec")
            val expectedMidBeat = sample.getDouble("midBeat")
            val expectedCurrentBeat = sample.getInt("currentBeat")

            val cursor = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec)

            assertEquals(
                "sample[$i] highlightTimeSec",
                expectedHighlightTimeSec,
                cursor.highlightTimeSec,
                0.0001,
            )
            assertEquals(
                "sample[$i] midBeat",
                expectedMidBeat,
                cursor.midBeat,
                0.0001,
            )
            assertEquals(
                "sample[$i] currentBeat",
                expectedCurrentBeat,
                cursor.currentBeat,
            )
        }
    }

    // US1 — Acceptance: pre-roll and start offset
    @Test
    fun `fixture 19 gap-aware pre-roll and start offset cursors match expected values`() {
        val stream = javaClass.classLoader.getResourceAsStream(
            "fixtures/parser/derived/19_beat_timing_gap_and_start/expected.beat_cursors.json"
        ) ?: error("fixture 19 not found")
        val json = stream.bufferedReader().readText()

        val root = org.json.JSONObject(json)
        val inputs = root.getJSONObject("inputs")
        val bpmFile = inputs.getDouble("BPM_file")
        val gapMs = inputs.getDouble("GAPms").toInt()
        val startSec = inputs.getDouble("startSec")

        val context = TimingContext(
            songIdentifier = "fixture-19",
            bpmFile = bpmFile,
            gapMs = gapMs,
            startSec = startSec,
        )

        val samples = root.getJSONArray("samples")
        assertEquals("fixture 19 should have 3 samples", 3, samples.length())

        for (i in 0 until samples.length()) {
            val sample = samples.getJSONObject(i)
            val lyricsTimeSec = sample.getDouble("lyricsTimeSec")
            val expectedHighlightTimeSec = sample.getDouble("highlightTimeSec")
            val expectedMidBeat = sample.getDouble("midBeat")
            val expectedCurrentBeat = sample.getInt("currentBeat")

            val cursor = BeatTimingEngine.computeBeatCursor(context, lyricsTimeSec)

            assertEquals(
                "sample[$i] highlightTimeSec",
                expectedHighlightTimeSec,
                cursor.highlightTimeSec,
                0.0001,
            )
            assertEquals(
                "sample[$i] midBeat",
                expectedMidBeat,
                cursor.midBeat,
                0.0001,
            )
            assertEquals(
                "sample[$i] currentBeat",
                expectedCurrentBeat,
                cursor.currentBeat,
            )
        }
    }

    // US2 — Acceptance: note boundary membership
    @Test
    fun `fixture 20 note boundary membership matches expected values`() {
        val stream = javaClass.classLoader.getResourceAsStream(
            "fixtures/parser/edge/20_beat_timing_boundary_case/expected.note_windows.json"
        ) ?: error("fixture 20 not found")
        val json = stream.bufferedReader().readText()

        val root = org.json.JSONObject(json)
        val context = TimingContext(
            songIdentifier = "fixture-20",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 0,
        )

        val noteWindows = root.getJSONArray("noteWindows")
        for (i in 0 until noteWindows.length()) {
            val nw = noteWindows.getJSONObject(i)
            val id = nw.getString("id")
            val startBeat = nw.getInt("startBeat")
            val durationBeats = nw.getInt("durationBeats")
            val expectedNoteStartTvMs = nw.getLong("noteStartTvMs")
            val expectedNoteEndTvMs = nw.getLong("noteEndTvMs")
            val expectedFinalizationTvMs = nw.getLong("finalizationTvMs")

            val note = NoteEvent.Note(
                noteType = NoteType.NORMAL,
                startBeat = startBeat,
                durationBeats = durationBeats,
                tone = 0,
                lyricText = "",
            )
            val window = BeatTimingEngine.computeNoteTimingWindow(
                context = context,
                trackId = TrackId.P1,
                lineStartBeat = 0,
                note = note,
            )

            assertEquals("$id noteStartTvMs", expectedNoteStartTvMs, window.noteStartTvMs)
            assertEquals("$id noteEndTvMs", expectedNoteEndTvMs, window.noteEndTvMs)
            assertEquals("$id finalizationTvMs", expectedFinalizationTvMs, window.finalizationTvMs)
        }
    }

    // US2 — Acceptance: late-frame rejection when latenessMs > 450
    @Test
    fun `fixture 20 frames with latenessMs over 450 are explicitly rejected`() {
        val stream = javaClass.classLoader.getResourceAsStream(
            "fixtures/parser/edge/20_beat_timing_boundary_case/expected.note_windows.json"
        ) ?: error("fixture 20 not found")
        val json = stream.bufferedReader().readText()

        val root = org.json.JSONObject(json)
        val context = TimingContext(
            songIdentifier = "fixture-20",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 0,
        )

        // Build a map from window id -> NoteTimingWindow
        val noteWindowsArray = root.getJSONArray("noteWindows")
        val windowMap = mutableMapOf<String, NoteTimingWindow>()
        for (i in 0 until noteWindowsArray.length()) {
            val nw = noteWindowsArray.getJSONObject(i)
            val id = nw.getString("id")
            val startBeat = nw.getInt("startBeat")
            val durationBeats = nw.getInt("durationBeats")
            val note = NoteEvent.Note(
                noteType = NoteType.NORMAL,
                startBeat = startBeat,
                durationBeats = durationBeats,
                tone = 0,
                lyricText = "",
            )
            windowMap[id] = BeatTimingEngine.computeNoteTimingWindow(
                context = context,
                trackId = TrackId.P1,
                lineStartBeat = 0,
                note = note,
            )
        }

        val eligibilityTests = root.getJSONArray("frameEligibilityTests")
        for (i in 0 until eligibilityTests.length()) {
            val test = eligibilityTests.getJSONObject(i)
            val windowId = test.getString("windowId")
            val frameTimestampTvMs = test.getLong("frameTimestampTvMs")
            val arrivalTimeTvMs = test.getLong("arrivalTimeTvMs")
            val expectEligible = test.getBoolean("expectEligible")

            val window = windowMap[windowId]
                ?: error("Window $windowId not found in map")
            val frame = PitchFrameTiming(
                frameTimestampTvMs = frameTimestampTvMs,
                arrivalTimeTvMs = arrivalTimeTvMs,
                eligibleForCollection = true,
            )

            val result = BeatTimingEngine.isPitchFrameEligible(frame, window)
            assertEquals(
                "test[$i] windowId=$windowId frame=$frameTimestampTvMs arrival=$arrivalTimeTvMs",
                expectEligible,
                result,
            )
        }
    }

    // US3 — Acceptance: mic-delay shifts and end boundary
    @Test
    fun `fixture 19 mic-delay shifts note windows without changing authored beats`() {
        val context0 = TimingContext(
            songIdentifier = "fixture-19-mic-delay",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 0,
        )
        val context100 = TimingContext(
            songIdentifier = "fixture-19-mic-delay",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 100,
        )

        val note = NoteEvent.Note(
            noteType = NoteType.NORMAL,
            startBeat = 0,
            durationBeats = 4,
            tone = 0,
            lyricText = "",
        )

        val window0 = BeatTimingEngine.computeNoteTimingWindow(
            context = context0,
            trackId = TrackId.P1,
            lineStartBeat = 0,
            note = note,
        )
        val window100 = BeatTimingEngine.computeNoteTimingWindow(
            context = context100,
            trackId = TrackId.P1,
            lineStartBeat = 0,
            note = note,
        )

        assertEquals(
            "micDelay=100 shifts noteStartTvMs by 100ms",
            window0.noteStartTvMs + 100L,
            window100.noteStartTvMs,
        )
        assertEquals(
            "micDelay=100 shifts noteEndTvMs by 100ms",
            window0.noteEndTvMs + 100L,
            window100.noteEndTvMs,
        )
        assertEquals(
            "authored startBeat unchanged across micDelay contexts",
            window0.startBeat,
            window100.startBeat,
        )
    }
}
