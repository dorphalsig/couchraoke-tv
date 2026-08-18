package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.BeatRange
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.domain.scoring.model.PitchSample
import com.couchraoke.tv.domain.scoring.model.PlayerScore
import com.couchraoke.tv.domain.scoring.model.ScoringConfig
import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.domain.usdx.model.CustomHeaderTag
import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry
import com.couchraoke.tv.domain.usdx.model.Line
import com.couchraoke.tv.domain.usdx.model.NoteEvent
import com.couchraoke.tv.domain.usdx.model.NoteType
import com.couchraoke.tv.domain.usdx.model.ParsedSong
import com.couchraoke.tv.domain.usdx.model.Severity
import com.couchraoke.tv.domain.usdx.model.SongHeader
import com.couchraoke.tv.domain.usdx.model.SongTiming
import com.couchraoke.tv.domain.usdx.model.Track
import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import com.couchraoke.tv.fixtures.ParsedSongSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.io.path.readBytes

class UsdxParserFixtureTest {
    private val parser = DefaultUsdxParser()

    @Test(timeout = 30_000)
    fun matchesF04ValidDuetParsedSongSnapshot() {
        val fixturePath = FixturePaths.fixtureFile(
            "F04_duet_parsing_track_routing",
            "songs_root/a/valid_duet_interleaved/song.txt",
        )
        val parsed = parser.parse("F04_valid_duet_interleaved", fixturePath.readBytes()).getOrThrow()
        val expected = FixtureJson.decode<ParsedSongSnapshot>(
            FixturePaths.fixtureFile(
                "F04_duet_parsing_track_routing",
                "songs_root/a/valid_duet_interleaved/expected.parsedSong.json",
            )
        )

        assertEquals(expected.songId, parsed.songId)
        assertEquals(expected.header.title, parsed.header.title)
        assertEquals(expected.header.artist, parsed.header.artist)
        assertEquals(expected.header.audio, parsed.header.audio)
        assertEquals(expected.header.p1Name, parsed.header.p1Name)
        assertEquals(expected.header.p2Name, parsed.header.p2Name)
        assertEquals(2, parsed.tracks.size)
        assertEquals(PlayerId.P1, parsed.tracks[0].playerId)
        assertEquals(PlayerId.P2, parsed.tracks[1].playerId)
        assertTrue(parsed.header.isDuet)
    }

    @Test(timeout = 30_000)
    fun rejectsF04InvalidDuetMarkerWithStableLineNumber() {
        val fixturePath = FixturePaths.fixtureFile(
            "F04_duet_parsing_track_routing",
            "songs_root/b/invalid_duet_marker_p3/song.txt",
        )
        val failure = parser
            .parse(
                "F04_invalid_duet_marker_p3",
                fixturePath.readBytes(),
            )
            .exceptionOrNull() as ParseException
        val invalid = failure.diagnostics.first { it.severity.name == "Invalid" }

        assertEquals("ERROR_CORRUPT_SONG_INVALID_DUET_MARKER", invalid.code)
        assertEquals(6, invalid.lineNumber)
    }

    @Test(timeout = 30_000)
    fun rejectsF05LegacyRelativeBodySyntax() {
        val fixturePath = FixturePaths.fixtureFile(
            "F05_legacy_relative_mode_semantics",
            "song_relative_duet_bpm_rel0/song.txt",
        )
        val failure = parser
            .parse(
                "F05_legacy_relative_duet_bpm_rel0",
                fixturePath.readBytes(),
            )
            .exceptionOrNull() as ParseException
        val invalid = failure.diagnostics.first { it.severity.name == "Invalid" }

        assertEquals("ERROR_CORRUPT_SONG_UNSUPPORTED_RELATIVE", invalid.code)
        assertEquals(7, invalid.lineNumber)
    }

    @Test(timeout = 30_000)
    fun convertsZeroDurationNotesToFreestyle() {
        val fixturePath = FixturePaths.fixtureFile(
            "F03_body_grammar_token_recognition",
            "songs_root/c/duration_zero_converts_to_freestyle/song.txt",
        )
        val parsed = parser.parse("F03_duration_zero_converts_to_freestyle", fixturePath.readBytes()).getOrThrow()
        val note = parsed.tracks.single().lines.single().notes.single()
        val warning = parsed.diagnostics.first { it.severity.name == "Warn" }

        assertEquals("Freestyle", note.noteType.name)
        assertEquals(0, note.durationBeats)
        assertEquals("WARN_ZERO_DURATION_CONVERTED_TO_FREESTYLE", warning.code)
        assertEquals(6, warning.lineNumber)
    }

    @Test(timeout = 30_000)
    fun rejectsHeaderOnlySongWhilePreservingHeaderPreviewSemantics() {
        val fixturePath = FixturePaths.fixtureFile(
            "F02_header_parsing_edge_cases",
            "songs_root/d/preview_from_start/song.txt",
        )
        val result = parser.parse("F02_preview_from_start", fixturePath.readBytes())
        val throwable = result.exceptionOrNull()
        if (throwable !is ParseException) {
            fail("Expected ParseException for header-only song")
        }
        val failure = throwable as ParseException
        val invalid = failure.diagnostics.first { it.severity == Severity.Invalid }

        assertEquals("ERROR_CORRUPT_SONG_NO_NOTES", invalid.code)
        assertNull(invalid.lineNumber)
    }

    @Test(timeout = 30_000)
    fun stopsParsingAfterEndMarker() {
        val txt = """
            #TITLE:End Marker
            #ARTIST:Artist
            #BPM:120
            #MP3:song.mp3
            : 0 4 0 hi
            E
            P3
        """.trimIndent()

        val parsed = parser.parse("inline_end_marker", txt.encodeToByteArray()).getOrThrow()

        assertTrue(parsed.diagnostics.none { it.severity == Severity.Invalid })
        assertEquals(1, parsed.tracks.single().lines.single().notes.size)
    }

    @Test(timeout = 30_000)
    fun rejectsNegativeNoteDuration() {
        val txt = """
            #TITLE:Negative Duration
            #ARTIST:Artist
            #BPM:120
            #MP3:song.mp3
            : 0 -1 0 bad
            E
        """.trimIndent()

        val failure = parser
            .parse("inline_negative_duration", txt.encodeToByteArray())
            .exceptionOrNull() as ParseException
        val invalid = failure.diagnostics.first { it.severity == Severity.Invalid }

        assertEquals("ERROR_CORRUPT_SONG_MALFORMED_BODY", invalid.code)
        assertEquals(5, invalid.lineNumber)
    }

    @Test(timeout = 30_000)
    fun coversPhase0HeaderAndParserModelsUsedByTheGate() {
        val fixture = sampleFixture()

        assertEquals("Song", fixture.parsedSong.header.title)
        assertEquals("Artist", fixture.parsedSong.header.artist)
        assertEquals(120f, fixture.parsedSong.header.bpmFile)
        assertEquals(1500f, fixture.parsedSong.header.gapMs)
        assertEquals("song.mp3", fixture.parsedSong.header.audio)
        assertEquals(1.5f, fixture.parsedSong.header.startSec)
        assertEquals(95_000, fixture.parsedSong.header.endMs)
        assertEquals(0.25f, fixture.parsedSong.header.videoGapSec)
        assertEquals(12.5f, fixture.parsedSong.header.previewStartSec)
        assertEquals("song.mp4", fixture.parsedSong.header.video)
        assertEquals("cover.jpg", fixture.parsedSong.header.cover)
        assertEquals("bg.jpg", fixture.parsedSong.header.background)
        assertEquals("1.0.0", fixture.parsedSong.header.version)
        assertEquals(2005, fixture.parsedSong.header.year)
        assertEquals("Pop", fixture.parsedSong.header.genre)
        assertEquals("Album", fixture.parsedSong.header.album)
        assertTrue(fixture.parsedSong.header.isDuet)
        assertEquals("P1", fixture.parsedSong.header.p1Name)
        assertEquals("P2", fixture.parsedSong.header.p2Name)
        assertEquals(16, fixture.parsedSong.header.medleyStartBeat)
        assertEquals(48, fixture.parsedSong.header.medleyEndBeat)
        assertEquals(fixture.customTag, fixture.parsedSong.header.customTags.single())
        assertEquals(120f, fixture.parsedSong.timing.bpmFile)
        assertEquals("song-id", fixture.parsedSong.songId)
        assertTrue(fixture.parsedSong.diagnostics.none { it.severity == Severity.Invalid })
    }

    @Test(timeout = 30_000)
    fun coversPhase0LineTrackAndDiagnosticModelsUsedByTheGate() {
        val fixture = sampleFixture()
        val line = fixture.line
        val firstNote = fixture.firstNote
        val secondNote = fixture.secondNote

        assertEquals(PlayerId.P2, fixture.parsedSong.tracks.single().playerId)
        assertEquals(25L, fixture.parsedSong.tracks.single().trackScoreValue)
        assertEquals(3, line.lineIndex)
        assertEquals(listOf(firstNote, secondNote), line.notes)
        assertEquals(25L, line.lineScoreValue)
        assertEquals(4, line.startBeatFile)
        assertEquals(16, line.endBeatFileExclusive)
        assertFalse(line.isEmpty)
        assertEquals(NoteType.Normal, firstNote.noteType)
        assertEquals(4, firstNote.startBeatFile)
        assertEquals(6, firstNote.durationBeats)
        assertEquals(60, firstNote.toneSemitone)
        assertEquals("la", firstNote.lyric)
        assertEquals(10, firstNote.endBeatFileExclusive)
        assertEquals(NoteType.Golden, secondNote.noteType)
        assertEquals(12, secondNote.startBeatFile)
        assertEquals(4, secondNote.durationBeats)
        assertEquals(62, secondNote.toneSemitone)
        assertEquals("li", secondNote.lyric)
        assertEquals(16, secondNote.endBeatFileExclusive)
        assertEquals(Severity.Info, fixture.parsedSong.diagnostics.single().severity)
        assertEquals("INFO_TEST", fixture.parsedSong.diagnostics.single().code)
        assertEquals("songs/song.txt", fixture.parsedSong.diagnostics.single().txtUri)
        assertEquals(9, fixture.parsedSong.diagnostics.single().lineNumber)
    }

    @Test(timeout = 30_000)
    fun coversPhase0IndexedSongAndScoringModelsUsedByTheGate() {
        val fixture = sampleFixture()
        val scoringConfig = ScoringConfig(
            playerDifficulties = mapOf(PlayerId.P1 to Difficulty.Medium),
            lineBonusEnabled = true,
        )
        val beatRange = BeatRange(startBeat = 16, endBeat = 48)
        val pitchSample = PitchSample(playerId = PlayerId.P1, midiNote = 61, tvTimeMs = 1_234L)
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

        assertEquals("song-id", fixture.indexedSong.songId)
        assertEquals("phone-1", fixture.indexedSong.phoneClientId)
        assertEquals("songs/song.txt", fixture.indexedSong.relativeTxtPath)
        assertEquals(123L, fixture.indexedSong.modifiedTimeMs)
        assertEquals("Song", fixture.indexedSong.title)
        assertEquals("Artist", fixture.indexedSong.artist)
        assertEquals("Album", fixture.indexedSong.album)
        assertEquals(2005, fixture.indexedSong.year)
        assertEquals("Pop", fixture.indexedSong.genre)
        assertEquals("https://example.test/song.txt", fixture.indexedSong.txtUrl)
        assertEquals("https://example.test/song.mp3", fixture.indexedSong.audioUrl)
        assertEquals("https://example.test/song.mp4", fixture.indexedSong.videoUrl)
        assertEquals("https://example.test/cover.jpg", fixture.indexedSong.coverUrl)
        assertEquals("https://example.test/bg.jpg", fixture.indexedSong.backgroundUrl)
        assertTrue(fixture.indexedSong.isDuet)
        assertFalse(fixture.indexedSong.hasRap)
        assertTrue(fixture.indexedSong.hasVideo)
        assertFalse(fixture.indexedSong.canMedley)
        assertNull(fixture.indexedSong.medleySource)
        assertEquals(16, fixture.indexedSong.medleyStartBeat)
        assertEquals(48, fixture.indexedSong.medleyEndBeat)
        assertEquals(1.5f, fixture.indexedSong.startSec)
        assertEquals(12.5f, fixture.indexedSong.previewStartSec)
        assertEquals(mapOf(PlayerId.P1 to Difficulty.Medium), scoringConfig.playerDifficulties)
        assertTrue(scoringConfig.lineBonusEnabled)
        assertEquals(16, beatRange.startBeat)
        assertEquals(48, beatRange.endBeat)
        assertEquals(PlayerId.P1, pitchSample.playerId)
        assertEquals(61, pitchSample.midiNote)
        assertEquals(1_234L, pitchSample.tvTimeMs)
        assertEquals(1.0, playerScore.score, 0.0)
        assertEquals(2.0, playerScore.scoreGolden, 0.0)
        assertEquals(3.0, playerScore.scoreLine, 0.0)
        assertEquals(4.0, playerScore.scoreLast, 0.0)
        assertEquals(10, playerScore.scoreInt)
        assertEquals(20, playerScore.scoreGoldenInt)
        assertEquals(30, playerScore.scoreLineInt)
        assertEquals(60, playerScore.scoreTotalInt)
    }

    private fun sampleFixture(): Phase0CoverageFixture {
        val customTag = CustomHeaderTag(tag = "X-TEST", content = "value")
        val header = sampleHeader(customTag)
        val firstNote = sampleFirstNote()
        val secondNote = sampleSecondNote()
        val line = Line(lineIndex = 3, notes = listOf(firstNote, secondNote), lineScoreValue = 25L)
        val parsedSong = sampleParsedSong(header, line)
        return Phase0CoverageFixture(
            customTag = customTag,
            firstNote = firstNote,
            secondNote = secondNote,
            line = line,
            parsedSong = parsedSong,
            indexedSong = sampleIndexedSong(parsedSong),
        )
    }

    private fun sampleHeader(customTag: CustomHeaderTag) = SongHeader(
        title = "Song",
        artist = "Artist",
        bpmFile = 120f,
        gapMs = 1500f,
        audio = "song.mp3",
        startSec = 1.5f,
        endMs = 95_000,
        videoGapSec = 0.25f,
        previewStartSec = 12.5f,
        video = "song.mp4",
        cover = "cover.jpg",
        background = "bg.jpg",
        version = "1.0.0",
        year = 2005,
        genre = "Pop",
        album = "Album",
        isDuet = true,
        p1Name = "P1",
        p2Name = "P2",
        medleyStartBeat = 16,
        medleyEndBeat = 48,
        customTags = listOf(customTag),
    )

    private fun sampleFirstNote() = NoteEvent(
        noteType = NoteType.Normal,
        startBeatFile = 4,
        durationBeats = 6,
        toneSemitone = 60,
        lyric = "la",
    )

    private fun sampleSecondNote() = NoteEvent(
        noteType = NoteType.Golden,
        startBeatFile = 12,
        durationBeats = 4,
        toneSemitone = 62,
        lyric = "li",
    )

    private fun sampleParsedSong(header: SongHeader, line: Line): ParsedSong {
        val track = Track(playerId = PlayerId.P2, lines = listOf(line), trackScoreValue = 25L)
        val diagnostic = DiagnosticEntry(
            severity = Severity.Info,
            code = "INFO_TEST",
            txtUri = "songs/song.txt",
            lineNumber = 9,
        )
        return ParsedSong(
            songId = "song-id",
            header = header,
            timing = SongTiming(bpmFile = 120f),
            tracks = listOf(track),
            diagnostics = listOf(diagnostic),
        )
    }

    private fun sampleIndexedSong(parsedSong: ParsedSong): IndexedSong {
        val hasValidSoloMedleyTags = !parsedSong.header.isDuet &&
            parsedSong.header.medleyStartBeat != null &&
            parsedSong.header.medleyEndBeat != null &&
            parsedSong.header.medleyStartBeat < parsedSong.header.medleyEndBeat

        return IndexedSong(
            songId = parsedSong.songId,
            phoneClientId = "phone-1",
            relativeTxtPath = "songs/song.txt",
            modifiedTimeMs = 123L,
            title = parsedSong.header.title,
            artist = parsedSong.header.artist,
            album = parsedSong.header.album,
            year = parsedSong.header.year,
            genre = parsedSong.header.genre,
            txtUrl = "https://example.test/song.txt",
            audioUrl = "https://example.test/song.mp3",
            videoUrl = "https://example.test/song.mp4",
            coverUrl = "https://example.test/cover.jpg",
            backgroundUrl = "https://example.test/bg.jpg",
            isDuet = parsedSong.header.isDuet,
            hasRap = false,
            hasVideo = true,
            hasInstrumental = false,
            canMedley = hasValidSoloMedleyTags,
            medleySource = if (hasValidSoloMedleyTags) "tag" else null,
            medleyStartBeat = parsedSong.header.medleyStartBeat,
            medleyEndBeat = parsedSong.header.medleyEndBeat,
            startSec = parsedSong.header.startSec ?: 0f,
            previewStartSec = parsedSong.header.previewStartSec ?: 0f,
        )
    }

    private data class Phase0CoverageFixture(
        val customTag: CustomHeaderTag,
        val firstNote: NoteEvent,
        val secondNote: NoteEvent,
        val line: Line,
        val parsedSong: ParsedSong,
        val indexedSong: IndexedSong,
    )
}
