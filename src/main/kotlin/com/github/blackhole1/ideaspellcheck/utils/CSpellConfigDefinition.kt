package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

/**
 * Unified CSpell configuration file definitions
 * Manages all supported file names and their priorities in one place
 */
object CSpellConfigDefinition {

    private const val PACKAGE_JSON: String = "package.json"

    data class ConfigFileInfo(
        val fileName: String,
        val isVSCodePath: Boolean = false,
        val priority: Int
    ) {
        fun getFullPath(basePath: String): String {
            return if (isVSCodePath) {
                "$basePath${File.separator}.vscode${File.separator}$fileName"
            } else {
                "$basePath${File.separator}$fileName"
            }
        }
    }

    private val configFiles = listOf(
        ConfigFileInfo(".cspell.json", priority = 0),
        ConfigFileInfo("cspell.json", priority = 1),
        ConfigFileInfo(".cSpell.json", priority = 2),
        ConfigFileInfo("cSpell.json", priority = 3),
        ConfigFileInfo("cspell.config.js", priority = 4),
        ConfigFileInfo("cspell.config.cjs", priority = 5),
        ConfigFileInfo("cspell.json", isVSCodePath = true, priority = 6),
        ConfigFileInfo("cSpell.json", isVSCodePath = true, priority = 7),
        ConfigFileInfo(".cspell.json", isVSCodePath = true, priority = 8),
        ConfigFileInfo("cspell.config.json", priority = 9),
        ConfigFileInfo("cspell.config.yaml", priority = 10),
        ConfigFileInfo("cspell.config.yml", priority = 11),
        ConfigFileInfo("cspell.yaml", priority = 12),
        ConfigFileInfo("cspell.yml", priority = 13),
        ConfigFileInfo(PACKAGE_JSON, priority = 14)
    )

    /**
     * Get all possible config file paths for a given directory
     */
    fun getAllSearchPaths(basePath: String): List<String> {
        return configFiles.map { it.getFullPath(basePath) }
    }

    /**
     * Check if a file is a CSpell configuration file
     */
    fun isConfigFile(file: File): Boolean {
        if (!file.isFile) return false

        val parentDir = file.parent ?: return false
        val isInVSCode = File(parentDir).name == ".vscode"

        return configFiles.any { config ->
            config.fileName == file.name && config.isVSCodePath == isInVSCode
        } || (file.name == PACKAGE_JSON) // package.json is always considered
    }

    /**
     * Get priority of a config file (lower number = higher priority)
     */
    private fun getPriority(file: File): Int {
        val parentDir = file.parent ?: return Int.MAX_VALUE
        val isInVSCode = File(parentDir).name == ".vscode"

        if (file.name == PACKAGE_JSON) {
            return configFiles.find { it.fileName == PACKAGE_JSON }?.priority ?: Int.MAX_VALUE
        }

        return configFiles.find { config ->
            config.fileName == file.name && config.isVSCodePath == isInVSCode
        }?.priority ?: Int.MAX_VALUE
    }

    /**
     * Compare two files by priority (for sorting)
     */
    fun hasHigherPriority(fileA: File, fileB: File): Boolean {
        return getPriority(fileA) < getPriority(fileB)
    }

    /**
     * Get all supported config file names (for quick lookup)
     */
    fun getAllFileNames(): Set<String> = configFiles.map { it.fileName }.toSet()
}
