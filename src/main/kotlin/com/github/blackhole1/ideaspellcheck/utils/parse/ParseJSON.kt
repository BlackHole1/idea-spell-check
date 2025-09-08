package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CSpellWordsFormat(
    val words: List<String>
)

@Serializable
data class PackageJSONFormat(
    val cspell: CSpellWordsFormat? = null
)

private val logger = Logger.getInstance("CSpell.ParseJSON")

val json = Json {
    ignoreUnknownKeys = true
}

fun parseJSON(file: File): List<String>? {
    val isPackageJSON = file.name == "package.json"

    return try {
        if (isPackageJSON) {
            val parseRawJSON = json.decodeFromString<PackageJSONFormat>(file.readText())
            parseRawJSON.cspell?.words ?: emptyList()
        } else {
            val parseRawJSON = json.decodeFromString<CSpellWordsFormat>(file.readText())
            parseRawJSON.words
        }
    } catch (e: Exception) {
        logger.debug("Failed to parse JSON from ${file.path}", e)
        null
    }

}
