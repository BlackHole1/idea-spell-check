package com.github.blackhole1.ideaspellcheck.services

import com.github.blackhole1.ideaspellcheck.replaceWords
import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.github.blackhole1.ideaspellcheck.utils.findCSpellConfigFile
import com.github.blackhole1.ideaspellcheck.utils.parseCSpellConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.*
import java.io.File

class SCProjectService(project: Project) {
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        watch(project)
    }

    private fun watch(project: Project) {
        scope.launch {
            while (isActive) {
                if (project.isDisposed) {
                    scope.cancel()
                    break
                }

                getWordsFromRoot(project)
                getWordsFromCustomPaths(project)

                delay(800L)
            }
        }
    }

    private fun getWordsFromRoot(project: Project) {
        project.guessProjectDir()?.let { projectDir ->
            findCSpellConfigFile(projectDir.path)?.let { configFile ->
                parseCSpellConfig(configFile, project)?.let { words ->
                    replaceWords(words)
                }
            }
        }
    }

    private fun getWordsFromCustomPaths(project: Project) {
        val settings = SCProjectSettings.instance(project)
        for (customPath in settings.state.customSearchPaths) {
            if (File(customPath).isDirectory) {
                findCSpellConfigFile(customPath)?.let { configFile ->
                    parseCSpellConfig(configFile, project)?.let { words ->
                        replaceWords(words)
                    }
                }
            }
        }
    }
}
