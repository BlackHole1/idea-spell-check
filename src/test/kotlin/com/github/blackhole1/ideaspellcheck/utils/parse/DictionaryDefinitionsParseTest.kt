package com.github.blackhole1.ideaspellcheck.utils.parse

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DictionaryDefinitionsParseTest {

    @Test
    fun `parseJSON merges dictionary definitions`() {
        val tempDir = Files.createTempDirectory("cspell-json-test").toFile()
        try {
            val dictionary = File(tempDir, "project-words.txt")
            dictionary.writeText("foo\nbar\n\n baz \n")

            val config = File(tempDir, "cspell.json")
            config.writeText(
                """
                {
                  "words": ["alpha"],
                  "dictionaryDefinitions": [
                    { "path": "./${dictionary.name}", "addWords": true },
                    { "path": "ignored.txt", "addWords": false }
                  ]
                }
                """.trimIndent()
            )

            val parsed = parseJSON(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("alpha", "foo", "bar", "baz"), result.words.toSet())
            assertTrue(result.dictionaryPaths.any { it.endsWith("project-words.txt") })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `parse packageJSON merges dictionary definitions`() {
        val tempDir = Files.createTempDirectory("cspell-package-json-test").toFile()
        try {
            val dictionary = File(tempDir, "extra.txt")
            dictionary.writeText("delta\n")

            val config = File(tempDir, "package.json")
            config.writeText(
                """
                {
                  "cspell": {
                    "words": ["gamma"],
                    "dictionaryDefinitions": [
                      { "path": "${dictionary.absolutePath.replace("\\", "\\\\")}", "addWords": true }
                    ]
                  }
                }
                """.trimIndent()
            )

            val parsed = parseJSON(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("gamma", "delta"), result.words.toSet())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `dictionaries array activates named dictionary regardless of addWords`() {
        val tempDir = Files.createTempDirectory("cspell-dictionaries-activation-test").toFile()
        try {
            val dictionary = File(tempDir, "custom-words.txt")
            dictionary.writeText("activated\n")

            val config = File(tempDir, "cspell.json")
            config.writeText(
                """
                {
                  "dictionaryDefinitions": [
                    { "name": "project-words", "path": "./${dictionary.name}", "addWords": false }
                  ],
                  "dictionaries": ["project-words"]
                }
                """.trimIndent()
            )

            val parsed = parseJSON(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("activated"), result.words.toSet())
            assertTrue(result.dictionaryPaths.any { it.endsWith("custom-words.txt") })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `dictionaries without matching definitions are ignored`() {
        val tempDir = Files.createTempDirectory("cspell-dictionaries-missing-test").toFile()
        try {
            val config = File(tempDir, "cspell.json")
            config.writeText(
                """
                {
                  "words": ["base"],
                  "dictionaries": ["missing-dict"]
                }
                """.trimIndent()
            )

            val parsed = parseJSON(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("base"), result.words.toSet())
            assertTrue(result.dictionaryPaths.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `parseYAML merges dictionary definitions`() {
        val tempDir = Files.createTempDirectory("cspell-yaml-test").toFile()
        try {
            val dictionary = File(tempDir, "words.txt")
            dictionary.writeText("one\n\ntwo\n")

            val config = File(tempDir, "cspell.yaml")
            config.writeText(
                """
                words:
                  - zero
                dictionaryDefinitions:
                  - name: project-words
                    path: ./words.txt
                    addWords: true
                """.trimIndent()
            )

            val parsed = parseYAML(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("zero", "one", "two"), result.words.toSet())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `missing dictionary file is still tracked`() {
        val tempDir = Files.createTempDirectory("cspell-missing-dict-test").toFile()
        try {
            val config = File(tempDir, "cspell.json")
            config.writeText(
                """
                {
                  "words": ["base"],
                  "dictionaryDefinitions": [
                    { "path": "./missing.txt", "addWords": true }
                  ]
                }
                """.trimIndent()
            )

            val parsed = parseJSON(config)
            assertNotNull(parsed)
            val result = mergeWordsWithDictionaryDefinitions(
                parsed!!.words,
                parsed.dictionaryDefinitions,
                parsed.dictionaries,
                config
            )
            assertEquals(setOf("base"), result.words.toSet())
            assertTrue(result.dictionaryPaths.any { it.endsWith("missing.txt") })
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
