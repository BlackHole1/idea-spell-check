package com.github.blackhole1.ideaspellcheck.utils

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files

class NodejsFinderTest {

    @Test
    fun `keeps symlinked executable paths and drops duplicates of the same target`() {
        assumeFalse(System.getProperty("os.name").contains("windows", ignoreCase = true))

        val root = Files.createTempDirectory("nodejs-finder-test").toFile()
        try {
            // Version managers install `node` as a symlink to a shim that dispatches on its invoked name
            val shim = File(root, "shim/vp").apply {
                parentFile.mkdirs()
                writeText("#!/bin/sh\n")
                setExecutable(true)
            }
            val firstBin = File(root, "first").apply { mkdirs() }
            val secondBin = File(root, "second").apply { mkdirs() }
            Files.createSymbolicLink(File(firstBin, "node").toPath(), shim.toPath())
            Files.createSymbolicLink(File(secondBin, "node").toPath(), shim.toPath())

            val found = NodejsFinder.findExecutablesIn(
                listOf(firstBin.path, secondBin.path, File(root, "missing").path, "")
            ).toList()

            assertEquals(listOf(File(firstBin, "node").path), found)
        } finally {
            root.deleteRecursively()
        }
    }
}
