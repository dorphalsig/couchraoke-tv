package com.couchraoke.tv

import com.couchraoke.quality.NoCoverageGenerated
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoCoverageGeneratedCompileTest {
    @Test(timeout = 30_000)
    fun annotation_is_available_to_app_sources() {
        assertNotNull(NoCoverageGenerated::class.java)
    }
}
