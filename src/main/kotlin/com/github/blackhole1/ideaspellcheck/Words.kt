package com.github.blackhole1.ideaspellcheck

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.project.Project

/**
 * Immutable snapshot of the current words. Readers (e.g. dictionary lookups) access it without locking or copying;
 * writers replace the whole snapshot.
 */
@Volatile
private var words: Set<String> = emptySet()
private val wordsLock = Any()

internal fun containsWord(word: String): Boolean = words.contains(word)

fun getWords(): MutableSet<String> = words.toMutableSet()

fun replaceWords(w: Collection<String>, project: Project? = null) {
    val changed = synchronized(wordsLock) {
        val newWords = w.toSet()
        if (words == newWords) false else {
            words = newWords
            true
        }
    }
    if (project != null && changed) {
        DaemonCodeAnalyzer.getInstance(project).restart("CSpell words changed")
    }
}
