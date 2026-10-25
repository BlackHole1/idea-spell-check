package com.github.blackhole1.ideaspellcheck.utils.parse

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.modules.EmptySerializersModule
import java.io.File

private val toml = Toml(
    TomlInputConfig(ignoreUnknownNames = true),
    TomlOutputConfig(),
    EmptySerializersModule()
)

/**
 * Parse a TOML config file
 * @throws Exception if the file cannot be read or is not a valid config
 */
fun parseTOML(file: File): ParsedCSpellConfig =
    toml.decodeFromString(CSpellWordsFormat.serializer(), file.readText()).toParsedConfig()
