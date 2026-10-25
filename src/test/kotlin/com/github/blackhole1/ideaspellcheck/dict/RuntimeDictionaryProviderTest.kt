package com.github.blackhole1.ideaspellcheck.dict

import com.github.blackhole1.ideaspellcheck.clearWords
import com.github.blackhole1.ideaspellcheck.replaceWords
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.spellchecker.SpellCheckerManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RuntimeDictionaryProviderTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            clearWords(project)
        } finally {
            super.tearDown()
        }
    }

    fun testProviderIsRegisteredWithTheSpellChecker() {
        val extensionPoint = ExtensionPointName<com.intellij.spellchecker.dictionary.RuntimeDictionaryProvider>(
            "com.intellij.spellchecker.dictionary.runtimeDictionaryProvider"
        )
        assertTrue(extensionPoint.extensionList.any { it is RuntimeDictionaryProvider })
    }

    fun testSpellCheckerAcceptsProjectWords() {
        val word = "zqxjkvword"
        val spellChecker = SpellCheckerManager.getInstance(project)
        assertTrue(spellChecker.hasProblem(word))

        replaceWords(listOf(word), project)

        assertFalse(spellChecker.hasProblem(word))
    }
}
