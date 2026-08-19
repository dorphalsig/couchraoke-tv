package com.couchraoke.tv.fixtures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureSnapshotsTest {

    @Test(timeout = 30_000)
    fun decodesManifest() {
        val snapshot = FixtureJson.decode<FixtureManifestSnapshot>(FixturePaths.manifestPath)
        assertNotNull(snapshot.specVersion)
        assertTrue(snapshot.fixtures.isNotEmpty())
    }

    @Test(timeout = 30_000)
    fun decodesF01Discovery() {
        val path = FixturePaths.fixtureFile("F01_song_discovery_validation_acceptance", "expected.discovery.json")
        val snapshot = FixtureJson.decode<DiscoverySnapshot>(path)
        assertNotNull(snapshot.rootRel)
        assertTrue(snapshot.songs.isNotEmpty())
        assertEquals(snapshot.songs.sortedBy(DiscoverySong::songTxtRel), snapshot.ordered().songs)
    }

    @Test(timeout = 30_000)
    fun decodesF05Discovery() {
        val path = FixturePaths.fixtureFile("F05_legacy_relative_mode_semantics", "expected.discovery.json")
        val snapshot = FixtureJson.decode<DiscoverySnapshot>(path)
        assertNotNull(snapshot.rootRel)
        assertTrue(snapshot.songs.isNotEmpty())
    }

    @Test(timeout = 30_000)
    fun decodesF08Score() {
        val path = FixturePaths.fixtureFile("F08_scoring_beat_stepping_interval_semantics", "expected.score.json")
        val snapshot = FixtureJson.decode<ScoreSnapshot>(path)
        val noteWindow = requireNotNull(snapshot.noteWindow)

        assertNotNull(snapshot.assumptions)
        assertNotNull(snapshot.expectedTotals)
        assertTrue(snapshot.perBeat.isEmpty())
        assertEquals(listOf(1L, 2L), noteWindow.qualifyingFrameSeq)
        assertEquals(noteWindow.samplesInNote, noteWindow.hits)
    }

    @Test(timeout = 30_000)
    fun decodesF04ParsedSong() {
        val path = FixturePaths.fixtureFile(
            "F04_duet_parsing_track_routing",
            "songs_root/a/valid_duet_interleaved/expected.parsedSong.json",
        )
        val snapshot = FixtureJson.decode<ParsedSongSnapshot>(path)
        assertEquals(snapshot.tracks.sortedBy(ParsedSongTrack::playerId), snapshot.ordered().tracks)
    }

    @Test(timeout = 30_000)
    fun decodesF03PlayerScores() {
        val path = FixturePaths.fixtureFile(
            "F03_body_grammar_token_recognition",
            "scoring/all_freestyle/expected.score.json",
        )
        val snapshot = FixtureJson.decode<PlayerScoresSnapshot>(path)
        assertEquals(0, snapshot.playerScores.getValue("P1").scoreTotalInt)
    }

    @Test(timeout = 30_000)
    fun decodesF11MedleyTotal() {
        val path = FixturePaths.fixtureFile("F11_line_bonus_and_rounding", "expected.medley_total.json")
        val snapshot = FixtureJson.decode<MedleyTotalSnapshot>(path)
        assertNotNull(snapshot.description)
        assertNotNull(snapshot.expectedTotals)
    }
}
