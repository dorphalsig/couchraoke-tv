package com.couchraoke.tv.data.network

import org.junit.Assert.*
import org.junit.Test

class MdnsAdvertiserTest {

    @Test
    fun `given code ABCDEFGH, when instanceName, then KaraokeTV-EFGH`() {
        assertEquals("KaraokeTV-EFGH", MdnsAdvertiser.instanceName("ABCDEFGH"))
    }

    @Test
    fun `given normalized code, when txtRecords, then code field present`() {
        val records = MdnsAdvertiser.txtRecords("ABCDEFGH")
        assertEquals("ABCDEFGH", records["code"])
    }

    @Test
    fun `given normalized code, when txtRecords, then v field is 1`() {
        val records = MdnsAdvertiser.txtRecords("ABCDEFGH")
        assertEquals("1", records["v"])
    }

    @Test
    fun `given hyphenated code, when normalize, then uppercase no hyphens`() {
        assertEquals("ABCDEFGH", MdnsAdvertiser.normalize("abcd-efgh"))
    }
}
