package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

/**
 * Unified CSpell configuration file definitions
 * Manages all supported file names and their priorities in one place
 */
object CSpellConfigDefinition {

    private const val PACKAGE_JSON: String = "package.json"

    data class ConfigFileInfo(val fileName: String) {
        fun getFullPath(basePath: String): String {
            val normalizedPath = fileName.replace('/', File.separatorChar)
            return "$basePath${File.separator}$normalizedPath"
        }
    }

    private val configFiles = listOf(
        ConfigFileInfo(".cspell.json"),
        ConfigFileInfo("cspell.json"),
        ConfigFileInfo(".cSpell.json"),
        ConfigFileInfo("cSpell.json"),
        ConfigFileInfo(".cspell.jsonc"),
        ConfigFileInfo("cspell.jsonc"),
        ConfigFileInfo(".cspell.yaml"),
        ConfigFileInfo("cspell.yaml"),
        ConfigFileInfo(".cspell.yml"),
        ConfigFileInfo("cspell.yml"),
        ConfigFileInfo(".cspell.config.json"),
        ConfigFileInfo("cspell.config.json"),
        ConfigFileInfo(".cspell.config.jsonc"),
        ConfigFileInfo("cspell.config.jsonc"),
        ConfigFileInfo(".cspell.config.yaml"),
        ConfigFileInfo("cspell.config.yaml"),
        ConfigFileInfo(".cspell.config.yml"),
        ConfigFileInfo("cspell.config.yml"),
        ConfigFileInfo(".cspell.config.mjs"),
        ConfigFileInfo("cspell.config.mjs"),
        ConfigFileInfo(".cspell.config.cjs"),
        ConfigFileInfo("cspell.config.cjs"),
        ConfigFileInfo(".cspell.config.js"),
        ConfigFileInfo("cspell.config.js"),
        ConfigFileInfo(".cspell.config.toml"),
        ConfigFileInfo("cspell.config.toml"),
        ConfigFileInfo(".config/.cspell.json"),
        ConfigFileInfo(".config/cspell.json"),
        ConfigFileInfo(".config/.cSpell.json"),
        ConfigFileInfo(".config/cSpell.json"),
        ConfigFileInfo(".config/.cspell.jsonc"),
        ConfigFileInfo(".config/cspell.jsonc"),
        ConfigFileInfo(".config/cspell.yaml"),
        ConfigFileInfo(".config/cspell.yml"),
        ConfigFileInfo(".config/.cspell.config.json"),
        ConfigFileInfo(".config/cspell.config.json"),
        ConfigFileInfo(".config/.cspell.config.jsonc"),
        ConfigFileInfo(".config/cspell.config.jsonc"),
        ConfigFileInfo(".config/.cspell.config.yaml"),
        ConfigFileInfo(".config/cspell.config.yaml"),
        ConfigFileInfo(".config/.cspell.config.yml"),
        ConfigFileInfo(".config/cspell.config.yml"),
        ConfigFileInfo(".config/.cspell.config.mjs"),
        ConfigFileInfo(".config/cspell.config.mjs"),
        ConfigFileInfo(".config/.cspell.config.cjs"),
        ConfigFileInfo(".config/cspell.config.cjs"),
        ConfigFileInfo(".config/.cspell.config.js"),
        ConfigFileInfo(".config/cspell.config.js"),
        ConfigFileInfo("config/.cspell.config.toml"),
        ConfigFileInfo("config/cspell.config.toml"),
        ConfigFileInfo(".vscode/.cspell.json"),
        ConfigFileInfo(".vscode/cSpell.json"),
        ConfigFileInfo(".vscode/cspell.json"),
        ConfigFileInfo(PACKAGE_JSON)
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

        val relativePath = getRelativePath(file)
        return configFiles.any { it.fileName == relativePath }
    }

    /**
     * Get priority of a config file (lower number = higher priority)
     */
    private fun getPriority(file: File): Int {
        val relativePath = getRelativePath(file)
        return configFiles.indexOfFirst { it.fileName == relativePath }.takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    /**
     * Get relative path from file (handles .vscode and .config directories)
     */
    private fun getRelativePath(file: File): String {
        val fileName = file.name
        val parentDir = file.parentFile ?: return fileName

        return when (parentDir.name) {
            ".vscode" -> ".vscode/$fileName"
            ".config" -> ".config/$fileName"
            "config" -> {
                // Check if this is config/.cspell.config.toml pattern
                if (fileName.startsWith(".cspell.config.toml") || fileName == "cspell.config.toml") {
                    "config/$fileName"
                } else {
                    fileName
                }
            }

            else -> fileName
        }
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
