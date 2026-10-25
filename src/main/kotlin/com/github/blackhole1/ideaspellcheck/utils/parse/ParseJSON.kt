package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PackageJSONFormat(
    val cspell: CSpellWordsFormat? = null
)

private val logger = Logger.getInstance("CSpell.ParseJSON")

val json = Json {
    ignoreUnknownKeys = true
    // FIXME: I don't know why there's always an error here in IDEA, but it can compile and run normally.
    allowComments = true
    allowTrailingComma = true
}

fun parseJSON(file: File): ParsedCSpellConfig? {
    val isPackageJSON = file.name == "package.json"

    return try {
        if (isPackageJSON) {
            json.decodeFromString<PackageJSONFormat>(file.readText()).cspell?.toParsedConfig() ?: ParsedCSpellConfig()
        } else {
            json.decodeFromString<CSpellWordsFormat>(file.readText()).toParsedConfig()
        }
    } catch (e: Exception) {
        logger.warn("Failed to parse JSON from ${file.path}", e)
        null
    }
}
