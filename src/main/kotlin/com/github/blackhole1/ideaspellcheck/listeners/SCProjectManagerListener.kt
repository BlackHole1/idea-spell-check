package com.github.blackhole1.ideaspellcheck.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.github.blackhole1.ideaspellcheck.services.SCProjectService

internal class SCProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        project.service<SCProjectService>()
    }
}
