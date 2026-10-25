package com.github.blackhole1.ideaspellcheck.utils

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.nio.file.Files

class ParseConfigTest : BasePlatformTestCase() {

    private val notifications = mutableListOf<Notification>()
    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = Files.createTempDirectory("cspell-parse-config").toFile()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == "CSpell Check") notifications.add(notification)
            }
        })
    }

    override fun tearDown() {
        try {
            NotificationManager.clear(project)
            tempDir.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testReportsEachBrokenConfigOnceUntilItParsesAgain() {
        val config = File(tempDir, "cspell.json").apply { writeText("""{ "words": [ """) }

        assertNull(parseCSpellConfig(config, project))
        assertNull(parseCSpellConfig(config, project))
        assertEquals(1, notifications.size)
        assertTrue(notifications.single().content.contains(config.path))

        config.writeText("""{ "words": ["alpha"] }""")
        assertEquals(listOf("alpha"), parseCSpellConfig(config, project)?.words)

        config.writeText("""{ "words": [ """)
        assertNull(parseCSpellConfig(config, project))
        assertEquals(2, notifications.size)
    }

    fun testReportsBrokenConfigsOfEveryFormat() {
        listOf(
            File(tempDir, "cspell.yaml") to "words: [unclosed",
            File(tempDir, "cspell.config.toml") to "words = [",
        ).forEach { (file, content) ->
            file.writeText(content)
            assertNull("Expected ${file.name} to fail", parseCSpellConfig(file, project))
        }

        assertEquals(2, notifications.size)
    }

    fun testOnlyLogsBrokenPackageJson() {
        val packageJson = File(tempDir, "package.json").apply { writeText("{ ") }

        assertNull(parseCSpellConfig(packageJson, project))
        assertEquals(0, notifications.size)
    }
}
