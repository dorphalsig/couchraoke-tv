package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.JoinCode
import kotlin.random.Random

/**
 * Generates the two-word [JoinCode] shown on the host TV at session start (FR-001, FR-003).
 *
 * [random] is forward-declared here with a fixed-shape placeholder value so
 * [com.couchraoke.tv.di.SessionComponent] can mint a session's `joinCode` at construction
 * time. T033 replaces this body with the bundled ~64-word adjective/noun lists and the
 * per-process issued-set that makes the result genuinely non-repeating.
 */
class JoinCodeGenerator(private val random: Random = Random.Default) {
    fun next(): JoinCode = JoinCode(adjective = "pending", noun = "code${random.nextInt(Int.MAX_VALUE)}")
}
