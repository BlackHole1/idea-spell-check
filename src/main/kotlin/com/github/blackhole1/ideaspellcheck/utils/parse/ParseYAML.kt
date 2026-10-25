package com.github.blackhole1.ideaspellcheck.utils.parse

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag
import java.io.File

// YAML scalars are untyped, so accept e.g. a numeric word where a string is expected
private val yamlJson = Json(json) { isLenient = true }

/**
 * Parse a YAML config file with the SnakeYAML library bundled in the IDE, then decode the loaded tree with the
 * same model as the JSON config
 * @throws Exception if the file cannot be read or is not a valid config
 */
fun parseYAML(file: File): ParsedCSpellConfig {
    // Yaml instances are not thread-safe, so create one per parse
    val document = Yaml(TimestampAsStringConstructor()).load<Any?>(file.readText())
        ?: return ParsedCSpellConfig() // empty document

    return yamlJson.decodeFromJsonElement(CSpellWordsFormat.serializer(), document.toJsonElement()).toParsedConfig()
}

/**
 * Keeps timestamp-like scalars (e.g. a word `2024-01-01`) as the literal text instead of converting them to Date
 */
private class TimestampAsStringConstructor : SafeConstructor(LoaderOptions()) {
    init {
        yamlConstructors[Tag.TIMESTAMP] = ConstructYamlStr()
    }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    else -> JsonPrimitive(toString())
}
