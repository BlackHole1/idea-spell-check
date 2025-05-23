package com.github.blackhole1.ideaspellcheck.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
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
    }

    fun setCustomSearchPaths(paths: List<String>) {
        state.customSearchPaths = paths.toMutableList()
    }

    companion object {
        fun instance(project: Project): SCProjectSettings = project.getService(SCProjectSettings::class.java)
    }
}
