package com.github.blackhole1.ideaspellcheck.utils.parse

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import java.io.File

@Serializable
private data class TomlCSpellFormat(
    val words: List<String> = emptyList(),
    val dictionaryDefinitions: List<DictionaryDefinition> = emptyList(),
    val dictionaries: List<String> = emptyList()
)

private val logger = Logger.getInstance("CSpell.ParseTOML")

private val toml = Toml(
    TomlInputConfig(ignoreUnknownNames = true),
    TomlOutputConfig(),
    EmptySerializersModule()
)

fun parseTOML(file: File): ParsedCSpellConfig? {
    return try {
        val data = toml.decodeFromString(TomlCSpellFormat.serializer(), file.readText())
        ParsedCSpellConfig(
            data.words,
            data.dictionaryDefinitions,
            data.dictionaries
        )
    } catch (e: Exception) {
        logger.warn("Failed to parse TOML from ${file.path}", e)
        null
    }
}
