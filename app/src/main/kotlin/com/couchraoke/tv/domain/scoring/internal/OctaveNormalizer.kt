package com.couchraoke.tv.domain.scoring.internal

internal object OctaveNormalizer {
    fun normalize(sampleToneSemitone: Int, targetToneSemitone: Int): Int {
        var normalized = sampleToneSemitone
        while (normalized - targetToneSemitone > 6) {
            normalized -= 12
        }
        while (normalized - targetToneSemitone < -6) {
            normalized += 12
        }
        return normalized
    }
}
