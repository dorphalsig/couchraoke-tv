package com.couchraoke.tv.domain.timing

import com.couchraoke.tv.domain.parser.NoteEvent
import com.couchraoke.tv.domain.parser.NoteType
import com.couchraoke.tv.domain.parser.TrackId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.*
import org.junit.Test

class BeatTimingEngineAcceptanceTest {

    private fun JsonObject.nullableDouble(key: String): Double? {
        val el = this[key] ?: return null
        return if (el is JsonNull) null else el.jsonPrimitive.double
    }

    private fun JsonObject.nullableInt(key: String): Int? {
        val el = this[key] ?: return null
        return if (el is JsonNull) null else el.jsonPrimitive.int
    }

    private fun loadFixture(path: String): String {
        val stream = checkNotNull(javaClass.classLoader).getResourceAsStream(path)
            ?: error("fixture not found: $path")
        return stream.bufferedReader().readText()
    }

    // US1 — Acceptance: baseline and gap/start fixture
    @Test
    fun `fixture 18 baseline beat cursors match expected values`() {
        val root = Json.parseToJsonElement(
            loadFixture("fixtures/parser/derived/18_beat_timing_basic/expected.beat_cursors.json")
        ).jsonObject
        val inputs = root["inputs"]!!.jsonObject
        val context = TimingContext(
            songIdentifier = "fixture-18",
            bpmFile = inputs["BPM_file"]!!.jsonPrimitive.double,
            gapMs = inputs["GAPms"]!!.jsonPrimitive.double.toInt(),
            micDelayMs = inputs["micDelayMs"]!!.jsonPrimitive.double.toInt(),
        )

        val samples = root["samples"]!!.jsonArray
        for ((i, el) in samples.withIndex()) {
            val s = el.jsonObject
            val cursor = BeatTimingEngine.computeBeatCursor(
                context, s["lyricsTimeSec"]!!.jsonPrimitive.double
            )
            assertEquals("sample[$i] highlightTimeSec",
                s["highlightTimeSec"]!!.jsonPrimitive.double, cursor.highlightTimeSec, 0.0001)
            assertEquals("sample[$i] midBeat",
                s["midBeat"]!!.jsonPrimitive.double, cursor.midBeat, 0.0001)
            assertEquals("sample[$i] currentBeat",
                s["currentBeat"]!!.jsonPrimitive.int, cursor.currentBeat)
        }
    }

    // US1 — Acceptance: pre-roll and start offset
    @Test
    fun `fixture 19 gap-aware pre-roll and start offset cursors match expected values`() {
        val root = Json.parseToJsonElement(
            loadFixture("fixtures/parser/derived/19_beat_timing_gap_and_start/expected.beat_cursors.json")
        ).jsonObject
        val inputs = root["inputs"]!!.jsonObject
        val context = TimingContext(
            songIdentifier = "fixture-19",
            bpmFile = inputs["BPM_file"]!!.jsonPrimitive.double,
            gapMs = inputs["GAPms"]!!.jsonPrimitive.double.toInt(),
            startSec = inputs["startSec"]!!.jsonPrimitive.double,
        )

        val samples = root["samples"]!!.jsonArray
        assertEquals("fixture 19 should have 3 samples", 3, samples.size)
        for ((i, el) in samples.withIndex()) {
            val s = el.jsonObject
            val cursor = BeatTimingEngine.computeBeatCursor(
                context, s["lyricsTimeSec"]!!.jsonPrimitive.double
            )
            assertEquals("sample[$i] highlightTimeSec",
                s["highlightTimeSec"]!!.jsonPrimitive.double, cursor.highlightTimeSec, 0.0001)
            assertEquals("sample[$i] midBeat",
                s["midBeat"]!!.jsonPrimitive.double, cursor.midBeat, 0.0001)
            assertEquals("sample[$i] currentBeat",
                s["currentBeat"]!!.jsonPrimitive.int, cursor.currentBeat)
        }
    }

    // US2 — Acceptance: note boundary membership
    @Test
    fun `fixture 20 note boundary membership matches expected values`() {
        val root = Json.parseToJsonElement(
            loadFixture("fixtures/parser/edge/20_beat_timing_boundary_case/expected.note_windows.json")
        ).jsonObject
        val context = TimingContext(
            songIdentifier = "fixture-20",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 0,
        )

        for ((i, el) in root["noteWindows"]!!.jsonArray.withIndex()) {
            val nw = el.jsonObject
            val id = nw["id"]!!.jsonPrimitive.content
            val note = NoteEvent.Note(
                noteType = NoteType.NORMAL,
                startBeat = nw["startBeat"]!!.jsonPrimitive.int,
                durationBeats = nw["durationBeats"]!!.jsonPrimitive.int,
                tone = 0,
                lyricText = "",
            )
            val window = BeatTimingEngine.computeNoteTimingWindow(context, TrackId.P1, 0, note)
            assertEquals("$id noteStartTvMs", nw["noteStartTvMs"]!!.jsonPrimitive.long, window.noteStartTvMs)
            assertEquals("$id noteEndTvMs", nw["noteEndTvMs"]!!.jsonPrimitive.long, window.noteEndTvMs)
            assertEquals("$id finalizationTvMs", nw["finalizationTvMs"]!!.jsonPrimitive.long, window.finalizationTvMs)
        }
    }

    // US2 — Acceptance: late-frame rejection when latenessMs > 450
    @Test
    fun `fixture 20 frames with latenessMs over 450 are explicitly rejected`() {
        val root = Json.parseToJsonElement(
            loadFixture("fixtures/parser/edge/20_beat_timing_boundary_case/expected.note_windows.json")
        ).jsonObject
        val context = TimingContext(
            songIdentifier = "fixture-20",
            bpmFile = 120.0,
            gapMs = 2000,
            songStartTvMs = 10000L,
            micDelayMs = 0,
        )

        val windowMap = root["noteWindows"]!!.jsonArray.associate { el ->
            val nw = el.jsonObject
            val note = NoteEvent.Note(NoteType.NORMAL,
                nw["startBeat"]!!.jsonPrimitive.int,
                nw["durationBeats"]!!.jsonPrimitive.int, 0, "")
            nw["id"]!!.jsonPrimitive.content to
                BeatTimingEngine.computeNoteTimingWindow(context, TrackId.P1, 0, note)
        }

        for ((i, el) in root["frameEligibilityTests"]!!.jsonArray.withIndex()) {
            val t = el.jsonObject
            val windowId = t["windowId"]!!.jsonPrimitive.content
            val frameTs = t["frameTimestampTvMs"]!!.jsonPrimitive.long
            val arrivalTs = t["arrivalTimeTvMs"]!!.jsonPrimitive.long
            val expectEligible = t["expectEligible"]!!.jsonPrimitive.boolean
            val window = windowMap[windowId] ?: error("Window $windowId not found")
            val frame = PitchFrameTiming(frameTs, arrivalTs, eligibleForCollection = true)
            assertEquals(
                "test[$i] windowId=$windowId frame=$frameTs arrival=$arrivalTs",
                expectEligible,
                BeatTimingEngine.isPitchFrameEligible(frame, window),
            )
        }
    }

    // US3 — Acceptance: playback bounds derivation
    @Test
    fun `fixture 19 explicit endMs produces correct playback bounds`() {
        val text = loadFixture("fixtures/parser/derived/19_beat_timing_gap_and_start/expected.playback_bounds.json")
        val root = Json.parseToJsonElement(text).jsonObject
        val scenarios = root["scenarios"]!!.jsonArray
        for (scenario in scenarios) {
            val id = scenario.jsonObject["id"]!!.jsonPrimitive.content
            val inputs = scenario.jsonObject["inputs"]!!.jsonObject
            val startSec = inputs.nullableDouble("startSec")
            val endMs = inputs.nullableInt("endMs")
            val mediaDurationSec = inputs.nullableDouble("mediaDurationSec")
            val context = TimingContext(
                songIdentifier = "fixture-19-bounds-$id",
                bpmFile = 120.0,
                gapMs = 2000,
                startSec = startSec,
                endMs = endMs,
                mediaDurationSec = mediaDurationSec,
            )
            val bounds = BeatTimingEngine.computePlaybackBounds(context)
            val exp = scenario.jsonObject["expected"]!!.jsonObject
            assertEquals("$id initialSongTimeSec",
                exp["initialSongTimeSec"]!!.jsonPrimitive.double, bounds.initialSongTimeSec, 0.0001)
            assertEquals("$id endsFromHeader",
                exp["endsFromHeader"]!!.jsonPrimitive.boolean, bounds.endsFromHeader)
            val expEnd = exp.nullableDouble("effectiveSongEndSec")
            if (expEnd == null) {
                assertNull("$id effectiveSongEndSec should be null", bounds.effectiveSongEndSec)
            } else {
                assertNotNull("$id effectiveSongEndSec should not be null", bounds.effectiveSongEndSec)
                assertEquals("$id effectiveSongEndSec", expEnd, bounds.effectiveSongEndSec!!, 0.0001)
            }
        }
    }

    // US3 — Acceptance: media-duration fallback and no-end scenarios
    @Test
    fun `fixture 19 media duration fallback uses mediaDurationSec when endMs absent`() {
        val text = loadFixture("fixtures/parser/derived/19_beat_timing_gap_and_start/expected.playback_bounds.json")
        val root = Json.parseToJsonElement(text).jsonObject
        val scenarios = root["scenarios"]!!.jsonArray
            .filter { it.jsonObject["id"]!!.jsonPrimitive.content in setOf("media-fallback", "no-end") }
        for (scenario in scenarios) {
            val id = scenario.jsonObject["id"]!!.jsonPrimitive.content
            val inputs = scenario.jsonObject["inputs"]!!.jsonObject
            val startSec = inputs.nullableDouble("startSec")
            val endMs = inputs.nullableInt("endMs")
            val mediaDurationSec = inputs.nullableDouble("mediaDurationSec")
            val context = TimingContext(
                songIdentifier = "fixture-19-fallback-$id",
                bpmFile = 120.0,
                gapMs = 2000,
                startSec = startSec,
                endMs = endMs,
                mediaDurationSec = mediaDurationSec,
            )
            val bounds = BeatTimingEngine.computePlaybackBounds(context)
            assertFalse("$id endsFromHeader should be false", bounds.endsFromHeader)
        }
    }

    // US3 — Acceptance: mic-delay shifts and end boundary
    @Test
    fun `fixture 19 mic-delay shifts note windows without changing authored beats`() {
        val note = NoteEvent.Note(NoteType.NORMAL, startBeat = 0, durationBeats = 4, tone = 0, lyricText = "")
        val base = TimingContext("fixture-19-mic", 120.0, 2000, songStartTvMs = 10000L, micDelayMs = 0)
        val delayed = TimingContext("fixture-19-mic", 120.0, 2000, songStartTvMs = 10000L, micDelayMs = 100)
        val w0 = BeatTimingEngine.computeNoteTimingWindow(base, TrackId.P1, 0, note)
        val w100 = BeatTimingEngine.computeNoteTimingWindow(delayed, TrackId.P1, 0, note)
        assertEquals("micDelay shifts noteStartTvMs by 100ms", w0.noteStartTvMs + 100L, w100.noteStartTvMs)
        assertEquals("micDelay shifts noteEndTvMs by 100ms", w0.noteEndTvMs + 100L, w100.noteEndTvMs)
        assertEquals("authored startBeat unchanged", w0.startBeat, w100.startBeat)
    }
}
