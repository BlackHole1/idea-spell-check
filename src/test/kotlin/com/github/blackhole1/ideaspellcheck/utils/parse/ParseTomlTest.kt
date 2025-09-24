package com.github.blackhole1.ideaspellcheck.utils.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ParseTomlTest {

    @Test
    fun `parse toml should read core fields`() {
        val tempDir = Files.createTempDirectory("cspell-toml-parse").toFile()
        try {
            val configFile = File(tempDir, "cspell.config.toml")
            configFile.writeText(
                """
                words = ["alpha", "beta"]
                dictionaries = ["custom"]

                [[dictionaryDefinitions]]
                name = "custom"
                path = "dict/custom.txt"
                addWords = true
                """.trimIndent()
            )

            val parsed = parseTOML(configFile)

            assertNotNull("Expected TOML config to parse", parsed)
            parsed!!
            assertEquals(listOf("alpha", "beta"), parsed.words)
            assertEquals(listOf("custom"), parsed.dictionaries)
            assertEquals(1, parsed.dictionaryDefinitions.size)
            val definition = parsed.dictionaryDefinitions.first()
            assertEquals("custom", definition.name)
            assertEquals("dict/custom.txt", definition.path)
            assertTrue(definition.addWords == true)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `mergeWordsWithDictionaryDefinitions should merge toml dictionary`() {
        val tempDir = Files.createTempDirectory("cspell-toml-merge").toFile()
        try {
            val dictionariesDir = File(tempDir, "dict").apply { mkdirs() }
            val dictionaryFile = File(dictionariesDir, "custom.txt").apply {
                writeText(
                    """
                    gamma
                    # comment
                    delta
                    
                    epsilon
                    """.trimIndent()
                )
            }

            val configFile = File(tempDir, "cspell.config.toml")
            configFile.writeText(
                """
                words = ["alpha"]
                dictionaries = ["custom"]

                [[dictionaryDefinitions]]
                name = "custom"
                path = "dict/custom.txt"
                """.trimIndent()
            )

            val parsed = parseTOML(configFile)
            assertNotNull("Expected TOML config to parse", parsed)
            parsed!!

            val merged = mergeWordsWithDictionaryDefinitions(
                parsed.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                configFile
            )

            assertTrue(merged.words.containsAll(listOf("alpha", "gamma", "delta", "epsilon")))
            assertTrue(merged.dictionaryPaths.contains(dictionaryFile.absolutePath))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
