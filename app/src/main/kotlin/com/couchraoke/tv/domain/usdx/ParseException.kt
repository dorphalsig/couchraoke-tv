package com.couchraoke.tv.domain.usdx

import com.couchraoke.tv.domain.usdx.model.DiagnosticEntry

class ParseException(
    val diagnostics: List<DiagnosticEntry>,
) : RuntimeException()
