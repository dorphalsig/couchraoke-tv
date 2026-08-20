package com.couchraoke.tv.domain.control

/**
 * Validates a raw `hello` handshake payload against the normative order in
 * contracts/domain-api.md: parse failure, then wrong `type`, then `protocolVersion`, then
 * missing required fields in `required`-array order, then out-of-range fields.
 *
 * Forward-declared here with its binding signature so
 * [com.couchraoke.tv.domain.session.SessionCoordinator] can be constructed; T049 implements
 * [validate] and adds [HelloValidation] alongside.
 */
class HandshakeValidator(private val supportedProtocolVersion: Int = 1) {

    fun validate(raw: String): HelloValidation = TODO(
        "HandshakeValidator.validate is completed by T049: raw=$raw " +
            "supportedProtocolVersion=$supportedProtocolVersion",
    )
}
