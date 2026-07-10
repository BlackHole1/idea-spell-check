package com.github.blackhole1.ideaspellcheck.listener

import com.github.blackhole1.ideaspellcheck.replaceWords
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
            replaceWords(emptySet())
        } finally {
            super.tearDown()
        }
    }

    fun testRecognizesConfigOnlyInsideProjectWatchRoot() {
        val watchedParent = Files.createTempDirectory("cspell-watch-root-parent").toFile()
        val watchedDirectory = File(watchedParent, "created-later")
        val outsideDirectory = Files.createTempDirectory("cspell-outside-watch-root").toFile()
        try {
            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(watchedDirectory.absolutePath))
            runBlocking { manager.initialize() }
            assertTrue(watchedDirectory.mkdirs())

            assertTrue(manager.isConfigFileUnderWatch(File(watchedDirectory, "cspell.json").path))
            assertFalse(manager.isConfigFileUnderWatch(File(outsideDirectory, "cspell.json").path))
        } finally {
            watchedParent.deleteRecursively()
            outsideDirectory.deleteRecursively()
        }
    }

    fun testInitializeRefreshesCustomAndDictionaryWatchPaths() {
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
            val dictionaryPath = toSystemIndependentName(dictionaryDirectory.canonicalPath)
            assertFalse(manager.getAllWatchPaths().contains(customPath))

            SCProjectSettings.instance(project).setCustomSearchPaths(listOf(customDirectory.absolutePath))
            runBlocking { manager.initialize() }

            val watchPaths = manager.getAllWatchPaths()
            assertTrue("Expected custom path in $watchPaths", watchPaths.contains(customPath))
            assertTrue("Expected dictionary path in $watchPaths", watchPaths.contains(dictionaryPath))
        } finally {
            customDirectory.deleteRecursively()
            dictionaryParent.deleteRecursively()
        }
    }
}
