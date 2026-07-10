package com.github.blackhole1.ideaspellcheck.listener

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*

/**
 * CSpell configuration file change listener
 * Monitors file system change events and forwards relevant events to the configuration manager
 */
class CSpellFileListener(
    private val configManager: CSpellConfigFileManager
) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        for (event in events) {
            when (event) {
                is VFileCreateEvent -> handleFileCreated(event)
                is VFileContentChangeEvent -> handleFileChanged(event)
                is VFileDeleteEvent -> handleFileDeleted(event)
                is VFileMoveEvent -> handleFileRenamed(event)
                is VFileCopyEvent -> handleFileCopied(event)
                is VFilePropertyChangeEvent -> handlePropertyChanged(event)
            }
        }
    }

    /**
     * Handle file creation events
     */
    private fun handleFileCreated(event: VFileCreateEvent) {
        dispatchPath(event.path, configManager::onFileCreated)
    }

    /**
     * Handle file content change events
     */
    private fun handleFileChanged(event: VFileContentChangeEvent) {
        dispatchPath(event.file.path, configManager::onFileChanged)
    }

    /**
     * Handle file deletion events
     */
    private fun handleFileDeleted(event: VFileDeleteEvent) {
        dispatchPath(event.file.path, configManager::onFileDeleted)
    }

    /**
     * Handle file rename/move events
     */
    private fun handleFileRenamed(event: VFileMoveEvent) {
        val oldPath = event.oldPath
        val newPath = event.newPath

        dispatchPath(oldPath, configManager::onFileDeleted)
        dispatchPath(newPath, configManager::onFileCreated)
    }

    /**
     * Handle file copy events
     */
    private fun handleFileCopied(event: VFileCopyEvent) {
        val filePath = event.newParent.path + "/" + event.newChildName
        dispatchPath(filePath, configManager::onFileCreated)
    }

    /**
     * Handle file property change events (e.g., rename within same directory)
     */
    private fun handlePropertyChanged(event: VFilePropertyChangeEvent) {
        if (event.propertyName != VirtualFile.PROP_NAME) return
        val parent = event.file.parent ?: return
        val oldPath = parent.path + "/" + (event.oldValue as? String ?: return)
        val newPath = parent.path + "/" + (event.newValue as? String ?: return)
        dispatchPath(oldPath, configManager::onFileDeleted)
        dispatchPath(newPath, configManager::onFileCreated)
    }

    private fun dispatchPath(filePath: String, onConfigFile: (String) -> Unit) {
        if (configManager.isDictionaryFile(filePath)) {
            configManager.onDictionaryFileChanged(filePath)
            return
        }

        if (configManager.isConfigFileUnderWatch(filePath)) {
            onConfigFile(filePath)
        }
    }
}
