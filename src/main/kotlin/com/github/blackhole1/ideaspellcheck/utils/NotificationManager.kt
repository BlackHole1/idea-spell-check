package com.github.blackhole1.ideaspellcheck.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

object NotificationManager {
    private const val NOTIFICATION_GROUP_ID = "CSpell Check"
    
    enum class NotificationKey {
        NODE_JS_CONFIG,
        NODE_EXECUTABLE_ERROR,
        PROJECT_DIR_ERROR,
        PARSE_ERROR
    }
    
    // Project-scoped notification states to avoid cross-project pollution
    private val notificationStates = ConcurrentHashMap<String, MutableSet<NotificationKey>>()
    
    private fun shouldShow(project: Project, key: NotificationKey): Boolean {
        val projectKey = project.locationHash
        return !notificationStates.getOrPut(projectKey) { mutableSetOf() }.contains(key)
    }
    
    private fun markShown(project: Project, key: NotificationKey) {
        val projectKey = project.locationHash
        notificationStates.getOrPut(projectKey) { mutableSetOf() }.add(key)
    }
    
    fun showNodeJsConfigurationNotification(project: Project) {
        if (!shouldShow(project, NotificationKey.NODE_JS_CONFIG)) return
        
        showNotificationWithAction(
            project = project,
            title = "CSpell Check: Node.js Configuration Required",
            message = "JavaScript configuration files detected, but Node.js executable path is not configured. Click to configure.",
            type = NotificationType.WARNING,
            actionText = "Configure Node.js Path"
        )
        markShown(project, NotificationKey.NODE_JS_CONFIG)
    }
    
    fun showNodeExecutableErrorNotification(project: Project, nodeExecutablePath: String) {
        if (!shouldShow(project, NotificationKey.NODE_EXECUTABLE_ERROR)) return
        
        showNotificationWithAction(
            project = project,
            title = "CSpell Check: Node.js Executable Not Found",
            message = "Node.js executable not found at configured path: $nodeExecutablePath",
            type = NotificationType.ERROR,
            actionText = "Configure Node.js Path"
        )
        markShown(project, NotificationKey.NODE_EXECUTABLE_ERROR)
    }
    
    fun showProjectDirErrorNotification(project: Project) {
        if (!shouldShow(project, NotificationKey.PROJECT_DIR_ERROR)) return
        
        showNotification(
            project,
            "CSpell Check: Project Directory Error",
            "Could not determine project directory.",
            NotificationType.ERROR
        )
        markShown(project, NotificationKey.PROJECT_DIR_ERROR)
    }
    
    fun showParseErrorNotification(project: Project, filePath: String, errorMessage: String?) {
        if (!shouldShow(project, NotificationKey.PARSE_ERROR)) return
        
        showNotification(
            project,
            "CSpell Check: JavaScript Parsing Error",
            "Error parsing JS file $filePath: ${errorMessage ?: "Unknown error"}",
            NotificationType.ERROR
        )
        markShown(project, NotificationKey.PARSE_ERROR)
    }
    
    private fun showNotificationWithAction(
        project: Project,
        title: String,
        message: String,
        type: NotificationType,
        actionText: String
    ) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID) ?: return
        
        val notification = notificationGroup.createNotification(title, message, type)
        
        notification.addAction(object : AnAction(actionText) {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, NOTIFICATION_GROUP_ID)
            }
        })
        
        notification.notify(project)
    }
    
    private fun showNotification(project: Project, title: String, message: String, type: NotificationType) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID) ?: return
        
        val notification = notificationGroup.createNotification(title, message, type)
        notification.notify(project)
    }
}
