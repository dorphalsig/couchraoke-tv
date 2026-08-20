package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.ConnectedDeviceDto
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.control.model.Refusal
import com.couchraoke.tv.domain.control.model.SessionState
import com.couchraoke.tv.domain.control.model.SlotDto
import com.couchraoke.tv.domain.control.model.Slots
import com.couchraoke.tv.fixtures.FixtureJson
import com.couchraoke.tv.fixtures.FixturePaths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Path

/**
 * Drives [ControlMessageCodec] against `fixtures/F20_websocket_message_validation/` —
 * read at test time, never restated in Kotlin (see `GamePhaseMachineFixtureTest` in
 * `com.couchraoke.tv.domain.session` for the convention this follows).
 *
 * The three refusal cases are asserted byte-for-byte against `expected.error.json`. The
 * accept case (`case_valid_hello`) is **not** asserted byte-for-byte: its
 * `expected.sessionState.json` omits the schema-required `connectedDevices` and fills
 * `slots.P1` from the joining phone, both of which contradict Appendix B.2.2 (spec.md
 * Out-of-Scope Observation 8; contracts/wire-protocol.md "Fixture correspondence"). Only
 * the admission decision (`accepted = true`, `connectionId = 1`) is read from that
 * fixture; the emitted `sessionState` payload is validated against B.2.2 directly.
 */
class ControlMessageCodecFixtureTest {

    private val codec = ControlMessageCodec(
        Json {
            explicitNulls = false
            ignoreUnknownKeys = false
        },
    )

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
    fun acceptsValidHelloAndEmitsSessionStatePerB22() {
        val input = readText("case_valid_hello", "input.hello.json")
        val decoded = codec.decodeHello(input)

        assertTrue("case_valid_hello's input must decode successfully", decoded.isSuccess)
        assertEquals(
            Hello(
                type = "hello",
                protocolVersion = 1,
                clientId = "phone-a-uuid",
                deviceName = "Pixel 7",
                appVersion = "1.0.0",
                httpPort = 34781,
            ),
            decoded.getOrThrow(),
        )

        // Decision-only: the fixture's payload contradicts B.2.2 (see class doc), so only
        // "accepted" and the assigned connectionId are read from it.
        val fixture = FixtureJson.readElement(fixturePath("case_valid_hello", "expected.sessionState.json")).jsonObject
        assertEquals(true, fixture.getValue("accepted").jsonPrimitive.boolean)
        val fixturePayload = fixture.getValue("payload").jsonObject
        assertEquals(1, fixturePayload.getValue("connectionId").jsonPrimitive.int)

        val hello = decoded.getOrThrow()
        val sessionState = SessionState(
            sessionId = "sess-001",
            slots = Slots(
                p1 = SlotDto(connected = false, deviceName = ""),
                p2 = SlotDto(connected = false, deviceName = ""),
            ),
            connectedDevices = listOf(
                ConnectedDeviceDto(
                    clientId = hello.clientId,
                    displayName = hello.deviceName,
                    state = "connected_unassigned",
                ),
            ),
            inSong = false,
            connectionId = 1,
        )

        val emitted = FixtureJson.parseElement(codec.encodeSessionState(sessionState)).jsonObject

        assertEquals("sessionState", emitted.getValue("type").jsonPrimitive.content)
        assertEquals(1, emitted.getValue("protocolVersion").jsonPrimitive.int)
        assertFalse("tsTvMs must be omitted, not null (additionalProperties: false)", emitted.containsKey("tsTvMs"))
        assertEquals("sess-001", emitted.getValue("sessionId").jsonPrimitive.content)

        val slots = emitted.getValue("slots").jsonObject
        val p1 = slots.getValue("P1").jsonObject
        val p2 = slots.getValue("P2").jsonObject
        assertEquals(false, p1.getValue("connected").jsonPrimitive.boolean)
        assertEquals("", p1.getValue("deviceName").jsonPrimitive.content)
        assertEquals(false, p2.getValue("connected").jsonPrimitive.boolean)
        assertEquals("", p2.getValue("deviceName").jsonPrimitive.content)

        val deviceArray = emitted.getValue("connectedDevices") as JsonArray
        assertEquals(1, deviceArray.size)
        val device = deviceArray.single().jsonObject
        assertEquals("phone-a-uuid", device.getValue("clientId").jsonPrimitive.content)
        assertEquals("Pixel 7", device.getValue("displayName").jsonPrimitive.content)
        assertEquals("connected_unassigned", device.getValue("state").jsonPrimitive.content)
        assertFalse("slot must be omitted, not null, when unassigned", device.containsKey("slot"))

        assertEquals(false, emitted.getValue("inSong").jsonPrimitive.boolean)
        assertFalse("songTimeSec must be omitted, not null", emitted.containsKey("songTimeSec"))
        assertEquals(1, emitted.getValue("connectionId").jsonPrimitive.int)
    }

    @Test(timeout = 30_000)
    fun rejectsMissingClientIdAndEmitsFixtureErrorByteForByte() {
        val decoded = codec.decodeHello(readText("case_missing_clientId", "input.hello.json"))
        assertTrue("decodeHello must fail when a required field is absent", decoded.isFailure)

        assertRefusalMatchesFixture("case_missing_clientId", RefusalReason.INVALID_MESSAGE)
    }

    @Test(timeout = 30_000)
    fun rejectsMissingHttpPortAndEmitsFixtureErrorByteForByte() {
        val decoded = codec.decodeHello(readText("case_missing_httpPort", "input.hello.json"))
        assertTrue("decodeHello must fail when a required field is absent", decoded.isFailure)

        assertRefusalMatchesFixture("case_missing_httpPort", RefusalReason.INVALID_MESSAGE)
    }

    @Test(timeout = 30_000)
    fun decodesBadProtocolVersionStructurallyAndEmitsFixtureErrorByteForByte() {
        // decodeHello only enforces JSON structure; validating the protocol version
        // itself is HandshakeValidator's job (a later unit), so the codec decodes this
        // structurally-complete payload successfully rather than rejecting it.
        val decoded = codec.decodeHello(readText("case_bad_protocolVersion", "input.hello.json"))
        assertTrue(
            "a structurally complete hello must decode even with an unsupported protocolVersion",
            decoded.isSuccess,
        )
        assertEquals(2, decoded.getOrThrow().protocolVersion)

        assertRefusalMatchesFixture("case_bad_protocolVersion", RefusalReason.PROTOCOL_MISMATCH)
    }

    /**
     * Builds a [Refusal] from the fixture's own `code`/`message` — never restated in
     * Kotlin — and asserts [ControlMessageCodec.encodeError] reproduces the fixture's
     * `payload` object exactly once both sides are canonicalized (sorted keys, no
     * whitespace differences). [expectedReason] cross-checks that the fixture's wire
     * `code` still matches [RefusalReason]'s own constant, so the two cannot drift apart.
     */
    private fun assertRefusalMatchesFixture(caseDir: String, expectedReason: RefusalReason) {
        val fixture = FixtureJson.readElement(fixturePath(caseDir, "expected.error.json")).jsonObject
        assertEquals(false, fixture.getValue("accepted").jsonPrimitive.boolean)
        val expectedPayload = fixture.getValue("payload").jsonObject

        val code = expectedPayload.getValue("code").jsonPrimitive.content
        val message = expectedPayload.getValue("message").jsonPrimitive.content
        assertEquals(expectedReason.code, code)

        val emitted = codec.encodeError(Refusal(code = code, message = message))

        assertEquals(
            "encodeError for $caseDir must match F20's expected.error.json payload byte-for-byte",
            FixtureJson.canonicalize(expectedPayload),
            FixtureJson.canonicalize(emitted),
        )
    }

    private fun readText(caseDir: String, fileName: String): String =
        FixtureJson.readText(fixturePath(caseDir, fileName))

    private fun fixturePath(caseDir: String, fileName: String): Path =
        FixturePaths.fixtureFile(FIXTURE_ID, "$caseDir/$fileName")

    private companion object {
        const val FIXTURE_ID = "F20_websocket_message_validation"
    }
}
