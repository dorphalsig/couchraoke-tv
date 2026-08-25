package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.session.model.JoinCode

/**
 * Matches a phone-presented token against the session's [JoinCode] (contracts/domain-api.md).
 * [matches] returns `false` for `null` or blank, producing `invalid_token` (FR-009).
 *
 * Implemented ahead of its nominal task (T048) because `SessionCoordinator.authorize` cannot
 * check a token without it, and until it did, every token was accepted — see spec.md
 * Observation 19.
 *
 * [normalize] upper-cases, trims surrounding whitespace and strips hyphens, so a token is
 * compared by the words it names rather than by how the phone happened to punctuate it: a
 * QR scan yields `SWIFT-PANDA` while someone typing the code by hand may send `swift panda`
 * or `SwiftPanda`. Inner whitespace is stripped for the same reason.
 */
object JoinCodeMatcher {
    fun matches(expected: JoinCode, presented: String?): Boolean {
        if (presented.isNullOrBlank()) return false
        return normalize(presented) == normalize(expected.display)
    }

    fun normalize(value: String): String =
        value.uppercase().filterNot { it == '-' || it.isWhitespace() }
}
