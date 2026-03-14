package com.couchraoke.tv.data.files

import com.couchraoke.tv.domain.parser.FileResolver
import java.io.File

class LocalFileResolver : FileResolver {
    override fun exists(path: String): Boolean = File(path).exists()
}
