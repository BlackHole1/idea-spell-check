package com.github.blackhole1.ideaspellcheck.utils.parse

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import java.io.File

@Serializable
data class Words(
    val words: List<String>
)

fun parseYAML(file: File): List<String>? {
    return try {
        val raw = file.readText()
        // See: https://github.com/Him188/yamlkt/issues/52
        val content = raw.replace(Regex("^\\$.+:.+$", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)), "")

        val data = Yaml.decodeFromString(Words.serializer(), content)
        return data.words
    } catch (e: Exception) {
        null
    }
}
