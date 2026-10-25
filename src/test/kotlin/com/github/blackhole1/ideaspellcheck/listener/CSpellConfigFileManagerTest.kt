package com.github.blackhole1.ideaspellcheck.listener

import com.github.blackhole1.ideaspellcheck.clearWords
import com.github.blackhole1.ideaspellcheck.containsWord
import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

class CSpellConfigFileManagerTest : BasePlatformTestCase() {

    private val manager: CSpellConfigFileManager
        get() = project.getService(CSpellConfigFileManager::class.java)

    override fun tearDown() {
        try {
            SCProjectSettings.instance(project).setCustomSearchPaths(emptyList())
            clearWords(project)
        } finally {
            super.tearDown()
        }
    }

    fun testRecognizesConfigOnlyAtSearchRoots() {
        val watchedParent = Files.createTempDirectory("cspell-watch-root-parent").toFile()
        val watchedDirectory = File(watchedParent, "created-later")
        val outsideDirectory = Files.createTempDirectory("cspell-outside-watch-root").toFile()
        try {
            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(watchedDirectory.absolutePath))
            runBlocking { manager.initialize() }
            assertTrue(watchedDirectory.mkdirs())

            assertTrue(manager.isSearchRootConfigFile(File(watchedDirectory, "cspell.json").path))
            assertTrue(manager.isSearchRootConfigFile(File(watchedDirectory, ".vscode/cspell.json").path))
            assertTrue(manager.isSearchRootConfigFile(File(watchedDirectory, "package.json").path))
            assertFalse(manager.isSearchRootConfigFile(File(watchedDirectory, "README.md").path))
            // Nested configs are ignored by the initial scan, so file events must ignore them as well
            assertFalse(manager.isSearchRootConfigFile(File(watchedDirectory, "packages/app/cspell.json").path))
            assertFalse(manager.isSearchRootConfigFile(File(watchedDirectory, "node_modules/foo/package.json").path))
            assertFalse(manager.isSearchRootConfigFile(File(outsideDirectory, "cspell.json").path))
        } finally {
            watchedParent.deleteRecursively()
            outsideDirectory.deleteRecursively()
        }
    }

    fun testInitializeRefreshesSearchRootsAndDictionaries() {
        val customDirectory = Files.createTempDirectory("cspell-custom-watch-root").toFile()
        val dictionaryParent = Files.createTempDirectory("cspell-dictionary-watch-root-parent").toFile()
        val dictionaryDirectory = File(dictionaryParent, "created-later")
        try {
            val missingDictionary = File(dictionaryDirectory, "missing.txt")
            File(customDirectory, "cspell.json").writeText(
                """
                {
                  "dictionaryDefinitions": [
                    { "path": "${missingDictionary.absolutePath.replace("\\", "\\\\")}", "addWords": true }
                  ]
                }
                """.trimIndent()
            )

            val customPath = toSystemIndependentName(customDirectory.absolutePath)
            assertFalse(manager.getConfigSearchRoots().contains(customPath))

            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(customDirectory.absolutePath))
            runBlocking { manager.initialize() }

            val searchRoots = manager.getConfigSearchRoots()
            assertTrue("Expected custom path in $searchRoots", searchRoots.contains(customPath))
            assertTrue(manager.isDictionaryFile(missingDictionary.path))
        } finally {
            customDirectory.deleteRecursively()
            dictionaryParent.deleteRecursively()
        }
    }

    fun testRecognizesDictionaryFilesOnlyAfterTheyAreReferenced() {
        val customDirectory = Files.createTempDirectory("cspell-dictionary-detection").toFile()
        try {
            val dictionary = File(customDirectory, "words.txt").apply { writeText("alpha\n") }
            val unrelated = File(customDirectory, "other.txt").apply { writeText("beta\n") }
            assertFalse(manager.isDictionaryFile(dictionary.path))

            File(customDirectory, "cspell.json").writeText(
                """{ "dictionaryDefinitions": [ { "path": "words.txt", "addWords": true } ] }"""
            )
            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(customDirectory.absolutePath))
            runBlocking { manager.initialize() }

            assertTrue(manager.isDictionaryFile(dictionary.path))
            assertTrue(manager.isDictionaryFile(dictionary.canonicalPath))
            assertFalse(manager.isDictionaryFile(unrelated.path))
        } finally {
            customDirectory.deleteRecursively()
        }
    }

    fun testFileEventsUpdateWordsOnlyForSearchRootConfigs() {
        val root = Files.createTempDirectory("cspell-events").toFile()
        try {
            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(root.absolutePath))
            runBlocking { manager.initialize() }

            val nested = File(root, "packages/app/cspell.json").apply {
                parentFile.mkdirs()
                writeText("""{ "words": ["nestedword"] }""")
            }
            val config = File(root, "cspell.json").apply { writeText("""{ "words": ["rootword"] }""") }
            manager.onFileCreated(nested.path)
            manager.onFileCreated(config.path)

            waitUntil { containsWord("rootword") }
            assertFalse(containsWord("nestedword"))

            config.writeText("""{ "words": ["changedword"] }""")
            manager.onFileChanged(config.path)
            waitUntil { containsWord("changedword") }
            assertFalse(containsWord("rootword"))

            config.delete()
            manager.onFileDeleted(config.path)
            waitUntil { !containsWord("changedword") }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!condition()) {
            assertTrue("Timed out waiting for condition", System.currentTimeMillis() < deadline)
            Thread.sleep(50)
        }
    }
}
