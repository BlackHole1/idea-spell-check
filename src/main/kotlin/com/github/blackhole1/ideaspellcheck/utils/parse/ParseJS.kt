package com.github.blackhole1.ideaspellcheck.utils.parse

import com.github.blackhole1.ideaspellcheck.utils.NotificationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import java.io.File

private const val NODE_TIMEOUT_MS = 5_000

/**
 * Thrown when a command cannot be started, times out or exits with a non-zero code
 */
class CommandFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Run a command and return its stdout.
 * Output is read while the process runs, so large outputs cannot fill the pipe buffer and block the process.
 * @throws CommandFailedException if the command cannot be started, does not finish in time or fails
 */
fun runCommand(vararg arguments: String, workingDir: File): String {
    val commandLine = GeneralCommandLine(*arguments)
        .withWorkDirectory(workingDir)
        .withCharset(Charsets.UTF_8)

    val output = try {
        CapturingProcessHandler(commandLine).runProcess(NODE_TIMEOUT_MS)
    } catch (e: ExecutionException) {
        throw CommandFailedException("Failed to run ${arguments.first()}: ${e.message}", e)
    }

    if (output.isTimeout) {
        throw CommandFailedException("Node.js did not finish within ${NODE_TIMEOUT_MS / 1000} seconds")
    }
    if (output.exitCode != 0) {
        val error = output.stderr.trim().ifEmpty { "exit code ${output.exitCode}" }
        throw CommandFailedException(error)
    }
    return output.stdout
}

/**
 * Parse a JS config file by loading it with Node.js
 * @return the parsed config, or null if the project directory cannot be determined
 * @throws Exception if Node.js fails to load the config or its output is not a valid config
 */
fun parseJS(file: File, project: Project, nodeExecutable: String): ParsedCSpellConfig? {
    val cwd = project.guessProjectDir()?.path
    if (cwd == null) {
        NotificationManager.showProjectDirErrorNotification(project)
        return null
    }

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
            process.exitCode = 1;
        }
    """.trimIndent()
    val output = runCommand(nodeExecutable, "-e", command, file.path, workingDir = File(cwd)).trim()
    if (output.isEmpty()) return ParsedCSpellConfig()

    return json.decodeFromString<CSpellWordsFormat>(output).toParsedConfig()
}
