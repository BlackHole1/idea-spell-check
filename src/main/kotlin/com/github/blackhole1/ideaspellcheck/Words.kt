package com.github.blackhole1.ideaspellcheck

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project

private val words = mutableSetOf<String>()
private val wordsLock = Any()

fun getWords(): MutableSet<String> {
    return synchronized(wordsLock) {
        words.toMutableSet()
    }
}

fun replaceWords(w: List<String>, project: Project? = null) {
    val changed = synchronized(wordsLock) {
        val newSet = w.toSet()
        if (words == newSet) false else {
            words.clear()
            words.addAll(newSet)
            true
        }
    }
    if (project != null && changed) {
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
