package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.openapi.diagnostic.Logger
import net.mamoe.yamlkt.Yaml
import java.io.File

private val logger = Logger.getInstance("CSpell.ParseYAML")

private val yaml = Yaml.Default

// See: https://github.com/Him188/yamlkt/issues/52
private val unsupportedLinePattern = Regex("^\\$.+:.+$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))

fun parseYAML(file: File): ParsedCSpellConfig? {
    return try {
        val content = file.readText().replace(unsupportedLinePattern, "")
        yaml.decodeFromString(CSpellWordsFormat.serializer(), content).toParsedConfig()
    } catch (e: Exception) {
        logger.warn("Failed to parse YAML from ${file.path}", e)
        null
    }
}
