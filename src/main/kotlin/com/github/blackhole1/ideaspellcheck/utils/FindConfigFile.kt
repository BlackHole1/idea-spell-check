package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

private val cspellFileName = listOf(
    ".cspell.json",
    "cspell.json",
    ".cSpell.json",
    "cSpell.json",
    "cspell.config.js",
    "cspell.config.cjs",
    "cspell.config.json",
    "cspell.config.yaml",
    "cspell.config.yml",
    "cspell.yaml",
    "cspell.yml",
    "package.json"
)

fun findCSpellConfigFile(path: String): File? {
    // for .vscode
    val vscode = "$path${File.separator}.vscode"
    val vscodeFile = File(vscode)
    if (vscodeFile.isDirectory) {
        val res = findCSpellConfigFile(vscode)
        if (res != null) {
            return res
        }
    }

    // for .idea
    val idea = "$path${File.separator}.idea"
    val ideaFile = File(idea)
    if (ideaFile.isDirectory) {
        val res = findCSpellConfigFile(idea)
        if (res != null) {
            return res
        }
    }

    for (fileName in cspellFileName) {
        val filePath = "$path${File.separator}$fileName"
        val file = File(filePath)

        if (file.isFile) {
            return file
        }
    }

    return null
}
