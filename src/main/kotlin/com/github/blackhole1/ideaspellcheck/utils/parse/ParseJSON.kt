package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CSpellWordsFormat(
    val words: List<String> = emptyList(),
    val dictionaryDefinitions: List<DictionaryDefinition> = emptyList(),
    val dictionaries: List<String> = emptyList()
)

@Serializable
data class PackageJSONFormat(
    val cspell: CSpellWordsFormat? = null
)

private val logger = Logger.getInstance("CSpell.ParseJSON")

val json = Json {
    ignoreUnknownKeys = true
}

fun parseJSON(file: File): ParsedCSpellConfig? {
    val isPackageJSON = file.name == "package.json"

    return try {
        if (isPackageJSON) {
            val parseRawJSON = json.decodeFromString<PackageJSONFormat>(file.readText())
            val config = parseRawJSON.cspell
            if (config == null) {
                ParsedCSpellConfig()
            } else {
                ParsedCSpellConfig(config.words, config.dictionaryDefinitions, config.dictionaries)
            }
        } else {
            val parseRawJSON = json.decodeFromString<CSpellWordsFormat>(file.readText())
            ParsedCSpellConfig(parseRawJSON.words, parseRawJSON.dictionaryDefinitions, parseRawJSON.dictionaries)
        }
    } catch (e: Exception) {
        logger.debug("Failed to parse JSON from ${file.path}", e)
        null
    }

}
