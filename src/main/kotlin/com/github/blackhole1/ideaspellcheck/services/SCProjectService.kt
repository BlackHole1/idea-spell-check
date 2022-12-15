package com.github.blackhole1.ideaspellcheck.services

import com.github.blackhole1.ideaspellcheck.replaceWords
import com.github.blackhole1.ideaspellcheck.utils.findCSpellConfigFile
import com.github.blackhole1.ideaspellcheck.utils.parseCSpellConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import kotlinx.coroutines.*

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

                project.guessProjectDir()?.let { it1 ->
                    findCSpellConfigFile(it1.path)?.let { it2 ->
                        parseCSpellConfig(it2, project)?.let { it3 ->
                            replaceWords(it3)
                        }
                    }
                }

                delay(800L)
            }
        }
    }
}
