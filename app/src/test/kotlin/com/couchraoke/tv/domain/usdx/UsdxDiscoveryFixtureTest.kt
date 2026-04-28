package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.fixtures.FixturePaths
import com.couchraoke.tv.fixtures.Phase0Assertions
import com.couchraoke.tv.fixtures.UsdxDiscoveryHarness
import org.junit.Test

class UsdxDiscoveryFixtureTest {
    @Test(timeout = 30_000)
    fun matchesF01DiscoverySnapshot() {
        val actual = UsdxDiscoveryHarness.discoverFixture("F01_song_discovery_validation_acceptance")
        Phase0Assertions.assertDiscoverySnapshot(
            FixturePaths.fixtureFile("F01_song_discovery_validation_acceptance", "expected.discovery.json"),
            actual,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF02DiscoverySnapshot() {
        val actual = UsdxDiscoveryHarness.discoverFixture("F02_header_parsing_edge_cases")
        Phase0Assertions.assertDiscoverySnapshot(
            FixturePaths.fixtureFile("F02_header_parsing_edge_cases", "expected.discovery.json"),
            actual,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF03DiscoverySnapshot() {
        val actual = UsdxDiscoveryHarness.discoverFixture("F03_body_grammar_token_recognition")
        Phase0Assertions.assertDiscoverySnapshot(
            FixturePaths.fixtureFile("F03_body_grammar_token_recognition", "expected.discovery.json"),
            actual,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF04DiscoverySnapshot() {
        val actual = UsdxDiscoveryHarness.discoverFixture("F04_duet_parsing_track_routing")
        Phase0Assertions.assertDiscoverySnapshot(
            FixturePaths.fixtureFile("F04_duet_parsing_track_routing", "expected.discovery.json"),
            actual,
        )
    }

    @Test(timeout = 30_000)
    fun matchesF05DiscoverySnapshot() {
        val actual = UsdxDiscoveryHarness.discoverFixture("F05_legacy_relative_mode_semantics")
        Phase0Assertions.assertDiscoverySnapshot(
            FixturePaths.fixtureFile("F05_legacy_relative_mode_semantics", "expected.discovery.json"),
            actual,
        )
    }
}
