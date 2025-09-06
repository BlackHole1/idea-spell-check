package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

fun findCSpellConfigFile(path: String): File? {
    val searchPaths = CSpellConfigDefinition.getAllSearchPaths(path)

    for (filePath in searchPaths) {
        val file = File(filePath)
        if (file.isFile) {
            return file
        }
    }

    return null
}
