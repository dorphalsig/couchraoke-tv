package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class UsdxParserPerformanceTest {
    private val parser = DefaultUsdxParser()

    @Test(timeout = 30_000)
    fun warmedTenKbTxtParsesUnderPhase0Budget() {
        val txtBytes = tenKbSongTxt().encodeToByteArray()
        repeat(20) { index ->
            parser.parse("warmup_$index", txtBytes).getOrThrow()
        }

        val elapsedMs = measureNanoTime {
            parser.parse("measured_10kb", txtBytes).getOrThrow()
        } / 1_000_000.0

        assertTrue("Expected warmed 10KB parse under 50ms, was $elapsedMs ms", elapsedMs < 50.0)
    }

    private fun tenKbSongTxt(): String {
        val builder = StringBuilder()
            .appendLine("#TITLE:Performance Fixture")
            .appendLine("#ARTIST:Fixture")
            .appendLine("#BPM:120")
            .appendLine("#MP3:song.mp3")
        var beat = 0
        var lineIndex = 0
        while (builder.length < 10_240) {
            builder.appendLine(": $beat 1 0 la$lineIndex")
            beat += 1
            if (beat % 8 == 0) {
                builder.appendLine("- $beat")
                lineIndex += 1
            }
        }
        builder.appendLine("E")
        return builder.toString()
    }
}
