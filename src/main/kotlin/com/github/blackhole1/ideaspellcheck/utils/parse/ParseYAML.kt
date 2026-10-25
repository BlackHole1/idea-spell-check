package com.github.blackhole1.ideaspellcheck.utils.parse

import net.mamoe.yamlkt.Yaml
import java.io.File

private val yaml = Yaml.Default

// See: https://github.com/Him188/yamlkt/issues/52
private val unsupportedLinePattern = Regex("^\\$.+:.+$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))

/**
 * Parse a YAML config file
 * @throws Exception if the file cannot be read or is not a valid config
 */
fun parseYAML(file: File): ParsedCSpellConfig {
    val content = file.readText().replace(unsupportedLinePattern, "")
    return yaml.decodeFromString(CSpellWordsFormat.serializer(), content).toParsedConfig()
}
