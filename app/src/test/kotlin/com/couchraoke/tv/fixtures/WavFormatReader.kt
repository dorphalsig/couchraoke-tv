package com.couchraoke.tv.fixtures

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import kotlin.io.path.readBytes

/**
 * Minimal RIFF/WAVE `fmt ` chunk reader for the F01 mix-pair fixtures.
 *
 * Mix-pair eligibility compares decoded sample rate and channel count (phone_app.md
 * "Phone-Side Mixing Policy"). That is phone-side work — the TV never decodes or mixes these
 * sources (tv_app.md §2.6.7) — so this lives in the fixture harness and reads only the container
 * header. It is not an audio decoder and is not used by production code.
 */
internal object WavFormatReader {
    private const val RIFF_HEADER_BYTES = 12
    private const val CHUNK_HEADER_BYTES = 8
    private const val TAG_BYTES = 4
    private const val WAVE_TAG_OFFSET = 8
    private const val CHUNK_SIZE_OFFSET = 4
    private const val FMT_CHUNK_ID = "fmt "
    private const val FMT_CHANNELS_OFFSET = 2
    private const val FMT_SAMPLE_RATE_OFFSET = 4
    private const val FMT_MIN_BODY_BYTES = 8

    data class WavFormat(val channels: Int, val sampleRateHz: Int)

    fun read(path: Path): WavFormat? {
        val bytes = path.readBytes()
        val isWave = bytes.size >= RIFF_HEADER_BYTES &&
            bytes.tag(0) == "RIFF" &&
            bytes.tag(WAVE_TAG_OFFSET) == "WAVE"
        return if (isWave) findFormatChunk(bytes) else null
    }

    private fun findFormatChunk(bytes: ByteArray): WavFormat? {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var offset = RIFF_HEADER_BYTES
        var format: WavFormat? = null
        while (format == null && offset + CHUNK_HEADER_BYTES <= bytes.size) {
            val chunkId = bytes.tag(offset)
            val chunkSize = buffer.getInt(offset + CHUNK_SIZE_OFFSET)
            if (chunkSize < 0) {
                break
            }
            val bodyOffset = offset + CHUNK_HEADER_BYTES
            if (chunkId == FMT_CHUNK_ID && bodyOffset + FMT_MIN_BODY_BYTES <= bytes.size) {
                format = WavFormat(
                    channels = buffer.getShort(bodyOffset + FMT_CHANNELS_OFFSET).toInt(),
                    sampleRateHz = buffer.getInt(bodyOffset + FMT_SAMPLE_RATE_OFFSET),
                )
            }
            // Chunk bodies are word-aligned: an odd size is followed by a pad byte.
            offset = bodyOffset + chunkSize + (chunkSize % 2)
        }
        return format
    }

    private fun ByteArray.tag(offset: Int): String =
        String(this, offset, TAG_BYTES, Charsets.US_ASCII)
}
