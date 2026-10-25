package com.github.blackhole1.ideaspellcheck

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordsTest {

    @After
    fun clearWords() {
        replaceWords(emptySet())
    }

    @Test
    fun `replaceWords keeps set semantics for all collection types`() {
        replaceWords(listOf("alpha", "alpha", "beta"))
        assertEquals(setOf("alpha", "beta"), getWords())

        replaceWords(linkedSetOf("gamma", "delta"))
        assertEquals(setOf("gamma", "delta"), getWords())
    }

    @Test
    fun `replaceWords does not retain the source collection`() {
        val source = mutableSetOf("alpha")
        replaceWords(source)

        source.add("beta")

        assertEquals(setOf("alpha"), getWords())
    }

    @Test
    fun `containsWord reflects the latest replaced words`() {
        replaceWords(listOf("alpha"))
        assertTrue(containsWord("alpha"))
        assertFalse(containsWord("beta"))

        replaceWords(listOf("beta"))
        assertFalse(containsWord("alpha"))
        assertTrue(containsWord("beta"))
    }
}
