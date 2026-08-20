package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.Hello
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Validates a raw `hello` handshake payload against the normative order in
 * contracts/domain-api.md: parse failure, then wrong `type`, then `protocolVersion`, then
 * missing required fields in `required`-array order, then out-of-range fields.
 *
 * The payload is decoded to a [JsonObject] first, never to [Hello] directly, because kotlinx
 * reports missing fields in declaration order and may batch several into one
 * `MissingFieldException` -- that would make the *reported* field non-deterministic. Field
 * presence and range checks therefore run against the [JsonObject], and [Hello] is only
 * decoded from it once every explicit check has passed; that final decode is what catches an
 * unknown extra field, since it runs with `ignoreUnknownKeys = false`
 * (contracts/wire-protocol.md).
 *
 * `protocolVersion` and `type` are checked only when the key is present with a coercible
 * value: a genuinely *absent* `type` or `protocolVersion` falls through to the missing-field
 * check below instead, which reports it by name. This keeps every "absent field" outcome
 * uniform, rather than one of them producing a "wrong value" message that names a value that
 * was never there.
 */
class HandshakeValidator(private val supportedProtocolVersion: Int = 1) {

    private val json = Json { ignoreUnknownKeys = false }

    fun validate(raw: String): HelloValidation {
        val jsonObject = runCatching { json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
        return jsonObject?.let(::validateFields) ?: invalid(MALFORMED_MESSAGE)
    }

    /**
     * Runs the normative checks in order, short-circuiting on the first failure. Wrapped in
     * [runCatching] because a field of the wrong JSON kind (for example `httpPort` sent as a
     * string) throws from the property accessors below rather than returning null -- that is
     * still a malformed `hello`, not a crash.
     */
    private fun validateFields(jsonObject: JsonObject): HelloValidation =
        runCatching {
            typeCheck(jsonObject)
                ?: protocolVersionCheck(jsonObject)
                ?: missingFieldCheck(jsonObject)
                ?: rangeCheck(jsonObject)
                ?: decodeHello(jsonObject)
        }.getOrElse { invalid(MALFORMED_MESSAGE) }

    private fun typeCheck(jsonObject: JsonObject): HelloValidation.Invalid? =
        jsonObject["type"]?.jsonPrimitive?.contentOrNull
            ?.takeUnless { it == "hello" }
            ?.let { invalid(UNSUPPORTED_TYPE_MESSAGE) }

    private fun protocolVersionCheck(jsonObject: JsonObject): HelloValidation.Invalid? =
        jsonObject["protocolVersion"]?.jsonPrimitive?.intOrNull
            ?.takeIf { it != supportedProtocolVersion }
            ?.let { HelloValidation.Invalid(RefusalReason.PROTOCOL_MISMATCH, "Unsupported protocolVersion: $it") }

    private fun missingFieldCheck(jsonObject: JsonObject): HelloValidation.Invalid? =
        REQUIRED_FIELDS.firstOrNull { it !in jsonObject }
            ?.let { invalid("Missing required field: $it") }

    private fun rangeCheck(jsonObject: JsonObject): HelloValidation.Invalid? {
        val clientId = jsonObject.getValue("clientId").jsonPrimitive.content
        if (clientId.length < MIN_CLIENT_ID_LENGTH) {
            return invalid("clientId must be at least $MIN_CLIENT_ID_LENGTH characters")
        }
        val httpPort = jsonObject.getValue("httpPort").jsonPrimitive.int
        return if (httpPort in HTTP_PORT_RANGE) {
            null
        } else {
            invalid("httpPort must be between ${HTTP_PORT_RANGE.first} and ${HTTP_PORT_RANGE.last}")
        }
    }

    private fun decodeHello(jsonObject: JsonObject): HelloValidation =
        runCatching { json.decodeFromJsonElement(Hello.serializer(), jsonObject) }
            .fold(
                onSuccess = { HelloValidation.Valid(it) },
                onFailure = { invalid(UNRECOGNIZED_FIELD_MESSAGE) },
            )

    private fun invalid(message: String): HelloValidation.Invalid =
        HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, message)

    private companion object {
        const val MALFORMED_MESSAGE = "Malformed message"
        const val UNSUPPORTED_TYPE_MESSAGE = "Unsupported message type"
        const val UNRECOGNIZED_FIELD_MESSAGE = "Unrecognized field in message"
        const val MIN_CLIENT_ID_LENGTH = 8
        val HTTP_PORT_RANGE = 1024..65535
        val REQUIRED_FIELDS = listOf("type", "protocolVersion", "clientId", "deviceName", "appVersion", "httpPort")
    }
}
