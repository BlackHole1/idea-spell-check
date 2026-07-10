package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

/**
 * Unified CSpell configuration file definitions
 * Manages all supported file names and their priorities in one place
 */
object CSpellConfigDefinition {

    private const val PACKAGE_JSON: String = "package.json"

    private val configFileNames = listOf(
        ".cspell.json",
        "cspell.json",
        ".cSpell.json",
        "cSpell.json",
        ".cspell.jsonc",
        "cspell.jsonc",
        ".cspell.yaml",
        "cspell.yaml",
        ".cspell.yml",
        "cspell.yml",
        ".cspell.config.json",
        "cspell.config.json",
        ".cspell.config.jsonc",
        "cspell.config.jsonc",
        ".cspell.config.yaml",
        "cspell.config.yaml",
        ".cspell.config.yml",
        "cspell.config.yml",
        ".cspell.config.mjs",
        "cspell.config.mjs",
        ".cspell.config.cjs",
        "cspell.config.cjs",
        ".cspell.config.js",
        "cspell.config.js",
        ".cspell.config.toml",
        "cspell.config.toml",
        ".config/.cspell.json",
        ".config/cspell.json",
        ".config/.cSpell.json",
        ".config/cSpell.json",
        ".config/.cspell.jsonc",
        ".config/cspell.jsonc",
        ".config/cspell.yaml",
        ".config/cspell.yml",
        ".config/.cspell.config.json",
        ".config/cspell.config.json",
        ".config/.cspell.config.jsonc",
        ".config/cspell.config.jsonc",
        ".config/.cspell.config.yaml",
        ".config/cspell.config.yaml",
        ".config/.cspell.config.yml",
        ".config/cspell.config.yml",
        ".config/.cspell.config.mjs",
        ".config/cspell.config.mjs",
        ".config/.cspell.config.cjs",
        ".config/cspell.config.cjs",
        ".config/.cspell.config.js",
        ".config/cspell.config.js",
        "config/.cspell.config.toml",
        "config/cspell.config.toml",
        ".vscode/.cspell.json",
        ".vscode/cSpell.json",
        ".vscode/cspell.json",
        PACKAGE_JSON
    )

    private val configFileNameSet: Set<String> = configFileNames.toSet()
    private val priorityByFileName: Map<String, Int> =
        configFileNames.withIndex().associate { (index, fileName) -> fileName to index }
    private val nestedConfigDirectories: Set<String> =
        configFileNames.mapNotNull { it.parentDirectoryName() }.toSet()

    /**
     * Get all possible config file paths for a given directory
     */
    fun getAllSearchPaths(basePath: String): List<String> {
        return configFileNames.map { fileName ->
            "$basePath${File.separator}${fileName.replace('/', File.separatorChar)}"
        }
    }

    /**
     * Check if a file is a CSpell configuration file
     */
    fun isConfigFile(file: File): Boolean {
        if (file.isDirectory) return false

        val relativePath = computeRelativePath(file)
        return relativePath in configFileNameSet
    }

    /**
     * Check if a path string represents a CSpell configuration file
     */
    fun isConfigFilePath(filePath: String): Boolean {
        val relativePath = computeRelativePath(filePath) ?: return false
        return relativePath in configFileNameSet
    }

    /**
     * Get priority of a config file (lower number = higher priority)
     */
    private fun getPriority(file: File): Int {
        val relativePath = computeRelativePath(file)
        return priorityByFileName[relativePath] ?: Int.MAX_VALUE
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
        return if (relativePath.parentDirectoryName() != null) {
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
    fun getAllFileNames(): Set<String> = configFileNameSet

    private fun String.parentDirectoryName(): String? =
        substringBefore('/', "").takeIf { contains('/') }
}
