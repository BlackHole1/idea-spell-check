package com.github.blackhole1.ideaspellcheck.startup

import com.github.blackhole1.ideaspellcheck.services.SCProjectService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class SCProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<SCProjectService>()
    }
}
