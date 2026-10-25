package com.github.blackhole1.ideaspellcheck.settings

import com.github.blackhole1.ideaspellcheck.services.SCProjectService
import com.github.blackhole1.ideaspellcheck.utils.NodejsFinder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import java.io.File
import javax.swing.JComponent

class SCProjectConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: SettingsComponent? = null

    companion object {
        /**
         * Validates if the given path points to a Node.js executable
         */
        private fun validateNodeExecutable(path: String): Boolean {
            if (path.isBlank()) return true // Empty path is allowed (unconfigured state)

            val file = File(path)
            if (!file.exists()) return false
            if (!file.canExecute()) return false

            // Check if it's likely a Node.js executable
            return NodejsFinder.isNodeExecutableName(file.name)
        }
    }

    internal class SettingsComponent(private val project: Project) {
        val pathsListModel = CollectionListModel<String>()
        private val pathsList = JBList(pathsListModel)
        val nodeExecutableComboBox = ComboBox<String>().apply { isEditable = true }

        val panel: JComponent = panel {
            row("Node.js executable path:") {
                cell(nodeExecutableComboBox).align(AlignX.FILL).resizableColumn()
                button("Browse...") { browseForNodeExecutable() }
            }
            row {
                label("Custom search paths:")
            }.topGap(TopGap.SMALL)
            row {
                cell(createPathsPanel()).align(Align.FILL)
            }.resizableRow()
        }

        /**
         * Discover Node.js executables on a background thread (it checks many locations on disk) and add them
         * to the combo box once found, selecting the first one when no path has been configured yet
         */
        fun loadDiscoveredNodeExecutables(isNodePathConfigured: () -> Boolean) {
            val modality = ModalityState.stateForComponent(panel)
            ApplicationManager.getApplication().executeOnPooledThread {
                val discoveredPaths = NodejsFinder.findNodejsExecutables()
                ApplicationManager.getApplication().invokeLater({
                    discoveredPaths.forEach(::addPathToComboBoxIfNotExists)
                    if (nodeExecutablePath() == null && !isNodePathConfigured() && nodeExecutableComboBox.itemCount > 0) {
                        nodeExecutableComboBox.selectedIndex = 0
                    }
                }, modality)
            }
        }

        private fun createPathsPanel(): JComponent {
            return ToolbarDecorator.createDecorator(pathsList)
                .setAddAction { _ ->
                    val descriptor = FileChooserDescriptorFactory.singleDir()
                    descriptor.title = "Select Directory"
                    descriptor.description = "Select a directory to search for CSpell configuration files"
                    FileChooser.chooseFile(descriptor, project, null) { file ->
                        if (!pathsListModel.items.contains(file.path)) {
                            pathsListModel.add(file.path)
                        }
                    }
                }
                .setRemoveAction { _ ->
                    val selectedIndex = pathsList.selectedIndex
                    if (selectedIndex != -1) {
                        pathsListModel.remove(selectedIndex)
                    }
                }
                .disableUpDownActions()
                .createPanel()
        }

        private fun browseForNodeExecutable() {
            val descriptor = FileChooserDescriptorFactory.singleFile()
            descriptor.title = "SELECT NODE.JS EXECUTABLE"
            descriptor.description = "Select the Node.js executable file"

            // Add file filter for executables
            descriptor.withFileFilter { file ->
                !file.isDirectory && NodejsFinder.isNodeExecutableName(file.name)
            }

            FileChooser.chooseFile(descriptor, project, null) { file ->
                val selectedPath = file.path

                if (validateNodeExecutable(selectedPath)) {
                    // Add the new path if it doesn't exist
                    addPathToComboBoxIfNotExists(selectedPath)
                    nodeExecutableComboBox.selectedItem = selectedPath
                } else {
                    Messages.showErrorDialog(
                        project,
                        "The selected file is not a valid Node.js executable. Please select the 'node' or 'node.exe' binary.",
                        "INVALID NODE.JS EXECUTABLE"
                    )
                }
            }
        }

        fun addPathToComboBoxIfNotExists(path: String) {
            val model = nodeExecutableComboBox.model
            if ((0 until model.size).none { model.getElementAt(it) == path }) {
                nodeExecutableComboBox.addItem(path)
            }
        }

        /**
         * Currently entered Node.js path, or null when empty
         */
        fun nodeExecutablePath(): String? =
            (nodeExecutableComboBox.selectedItem as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }

    override fun getDisplayName(): String = "CSpell Check"

    override fun getPreferredFocusedComponent() = settingsComponent?.nodeExecutableComboBox

    override fun createComponent(): JComponent {
        val component = SettingsComponent(project)
        settingsComponent = component
        component.loadDiscoveredNodeExecutables {
            !SCProjectSettings.instance(project).state.nodeExecutablePath.isNullOrEmpty()
        }
        return component.panel
    }

    override fun isModified(): Boolean {
        val component = settingsComponent ?: return false
        val settings = SCProjectSettings.instance(project)

        return settings.state.customSearchPaths != component.pathsListModel.items ||
                settings.state.nodeExecutablePath != component.nodeExecutablePath()
    }

    override fun apply() {
        val component = settingsComponent ?: return
        val settings = SCProjectSettings.instance(project)
        val nodePathText = component.nodeExecutablePath()

        // Validate Node.js path before saving
        if (!validateNodeExecutable(nodePathText ?: "")) {
            throw ConfigurationException(
                "The specified Node.js executable path is invalid. Please select a valid Node.js binary.",
                "INVALID NODE.JS EXECUTABLE"
            )
        }

        // Check if custom search paths changed
        val pathsChanged = settings.state.customSearchPaths != component.pathsListModel.items
        val nodePathChanged = settings.state.nodeExecutablePath != nodePathText

        settings.setCustomSearchPaths(component.pathsListModel.items)
        settings.setNodeExecutablePath(nodePathText)

        // Trigger rescan if settings that affect config discovery or parsing changed
        if (pathsChanged || nodePathChanged) {
            project.getServiceIfCreated(SCProjectService::class.java)?.rescanAllConfigFiles()
        }
    }

    override fun reset() {
        val component = settingsComponent ?: return
        val settings = SCProjectSettings.instance(project)
        component.pathsListModel.replaceAll(settings.state.customSearchPaths)

        // Set saved path in ComboBox
        val savedPath = settings.state.nodeExecutablePath
        if (!savedPath.isNullOrEmpty()) {
            // Add saved path to ComboBox if it's not already there
            component.addPathToComboBoxIfNotExists(savedPath)
            component.nodeExecutableComboBox.selectedItem = savedPath
        } else {
            // Select first item if available (discovered paths may still be loading), otherwise leave empty
            if (component.nodeExecutableComboBox.itemCount > 0) {
                component.nodeExecutableComboBox.selectedIndex = 0
            } else {
                component.nodeExecutableComboBox.selectedItem = ""
            }
        }
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
