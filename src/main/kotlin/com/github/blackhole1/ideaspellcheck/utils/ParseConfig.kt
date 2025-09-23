package com.github.blackhole1.ideaspellcheck.utils

import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.github.blackhole1.ideaspellcheck.utils.parse.MergedWordList
import com.github.blackhole1.ideaspellcheck.utils.parse.mergeWordsWithDictionaryDefinitions
import com.github.blackhole1.ideaspellcheck.utils.parse.parseJS
import com.github.blackhole1.ideaspellcheck.utils.parse.parseJSON
import com.github.blackhole1.ideaspellcheck.utils.parse.parseYAML
import com.intellij.openapi.project.Project
import java.io.File

private fun getAvailableNodeExecutable(settings: SCProjectSettings): String? {
    // Priority 1: Use user-configured path if valid
    settings.state.nodeExecutablePath?.takeIf { it.isNotBlank() }?.let { configuredPath ->
        if (File(configuredPath).exists() && File(configuredPath).canExecute()) {
            return configuredPath
        }
    }

    // Priority 2: Use system auto-discovered Node.js
    return NodejsFinder.findNodejsExecutables().firstOrNull()
}

fun parseCSpellConfig(file: File, project: Project): MergedWordList? {
    val parsed = when (file.extension) {
        "json" -> parseJSON(file)

        "js", "cjs" -> {
            val settings = SCProjectSettings.instance(project)
            val nodeExecutable = getAvailableNodeExecutable(settings)
            if (nodeExecutable == null) {
                NotificationManager.showNodeJsConfigurationNotification(project)
                return null
            }
            parseJS(file, project)
        }

        "yaml", "yml" -> parseYAML(file)
        else -> null
    } ?: return null

    return mergeWordsWithDictionaryDefinitions(parsed.words, parsed.dictionaryDefinitions, parsed.dictionaries, file, project)
}
