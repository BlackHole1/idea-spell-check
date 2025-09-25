package com.github.blackhole1.ideaspellcheck.utils.parse

import com.github.blackhole1.ideaspellcheck.utils.NotificationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = Logger.getInstance("CSpell.ParseJS")

fun runCommand(vararg arguments: String, workingDir: File): String? {
    return try {
        val proc = ProcessBuilder(*arguments)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val finished = proc.waitFor(5, TimeUnit.SECONDS)
        if (!finished) {
            logger.warn("Node.js command did not finish within 5 seconds: ${arguments.joinToString(" ")} in $workingDir")
            proc.destroyForcibly()
            proc.waitFor() // Wait for process termination
            proc.inputStream.close()
            proc.errorStream.close()
            return null
        }

        val out = proc.inputStream.bufferedReader().use { it.readText() }
        val err = proc.errorStream.bufferedReader().use { it.readText() }
        if (proc.exitValue() != 0 && err.isNotBlank()) {
            logger.warn("Node parse stderr output: $err")
        }
        out
    } catch (e: IOException) {
        logger.warn("Failed to run Node.js command: ${arguments.joinToString(" ")} in $workingDir", e)
        null
    }
}

fun parseJS(file: File, project: Project, nodeExecutable: String): ParsedCSpellConfig? {
    val cwd = project.guessProjectDir()?.path
    if (cwd == null) {
        NotificationManager.showProjectDirErrorNotification(project)
        return null
    }

    try {
        val sanitizedPath = file.path
            .replace("\\", "\\\\")
            .replace("'", "\\'")

        val command = """
            const path = require('path');
            const configPath = '$sanitizedPath';

            try {
                const rawConfig = require(configPath);
                const config = rawConfig && rawConfig.default ? rawConfig.default : rawConfig;

                const words = Array.isArray(config && config.words)
                    ? config.words.filter((value) => typeof value === 'string')
                    : [];

                const dictionaryDefinitions = Array.isArray(config && config.dictionaryDefinitions)
                    ? config.dictionaryDefinitions.map((def) => {
                        if (!def || typeof def !== 'object') {
                            return {};
                        }

                        const normalized = {};
                        if (typeof def.name === 'string') {
                            normalized.name = def.name;
                        }
                        if (typeof def.path === 'string') {
                            normalized.path = def.path;
                        }
                        if (typeof def.addWords === 'boolean') {
                            normalized.addWords = def.addWords;
                        }
                        return normalized;
                    }).filter((entry) => Object.keys(entry).length > 0)
                    : [];

                const dictionaries = Array.isArray(config && config.dictionaries)
                    ? config.dictionaries.filter((value) => typeof value === 'string')
                    : [];

                const output = { words, dictionaryDefinitions, dictionaries };
                process.stdout.write(JSON.stringify(output));
            } catch (error) {
                console.error(error && error.message ? error.message : String(error));
            }
        """.trimIndent()
        runCommand(nodeExecutable, "-e", command, workingDir = File(cwd))?.let {
            val result = it.trim()
            if (result.isNotEmpty()) {
                return try {
                    val config = json.decodeFromString<CSpellWordsFormat>(result)
                    ParsedCSpellConfig(config.words, config.dictionaryDefinitions, config.dictionaries)
                } catch (e: Exception) {
                    logger.warn("Failed to decode JS output from ${file.path}", e)
                    null
                }
            }
        }
    } catch (e: Exception) {
        NotificationManager.showParseErrorNotification(project, file.path, e.message)
        return null
    }

    return ParsedCSpellConfig()
}
