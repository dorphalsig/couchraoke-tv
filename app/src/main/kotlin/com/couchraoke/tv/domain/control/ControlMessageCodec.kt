package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.control.model.Refusal
import com.couchraoke.tv.domain.control.model.SessionState
import kotlinx.serialization.json.Json

/**
 * Encodes and decodes the three control-channel wire frames (contracts/domain-api.md).
 *
 * [json] is the decode-side configuration handed in by the caller: `explicitNulls = false`
 * and `ignoreUnknownKeys = false` per contracts/wire-protocol.md, so an inbound `hello`
 * carrying an extra field is a schema violation (FR-011) rather than tolerated noise.
 * `encodeDefaults` has no effect on decoding, so [json] alone is never sufficient for the
 * two encode operations below.
 *
 * Encoding has a genuinely different requirement from decoding, which is why a second,
 * private `Json` is derived here instead of reusing [json] directly. `sessionState` and
 * `error` both declare `type` and `protocolVersion` with Kotlin defaults, but both
 * schemas mark those two fields *required* — they must always be written even though
 * they equal their default value. At the same time every schema sets
 * `additionalProperties: false`, so every other absent optional (`tsTvMs`, `songTimeSec`,
 * and `connectionId` outside the direct reply to a `hello`, FR-014) must be omitted
 * entirely, never written as an explicit `null`. One `Json` instance cannot satisfy both
 * halves at once: `encodeDefaults = false` would drop the required constants when they
 * hold their default value, while `encodeDefaults = true` on its own would also force the
 * nullable optionals out as explicit `null`s unless paired with `explicitNulls = false`.
 * The encode-side `Json` below is therefore built from [json] (so `ignoreUnknownKeys`
 * stays consistent) with `encodeDefaults = true` and `explicitNulls = false` layered on
 * top. `ControlMessageCodecFixtureTest` verifies this actually produces the bytes
 * Appendix B.2.2/B.2.5 require: the required constants present, the nullable optionals
 * absent, and no stray `null` literals.
 */
class ControlMessageCodec(private val json: Json) {

    private val encodeJson: Json =
        Json(from = json) {
            encodeDefaults = true
            explicitNulls = false
        }

    fun decodeHello(raw: String): Result<Hello> =
        runCatching { json.decodeFromString(Hello.serializer(), raw) }

    fun encodeSessionState(state: SessionState): String =
        encodeJson.encodeToString(SessionState.serializer(), state)

    fun encodeError(refusal: Refusal): String =
        encodeJson.encodeToString(Refusal.serializer(), refusal)
}
