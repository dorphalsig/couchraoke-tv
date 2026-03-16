package com.couchraoke.tv.domain.network.pitch

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PitchFrameCodec {

    private const val FRAME_SIZE = 16

    fun decode(bytes: ByteArray): PitchFrame? {
        if (bytes.size != FRAME_SIZE) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return PitchFrame(
            seq            = buf.int.toLong() and 0xFFFFFFFFL,
            tvTimeMs       = buf.int,
            songInstanceSeq = buf.int.toLong() and 0xFFFFFFFFL,
            playerId       = buf.get().toInt() and 0xFF,
            midiNote       = buf.get().toInt() and 0xFF,
            connectionId   = buf.short.toInt() and 0xFFFF,
        )
    }

    fun encode(frame: PitchFrame): ByteArray {
        val buf = ByteBuffer.allocate(FRAME_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(frame.seq.toInt())
        buf.putInt(frame.tvTimeMs)
        buf.putInt(frame.songInstanceSeq.toInt())
        buf.put(frame.playerId.toByte())
        buf.put(frame.midiNote.toByte())
        buf.putShort(frame.connectionId.toShort())
        return buf.array()
    }
}
