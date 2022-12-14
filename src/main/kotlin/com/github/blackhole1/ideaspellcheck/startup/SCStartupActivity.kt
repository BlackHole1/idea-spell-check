package com.github.blackhole1.ideaspellcheck.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.github.blackhole1.ideaspellcheck.services.SCProjectService
import com.intellij.openapi.startup.StartupActivity

internal class SCStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        project.service<SCProjectService>()
    }
}
