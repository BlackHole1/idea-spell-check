package com.github.blackhole1.ideaspellcheck

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.util.text.EditDistance
import kotlin.math.abs

/**
 * Words of each open project (the `null` key holds words replaced without a project).
 * The runtime dictionary is application-wide, so lookups use the union of all projects' words.
 * Guarded by [wordsLock].
 */
private val wordsByProject = HashMap<Project?, Set<String>>()
private val wordsLock = Any()

/**
 * Immutable snapshot of the union of all projects' words. Readers (e.g. dictionary lookups) access it without
 * locking or copying; writers replace the whole snapshot.
 */
@Volatile
private var words: Set<String> = emptySet()

internal fun containsWord(word: String): Boolean = words.contains(word)

fun getWords(): MutableSet<String> = words.toMutableSet()

/**
 * Words within a small edit distance of [word], closest first, offered by the spell checker as typo fixes.
 * Words shorter than 3 characters get no suggestions, as almost any short word would match.
 */
internal fun suggestWords(word: String): List<String> {
    if (word.length < 3) return emptyList()
    val maxDistance = if (word.length <= 4) 1 else 2

    return words.asSequence()
        .filter { abs(it.length - word.length) <= maxDistance && !it.equals(word, ignoreCase = true) }
        .map { it to EditDistance.optimalAlignment(word, it, false, maxDistance) }
        .filter { (_, distance) -> distance <= maxDistance }
        .sortedWith(compareBy({ it.second }, { it.first }))
        .map { it.first }
        .toList()
}

/**
 * Replace the words of [project] and re-highlight the projects using the dictionary if its content changed
 */
fun replaceWords(w: Collection<String>, project: Project? = null) {
    updateWords(w.toSet(), project, restartOwner = true)
}

/**
 * Drop the words of a project that is being closed, without re-highlighting that project
 */
fun clearWords(project: Project) {
    updateWords(emptySet(), project, restartOwner = false)
}

private fun updateWords(newWords: Set<String>, project: Project?, restartOwner: Boolean) {
    val projectsToRestart = synchronized(wordsLock) {
        if ((wordsByProject[project] ?: emptySet<String>()) == newWords) return
        if (newWords.isEmpty()) wordsByProject.remove(project) else wordsByProject[project] = newWords

        val merged = when (wordsByProject.size) {
            0 -> emptySet()
            1 -> wordsByProject.values.first()
            else -> wordsByProject.values.flatMapTo(HashSet()) { it }
        }
        if (merged == words) return
        words = merged

        // Every project sees the shared dictionary, so all of them need to be re-highlighted
        val owners = wordsByProject.keys.filterNotNullTo(LinkedHashSet())
        if (project != null && restartOwner) owners.add(project) else owners.remove(project)
        owners.toList()
    }

    restartHighlighting(projectsToRestart)
}

/**
 * Words are updated from background threads, possibly while another project is closing, so restart highlighting
 * on the EDT and skip projects that were disposed in the meantime
 */
private fun restartHighlighting(projects: List<Project>) {
    if (projects.isEmpty()) return
    ApplicationManager.getApplication().invokeLater({
        projects.filterNot { it.isDisposed }.forEach {
            DaemonCodeAnalyzer.getInstance(it).restart("CSpell words changed")
        }
    }, ModalityState.nonModal())
}
