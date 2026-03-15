package com.couchraoke.tv.domain.timing

import org.junit.Test
import org.junit.Assert.*

class BeatTimingEngineAcceptanceTest {

    // US1 — Acceptance: baseline and gap/start fixture
    @Test
    fun `fixture 18 baseline beat cursors match expected values`() {
        TODO("Not yet implemented")
    }

    // US1 — Acceptance: pre-roll and start offset
    @Test
    fun `fixture 19 gap-aware pre-roll and start offset cursors match expected values`() {
        TODO("Not yet implemented")
    }

    // US2 — Acceptance: note boundary membership
    @Test
    fun `fixture 20 note boundary membership matches expected values`() {
        TODO("Not yet implemented")
    }

    // US2 — Acceptance: late-frame rejection when latenessMs > 450
    @Test
    fun `fixture 20 frames with latenessMs over 450 are explicitly rejected`() {
        TODO("Not yet implemented")
    }

    // US3 — Acceptance: mic-delay shifts and end boundary
    @Test
    fun `fixture 19 mic-delay shifts note windows without changing authored beats`() {
        TODO("Not yet implemented")
    }
}
