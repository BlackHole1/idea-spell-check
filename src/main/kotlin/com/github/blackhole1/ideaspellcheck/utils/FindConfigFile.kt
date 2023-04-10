package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

private val cspellFileName = listOf(
    ".cspell.json",
    "cspell.json",
    ".cSpell.json",
    "cSpell.json",
    "cspell.config.js",
    "cspell.config.cjs",
    ".vscode/cspell.json",
    ".vscode/cSpell.json",
    ".vscode/.cspell.json",
    "cspell.config.json",
    "cspell.config.yaml",
    "cspell.config.yml",
    "cspell.yaml",
    "cspell.yml",
    "package.json"
)

fun findCSpellConfigFile(path: String): File? {
    for (fileName in cspellFileName) {
        val filePath = "$path${File.separator}$fileName"
        val file = File(filePath)

        if (file.isFile) {
            return file
        }
    }

    return null
}
