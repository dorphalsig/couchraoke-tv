package com.couchraoke.tv.fixtures

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import java.nio.file.Path

object Phase0Assertions {
    fun assertDiscoverySnapshot(expectedPath: Path, actual: JsonElement) {
        assertSnapshot(expectedPath, canonicalizeDiscovery(actual))
    }

    fun assertParsedSongSnapshot(expectedPath: Path, actual: JsonElement) {
        assertSnapshot(expectedPath, actual)
    }

    fun assertScoreSnapshot(expectedPath: Path, actual: JsonElement) {
        assertSnapshot(expectedPath, actual)
    }

    fun assertDiagnostics(expected: List<JsonObject>, actual: List<JsonObject>) {
        assertEquals(JsonArray(expected), JsonArray(actual))
    }

    fun assertDiagnostic(
        actual: JsonObject,
        severity: String,
        code: String,
        txtUri: String,
        lineNumber: Int? = null,
    ) {
        assertEquals(diagnostic(severity, code, txtUri, lineNumber), actual)
    }

    fun diagnostic(
        severity: String,
        code: String,
        txtUri: String,
        lineNumber: Int? = null,
    ): JsonObject = buildJsonObject {
        put("severity", severity)
        put("code", code)
        put("txtUri", txtUri)
        putNullableInt("lineNumber", lineNumber)
    }

    fun assertPitchSamples(expected: List<JsonObject>, actual: List<JsonObject>) {
        assertEquals(JsonArray(expected), JsonArray(actual))
    }

    fun assertPitchSample(actual: JsonObject, expected: JsonObject) {
        assertEquals(expected, actual)
    }

    fun pitchSample(
        midiNote: Int,
        captureTimeMs: Long,
        toneValid: Boolean = midiNote != 255,
        playerId: String? = null,
        seq: Long? = null,
        type: String? = null,
        protocolVersion: Int? = null,
    ): JsonObject = buildJsonObject {
        putNullableString("type", type)
        putNullableInt("protocolVersion", protocolVersion)
        putNullableString("playerId", playerId)
        putNullableLong("seq", seq)
        put("tCaptureMs", captureTimeMs)
        put("toneValid", toneValid)
        put("midiNote", midiNote)
    }

    fun pitchSamples(vararg samples: JsonObject): List<JsonObject> = samples.toList()

    fun diagnostics(vararg entries: JsonObject): List<JsonObject> = entries.toList()

    fun jsonArrayOf(vararg elements: JsonElement): JsonArray = buildJsonArray {
        elements.forEach(::add)
    }

    private fun assertSnapshot(expectedPath: Path, actual: JsonElement) {
        val expectedRaw = FixtureJson.readElement(expectedPath)
        val expected = canonicalizeDiscovery(expectedRaw)
        assertEquals(FixtureJson.ordered(expected), FixtureJson.ordered(actual))
    }

    private fun canonicalizeDiscovery(element: JsonElement): JsonElement {
        val jsonObject = element as? JsonObject
        val songs = jsonObject?.get("songs") as? JsonArray
        return if (jsonObject == null || songs == null) {
            element
        } else {
            buildJsonObject {
                jsonObject.forEach { (key, value) ->
                    put(key, if (key == "songs") sortedSongs(songs) else value)
                }
            }
        }
    }

    private fun sortedSongs(songs: JsonArray): JsonArray =
        JsonArray(
            songs.sortedBy { song ->
                (song as JsonObject)["songTxtRel"]?.jsonPrimitive?.content ?: ""
            }
        )

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableString(key: String, value: String?) {
        if (value == null) {
            put(key, JsonNull)
        } else {
            put(key, JsonPrimitive(value))
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableInt(key: String, value: Int?) {
        if (value == null) {
            put(key, JsonNull)
        } else {
            put(key, JsonPrimitive(value))
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putNullableLong(key: String, value: Long?) {
        if (value == null) {
            put(key, JsonNull)
        } else {
            put(key, JsonPrimitive(value))
        }
    }
}
