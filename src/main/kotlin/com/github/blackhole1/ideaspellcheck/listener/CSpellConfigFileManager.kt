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
import java.util.Collections
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
     * Track dictionary file paths (each config file -> set of dependent dictionary absolute paths)
     */
    private val configFileDictionaryPaths = ConcurrentHashMap<String, Set<String>>()

    /**
     * Reverse index from dictionary files to config files to quickly locate impacted configurations
     */
    private val dictionaryFileToConfigs = ConcurrentHashMap<String, MutableSet<String>>()

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

        // Add directories containing dictionary files
        dictionaryFileToConfigs.keys.forEach { dictionaryPath ->
            val parent = File(dictionaryPath).parentFile
            if (parent != null && parent.isDirectory) {
                paths.add(toSystemIndependentName(parent.absolutePath).trimEnd('/'))
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
        configFileDictionaryPaths.clear()
        dictionaryFileToConfigs.clear()
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
        val configPath = normalizeFilePath(file.absolutePath)

        // Remove from word mapping
        configFileWords.remove(configPath)
        removeDictionaryMappings(configPath)
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
            try {
                loadConfigFile(file)
                updateGlobalWordList()
            } finally {
                debounceTimers.remove(filePath, coroutineContext[Job])
            }
        }

        debounceTimers[filePath] = job
    }

    /**
     * Load configuration file and parse words
     */
    private suspend fun loadConfigFile(file: File) = withContext(Dispatchers.IO) {
        try {
            val configPath = normalizeFilePath(file.absolutePath)
            val result = parseCSpellConfig(file, project)
            val words = result?.words ?: emptyList()
            val dictionaryPaths = result?.dictionaryPaths ?: emptySet()

            configFileWords[configPath] = words
            updateDictionaryMappings(configPath, dictionaryPaths)
        } catch (e: Exception) {
            logger.warn("Failed to parse CSpell config: ${file.absolutePath}", e)
            val configPath = normalizeFilePath(file.absolutePath)
            configFileWords[configPath] = emptyList()
            updateDictionaryMappings(configPath, emptySet())
        }
    }

    /**
     * Update global word list
     */
    private fun updateGlobalWordList() {
        val allWords = mutableSetOf<String>()

        // Collect words from all active configuration files
        for (activeFile in activeConfigFiles.values) {
            val configPath = normalizeFilePath(activeFile.absolutePath)
            val words = configFileWords[configPath] ?: emptyList()
            allWords.addAll(words)
        }

        // Update global words with project reference to trigger spell checker update
        com.github.blackhole1.ideaspellcheck.replaceWords(allWords.toList(), project)
    }

    fun isDictionaryFile(path: String): Boolean {
        val normalized = normalizeFilePath(path)
        return dictionaryFileToConfigs.containsKey(normalized)
    }

    fun onDictionaryFileChanged(path: String) {
        val normalized = normalizeFilePath(path)
        val configs = dictionaryFileToConfigs[normalized]?.toList() ?: emptyList()
        configs.forEach { configPath ->
            debounceParseFile(File(configPath))
        }
    }

    private fun updateDictionaryMappings(configPath: String, newPaths: Set<String>) {
        val normalizedNew = newPaths.map { normalizeFilePath(it) }.toSet()
        val previous = configFileDictionaryPaths.put(configPath, normalizedNew) ?: emptySet()

        val removed = previous - normalizedNew
        val added = normalizedNew - previous

        removed.forEach { dictionaryPath ->
            dictionaryFileToConfigs.compute(dictionaryPath) { _, configs ->
                configs?.remove(configPath)
                if (configs != null && configs.isEmpty()) null else configs
            }
        }

        added.forEach { dictionaryPath ->
            val configs = dictionaryFileToConfigs.computeIfAbsent(dictionaryPath) {
                Collections.newSetFromMap(ConcurrentHashMap())
            }
            configs.add(configPath)
        }
    }

    private fun removeDictionaryMappings(configPath: String) {
        val previous = configFileDictionaryPaths.remove(configPath) ?: emptySet()
        previous.forEach { dictionaryPath ->
            dictionaryFileToConfigs.compute(dictionaryPath) { _, configs ->
                configs?.remove(configPath)
                if (configs != null && configs.isEmpty()) null else configs
            }
        }
    }

    private fun normalizeFilePath(path: String): String {
        return try {
            toSystemIndependentName(File(path).canonicalPath)
        } catch (e: java.io.IOException) {
            toSystemIndependentName(File(path).absolutePath)
        }
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
