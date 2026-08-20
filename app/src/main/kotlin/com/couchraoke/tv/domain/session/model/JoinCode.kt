package com.couchraoke.tv.domain.session.model

/**
 * A join code rendered to the host TV as two words. `display` is the human-facing
 * `ADJECTIVE-NOUN` form; `noun` alone forms the mDNS instance name.
 *
 * Equality here is structural. Matching a phone-presented token against a `JoinCode` is
 * a different operation — see `JoinCodeMatcher` in contracts/domain-api.md — because a
 * token may differ in case or hyphenation while still referring to the same code.
 */
data class JoinCode(val adjective: String, val noun: String) {
    init {
        require(adjective.isNotBlank() && adjective.none(Char::isWhitespace)) {
            "JoinCode adjective must be a single non-empty word, was \"$adjective\""
        }
        require(noun.isNotBlank() && noun.none(Char::isWhitespace)) {
            "JoinCode noun must be a single non-empty word, was \"$noun\""
        }
    }

    val display: String
        get() = "${adjective.uppercase()}-${noun.uppercase()}"
}
