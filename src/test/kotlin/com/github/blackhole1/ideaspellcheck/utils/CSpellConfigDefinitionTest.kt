package com.github.blackhole1.ideaspellcheck.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CSpellConfigDefinitionTest {

    @Test
    fun `recognizes common config paths`() {
        val paths = listOf(
            "/workspace/.config/cspell.yml",
            "/workspace/.vscode/cSpell.json",
            "/workspace/cspell.config.json",
            "/workspace/packages/module/.config/cspell.config.yaml"
        )

        paths.forEach { path ->
            assertTrue("Expected to recognize config path: $path", CSpellConfigDefinition.isConfigFilePath(path))
        }
    }

    @Test
    fun `recognizes windows style paths`() {
        val path = "C:\\Users\\foo\\project\\.config\\cspell.config.json"
        assertTrue("Expected to recognize config path: $path", CSpellConfigDefinition.isConfigFilePath(path))
    }

    @Test
    fun `filters non config files`() {
        val paths = listOf(
            "/workspace/.config/some-other.yml",
            "/workspace/cspell.unknown",
            "/workspace/.config/"
        )

        paths.forEach { path ->
            assertFalse("Expected to ignore non-config path: $path", CSpellConfigDefinition.isConfigFilePath(path))
        }
    }

    @Test
    fun `preserves config priority order`() {
        val directory = Files.createTempDirectory("cspell-priority-test").toFile()
        try {
            val highestPriority = File(directory, ".cspell.json")
            val lowerPriority = File(directory, "cspell.json")
            val packageJson = File(directory, "package.json")

            assertTrue(CSpellConfigDefinition.hasHigherPriority(highestPriority, lowerPriority))
            assertTrue(CSpellConfigDefinition.hasHigherPriority(lowerPriority, packageJson))
            assertFalse(CSpellConfigDefinition.hasHigherPriority(packageJson, highestPriority))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `resolves nested config directories to project root`() {
        val project = Files.createTempDirectory("cspell-search-root-test").toFile()
        try {
            val nestedDirectories = listOf(".config", ".vscode", "config")
            val configNames = listOf("cspell.json", "cSpell.json", "cspell.config.toml")

            nestedDirectories.zip(configNames).forEach { (directoryName, fileName) ->
                val directory = File(project, directoryName).apply { mkdirs() }
                val config = File(directory, fileName)
                assertEquals(project, CSpellConfigDefinition.getSearchRootDirectory(config))
                assertEquals(project, CSpellConfigDefinition.normalizeContainingDirectory(directory))
            }
        } finally {
            project.deleteRecursively()
        }
    }

    @Test
    fun `builds every supported search path`() {
        val basePath = File("workspace").absolutePath
        val paths = CSpellConfigDefinition.getAllSearchPaths(basePath).toSet()

        assertEquals(CSpellConfigDefinition.getAllFileNames().size, paths.size)
        assertTrue(paths.contains(File(basePath, ".cspell.json").path))
        assertTrue(paths.contains(File(basePath, ".config/cspell.config.js").path))
        assertTrue(paths.contains(File(basePath, ".vscode/cSpell.json").path))
        assertTrue(paths.contains(File(basePath, "config/cspell.config.toml").path))
        assertTrue(paths.contains(File(basePath, "package.json").path))
    }
}
