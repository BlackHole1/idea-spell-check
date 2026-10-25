package com.github.blackhole1.ideaspellcheck.utils.parse

import com.github.blackhole1.ideaspellcheck.utils.NodejsFinder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.nio.file.Files

class ParseJSTest : BasePlatformTestCase() {

    fun testRunCommandReadsOutputLargerThanThePipeBuffer() {
        val node = NodejsFinder.findFirstNodejsExecutable()
        if (node == null) {
            println("Skipping ${name}: Node.js is not installed")
            return
        }

        val tempDir = Files.createTempDirectory("cspell-js-large-output").toFile()
        try {
            // 200 KB is well over the 64 KB a pipe buffers before the writer blocks
            val output = runCommand(
                node, "-e", "process.stdout.write(process.argv[1].repeat(20000))", "it's \"0123\"",
                workingDir = tempDir
            )

            assertEquals("it's \"0123\"".repeat(20000), output)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    fun testRunCommandFailsWhenTheProcessCannotStart() {
        try {
            runCommand(File("missing-node-executable").absolutePath, "-e", "", workingDir = File("."))
            fail("Expected CommandFailedException")
        } catch (_: CommandFailedException) {
        }
    }

    fun testRunCommandFailsWithStderrOnNonZeroExit() {
        val node = NodejsFinder.findFirstNodejsExecutable()
        if (node == null) {
            println("Skipping ${name}: Node.js is not installed")
            return
        }

        try {
            runCommand(node, "-e", "console.error('broken config'); process.exitCode = 1", workingDir = File("."))
            fail("Expected CommandFailedException")
        } catch (e: CommandFailedException) {
            assertEquals("broken config", e.message)
        }
    }
}
