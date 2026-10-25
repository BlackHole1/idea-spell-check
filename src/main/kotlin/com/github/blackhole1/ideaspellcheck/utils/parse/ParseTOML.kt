package com.github.blackhole1.ideaspellcheck.utils.parse

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.modules.EmptySerializersModule
import java.io.File

private val logger = Logger.getInstance("CSpell.ParseTOML")

private val toml = Toml(
    TomlInputConfig(ignoreUnknownNames = true),
    TomlOutputConfig(),
    EmptySerializersModule()
)

fun parseTOML(file: File): ParsedCSpellConfig? {
    return try {
        toml.decodeFromString(CSpellWordsFormat.serializer(), file.readText()).toParsedConfig()
    } catch (e: Exception) {
        logger.warn("Failed to parse TOML from ${file.path}", e)
        null
    }
}
