package com.github.blackhole1.ideaspellcheck.settings

import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SCProjectConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: SCProjectConfigurable

    override fun setUp() {
        super.setUp()
        configurable = SCProjectConfigurable(project)
        configurable.createComponent()
    }

    override fun tearDown() {
        try {
            configurable.disposeUIResources()
            SCProjectSettings.instance(project).setCustomSearchPaths(emptyList())
            SCProjectSettings.instance(project).setNodeExecutablePath(null)
        } finally {
            super.tearDown()
        }
    }

    fun testResetLoadsSavedSettings() {
        val settings = SCProjectSettings.instance(project)
        settings.setCustomSearchPaths(listOf("/workspace/a", "/workspace/b"))
        settings.setNodeExecutablePath("/opt/node/bin/node")

        configurable.reset()

        assertFalse(configurable.isModified)
    }

    fun testApplyRejectsInvalidNodeExecutable() {
        SCProjectSettings.instance(project).setNodeExecutablePath("/missing/node")
        configurable.reset()

        try {
            configurable.apply()
            fail("Expected ConfigurationException")
        } catch (_: ConfigurationException) {
        }
    }
}
