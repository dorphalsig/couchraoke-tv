package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.session.model.JoinCode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [JoinCodeMatcher] (nominally T048, implemented early — see spec.md Observation 19).
 *
 * The matcher is what makes `SessionCoordinator.authorize` a real decision, so these cases
 * pin both halves: a token that names the same two words matches however it is punctuated,
 * and anything else does not.
 */
class JoinCodeMatcherTest {

    @Test(timeout = 30_000)
    fun matchesTheExactDisplayedCode() {
        assertTrue(JoinCodeMatcher.matches(CODE, "SWIFT-PANDA"))
    }

    @Test(timeout = 30_000)
    fun matchesRegardlessOfCaseHyphenationOrSurroundingWhitespace() {
        assertTrue("lower case", JoinCodeMatcher.matches(CODE, "swift-panda"))
        assertTrue("no hyphen", JoinCodeMatcher.matches(CODE, "SwiftPanda"))
        assertTrue("spaced instead of hyphenated", JoinCodeMatcher.matches(CODE, "swift panda"))
        assertTrue("padded", JoinCodeMatcher.matches(CODE, "  SWIFT-PANDA  "))
    }

    @Test(timeout = 30_000)
    fun rejectsADifferentCode() {
        assertFalse("different noun", JoinCodeMatcher.matches(CODE, "SWIFT-OTTER"))
        assertFalse("different adjective", JoinCodeMatcher.matches(CODE, "BRAVE-PANDA"))
        assertFalse("the words reversed", JoinCodeMatcher.matches(CODE, "PANDA-SWIFT"))
    }

    @Test(timeout = 30_000)
    fun rejectsNullOrBlank() {
        assertFalse("null", JoinCodeMatcher.matches(CODE, null))
        assertFalse("empty", JoinCodeMatcher.matches(CODE, ""))
        assertFalse("whitespace only", JoinCodeMatcher.matches(CODE, "   "))
    }

    private companion object {
        val CODE = JoinCode(adjective = "swift", noun = "panda")
    }
}
