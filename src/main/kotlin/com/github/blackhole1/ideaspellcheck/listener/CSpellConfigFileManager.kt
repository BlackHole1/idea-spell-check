package com.github.blackhole1.ideaspellcheck.listener

import com.github.blackhole1.ideaspellcheck.settings.SCProjectSettings
import com.github.blackhole1.ideaspellcheck.utils.CSpellConfigDefinition
import com.github.blackhole1.ideaspellcheck.utils.findCSpellConfigFile
import com.github.blackhole1.ideaspellcheck.utils.parseCSpellConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Configuration file priority manager
 * Manages the currently active configuration files and their corresponding words for each search path
 */
@Service(Service.Level.PROJECT)
class CSpellConfigFileManager(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(CSpellConfigFileManager::class.java)

    /**
     * Currently active configuration file for each search path
     * key: absolute path of the search directory
     * value: currently active config file
     */
    private val activeConfigFiles = ConcurrentHashMap<String, File>()

    /**
     * Word list for each configuration file
     * key: absolute path of the config file
     * value: list of words parsed from that file
     */
    private val configFileWords = ConcurrentHashMap<String, List<String>>()

    /**
     * Debounce timer management
     * key: absolute path of the file
     * value: corresponding debounce coroutine Job
     */
    private val debounceTimers = ConcurrentHashMap<String, Job>()

    private val scope = CoroutineScope(Dispatchers.Default)
    private val debounceDelay = 500L

    /**
     * Get all watched paths
     */
    fun getAllWatchPaths(): List<String> {
        val paths = mutableSetOf<String>()

        // Add project root directory
        project.guessProjectDir()?.path?.let { rootPath ->
            val file = File(rootPath)
            if (file.isDirectory) {
                paths.add(toSystemIndependentName(file.absolutePath).trimEnd('/'))
            }
        }

        // Add custom search paths
        val settings = SCProjectSettings.instance(project)
        settings.state.customSearchPaths.forEach { path ->
            val file = File(path)
            if (file.isDirectory) {
                paths.add(toSystemIndependentName(file.absolutePath).trimEnd('/'))
            }
        }

        return paths.toList()
    }

    /**
     * Initialize: scan all paths and set initial active configuration files
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        activeConfigFiles.clear()
        configFileWords.clear()
        val allPaths = getAllWatchPaths()

        for (path in allPaths) {
            val configFile = findCSpellConfigFile(path)
            if (configFile != null) {
                activeConfigFiles[path] = configFile
                loadConfigFile(configFile)
            }
        }

        updateGlobalWordList()
    }

    /**
     * Handle file creation events
     */
    fun onFileCreated(filePath: String) {
        val file = File(filePath)
        if (!CSpellConfigDefinition.isConfigFile(file)) return

        val searchRoot = resolveSearchRoot(file) ?: return
        val currentActiveFile = activeConfigFiles[searchRoot]

        // Check if the new file has higher priority
        if (currentActiveFile == null || CSpellConfigDefinition.hasHigherPriority(file, currentActiveFile)) {
            activeConfigFiles[searchRoot] = file
            debounceParseFile(file)
        }
    }

    /**
     * Handle file modification events
     */
    fun onFileChanged(filePath: String) {
        val file = File(filePath)

        // Check if this is the currently active config file
        if (isActiveConfigFile(file)) {
            debounceParseFile(file)
        } else if (file.name == "package.json") {
            // package.json may have added or removed cspell field, need to re-evaluate
            resolveSearchRoot(file)?.let { reevaluatePriorityForPath(it) }
        }
    }

    /**
     * Handle file deletion events
     */
    fun onFileDeleted(filePath: String) {
        val file = File(filePath)
        val searchRoot = resolveSearchRoot(file) ?: return

        // Remove from word mapping
        configFileWords.remove(filePath)
        // Cancel pending debounce
        debounceTimers.remove(filePath)?.cancel()

        // If the deleted file is the currently active file, need to re-evaluate priority
        if (activeConfigFiles[searchRoot]?.absolutePath == filePath) {
            reevaluatePriorityForPath(searchRoot)
        }

        updateGlobalWordList()
    }

    /**
     * Re-evaluate configuration file priority for the specified path
     */
    private fun reevaluatePriorityForPath(path: String) {
        val normalized = normalizeSearchRootPath(path) ?: return
        val newActiveFile = findCSpellConfigFile(normalized)

        if (newActiveFile != null) {
            activeConfigFiles[normalized] = newActiveFile
            debounceParseFile(newActiveFile)
        } else {
            activeConfigFiles.remove(normalized)
            updateGlobalWordList()
        }
    }

    /**
     * Debounce file parsing
     */
    private fun debounceParseFile(file: File) {
        val filePath = file.absolutePath

        // Cancel existing debounce timer
        debounceTimers[filePath]?.cancel()

        // Start new debounce timer
        val job = scope.launch {
            delay(debounceDelay)
            loadConfigFile(file)
            updateGlobalWordList()
        }

        debounceTimers[filePath] = job
    }

    /**
     * Load configuration file and parse words
     */
    private suspend fun loadConfigFile(file: File) = withContext(Dispatchers.IO) {
        try {
            val words = parseCSpellConfig(file, project) ?: emptyList()
            configFileWords[file.absolutePath] = words
        } catch (e: Exception) {
            logger.warn("Failed to parse CSpell config: ${file.absolutePath}", e)
            configFileWords[file.absolutePath] = emptyList()
        }
    }

    /**
     * Update global word list
     */
    private fun updateGlobalWordList() {
        val allWords = mutableSetOf<String>()

        // Collect words from all active configuration files
        for (activeFile in activeConfigFiles.values) {
            val words = configFileWords[activeFile.absolutePath] ?: emptyList()
            allWords.addAll(words)
        }

        // Update global words with project reference to trigger spell checker update
        com.github.blackhole1.ideaspellcheck.replaceWords(allWords.toList(), project)
    }


    /**
     * Check if file is the currently active configuration file
     */
    private fun isActiveConfigFile(file: File): Boolean {
        val searchRoot = resolveSearchRoot(file) ?: return false
        return activeConfigFiles[searchRoot]?.absolutePath == file.absolutePath
    }

    private fun normalizeSearchRootPath(path: String): String? {
        val abs = File(path).absolutePath
        val f = File(abs)
        val dir = if (f.name == ".vscode") f.parentFile else f
        return dir?.let { toSystemIndependentName(it.absolutePath).trimEnd('/') }
    }

    private fun resolveSearchRoot(file: File): String? {
        val parent = file.parentFile ?: return null
        return normalizeSearchRootPath(parent.absolutePath)
            ?.takeIf { getAllWatchPaths().any { root -> it == root || it.startsWith("$root/") } }
    }

    /**
     * Clean up resources
     */
    override fun dispose() {
        debounceTimers.values.forEach { it.cancel() }
        debounceTimers.clear()
        scope.cancel()
    }
}
