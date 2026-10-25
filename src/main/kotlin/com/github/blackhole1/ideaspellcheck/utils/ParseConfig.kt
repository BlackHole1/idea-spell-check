package com.github.blackhole1.ideaspellcheck.utils

import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.github.blackhole1.ideaspellcheck.utils.parse.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

private val logger = Logger.getInstance("CSpell.ParseConfig")

private fun getAvailableNodeExecutable(settings: SCProjectSettings): String? {
    // Priority 1: Use user-configured path if valid
    settings.state.nodeExecutablePath?.takeIf { it.isNotBlank() }?.let { configuredPath ->
        val configuredFile = File(configuredPath)
        if (configuredFile.exists() && configuredFile.canExecute()) {
            return configuredPath
        }
    }

    // Priority 2: Use system auto-discovered Node.js
    return NodejsFinder.findFirstNodejsExecutable()
}

/**
 * Parse a config file and merge in the words of its dictionary files.
 * Returns null if the file cannot be parsed; parse errors of CSpell config files are reported to the user once per
 * file until it parses successfully again. A broken package.json is only logged: the plugin merely reads its
 * `cspell` field, and the JSON editor already highlights the error.
 */
fun parseCSpellConfig(file: File, project: Project): MergedWordList? {
    val ext = file.extension.lowercase()
    val parsed = try {
        when (ext) {
            "json", "jsonc" -> parseJSON(file)

            "js", "cjs", "mjs" -> {
                val settings = SCProjectSettings.instance(project)
                val nodeExecutable = getAvailableNodeExecutable(settings)
                if (nodeExecutable.isNullOrBlank()) {
                    NotificationManager.showNodeJsConfigurationNotification(project)
                    return null
                }
                parseJS(file, project, nodeExecutable) ?: return null
            }

            "yaml", "yml" -> parseYAML(file)

            "toml" -> parseTOML(file)
            else -> return null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("Failed to parse CSpell config: ${file.path}", e)
        if (file.name != "package.json") {
            NotificationManager.showParseErrorNotification(project, file.path, e.message)
        }
        return null
    }
    NotificationManager.clearParseError(project, file.path)

    return mergeWordsWithDictionaryDefinitions(
        parsed.words,
        parsed.dictionaryDefinitions,
        parsed.dictionaries,
        file
    )
}
