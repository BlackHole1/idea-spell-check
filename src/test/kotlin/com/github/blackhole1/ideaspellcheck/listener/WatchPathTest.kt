package com.github.blackhole1.ideaspellcheck.listener

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPathTest {

    @Test
    fun `matches a path only within the watched directory`() {
        assertTrue(isPathUnderWatchRoot("/workspace", "/workspace"))
        assertTrue(isPathUnderWatchRoot("/workspace/config/cspell.json", "/workspace"))
        assertFalse(isPathUnderWatchRoot("/workspace-other/cspell.json", "/workspace"))
    }

    @Test
    fun `filesystem root includes absolute paths`() {
        assertTrue(isPathUnderWatchRoot("/", "/"))
        assertTrue(isPathUnderWatchRoot("/workspace/cspell.json", "/"))
        assertFalse(isPathUnderWatchRoot("relative/cspell.json", "/"))
    }
}
