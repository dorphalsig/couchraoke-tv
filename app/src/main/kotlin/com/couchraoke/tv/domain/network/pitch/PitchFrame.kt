package com.couchraoke.tv.domain.network.pitch

data class PitchFrame(
    val seq: Long,              // uint32
    val tvTimeMs: Int,          // int32
    val songInstanceSeq: Long,  // uint32
    val playerId: Int,          // uint8 — 0=P1, 1=P2
    val midiNote: Int,          // uint8 — 0..127 voiced; 255 = unvoiced
    val connectionId: Int,      // uint16
) {
    val toneValid: Boolean get() = midiNote != 255
    val tone: Int get() = midiNote - 36   // USDX semitone scale: C2=36 → tone=0
}
