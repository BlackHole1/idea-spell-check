package com.github.blackhole1.ideaspellcheck.services

import com.github.blackhole1.ideaspellcheck.listener.CSpellConfigFileManager
import com.github.blackhole1.ideaspellcheck.listener.CSpellFileListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class SCProjectService(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(SCProjectService::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val configManager = project.getService(CSpellConfigFileManager::class.java)
    private lateinit var fileListener: CSpellFileListener

    init {
        initializeFileWatching()
    }

    private fun initializeFileWatching() {
        scope.launch {
            try {
                // Initialize scan of all configuration files
                configManager.initialize()

                // Create file listener
                fileListener = CSpellFileListener(configManager)

                // Register to message bus
                project.messageBus.connect(this@SCProjectService)
                    .subscribe(VirtualFileManager.VFS_CHANGES, fileListener)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to initialize file watching", e)
                // Keep scope alive to allow rescan/retry.
            }
        }
    }

    /**
     * Manually trigger rescan of all configuration files
     * Called when settings change
     */
    fun rescanAllConfigFiles() {
        scope.launch {
            configManager.initialize()
        }
    }

    override fun dispose() {
        // Message bus is connected with this Disposable; framework will handle disconnect.
        // CSpellConfigFileManager is a project service; the platform disposes it.
        scope.cancel()
    }
}
