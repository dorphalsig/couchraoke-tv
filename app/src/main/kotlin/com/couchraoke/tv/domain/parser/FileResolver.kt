package com.couchraoke.tv.domain.parser

fun interface FileResolver {
    fun exists(path: String): Boolean
}
