package com.github.blackhole1.ideaspellcheck.utils.parse

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PackageJSONFormat(
    val cspell: CSpellWordsFormat? = null
)

val json = Json {
    ignoreUnknownKeys = true
    // FIXME: I don't know why there's always an error here in IDEA, but it can compile and run normally.
    allowComments = true
    allowTrailingComma = true
}

/**
 * Parse a JSON config file (or the `cspell` field of a package.json)
 * @throws Exception if the file cannot be read or is not a valid config
 */
fun parseJSON(file: File): ParsedCSpellConfig {
    return if (file.name == "package.json") {
        json.decodeFromString<PackageJSONFormat>(file.readText()).cspell?.toParsedConfig() ?: ParsedCSpellConfig()
    } else {
        json.decodeFromString<CSpellWordsFormat>(file.readText()).toParsedConfig()
    }
}
