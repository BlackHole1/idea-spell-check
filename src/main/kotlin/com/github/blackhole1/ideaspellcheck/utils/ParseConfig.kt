package com.github.blackhole1.ideaspellcheck.utils

import com.github.blackhole1.ideaspellcheck.utils.parse.parseJS
import com.github.blackhole1.ideaspellcheck.utils.parse.parseJSON
import com.github.blackhole1.ideaspellcheck.utils.parse.parseYAML
import com.intellij.openapi.project.Project
import java.io.File

fun parseCSpellConfig(file: File, project: Project): List<String>? {
    when (file.extension) {
        "json" -> {
            return parseJSON(file)
        }
        "js", "cjs" -> {
            return parseJS(file, project)
        }
        "yaml", "yml" -> {
            return parseYAML(file)
        }
    }

    return null
}
