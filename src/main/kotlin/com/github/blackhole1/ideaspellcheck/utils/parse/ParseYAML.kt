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
        val data = Yaml.decodeFromString(Words.serializer(), file.readText())
        return data.words
    } catch (e: Exception) {
        null
    }
}
