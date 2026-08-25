package com.couchraoke.tv.domain.session

import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives every entry of `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json`
 * against [GamePhaseMachine] — read at test time, never restated in Kotlin, so the test
 * agrees with the fixture rather than with the implementation.
 */
class GamePhaseMachineFixtureTest {

    @Test(timeout = 30_000)
    fun acceptsEveryValidF22Transition() {
        val expected = loadExpectedTransitions()

        // Guard against a vacuous pass: a truncated file, a renamed key, or a future
        // fixture revision must fail this test loudly rather than iterate zero times.
        assertEquals(
            "F22 fixture's valid list must have exactly $EXPECTED_VALID_COUNT entries",
            EXPECTED_VALID_COUNT,
            expected.valid.size,
        )

        expected.valid.forEach { entry ->
            val (from, to) = parseEdge(entry)
            val machine = GamePhaseMachine(initial = from)

            assertTrue("Expected canTransition($from, $to) to be true", machine.canTransition(from, to))

            val result = machine.transition(to)
            assertTrue(
                "Expected $from -> $to to be Accepted, was $result",
                result == PhaseTransitionResult.Accepted(from, to),
            )
            assertTrue("current must advance to $to after an accepted transition", machine.current == to)
        }
    }

    @Test(timeout = 30_000)
    fun rejectsEveryInvalidF22Transition() {
        val expected = loadExpectedTransitions()

        // Same vacuous-pass guard as above, for the invalid side of the fixture.
        assertEquals(
            "F22 fixture's invalid list must have exactly $EXPECTED_INVALID_COUNT entries",
            EXPECTED_INVALID_COUNT,
            expected.invalid.size,
        )

        expected.invalid.forEach { entry ->
            val (from, to) = parseEdge(entry)
            val machine = GamePhaseMachine(initial = from)

            assertFalse("Expected canTransition($from, $to) to be false", machine.canTransition(from, to))

            val result = machine.transition(to)
            assertTrue(
                "Expected $from -> $to to be Rejected, was $result",
                result == PhaseTransitionResult.Rejected(from, to),
            )
            assertTrue("current must stay $from after a rejected transition (FR-026)", machine.current == from)
        }
    }

    @Test(timeout = 30_000)
    fun openToErrorIsAbsentFromTheValidList() {
        val expected = loadExpectedTransitions()
        val edges = expected.valid.map(::parseEdge)

        // An empty or truncated valid list would make the absence check below trivially
        // true. Require the list to actually contain entries before trusting it.
        assertTrue("F22 fixture's valid list must not be empty", edges.isNotEmpty())

        assertFalse(
            "Open -> Error must not be a valid transition; FR-028's modal depends on its absence",
            Pair(GamePhase.Open, GamePhase.Error) in edges,
        )
    }

    private fun loadExpectedTransitions(): ExpectedTransitions =
        FixtureJson.decode<ExpectedTransitions>(
            FixturePaths.fixtureFile("F22_gamephase_fsm_transitions", "expected.transitions.json"),
        )

    /**
     * Entries are `From->To` or `From->To:trigger_name`; the optional trigger suffix is
     * documentation only and is not part of the (from, to) edge.
     */
    private fun parseEdge(entry: String): Pair<GamePhase, GamePhase> {
        val edge = entry.substringBefore(':')
        val parts = edge.split("->")
        check(parts.size == 2) { "Malformed F22 transition entry: $entry" }
        return Pair(GamePhase.valueOf(parts[0]), GamePhase.valueOf(parts[1]))
    }

    @Serializable
    private data class ExpectedTransitions(
        val valid: List<String>,
        val invalid: List<String>,
    )

    private companion object {
        // Shape check only, not a restatement of the transition table: F22 currently
        // declares 20 valid entries (19 distinct edges — DisconnectPaused->Live appears
        // twice with different trigger suffixes) and 8 invalid entries.
        const val EXPECTED_VALID_COUNT = 20
        const val EXPECTED_INVALID_COUNT = 8
    }
}
