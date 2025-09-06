package com.github.blackhole1.ideaspellcheck.listener

import com.github.blackhole1.ideaspellcheck.utils.CSpellConfigDefinition
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil.toSystemIndependentName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*

/**
 * CSpell configuration file change listener
 * Monitors file system change events and forwards relevant events to the configuration manager
 */
class CSpellFileListener(
    private val project: Project,
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
        val filePath = event.path

        if (isRelevantPath(filePath)) {
            configManager.onFileCreated(filePath)
        }
    }

    /**
     * Handle file content change events
     */
    private fun handleFileChanged(event: VFileContentChangeEvent) {
        val filePath = event.file.path

        if (isRelevantPath(filePath)) {
            configManager.onFileChanged(filePath)
        }
    }

    /**
     * Handle file deletion events
     */
    private fun handleFileDeleted(event: VFileDeleteEvent) {
        val filePath = event.file.path

        if (isRelevantPath(filePath)) {
            configManager.onFileDeleted(filePath)
        }
    }

    /**
     * Handle file rename/move events
     */
    private fun handleFileRenamed(event: VFileMoveEvent) {
        val oldPath = event.oldPath
        val newPath = event.newPath

        // Handle old file deletion
        if (isRelevantPath(oldPath)) {
            configManager.onFileDeleted(oldPath)
        }

        // Handle new file creation
        if (isRelevantPath(newPath)) {
            configManager.onFileCreated(newPath)
        }
    }

    /**
     * Handle file copy events
     */
    private fun handleFileCopied(event: VFileCopyEvent) {
        val filePath = event.newParent.path + "/" + event.newChildName

        if (isRelevantPath(filePath)) {
            configManager.onFileCreated(filePath)
        }
    }

    /**
     * Handle file property change events (e.g., rename within same directory)
     */
    private fun handlePropertyChanged(event: VFilePropertyChangeEvent) {
        if (event.propertyName != VirtualFile.PROP_NAME) return
        val parent = event.file.parent ?: return
        val oldPath = parent.path + "/" + (event.oldValue as? String ?: return)
        val newPath = parent.path + "/" + (event.newValue as? String ?: return)
        if (isRelevantPath(oldPath)) configManager.onFileDeleted(oldPath)
        if (isRelevantPath(newPath)) configManager.onFileCreated(newPath)
    }

    /**
     * Check if file path needs attention
     * Only process files that may contain CSpell configuration
     */
    private fun isRelevantPath(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')

        if (CSpellConfigDefinition.getAllFileNames().contains(fileName)) {
            return isInWatchedDirectory(filePath)
        }

        return false
    }

    /**
     * Check if file is in a watched directory
     */
    private fun isInWatchedDirectory(filePath: String): Boolean {
        val normalized =
            toSystemIndependentName(filePath)
                .trimEnd('/')
        val watchPaths = configManager.getAllWatchPaths()
            .map {
                toSystemIndependentName(it)
                    .trimEnd('/')
            }
        return watchPaths.any { watchPath ->
            normalized == watchPath || normalized.startsWith("$watchPath/")
        }
    }
}
