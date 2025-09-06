package com.github.blackhole1.ideaspellcheck.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "SCProjectSettingsState",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    category = SettingsCategory.TOOLS
)
internal class SCProjectSettings : SimplePersistentStateComponent<SCProjectSettings.State>(State()) {
    internal class State : BaseState() {
        var customSearchPaths by list<String>()
        var nodeExecutablePath by string()
    }

    fun setCustomSearchPaths(paths: List<String>) {
        state.customSearchPaths = paths.toMutableList()
    }

    fun setNodeExecutablePath(path: String?) {
        state.nodeExecutablePath = path
    }

    companion object {
        fun instance(project: Project): SCProjectSettings = project.getService(SCProjectSettings::class.java)
    }
}
