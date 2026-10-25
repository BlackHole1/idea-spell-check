package com.github.blackhole1.ideaspellcheck

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ProjectWordsTest : BasePlatformTestCase() {

    override fun tearDown() {
        try {
            clearWords(project)
            replaceWords(emptySet())
        } finally {
            super.tearDown()
        }
    }

    fun testWordsOfDifferentOwnersDoNotReplaceEachOther() {
        replaceWords(listOf("alpha"), project)
        replaceWords(listOf("beta"))

        assertTrue(containsWord("alpha"))
        assertTrue(containsWord("beta"))
        assertEquals(setOf("alpha", "beta"), getWords())

        replaceWords(listOf("gamma"), project)

        assertFalse(containsWord("alpha"))
        assertEquals(setOf("beta", "gamma"), getWords())
    }

    fun testClearWordsRemovesOnlyThatProject() {
        replaceWords(listOf("alpha"), project)
        replaceWords(listOf("beta"))

        clearWords(project)

        assertEquals(setOf("beta"), getWords())
    }
}
