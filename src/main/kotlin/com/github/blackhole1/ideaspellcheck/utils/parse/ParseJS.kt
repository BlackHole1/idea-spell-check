package com.github.blackhole1.ideaspellcheck.utils.parse

import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.github.blackhole1.ideaspellcheck.utils.NotificationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = Logger.getInstance("CSpell.ParseJS")

fun runCommand(vararg arguments: String, workingDir: File): String? {
    return try {
        val proc = ProcessBuilder(*arguments)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(5, TimeUnit.SECONDS)
        proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        logger.warn("Failed to run Node.js command: ${arguments.joinToString(" ")} in $workingDir", e)
        null
    }
}

fun parseJS(file: File, project: Project): List<String>? {
    val settings = SCProjectSettings.instance(project)
    val nodeExecutablePath = settings.state.nodeExecutablePath

    if (nodeExecutablePath.isNullOrBlank()) {
        // Node.js path not configured, return null to indicate unavailable
        return null
    }

    val nodeFile = File(nodeExecutablePath)
    if (!nodeFile.exists() || !nodeFile.canExecute()) {
        NotificationManager.showNodeExecutableErrorNotification(project, nodeExecutablePath)
        return null
    }

    val cwd = project.guessProjectDir()?.path
    if (cwd == null) {
        NotificationManager.showProjectDirErrorNotification(project)
        return null
    }

    try {
        val command = "const c = require('${file.path.replace("\\", "\\\\")}'); console.log(c.words.join('-&&-'))"
        runCommand(nodeExecutablePath, "-e", command, workingDir = File(cwd))?.let {
            val result = it.trim()
            if (result.isNotEmpty()) {
                return result.split("-&&-").filter { word -> word.isNotBlank() }
            }
        }
    } catch (e: Exception) {
        NotificationManager.showParseErrorNotification(project, file.path, e.message)
        return null
    }

    return null
}
