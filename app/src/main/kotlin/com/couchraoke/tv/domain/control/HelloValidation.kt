package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.Hello

/**
 * The outcome of [HandshakeValidator.validate] (contracts/domain-api.md). T049 owns the
 * branches that produce each case, in the normative validation order (parse failure, wrong
 * `type`, `protocolVersion`, missing required field, out-of-range field).
 */
sealed interface HelloValidation {
    data class Valid(val hello: Hello) : HelloValidation
    data class Invalid(val reason: RefusalReason, val message: String) : HelloValidation
}
