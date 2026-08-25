package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Path

/**
 * Drives [HandshakeValidator] against `fixtures/F20_websocket_message_validation/` — read at
 * test time, never restated in Kotlin (see `ControlMessageCodecFixtureTest`, the convention
 * this follows). The three refusal cases are asserted for both the reason code and the exact
 * `message` string the fixture pins; the accept case is asserted against the decoded [Hello].
 *
 * Two cases with no F20 fixture of their own are added directly (T043): unparseable JSON and
 * an unrecognised extra field. Both are `invalid_message` per contracts/domain-api.md, but
 * neither their reason nor their message is fixture-pinned, so their exact wording is this
 * class's own choice, not a restatement of a spec string.
 *
 * The remaining cases exercise branches the four F20 cases and the two additions above never
 * reach: the wrong `type`, both `clientId`/`httpPort` range checks, and the normative ordering
 * itself (`protocolVersion` precedes field-presence; an absent `type`/`protocolVersion` falls
 * through to the missing-field check rather than reporting a bogus "wrong value").
 */
class HandshakeValidatorFixtureTest {

    private val validator = HandshakeValidator()

    @Test(timeout = 30_000)
    fun fixtureHasExactlyFourCases() {
        // Guards every test below against a vacuous pass: if the fixture directory were
        // emptied, renamed, or only partially checked out, this fails loudly instead of
        // letting the per-case tests silently read fewer files than the corpus defines.
        val caseCount = FixturePaths.fixtureGroupDir(FIXTURE_ID).toFile()
            .listFiles(File::isDirectory)
            ?.size ?: 0
        assertEquals("F20 fixture must declare exactly 4 cases", 4, caseCount)
    }

    @Test(timeout = 30_000)
    fun acceptsValidHello() {
        val result = validator.validate(readText("case_valid_hello", "input.hello.json"))

        assertTrue("case_valid_hello must be Valid", result is HelloValidation.Valid)
        assertEquals(
            Hello(
                type = "hello",
                protocolVersion = 1,
                clientId = "phone-a-uuid",
                deviceName = "Pixel 7",
                appVersion = "1.0.0",
                httpPort = 34781,
            ),
            (result as HelloValidation.Valid).hello,
        )
    }

    @Test(timeout = 30_000)
    fun rejectsMissingClientIdWithFixtureReasonAndMessage() {
        assertMatchesFixture("case_missing_clientId", RefusalReason.INVALID_MESSAGE)
    }

    @Test(timeout = 30_000)
    fun rejectsMissingHttpPortWithFixtureReasonAndMessage() {
        assertMatchesFixture("case_missing_httpPort", RefusalReason.INVALID_MESSAGE)
    }

    @Test(timeout = 30_000)
    fun rejectsBadProtocolVersionWithFixtureReasonAndMessage() {
        assertMatchesFixture("case_bad_protocolVersion", RefusalReason.PROTOCOL_MISMATCH)
    }

    @Test(timeout = 30_000)
    fun rejectsUnparseableJsonAsInvalidMessage() {
        val result = validator.validate("{ this is not json")

        assertEquals(HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "Malformed message"), result)
    }

    @Test(timeout = 30_000)
    fun rejectsUnknownExtraFieldAsInvalidMessage() {
        val result = validator.validate(VALID_HELLO_WITH_EXTRA_FIELD)

        assertTrue("an unrecognised extra field must be invalid_message", result is HelloValidation.Invalid)
        assertEquals(RefusalReason.INVALID_MESSAGE, (result as HelloValidation.Invalid).reason)
    }

    @Test(timeout = 30_000)
    fun rejectsWrongTypeAsInvalidMessage() {
        val result = validator.validate(helloJson(type = "\"goodbye\""))

        assertTrue("a non-hello type must be invalid_message", result is HelloValidation.Invalid)
        assertEquals(RefusalReason.INVALID_MESSAGE, (result as HelloValidation.Invalid).reason)
    }

    @Test(timeout = 30_000)
    fun rejectsClientIdShorterThanEightCharacters() {
        val result = validator.validate(helloJson(clientId = "\"short\""))

        assertEquals(
            HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "clientId must be at least 8 characters"),
            result,
        )
    }

    @Test(timeout = 30_000)
    fun rejectsHttpPortBelowRange() {
        val result = validator.validate(helloJson(httpPort = "80"))

        assertEquals(
            HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "httpPort must be between 1024 and 65535"),
            result,
        )
    }

    @Test(timeout = 30_000)
    fun rejectsHttpPortAboveRange() {
        val result = validator.validate(helloJson(httpPort = "70000"))

        assertEquals(
            HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "httpPort must be between 1024 and 65535"),
            result,
        )
    }

    @Test(timeout = 30_000)
    fun protocolVersionMismatchTakesPrecedenceOverAMissingField() {
        // Proves the normative order (contracts/domain-api.md): protocolVersion is checked
        // before field presence, so a wrong version reports protocol_mismatch even when
        // clientId is also absent -- never "Missing required field: clientId".
        val result = validator.validate(
            """
            {
              "type": "hello",
              "protocolVersion": 2,
              "deviceName": "Pixel 7",
              "appVersion": "1.0.0",
              "httpPort": 34781
            }
            """.trimIndent(),
        )

        assertEquals(
            HelloValidation.Invalid(RefusalReason.PROTOCOL_MISMATCH, "Unsupported protocolVersion: 2"),
            result,
        )
    }

    @Test(timeout = 30_000)
    fun missingTypeFallsThroughToTheMissingFieldMessage() {
        val result = validator.validate(
            """
            {
              "protocolVersion": 1,
              "clientId": "phone-a-uuid",
              "deviceName": "Pixel 7",
              "appVersion": "1.0.0",
              "httpPort": 34781
            }
            """.trimIndent(),
        )

        assertEquals(
            HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "Missing required field: type"),
            result,
        )
    }

    @Test(timeout = 30_000)
    fun missingProtocolVersionFallsThroughToTheMissingFieldMessage() {
        val result = validator.validate(
            """
            {
              "type": "hello",
              "clientId": "phone-a-uuid",
              "deviceName": "Pixel 7",
              "appVersion": "1.0.0",
              "httpPort": 34781
            }
            """.trimIndent(),
        )

        assertEquals(
            HelloValidation.Invalid(RefusalReason.INVALID_MESSAGE, "Missing required field: protocolVersion"),
            result,
        )
    }

    /**
     * Asserts [HandshakeValidator.validate] against `expected.error.json` for [caseDir]: the
     * reason code and the exact `message` string, both read from the fixture rather than
     * restated in Kotlin.
     */
    private fun assertMatchesFixture(caseDir: String, expectedReason: RefusalReason) {
        val fixture = FixtureJson.readElement(fixturePath(caseDir, "expected.error.json")).jsonObject
        assertEquals(false, fixture.getValue("accepted").jsonPrimitive.boolean)
        val expectedPayload = fixture.getValue("payload").jsonObject
        assertEquals(expectedReason.code, expectedPayload.getValue("code").jsonPrimitive.content)
        val expectedMessage = expectedPayload.getValue("message").jsonPrimitive.content

        val result = validator.validate(readText(caseDir, "input.hello.json"))

        assertEquals(HelloValidation.Invalid(expectedReason, expectedMessage), result)
    }

    /** Builds a structurally valid `hello` with one field overridden, as a raw JSON literal. */
    private fun helloJson(
        type: String = "\"hello\"",
        protocolVersion: String = "1",
        clientId: String = "\"phone-a-uuid\"",
        deviceName: String = "\"Pixel 7\"",
        appVersion: String = "\"1.0.0\"",
        httpPort: String = "34781",
    ): String =
        """
        {
          "type": $type,
          "protocolVersion": $protocolVersion,
          "clientId": $clientId,
          "deviceName": $deviceName,
          "appVersion": $appVersion,
          "httpPort": $httpPort
        }
        """.trimIndent()

    private fun readText(caseDir: String, fileName: String): String =
        FixtureJson.readText(fixturePath(caseDir, fileName))

    private fun fixturePath(caseDir: String, fileName: String): Path =
        FixturePaths.fixtureFile(FIXTURE_ID, "$caseDir/$fileName")

    private companion object {
        const val FIXTURE_ID = "F20_websocket_message_validation"
        val VALID_HELLO_WITH_EXTRA_FIELD = """
            {
              "type": "hello",
              "protocolVersion": 1,
              "clientId": "phone-a-uuid",
              "deviceName": "Pixel 7",
              "appVersion": "1.0.0",
              "httpPort": 34781,
              "extra": "surprise"
            }
        """.trimIndent()
    }
}
