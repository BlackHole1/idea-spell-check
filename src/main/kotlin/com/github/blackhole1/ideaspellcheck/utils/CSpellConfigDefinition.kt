package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

/**
 * Unified CSpell configuration file definitions
 * Manages all supported file names and their priorities in one place
 */
object CSpellConfigDefinition {

    private const val PACKAGE_JSON: String = "package.json"

    data class ConfigFileInfo(val fileName: String) {
        val topDirectory: String? = fileName.substringBefore('/', "").takeIf { fileName.contains('/') }

        fun getFullPath(basePath: String): String {
            val normalizedPath = fileName.replace('/', File.separatorChar)
            return "$basePath${File.separator}$normalizedPath"
        }

        fun requiresParentRoot(): Boolean = topDirectory != null
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

    private val configFileMap = configFiles.associateBy { it.fileName }
    private val nestedConfigDirectories: Set<String> =
        configFiles.mapNotNull { it.topDirectory }.toSet()

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
        if (file.isDirectory) return false

        val relativePath = computeRelativePath(file)
        return configFileMap.containsKey(relativePath)
    }

    /**
     * Check if a path string represents a CSpell configuration file
     */
    fun isConfigFilePath(filePath: String): Boolean {
        val relativePath = computeRelativePath(filePath) ?: return false
        return configFileMap.containsKey(relativePath)
    }

    /**
     * Get priority of a config file (lower number = higher priority)
     */
    private fun getPriority(file: File): Int {
        val relativePath = computeRelativePath(file)
        return configFiles.indexOfFirst { it.fileName == relativePath }.takeIf { it >= 0 } ?: Int.MAX_VALUE
    }

    /**
     * Get relative path from file (handles .vscode and .config directories)
     */
    private fun computeRelativePath(file: File): String {
        return computeRelativePath(file.name, file.parentFile?.name)
    }

    private fun computeRelativePath(fileName: String, parentName: String?): String {
        return when (parentName) {
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
    private fun computeRelativePath(filePath: String): String? {
        val normalizedPath = filePath.replace('\\', '/').trimEnd('/')
        if (normalizedPath.isEmpty()) return null

        val separatorIndex = normalizedPath.lastIndexOf('/')
        val fileName: String
        val parentName: String?

        if (separatorIndex >= 0) {
            fileName = normalizedPath.substring(separatorIndex + 1)
            val parentPath = normalizedPath.substring(0, separatorIndex)
            parentName = parentPath.substringAfterLast('/', parentPath).takeIf { parentPath.isNotEmpty() }
        } else {
            fileName = normalizedPath
            parentName = null
        }

        return computeRelativePath(fileName, parentName)
    }

    /**
     * Return the directory that should act as the search root for the given config file path
     */
    fun getSearchRootDirectory(file: File): File? {
        val parent = file.parentFile ?: return null
        if (!isConfigFile(file)) return parent

        val relativePath = computeRelativePath(file)
        val info = configFileMap[relativePath]
        return if (info?.requiresParentRoot() == true) {
            parent.parentFile ?: parent
        } else {
            parent
        }
    }

    /**
     * Normalize the directory that contains config files so it matches configuration expectations
     */
    fun normalizeContainingDirectory(directory: File): File {
        if (directory.name in nestedConfigDirectories) {
            directory.parentFile?.let { return it }
        }
        return directory
    }

    fun hasHigherPriority(fileA: File, fileB: File): Boolean {
        return getPriority(fileA) < getPriority(fileB)
    }

    /**
     * Get all supported config file names (for quick lookup)
     */
    fun getAllFileNames(): Set<String> = configFiles.map { it.fileName }.toSet()
}
