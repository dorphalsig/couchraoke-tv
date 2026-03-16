package com.couchraoke.tv.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTokenTest {

    @Test
    fun `generate produces a string of length at least 8 characters`() {
        val token = SessionToken.generate()
        val normalized = SessionToken.normalize(token)
        assertTrue("Token length should be at least 8: $normalized", normalized.length >= 8)
    }

    @Test
    fun `normalize strips hyphens and spaces and uppercases`() {
        assertEquals("ABCDEFGH", SessionToken.normalize("abcd-ef gh"))
        assertEquals("ABCDEFGH", SessionToken.normalize(" ABCD EFGH "))
        assertEquals("ABCDEFGH", SessionToken.normalize("A-B-C-D-E-F-G-H"))
    }

    @Test
    fun `matches is case-insensitive`() {
        assertTrue(SessionToken.matches("abcd-efgh", "ABCDEFGH"))
        assertTrue(SessionToken.matches("ABCDEFGH", "abcd-efgh"))
        assertTrue(SessionToken.matches("a b c d e f g h i j", "ABCDEFGHIJ"))
    }

    @Test
    fun `display inserts hyphen at position 5`() {
        assertEquals("ABCDE-FGHIJ", SessionToken.display("ABCDEFGHIJ"))
        assertEquals("ABCDE-FGHIJ", SessionToken.display("abcde fghij"))
        assertEquals("ABCDE-FGHIJ", SessionToken.display("abc-de-fgh-ij"))
    }
}
