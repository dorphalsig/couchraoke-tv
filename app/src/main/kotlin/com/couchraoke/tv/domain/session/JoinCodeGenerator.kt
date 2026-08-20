package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.JoinCode
import kotlin.random.Random

/**
 * Generates the two-word [JoinCode] shown on the host TV at session start (FR-001, FR-003,
 * research.md R7).
 *
 * [random] is injected so tests can seed it deterministically. This instance retains every
 * code it has issued in [issued] and never hands out the same pair twice for as long as it
 * lives, which is what makes FR-003's "MUST NOT be reused across sessions" hold: the same
 * generator is expected to mint the join code for every session this TV process creates
 * (see [com.couchraoke.tv.di.SessionComponent]), not a fresh one per session.
 */
class JoinCodeGenerator(private val random: Random = Random.Default) {

    private val issued = mutableSetOf<JoinCode>()

    fun next(): JoinCode {
        while (true) {
            val candidate = JoinCode(
                adjective = ADJECTIVES[random.nextInt(ADJECTIVES.size)],
                noun = NOUNS[random.nextInt(NOUNS.size)],
            )
            if (issued.add(candidate)) {
                return candidate
            }
        }
    }

    private companion object {
        // 64 single-word adjectives, no homophones and no ambiguous first letter when read
        // aloud from across a room (research.md R7).
        val ADJECTIVES = listOf(
            "brave", "calm", "swift", "bold", "quiet", "happy", "lucky", "clever", "gentle", "mighty",
            "silent", "sunny", "rapid", "cosmic", "golden", "silver", "velvet", "crimson", "azure", "violet",
            "scarlet", "emerald", "jolly", "breezy", "cheerful", "curious", "daring", "eager", "fancy", "friendly",
            "glad", "grand", "honest", "jovial", "keen", "lively", "merry", "nimble", "plucky", "proud",
            "radiant", "rustic", "sturdy", "tidy", "upbeat", "vivid", "witty", "zesty", "amiable", "blithe",
            "chipper", "dapper", "earnest", "faithful", "genial", "hearty", "jaunty", "lucid", "mellow", "noble",
            "playful", "spry", "dashing", "peppy",
        )

        // 64 single-word nouns, chosen for the same reasons and so the noun half can also
        // serve as the mDNS instance-name suffix (FR-004) without punctuation.
        val NOUNS = listOf(
            "otter", "falcon", "panda", "tiger", "dolphin", "eagle", "koala", "rabbit", "badger", "heron",
            "walrus", "penguin", "raccoon", "fox", "wolf", "lynx", "puffin", "gecko", "cricket", "sparrow",
            "hamster", "beaver", "cobra", "toucan", "gazelle", "jaguar", "panther", "stallion", "mustang", "condor",
            "pelican", "osprey", "marlin", "salmon", "urchin", "beacon", "meadow", "harbor", "canyon", "summit",
            "comet", "meteor", "nebula", "aurora", "glacier", "lagoon", "prairie", "tundra", "oasis", "plateau",
            "cascade", "boulder", "willow", "cedar", "maple", "aspen", "orchid", "tulip", "daisy", "clover",
            "compass", "lantern", "anchor", "voyager",
        )
    }
}
