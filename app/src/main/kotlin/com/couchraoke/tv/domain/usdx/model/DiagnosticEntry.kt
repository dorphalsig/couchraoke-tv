package com.couchraoke.tv.domain.usdx.model

data class DiagnosticEntry(
    val severity: Severity,
    val code: String,
    val txtUri: String,
    val lineNumber: Int? = null,
)
