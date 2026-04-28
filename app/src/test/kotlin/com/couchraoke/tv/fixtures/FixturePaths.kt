package com.couchraoke.tv.fixtures

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FixturePaths {
    private val repoRoot: Path by lazy {
        generateSequence(Paths.get("").toAbsolutePath().normalize()) { current ->
            current.parent
        }.firstOrNull { candidate ->
            Files.isRegularFile(candidate.resolve("fixtures/manifest.json"))
        } ?: error("Could not locate repository root from ${Paths.get("").toAbsolutePath()}")
    }

    val fixturesRoot: Path by lazy { repoRoot.resolve("fixtures") }
    val manifestPath: Path by lazy { fixturesRoot.resolve("manifest.json") }

    fun fixtureGroupDir(fixtureId: String): Path =
        resolveWithin(fixturesRoot, fixtureId)

    fun songsRootDir(fixtureId: String): Path {
        val groupDir = fixtureGroupDir(fixtureId)
        val nestedSongsRoot = groupDir.resolve("songs_root")
        return if (Files.isDirectory(nestedSongsRoot)) nestedSongsRoot else groupDir
    }

    fun fixtureFile(fixtureId: String, relativePath: String): Path =
        resolveWithin(fixtureGroupDir(fixtureId), relativePath)

    fun songDir(fixtureId: String, relativeSongDir: String): Path =
        resolveWithin(songsRootDir(fixtureId), relativeSongDir)

    fun songFile(fixtureId: String, relativeSongDir: String, fileName: String): Path =
        resolveWithin(songDir(fixtureId, relativeSongDir), fileName)

    fun invariantRelativePath(root: Path, path: Path): String {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val normalizedPath = path.toAbsolutePath().normalize()
        require(normalizedPath.startsWith(normalizedRoot)) {
            "Path $normalizedPath is not inside $normalizedRoot"
        }
        return normalizedRoot.relativize(normalizedPath).toString().replace('\\', '/')
    }

    private fun resolveWithin(root: Path, relativePath: String): Path {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val resolved = normalizedRoot.resolve(relativePath).normalize()
        require(resolved.startsWith(normalizedRoot)) {
            "Path escapes fixture root: $relativePath"
        }
        return resolved
    }
}
