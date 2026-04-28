package com.couchraoke.tv.fixtures

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    fun <T> decode(path: Path, serializer: DeserializationStrategy<T>): T =
        format.decodeFromString(serializer, readText(path))

    inline fun <reified T> encodeString(value: T): String =
        canonicalize(format.encodeToString(value))

    fun <T> encodeString(serializer: SerializationStrategy<T>, value: T): String =
        canonicalize(format.encodeToJsonElement(serializer, value))

    inline fun <reified T> write(path: Path, value: T) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, encodeString(value), UTF_8)
    }

    fun <T> write(path: Path, serializer: SerializationStrategy<T>, value: T) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, encodeString(serializer, value), UTF_8)
    }

    fun parseElement(text: String): JsonElement =
        format.parseToJsonElement(text)

    fun readElement(path: Path): JsonElement =
        parseElement(readText(path))

    fun encodeElement(element: JsonElement): String =
        format.encodeToString(JsonElement.serializer(), ordered(element))

    fun canonicalize(text: String): String =
        canonicalize(parseElement(text))

    fun canonicalize(path: Path): String =
        canonicalize(readText(path))

    fun canonicalize(element: JsonElement): String =
        encodeElement(element)

    fun ordered(element: JsonElement): JsonElement =
        when (element) {
            is JsonObject -> JsonObject(element.toSortedMap().mapValues { (_, value) -> ordered(value) })
            is JsonArray -> JsonArray(element.map(::ordered))
            is JsonPrimitive -> element
        }

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
