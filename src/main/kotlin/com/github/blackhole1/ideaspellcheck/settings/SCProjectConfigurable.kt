package com.github.blackhole1.ideaspellcheck.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.openapi.fileChooser.FileChooser
import com.github.blackhole1.ideaspellcheck.utils.NodejsFinder
import com.intellij.util.ui.JBUI
import com.intellij.openapi.ui.ComboBox
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class SCProjectConfigurable : Configurable {
    private lateinit var project: Project
    private lateinit var settingsComponent: SettingsComponent

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
            val fileName = file.name.lowercase()
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            
            return if (isWindows) {
                fileName == "node.exe"
            } else {
                fileName == "node"
            }
        }
    }

    internal class SettingsComponent(
        val project: Project,
        val pathsListModel: CollectionListModel<String> = CollectionListModel<String>(),
    ) {
        val pathsList: JBList<String> = JBList(pathsListModel)
        val nodeExecutableComboBox: ComboBox<String> = ComboBox<String>()
        val browseButton: JButton = JButton("Browse...")
        
        val panel: JPanel = createMainPanel()
        
        init {
            initializeNodeExecutableComboBox()
            initializeBrowseButton()
        }
        
        private fun initializeNodeExecutableComboBox() {
            nodeExecutableComboBox.isEditable = true
            
            // Set reasonable size to prevent excessive expansion
            nodeExecutableComboBox.preferredSize = java.awt.Dimension(350, nodeExecutableComboBox.preferredSize.height)
            
            // Load discovered Node.js executables
            val discoveredPaths = NodejsFinder.findNodejsExecutables()
            discoveredPaths.forEach { path ->
                nodeExecutableComboBox.addItem(path)
            }
        }
        
        private fun initializeBrowseButton() {
            browseButton.addActionListener {
                browseForNodeExecutable()
            }
        }
        
        private fun browseForNodeExecutable() {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            descriptor.title = "SELECT NODE.JS EXECUTABLE"
            descriptor.description = "Select the Node.js executable file"
            
            // Add file filter for executables
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            descriptor.withFileFilter { file ->
                if (file.isDirectory) return@withFileFilter false
                val fileName = file.name.lowercase()
                if (isWindows) {
                    fileName == "node.exe"
                } else {
                    fileName == "node"
                }
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
            // Check if path already exists
            val model = nodeExecutableComboBox.model
            for (i in 0 until model.size) {
                val item = model.getElementAt(i)
                if (item == path) {
                    return // Path already exists
                }
            }
            // Add the new path
            nodeExecutableComboBox.addItem(path)
        }
        
        private fun createMainPanel(): JPanel {
            val mainPanel = JPanel(GridBagLayout())
            
            // Node.js executable path section
            addWithConstraints(mainPanel, JBLabel("Node.js executable path:"), 
                               gridx = 0, gridy = 0, anchor = GridBagConstraints.WEST, 
                               insets = JBUI.insets(0, 0, 5, 10)
            )
            
            addWithConstraints(mainPanel, nodeExecutableComboBox, 
                               gridx = 1, gridy = 0, fill = GridBagConstraints.HORIZONTAL, 
                               weightx = 0.7, insets = JBUI.insets(0, 0, 5, 5)
            )
            
            addWithConstraints(mainPanel, browseButton, 
                               gridx = 2, gridy = 0, insets = JBUI.insetsBottom(5)
            )
            
            
            // Custom search paths section
            addWithConstraints(mainPanel, JBLabel("Custom search paths:"), 
                               gridx = 0, gridy = 1, anchor = GridBagConstraints.NORTHWEST, 
                               insets = JBUI.insets(10, 0, 5, 10)
            )
            
            val pathsPanel = ToolbarDecorator.createDecorator(pathsList)
                .setAddAction { _ ->
                    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                    descriptor.title = "Select Directory"
                    descriptor.description = "Select a directory to search for CSpell configuration files"
                    FileChooser.chooseFile(
                        descriptor,
                        project,
                        null,
                    ) { file ->
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
            
            addWithConstraints(mainPanel, pathsPanel, 
                               gridx = 0, gridy = 2, gridwidth = 3, 
                               fill = GridBagConstraints.BOTH, weightx = 1.0, weighty = 1.0)
            
            return mainPanel
        }
        
        private fun addWithConstraints(
            parent: JPanel, 
            component: JComponent, 
            gridx: Int = 0, 
            gridy: Int = 0,
            gridwidth: Int = 1,
            fill: Int = GridBagConstraints.NONE,
            anchor: Int = GridBagConstraints.CENTER,
            weightx: Double = 0.0,
            weighty: Double = 0.0,
            insets: Insets = JBUI.emptyInsets()
        ) {
            val gbc = GridBagConstraints().apply {
                this.gridx = gridx
                this.gridy = gridy
                this.gridwidth = gridwidth
                this.fill = fill
                this.anchor = anchor
                this.weightx = weightx
                this.weighty = weighty
                this.insets = insets
            }
            parent.add(component, gbc)
        }
        
    }

    fun setProject(project: Project) {
        this.project = project
    }

    override fun getDisplayName(): String = "CSpell Check"

    override fun getPreferredFocusedComponent() = settingsComponent.nodeExecutableComboBox

    override fun createComponent(): JComponent {
        settingsComponent = SettingsComponent(project)
        return settingsComponent.panel
    }

    override fun isModified(): Boolean {
        val settings = SCProjectSettings.instance(project)
        val nodePathText = (settingsComponent.nodeExecutableComboBox.selectedItem as? String)?.trim()?.takeIf { it.isNotEmpty() }
        
        return settings.state.customSearchPaths != settingsComponent.pathsListModel.items ||
               settings.state.nodeExecutablePath != nodePathText
    }

    override fun apply() {
        val settings = SCProjectSettings.instance(project)
        val nodePathText = (settingsComponent.nodeExecutableComboBox.selectedItem as? String)?.trim()?.takeIf { it.isNotEmpty() }
        
        // Validate Node.js path before saving
        if (!validateNodeExecutable(nodePathText ?: "")) {
            Messages.showErrorDialog(
                project,
                "The specified Node.js executable path is invalid. Please select a valid Node.js binary.",
                "INVALID NODE.JS EXECUTABLE"
            )
            return
        }
        
        settings.setCustomSearchPaths(settingsComponent.pathsListModel.items)
        settings.setNodeExecutablePath(nodePathText)
    }

    override fun reset() {
        val settings = SCProjectSettings.instance(project)
        settingsComponent.pathsListModel.replaceAll(settings.state.customSearchPaths)
        
        // Set saved path in ComboBox
        val savedPath = settings.state.nodeExecutablePath
        if (savedPath != null && savedPath.isNotEmpty()) {
            // Add saved path to ComboBox if it's not already there
            settingsComponent.addPathToComboBoxIfNotExists(savedPath)
            settingsComponent.nodeExecutableComboBox.selectedItem = savedPath
        } else {
            // Select first item if available, otherwise leave empty
            if (settingsComponent.nodeExecutableComboBox.itemCount > 0) {
                settingsComponent.nodeExecutableComboBox.selectedIndex = 0
            } else {
                settingsComponent.nodeExecutableComboBox.selectedItem = ""
            }
        }
    }
}

class SCProjectConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        val cfg = SCProjectConfigurable()
        cfg.setProject(project)

        return cfg
    }
}
