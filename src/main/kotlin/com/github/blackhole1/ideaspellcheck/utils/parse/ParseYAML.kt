package com.github.blackhole1.ideaspellcheck.utils.parse

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import java.io.File

@Serializable
data class YamlCSpellFormat(
    val words: List<String> = emptyList(),
    val dictionaryDefinitions: List<DictionaryDefinition> = emptyList(),
    val dictionaries: List<String> = emptyList()
)

private val yaml = Yaml.Default

fun parseYAML(file: File): ParsedCSpellConfig? {
    return try {
        val raw = file.readText()
        // See: https://github.com/Him188/yamlkt/issues/52
        val content = raw.replace(Regex("^\\$.+:.+$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "")

        val data = yaml.decodeFromString(YamlCSpellFormat.serializer(), content)
        return ParsedCSpellConfig(data.words, data.dictionaryDefinitions, data.dictionaries)
    } catch (e: Exception) {
        null
    }
}
