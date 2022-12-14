package com.github.blackhole1.ideaspellcheck.utils.parse

import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun runCommand(vararg arguments: String, workingDir: File): String? {
    return try {
        val proc = ProcessBuilder(*arguments)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(5, TimeUnit.SECONDS)
        proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        null
    }
}

private val interpreter = NodeJsLocalInterpreterManager.getInstance().detectMostRelevant()

fun parseJS(file: File, project: Project): List<String>? {
    val exePath = interpreter?.interpreterSystemDependentPath ?: return null
    val cwd = project.guessProjectDir()?.path ?: return null

    try {
        runCommand(exePath, "-e", "const c = require('${file.path}'); console.log(c.words.join('-&&-'))", workingDir = File(cwd))?.let {
            return it.trim().split("-&&-")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    return null
}
