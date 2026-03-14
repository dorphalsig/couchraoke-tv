package com.couchraoke.tv.domain.parser

class UsdxParser(
    private val headerParser: HeaderParser = HeaderParser(),
    private val bodyParser: BodyParser = BodyParser(),
    private val postParseValidator: PostParseValidator = PostParseValidator(),
) {
    fun parse(
        songIdentifier: String,
        rawText: String,
        fileResolver: FileResolver,
    ): ParseResult {
        val allLines = rawText.lines()
        val headerLines = allLines.takeWhile { it.trim().startsWith("#") }
        val bodyLines = allLines.drop(headerLines.size)

        val headerResult = headerParser.parse(
            songIdentifier = songIdentifier,
            lines = headerLines,
            fileResolver = fileResolver,
        )
        val bodyResult = bodyParser.parse(
            songIdentifier = songIdentifier,
            lines = bodyLines,
            lineNumberOffset = headerLines.size,
        )
        val postParseResult = if (bodyResult.hasFatalError) {
            PostParseValidationResult(
                tracks = bodyResult.trackSections.map { Track(trackId = it.trackId, lines = emptyList()) },
                derivedSummary = DerivedSongSummary(),
                diagnostics = emptyList(),
                hasFatalError = false,
            )
        } else {
            postParseValidator.finalize(
                songIdentifier = songIdentifier,
                header = headerResult.header,
                bodyResult = bodyResult,
                fileResolver = fileResolver,
            )
        }

        val diagnostics = buildList {
            addAll(headerResult.diagnostics)
            addAll(bodyResult.diagnostics)
            addAll(postParseResult.diagnostics)
        }

        val hasFatalError = headerResult.hasFatalError || bodyResult.hasFatalError || postParseResult.hasFatalError
        val parsedSong = ParsedSong(
            songIdentifier = songIdentifier,
            isValid = !hasFatalError,
            header = headerResult.header,
            tracks = postParseResult.tracks,
            derivedSummary = postParseResult.derivedSummary,
            diagnostics = diagnostics,
        )

        return ParseResult(
            parsedSong = parsedSong,
            invalidCode = diagnostics.firstOrNull { it.severity == DiagnosticSeverity.ERROR }?.code,
        )
    }
}
