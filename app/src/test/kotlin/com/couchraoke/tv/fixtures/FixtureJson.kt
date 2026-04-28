package com.couchraoke.tv.fixtures

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

object FixtureJson {
    val format: Json = Json {
        prettyPrint = true
        explicitNulls = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun readText(path: Path): String = Files.readString(path, UTF_8)

    inline fun <reified T> decodeString(text: String): T =
        format.decodeFromString(text)

    inline fun <reified T> decode(path: Path): T =
        decodeString(readText(path))

    fun <T> decode(path: Path, serializer: KSerializer<T>): T =
        format.decodeFromString(serializer, readText(path))

    inline fun <reified T> encodeString(value: T): String =
        format.encodeToString(value)

    fun <T> encodeString(serializer: KSerializer<T>, value: T): String =
        format.encodeToString(serializer, value)

    inline fun <reified T> write(path: Path, value: T) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, encodeString(value), UTF_8)
    }

    fun <T> write(path: Path, serializer: KSerializer<T>, value: T) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, encodeString(serializer, value), UTF_8)
    }

    fun parseElement(text: String): JsonElement =
        format.parseToJsonElement(text)

    fun readElement(path: Path): JsonElement =
        parseElement(readText(path))

    fun encodeElement(element: JsonElement): String =
        format.encodeToString(JsonElement.serializer(), element)

    fun canonicalize(text: String): String =
        encodeElement(parseElement(text))

    fun canonicalize(path: Path): String =
        canonicalize(readText(path))

    fun readJsonLines(path: Path): List<JsonObject> =
        Files.readAllLines(path, UTF_8)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { format.decodeFromString<JsonObject>(it) }
            .toList()

    inline fun <reified T> decodeJsonLines(path: Path): List<T> =
        Files.readAllLines(path, UTF_8)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { format.decodeFromString<T>(it) }
            .toList()
}
