package com.couchraoke.tv.domain.parser

data class ParseResult(
    val parsedSong: ParsedSong,
    val invalidCode: DiagnosticCode? = null,
)
