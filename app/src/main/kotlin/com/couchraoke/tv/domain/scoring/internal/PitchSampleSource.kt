package com.couchraoke.tv.domain.scoring.internal

import com.couchraoke.tv.domain.scoring.model.PitchSample

internal fun interface PitchSampleSource {
    suspend fun samples(): List<PitchSample>
}
