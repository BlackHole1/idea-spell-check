package com.github.blackhole1.ideaspellcheck.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import javax.swing.JComponent
import javax.swing.JPanel

class SCProjectConfigurable : Configurable {
    private lateinit var project: Project
    private lateinit var settingsComponent: SettingsComponent

    internal class SettingsComponent(
        val project: Project,
        val pathsListModel: CollectionListModel<String> = CollectionListModel<String>(),
    ) {
        val pathsList: JBList<String> = JBList<String>(pathsListModel)
        val panel: JPanel = ToolbarDecorator.createDecorator(pathsList)
            .setAddAction { _ ->
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                descriptor.title = "Select Directory"
                descriptor.description = "Select a directory to search for CSpell configuration files"
                com.intellij.openapi.fileChooser.FileChooser.chooseFile(
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
    }

    fun setProject(project: Project) {
        this.project = project
    }

    override fun getDisplayName(): String = "CSpell Check"

    override fun getPreferredFocusedComponent() = settingsComponent.pathsList

    override fun createComponent(): JComponent {
        settingsComponent = SettingsComponent(project)
        return settingsComponent.panel
    }

    override fun isModified() = SCProjectSettings.instance(project).state != settingsComponent.pathsListModel.items

    override fun apply() {
        SCProjectSettings.instance(project).setCustomSearchPaths(settingsComponent.pathsListModel.items)
    }

    override fun reset() {
        settingsComponent.pathsListModel.replaceAll(SCProjectSettings.instance(project).state.customSearchPaths);
    }
}

class SCProjectConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable? {
        val cfg = SCProjectConfigurable()
        cfg.setProject(project)

        return cfg
    }
}
