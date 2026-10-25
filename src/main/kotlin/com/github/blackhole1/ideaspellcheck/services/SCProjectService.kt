package com.github.blackhole1.ideaspellcheck.services

import com.github.blackhole1.ideaspellcheck.listener.CSpellConfigFileManager
import com.github.blackhole1.ideaspellcheck.listener.CSpellFileListener
import com.github.blackhole1.ideaspellcheck.utils.NotificationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class SCProjectService(
    private val project: Project,
    private val scope: CoroutineScope,
) : Disposable {
    private val logger = Logger.getInstance(SCProjectService::class.java)
    private val configManager = project.service<CSpellConfigFileManager>()

    init {
        // Register the listener before scanning, so a failed scan still leaves file watching in place and a later
        // rescan (e.g. after changing settings) can recover. Events received during the scan are handled after it.
        project.messageBus.connect(this)
            .subscribe(VirtualFileManager.VFS_CHANGES, CSpellFileListener(configManager))

        rescanAllConfigFiles()
    }

    /**
     * Manually trigger rescan of all configuration files
     * Called when settings change
     */
    fun rescanAllConfigFiles() {
        scope.launch {
            try {
                configManager.initialize()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Failed to scan CSpell configuration files", e)
            }
        }
    }

    override fun dispose() {
        // The message bus connection is disposed with this service and the scope is cancelled by the platform.
        NotificationManager.clear(project)
    }
}
