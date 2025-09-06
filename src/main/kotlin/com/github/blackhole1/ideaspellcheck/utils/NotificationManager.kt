package com.github.blackhole1.ideaspellcheck.utils

import com.github.blackhole1.ideaspellcheck.settings.SCProjectConfigurable
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

    // Project-scoped, thread-safe notification tracking
    private val shownNotifications = ConcurrentHashMap<String, MutableSet<NotificationKey>>()

    private fun showOnce(project: Project, key: NotificationKey, action: () -> Unit) {
        val projectKey = project.locationHash
        val shownSet = shownNotifications.computeIfAbsent(projectKey) {
            ConcurrentHashMap.newKeySet()
        }
        if (shownSet.add(key)) {
            action()
        }
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

    fun showNodeExecutableErrorNotification(project: Project, nodeExecutablePath: String) {
        showOnce(project, NotificationKey.NODE_EXECUTABLE_ERROR) {
            showNotificationWithAction(
                project = project,
                title = "CSpell Check: Node.js Executable Not Found",
                message = "Node.js executable not found at configured path: $nodeExecutablePath",
                type = NotificationType.ERROR,
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

    fun showParseErrorNotification(project: Project, filePath: String, errorMessage: String?) {
        showOnce(project, NotificationKey.PARSE_ERROR) {
            showNotification(
                project,
                "CSpell Check: JavaScript Parsing Error",
                "Error parsing JS file $filePath: ${errorMessage ?: "Unknown error"}",
                NotificationType.ERROR
            )
        }
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
