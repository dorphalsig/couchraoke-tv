package com.couchraoke.tv.domain.library

import com.couchraoke.tv.domain.parser.FileResolver
import com.couchraoke.tv.domain.parser.UsdxParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

class SongDiscovery {
    fun discoverFromDirectory(rootDir: Path): List<SongEntry> {
        return Files.walk(rootDir)
            .filter { it.toString().endsWith(".txt") }
            .map { txtPath ->
                val relativeTxtPath = rootDir.relativize(txtPath).toString().replace("\\", "/").removePrefix("/")
                val text = txtPath.toFile().readText()
                val result = UsdxParser().parse(
                    songIdentifier = relativeTxtPath,
                    rawText = text,
                    fileResolver = FileResolver { file ->
                        val resolved = txtPath.parent.resolve(file)
                        val exists = resolved.toFile().exists()
                        exists
                    }
                )
                SongIndexer.fromParseResult(
                    parseResult = result,
                    phoneClientId = "local",
                    relativeTxtPath = relativeTxtPath,
                    modifiedTimeMs = txtPath.toFile().lastModified(),
                    txtUrl = relativeTxtPath,
                    audioUrl = null,
                    videoUrl = null,
                    coverUrl = null,
                    backgroundUrl = null,
                    instrumentalUrl = null,
                    vocalsUrl = null
                )
            }
            .toList()
    }
}
