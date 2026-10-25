package com.github.blackhole1.ideaspellcheck.utils.parse

import com.github.blackhole1.ideaspellcheck.utils.NotificationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import java.io.File

private val logger = Logger.getInstance("CSpell.ParseJS")

private const val NODE_TIMEOUT_MS = 5_000

/**
 * Run a command and return its stdout, or null if it could not be started or did not finish in time.
 * Output is read while the process runs, so large outputs cannot fill the pipe buffer and block the process.
 */
fun runCommand(vararg arguments: String, workingDir: File): String? {
    val commandLine = GeneralCommandLine(*arguments)
        .withWorkDirectory(workingDir)
        .withCharset(Charsets.UTF_8)

    val output = try {
        CapturingProcessHandler(commandLine).runProcess(NODE_TIMEOUT_MS)
    } catch (e: ExecutionException) {
        logger.warn("Failed to run Node.js command: ${commandLine.commandLineString} in $workingDir", e)
        return null
    }

    if (output.isTimeout) {
        logger.warn("Node.js command did not finish within ${NODE_TIMEOUT_MS / 1000} seconds: ${commandLine.commandLineString} in $workingDir")
        return null
    }
    if (output.exitCode != 0 && output.stderr.isNotBlank()) {
        logger.warn("Node parse stderr output: ${output.stderr}")
    }
    return output.stdout
}

fun parseJS(file: File, project: Project, nodeExecutable: String): ParsedCSpellConfig? {
    val cwd = project.guessProjectDir()?.path
    if (cwd == null) {
        NotificationManager.showProjectDirErrorNotification(project)
        return null
    }

    try {
        // The config path is passed as an argument (process.argv[1]) instead of being embedded in the script
        val command = """
            const configPath = process.argv[1];

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
        runCommand(nodeExecutable, "-e", command, file.path, workingDir = File(cwd))?.let {
            val result = it.trim()
            if (result.isNotEmpty()) {
                return try {
                    json.decodeFromString<CSpellWordsFormat>(result).toParsedConfig()
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
