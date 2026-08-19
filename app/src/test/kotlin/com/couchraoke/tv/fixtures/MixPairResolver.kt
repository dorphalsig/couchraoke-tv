package com.couchraoke.tv.fixtures

import com.couchraoke.tv.domain.usdx.internal.DiagnosticFactory
import com.couchraoke.tv.domain.usdx.model.SongHeader
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Resolves a song's effective playback source the way the phone does at scan time.
 *
 * The TV parser decides only what the chart text can tell it: whether an `#INSTRUMENTAL`+`#VOCALS`
 * pair was authored, and therefore whether base audio is required (tv_app.md §2.4.7). Deciding
 * whether that pair is *accepted* needs decoded sample rate and channel count, and producing the
 * mixed resource needs a mixer — both phone-side (tv_app.md §2.6.7, §2.5.5; phone_app.md
 * "Phone-Side Mixing Policy"). None of this belongs in TV production code, so the F01 fixture
 * models it here.
 */
internal object MixPairResolver {
    const val MIX_MODE_BASE_AUDIO = "base_audio"
    const val MIX_MODE_GENERATED_WAV = "generated_wav"

    /** phone_app.md: the generated mix is published under this reserved namespace as a WAV. */
    const val GENERATED_AUDIO_PATH_PREFIX = "/songs/generated/"
    const val GENERATED_AUDIO_EXTENSION = ".wav"

    /** tv_app.md §2.5.6: the phone serves one effective `audioUrl`; these never reach the TV. */
    val MANIFEST_EXCLUDES = listOf("instrumentalUrl", "vocalsUrl")

    data class Resolution(
        val hasInstrumental: Boolean,
        val mixMode: String,
        val warnCodes: List<String>,
        val resolvedAudioRel: String?,
    ) {
        val usesGeneratedMix: Boolean get() = mixMode == MIX_MODE_GENERATED_WAV
    }

    fun resolve(songDir: Path, header: SongHeader): Resolution {
        val instrumental = header.instrumental?.let(songDir::resolve)?.takeIf(Path::exists)
        val vocals = header.vocals?.let(songDir::resolve)?.takeIf(Path::exists)
        val baseAudio = header.audio

        if (instrumental == null) {
            val warnCodes = if (vocals != null) {
                listOf(DiagnosticFactory.WARN_VOCALS_WITHOUT_INSTRUMENTAL)
            } else {
                emptyList()
            }
            return Resolution(
                hasInstrumental = false,
                mixMode = MIX_MODE_BASE_AUDIO,
                warnCodes = warnCodes,
                resolvedAudioRel = baseAudio,
            )
        }

        val ineligibleCode = vocals?.let { vocalsPath -> ineligibilityCode(instrumental, vocalsPath) }
        return if (vocals != null && ineligibleCode == null) {
            Resolution(
                hasInstrumental = true,
                mixMode = MIX_MODE_GENERATED_WAV,
                warnCodes = emptyList(),
                resolvedAudioRel = null,
            )
        } else {
            Resolution(
                hasInstrumental = true,
                mixMode = MIX_MODE_BASE_AUDIO,
                warnCodes = listOfNotNull(ineligibleCode),
                resolvedAudioRel = baseAudio,
            )
        }
    }

    /**
     * MVP forbids resampling and channel remixing, so a pair differing in either is ineligible and
     * the song falls back to base audio (phone_app.md "Phone-Side Mixing Policy").
     */
    private fun ineligibilityCode(instrumental: Path, vocals: Path): String? {
        val instrumentalFormat = WavFormatReader.read(instrumental)
        val vocalsFormat = WavFormatReader.read(vocals)
        return when {
            instrumentalFormat == null || vocalsFormat == null -> null

            instrumentalFormat.sampleRateHz != vocalsFormat.sampleRateHz ->
                DiagnosticFactory.WARN_MIX_PAIR_INELIGIBLE_SAMPLE_RATE

            instrumentalFormat.channels != vocalsFormat.channels ->
                DiagnosticFactory.WARN_MIX_PAIR_INELIGIBLE_CHANNELS

            else -> null
        }
    }
}
