package com.couchraoke.tv.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * T029: covers [JoinCodeGenerator] (contracts/domain-api.md, research.md R7) — a seeded
 * [Random] yields a deterministic pair, [com.couchraoke.tv.domain.session.model.JoinCode.display]
 * renders `ADJECTIVE-NOUN` uppercase, and no code repeats across many generations (FR-003).
 */
class JoinCodeGeneratorTest {

    @Test(timeout = 30_000)
    fun seededRandomYieldsADeterministicPair() {
        val first = JoinCodeGenerator(random = Random(SEED)).next()
        val second = JoinCodeGenerator(random = Random(SEED)).next()

        assertEquals(
            "two generators seeded identically must mint the same JoinCode",
            first,
            second,
        )
    }

    @Test(timeout = 30_000)
    fun displayRendersAdjectiveHyphenNounUppercase() {
        val code = JoinCodeGenerator(random = Random(SEED)).next()

        // Real dictionary words, not a placeholder that embeds a raw number: this is what
        // actually distinguishes the bundled word lists from the T033 forward-declaration
        // stub, which produced a noun like "code1834131627".
        assertTrue(
            "adjective must be a single alphabetic word, was \"${code.adjective}\"",
            code.adjective.isNotEmpty() && code.adjective.all(Char::isLetter),
        )
        assertTrue(
            "noun must be a single alphabetic word, was \"${code.noun}\"",
            code.noun.isNotEmpty() && code.noun.all(Char::isLetter),
        )
        assertEquals(
            "display must be ADJECTIVE-NOUN, both uppercased",
            "${code.adjective.uppercase()}-${code.noun.uppercase()}",
            code.display,
        )
        assertTrue("display must be fully uppercase", code.display == code.display.uppercase())
        assertTrue("display must contain exactly one hyphen", code.display.count { it == '-' } == 1)
    }

    @Test(timeout = 30_000)
    fun noCodeRepeatsAcrossManyGenerations() {
        val generator = JoinCodeGenerator(random = Random(SEED))

        val codes = (1..SAMPLE_SIZE).map { generator.next() }

        // Guards the uniqueness assertion below against a vacuous pass: if the generator
        // silently produced fewer codes than requested, this fails loudly first.
        assertEquals(
            "expected $SAMPLE_SIZE codes to be minted",
            SAMPLE_SIZE,
            codes.size,
        )
        assertEquals(
            "JoinCodeGenerator must never mint a repeat code within a process (FR-003)",
            codes.size,
            codes.toSet().size,
        )
    }

    private companion object {
        const val SEED = 42L

        // Word lists are documented as ~64 entries each (research.md R7): 64 * 64 = 4096
        // possible pairs. 2000 draws without a repeat is well beyond chance collision for a
        // correct issued-set guard, while staying inside the available combination space.
        const val SAMPLE_SIZE = 2_000
    }
}
