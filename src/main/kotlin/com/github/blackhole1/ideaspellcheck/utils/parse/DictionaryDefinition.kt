package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import java.io.File
import java.util.LinkedHashSet

@Serializable
data class DictionaryDefinition(
    val name: String? = null,
    val path: String? = null,
    val addWords: Boolean? = null
)

data class ParsedCSpellConfig(
    val words: List<String> = emptyList(),
    val dictionaryDefinitions: List<DictionaryDefinition> = emptyList(),
    val dictionaries: List<String> = emptyList()
)

data class DictionaryWordsResult(
    val words: List<String>,
    val dictionaryPaths: Set<String>
)

data class MergedWordList(
    val words: List<String>,
    val dictionaryPaths: Set<String>
)

fun readWordsFromDictionaryDefinitions(
    definitions: List<DictionaryDefinition>?,
    configFile: File,
    activeDictionaryNames: Set<String> = emptySet()
): DictionaryWordsResult {
    if (definitions.isNullOrEmpty()) {
        return DictionaryWordsResult(emptyList(), emptySet())
    }

    val configDir = configFile.absoluteFile.parentFile ?: run {
        logger.warn("CSpell config file has no parent directory: ${configFile.absolutePath}")
        return DictionaryWordsResult(emptyList(), emptySet())
    }
    val result = mutableListOf<String>()
    val usedPaths = mutableSetOf<String>()

    for (definition in definitions) {
        val normalizedName = definition.name?.trim()?.takeIf { it.isNotEmpty() }
        val enabledByName = normalizedName?.let { activeDictionaryNames.contains(it) } == true
        if (definition.addWords != true && !enabledByName) {
            continue
        }
        val rawPath = definition.path?.trim()
        if (rawPath.isNullOrEmpty()) {
            continue
        }

        val file = resolveDictionaryPath(configDir, rawPath)
        val normalizedFile = file.absoluteFile.normalize()
        usedPaths.add(normalizedFile.absolutePath)
        if (!normalizedFile.exists() || !normalizedFile.isFile) {
            continue
        }

        try {
            val words = normalizedFile.readLines()
                .map { it.trim().removePrefix("\uFEFF") }
                .filter {
                    it.isNotEmpty() &&
                    !it.startsWith("#") &&
                    !it.startsWith("//") &&
                    !it.startsWith(";")
                }
            if (words.isNotEmpty()) {
                result.addAll(words)
            }
        } catch (_: Exception) {
            // Ignore this dictionary file if reading fails to avoid disrupting the overall process
        }
    }

    return DictionaryWordsResult(result, usedPaths)
}

fun mergeWordsWithDictionaryDefinitions(
    baseWords: List<String>,
    definitions: List<DictionaryDefinition>,
    activeDictionaryNames: List<String>,
    configFile: File,
): MergedWordList {
    val normalizedActiveNames = activeDictionaryNames
        .mapNotNull { it.trim().takeIf { name -> name.isNotEmpty() } }
        .toSet()

    val fromDefinitions = readWordsFromDictionaryDefinitions(definitions, configFile, normalizedActiveNames)
    val merged = LinkedHashSet<String>().apply {
        addAll(baseWords)
        addAll(fromDefinitions.words)
    }.toList()
    return MergedWordList(merged, fromDefinitions.dictionaryPaths)
}

private val logger = Logger.getInstance("CSpell.DictionaryDefinition")

private fun resolveDictionaryPath(baseDir: File, path: String): File {
    val candidate = File(path)
    return if (candidate.isAbsolute) {
        candidate
    } else {
        File(baseDir, path).normalize()
    }
}
