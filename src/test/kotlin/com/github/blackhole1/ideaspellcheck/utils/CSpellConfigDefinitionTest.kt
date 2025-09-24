package com.github.blackhole1.ideaspellcheck.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
