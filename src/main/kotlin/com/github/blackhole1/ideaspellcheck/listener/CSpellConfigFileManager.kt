package com.github.blackhole1.ideaspellcheck.listener

import com.github.blackhole1.ideaspellcheck.clearWords
import com.github.blackhole1.ideaspellcheck.replaceWords
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
import java.util.*
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
    private val activeConfigFiles = ConcurrentHashMap<String, ActiveConfigFile>()

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
     * File names of tracked dictionary files (both as declared and canonicalized), used to cheaply reject
     * unrelated VFS events before resolving canonical paths
     */
    private val configFileDictionaryNames = ConcurrentHashMap<String, Set<String>>()
    private val dictionaryNamesLock = Any()
    @Volatile
    private var dictionaryFileNames: Set<String> = emptySet()

    private val searchRootsLock = Any()
    @Volatile
    private var cachedSearchRoots: Set<String>? = null

    /**
     * Debounce timer management
     * key: absolute path of the file
     * value: corresponding debounce coroutine Job
     */
    private val debounceTimers = ConcurrentHashMap<String, Job>()

    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Default)
    private val debounceDelay = 500L

    /**
     * Get the directories searched for config files: the project root and the custom search paths.
     * Only config files located directly in one of them (or in its .config/.vscode/config subdirectory) are used,
     * both when scanning and when handling file events.
     */
    fun getConfigSearchRoots(): Set<String> {
        cachedSearchRoots?.let { return it }

        return synchronized(searchRootsLock) {
            cachedSearchRoots ?: buildSearchRoots().also { cachedSearchRoots = it }
        }
    }

    private fun buildSearchRoots(): Set<String> {
        val paths = linkedSetOf<String>()

        // Add project root directory
        project.guessProjectDir()?.path?.let { rootPath ->
            val file = File(rootPath)
            if (file.isDirectory) {
                paths.add(normalizeSearchRoot(file.absolutePath))
            }
        }

        // Add custom search paths
        val settings = SCProjectSettings.instance(project)
        settings.state.customSearchPaths.filter { it.isNotBlank() }.forEach { path ->
            paths.add(normalizeSearchRoot(File(path).absolutePath))
        }

        return paths
    }

    /**
     * Initialize: scan all paths and set initial active configuration files
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        activeConfigFiles.clear()
        configFileWords.clear()
        configFileDictionaryPaths.clear()
        dictionaryFileToConfigs.clear()
        configFileDictionaryNames.clear()
        refreshDictionaryFileNames()
        invalidateSearchRoots()

        for (path in getConfigSearchRoots()) {
            val configFile = findCSpellConfigFile(path)
            if (configFile != null) {
                activeConfigFiles[path] = ActiveConfigFile(configFile)
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

        val searchRoot = resolveSearchRoot(file.absolutePath) ?: return
        val currentActiveFile = activeConfigFiles[searchRoot]

        // Check if the new file has higher priority
        if (currentActiveFile == null || CSpellConfigDefinition.hasHigherPriority(file, currentActiveFile.file)) {
            activeConfigFiles[searchRoot] = ActiveConfigFile(file)
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
            resolveSearchRoot(file.absolutePath)?.let { reevaluatePriorityForPath(it) }
        }
    }

    /**
     * Handle file deletion events
     */
    fun onFileDeleted(filePath: String) {
        val file = File(filePath)
        val searchRoot = resolveSearchRoot(file.absolutePath) ?: return
        val configPath = normalizeFilePath(file.absolutePath)

        // Remove from word mapping
        configFileWords.remove(configPath)
        removeDictionaryMappings(configPath)
        // Cancel pending debounce
        debounceTimers.remove(configPath)?.cancel()

        // If the deleted file is the currently active file, need to re-evaluate priority.
        // Re-resolve instead of using the cached path: a deleted symlink no longer resolves to its former target.
        if (activeConfigFiles[searchRoot]?.let { normalizeFilePath(it.file.absolutePath) } == configPath) {
            reevaluatePriorityForPath(searchRoot)
        }

        updateGlobalWordList()
    }

    /**
     * Re-evaluate configuration file priority for the specified path
     */
    private fun reevaluatePriorityForPath(searchRoot: String) {
        val newActiveFile = findCSpellConfigFile(searchRoot)

        if (newActiveFile != null) {
            activeConfigFiles[searchRoot] = ActiveConfigFile(newActiveFile)
            debounceParseFile(newActiveFile)
        } else {
            activeConfigFiles.remove(searchRoot)
            updateGlobalWordList()
        }
    }

    /**
     * Debounce file parsing
     */
    private fun debounceParseFile(file: File) {
        val filePath = normalizeFilePath(file.absolutePath)

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
        val configPath = normalizeFilePath(file.absolutePath)
        try {
            val result = parseCSpellConfig(file, project)
            val words = result?.words ?: emptyList()
            val dictionaryPaths = result?.dictionaryPaths ?: emptySet()

            configFileWords[configPath] = words
            updateDictionaryMappings(configPath, dictionaryPaths)
        } catch (e: Exception) {
            logger.warn("Failed to parse CSpell config: ${file.absolutePath}", e)
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
            configFileWords[activeFile.path]?.let { allWords.addAll(it) }
        }

        // Update global words with project reference to trigger spell checker update
        replaceWords(allWords, project)
    }

    fun isDictionaryFile(path: String): Boolean {
        // Fast paths: this is called for every VFS event, so avoid resolving canonical paths for unrelated files
        if (dictionaryFileToConfigs.isEmpty()) return false
        if (fileNameOf(path) !in dictionaryFileNames) return false

        val normalized = normalizeFilePath(path)
        return dictionaryFileToConfigs.containsKey(normalized)
    }

    /**
     * Check if the path is a config file belonging to one of the search roots. Only inspects the path string,
     * since it is called for every VFS event.
     */
    fun isSearchRootConfigFile(path: String): Boolean = resolveSearchRoot(path) != null

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
        updateDictionaryFileNames(configPath, (newPaths + normalizedNew).mapTo(mutableSetOf(), ::fileNameOf))

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
        updateDictionaryFileNames(configPath, emptySet())
        previous.forEach { dictionaryPath ->
            dictionaryFileToConfigs.compute(dictionaryPath) { _, configs ->
                configs?.remove(configPath)
                if (configs != null && configs.isEmpty()) null else configs
            }
        }
    }

    private fun updateDictionaryFileNames(configPath: String, names: Set<String>) {
        val previous = if (names.isEmpty()) {
            configFileDictionaryNames.remove(configPath)
        } else {
            configFileDictionaryNames.put(configPath, names)
        }
        if (previous != names) {
            refreshDictionaryFileNames()
        }
    }

    private fun refreshDictionaryFileNames() {
        synchronized(dictionaryNamesLock) {
            dictionaryFileNames = configFileDictionaryNames.values.flatMapTo(mutableSetOf()) { it }
        }
    }

    /**
     * Check if file is the currently active configuration file
     */
    private fun isActiveConfigFile(file: File): Boolean {
        val searchRoot = resolveSearchRoot(file.absolutePath) ?: return false
        val filePath = normalizeFilePath(file.absolutePath)
        return activeConfigFiles[searchRoot]?.path == filePath
    }

    /**
     * Return the search root a config file path belongs to, or null if it is not a config file of any search root
     */
    private fun resolveSearchRoot(filePath: String): String? {
        val root = CSpellConfigDefinition.getSearchRootPath(filePath) ?: return null
        return normalizeSearchRoot(root).takeIf { it in getConfigSearchRoots() }
    }

    private fun normalizeSearchRoot(path: String): String =
        toSystemIndependentName(path).trimEnd('/').ifEmpty { "/" }

    private fun invalidateSearchRoots() {
        synchronized(searchRootsLock) {
            cachedSearchRoots = null
        }
    }

    /**
     * Clean up resources
     */
    override fun dispose() {
        scope.cancel()
        debounceTimers.values.forEach { it.cancel() }
        debounceTimers.clear()
        clearWords(project)
    }
}

/**
 * Active config file together with its normalized path, so the path is resolved once instead of on every lookup
 */
private class ActiveConfigFile(val file: File) {
    val path: String = normalizeFilePath(file.absolutePath)
}

private fun normalizeFilePath(path: String): String {
    return try {
        toSystemIndependentName(File(path).canonicalPath)
    } catch (_: java.io.IOException) {
        toSystemIndependentName(File(path).absolutePath)
    }
}

private fun fileNameOf(path: String): String =
    toSystemIndependentName(path).trimEnd('/').substringAfterLast('/')
