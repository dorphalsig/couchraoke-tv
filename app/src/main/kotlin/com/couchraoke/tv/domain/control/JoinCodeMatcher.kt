package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.session.model.JoinCode

/**
 * Matches a phone-presented token against the session's [JoinCode] (contracts/domain-api.md).
 * `matches` returns `false` for `null` or blank, producing `invalid_token` (FR-009).
 *
 * Forward-declared here with its binding signature so
 * [com.couchraoke.tv.domain.session.SessionCoordinator] can be constructed; T048 implements
 * `normalize` (uppercase, strip hyphens and surrounding whitespace) and `matches`.
 */
object JoinCodeMatcher {
    fun matches(expected: JoinCode, presented: String?): Boolean = TODO(
        "JoinCodeMatcher.matches is completed by T048: expected=$expected presented=$presented",
    )

    fun normalize(value: String): String =
        TODO("JoinCodeMatcher.normalize is completed by T048: value=$value")
}
