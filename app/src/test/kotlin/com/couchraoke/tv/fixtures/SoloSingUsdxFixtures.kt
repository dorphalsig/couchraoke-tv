package com.couchraoke.tv.fixtures

import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.domain.usdx.model.ParsedSong

object SoloSingUsdxFixtures {
    const val SongId: String = SoloSingFixtures.SongId

    val StaticSoloChart: String = """
        #TITLE:${SoloSingFixtures.SongTitle}
        #ARTIST:${SoloSingFixtures.SongArtist}
        #BPM:120
        #GAP:0
        #MP3:demo-song.mp3
        #START:${SoloSingFixtures.StartSec}
        #PREVIEWSTART:${SoloSingFixtures.PreviewStartSec}
        : 0 4 0 Hel
        : 4 4 2 lo
        - 8
        : 12 4 4 couch
        : 16 4 5 raoke
        E
    """.trimIndent()

    val StaticSoloChartBytes: ByteArray
        get() = StaticSoloChart.encodeToByteArray()

    val InstrumentalGapChart: String = """
        #TITLE:${SoloSingFixtures.SongTitle}
        #ARTIST:${SoloSingFixtures.SongArtist}
        #BPM:120
        #GAP:0
        #MP3:demo-song.mp3
        : 0 4 0 Hold
        - 32
        : 48 4 2 next
        E
    """.trimIndent()

    val InstrumentalGapChartBytes: ByteArray
        get() = InstrumentalGapChart.encodeToByteArray()

    fun staticSoloChart(
        title: String = SoloSingFixtures.SongTitle,
        artist: String = SoloSingFixtures.SongArtist,
        bpmFile: Float = 120f,
        gapMs: Float = 0f,
        startSec: Float = SoloSingFixtures.StartSec,
        previewStartSec: Float = SoloSingFixtures.PreviewStartSec,
        notes: List<SoloSingUsdxNote> = staticSoloNotes(),
    ): String = buildString {
        appendLine("#TITLE:$title")
        appendLine("#ARTIST:$artist")
        appendLine("#BPM:${bpmFile.toPlainString()}")
        appendLine("#GAP:${gapMs.toPlainString()}")
        appendLine("#MP3:demo-song.mp3")
        appendLine("#START:${startSec.toPlainString()}")
        appendLine("#PREVIEWSTART:${previewStartSec.toPlainString()}")
        notes.forEach { note ->
            if (note.isLineBreak) {
                appendLine("- ${note.startBeatFile}")
            } else {
                appendLine(": ${note.startBeatFile} ${note.durationBeats} ${note.toneSemitone} ${note.lyric}")
            }
        }
        appendLine("E")
    }.trimEnd()

    fun staticSoloChartBytes(chart: String = staticSoloChart()): ByteArray = chart.encodeToByteArray()

    fun parsedStaticSoloChart(
        songId: String = SongId,
        chart: String = staticSoloChart(),
    ): ParsedSong = DefaultUsdxParser().parse(songId, staticSoloChartBytes(chart)).getOrThrow()

    fun staticSoloNotes(): List<SoloSingUsdxNote> = listOf(
        SoloSingUsdxNote(startBeatFile = 0, durationBeats = 4, toneSemitone = 0, lyric = "Hel"),
        SoloSingUsdxNote(startBeatFile = 4, durationBeats = 4, toneSemitone = 2, lyric = "lo"),
        SoloSingUsdxNote.lineBreak(startBeatFile = 8),
        SoloSingUsdxNote(startBeatFile = 12, durationBeats = 4, toneSemitone = 4, lyric = "couch"),
        SoloSingUsdxNote(startBeatFile = 16, durationBeats = 4, toneSemitone = 5, lyric = "raoke"),
    )

    private fun Float.toPlainString(): String = if (this % 1f == 0f) {
        toInt().toString()
    } else {
        toString()
    }
}

data class SoloSingUsdxNote(
    val startBeatFile: Int,
    val durationBeats: Int,
    val toneSemitone: Int,
    val lyric: String,
    val isLineBreak: Boolean = false,
) {
    companion object {
        fun lineBreak(startBeatFile: Int): SoloSingUsdxNote = SoloSingUsdxNote(
            startBeatFile = startBeatFile,
            durationBeats = 0,
            toneSemitone = 0,
            lyric = "",
            isLineBreak = true,
        )
    }
}
