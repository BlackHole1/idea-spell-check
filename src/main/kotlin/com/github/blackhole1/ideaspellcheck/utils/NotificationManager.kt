package com.github.blackhole1.ideaspellcheck.utils

import com.github.blackhole1.ideaspellcheck.settings.SCProjectConfigurable
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.util.concurrent.ConcurrentHashMap

object NotificationManager {
    private const val NOTIFICATION_GROUP_ID = "CSpell Check"

    enum class NotificationKey {
        NODE_JS_CONFIG,
        PROJECT_DIR_ERROR
    }

    // Project-scoped, thread-safe notification tracking
    private val shownNotifications = ConcurrentHashMap<String, MutableSet<NotificationKey>>()

    // Config files whose parse error has been reported, per project (path set is cleared when the file parses again)
    private val failedConfigFiles = ConcurrentHashMap<String, MutableSet<String>>()

    private fun showOnce(project: Project, key: NotificationKey, action: () -> Unit) {
        val projectKey = project.locationHash
        val shownSet = shownNotifications.computeIfAbsent(projectKey) {
            ConcurrentHashMap.newKeySet()
        }
        if (shownSet.add(key)) {
            action()
        }
    }

    fun clear(project: Project) {
        val projectKey = project.locationHash
        shownNotifications.remove(projectKey)
        failedConfigFiles.remove(projectKey)
    }

    fun showNodeJsConfigurationNotification(project: Project) {
        showOnce(project, NotificationKey.NODE_JS_CONFIG) {
            showNotificationWithAction(
                project = project,
                title = "CSpell Check: Node.js Configuration Required",
                message = "JavaScript configuration files detected, but Node.js executable path is not configured. Click to configure.",
                type = NotificationType.WARNING,
                actionText = "Configure Node.js Path"
            )
        }
    }

    fun showProjectDirErrorNotification(project: Project) {
        showOnce(project, NotificationKey.PROJECT_DIR_ERROR) {
            showNotification(
                project,
                "CSpell Check: Project Directory Error",
                "Could not determine project directory.",
                NotificationType.ERROR
            )
        }
    }

    /**
     * Report a config file that failed to parse. Repeated failures of the same file are reported once, until
     * [clearParseError] is called after the file parses successfully.
     */
    fun showParseErrorNotification(project: Project, filePath: String, errorMessage: String?) {
        val failedFiles = failedConfigFiles.computeIfAbsent(project.locationHash) { ConcurrentHashMap.newKeySet() }
        if (!failedFiles.add(filePath)) return

        val reason = StringUtil.escapeXmlEntities(errorMessage?.takeIf { it.isNotBlank() } ?: "Unknown error")
        showNotification(
            project,
            "CSpell Check: Configuration Parsing Error",
            "Failed to parse ${StringUtil.escapeXmlEntities(filePath)}: $reason",
            NotificationType.ERROR
        )
    }

    fun clearParseError(project: Project, filePath: String) {
        failedConfigFiles[project.locationHash]?.remove(filePath)
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
                ShowSettingsUtil.getInstance().showSettingsDialog(project, SCProjectConfigurable::class.java)
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
