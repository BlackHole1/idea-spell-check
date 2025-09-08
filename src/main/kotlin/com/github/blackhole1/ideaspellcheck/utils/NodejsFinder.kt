package com.github.blackhole1.ideaspellcheck.utils

import java.io.File

object NodejsFinder {

    fun findNodejsExecutables(): List<String> {
        val executableName = if (isWindows()) "node.exe" else "node"
        val searchPaths = getSearchPaths()

        return searchPaths
            .asSequence()
            .filter { it.isNotEmpty() }
            .map { path -> File(path, executableName) }
            .filter { it.exists() && it.canExecute() }
            .map { file ->
                try {
                    file.canonicalPath
                } catch (_: Exception) {
                    file.absolutePath
                }
            }
            .distinct()
            .toList()
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("windows")

    private fun getSearchPaths(): List<String> {
        val paths = mutableListOf<String>()

        // System PATH
        System.getenv("PATH")?.split(File.pathSeparator)?.let { paths.addAll(it) }

        // Common installation paths
        if (isWindows()) {
            paths.addAll(getWindowsPaths())
        } else {
            paths.addAll(getUnixPaths())
        }

        return paths.distinct()
    }

    private fun getWindowsPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
        val programFilesX86 = System.getenv("ProgramFiles(x86)") ?: "C:\\Program Files (x86)"
        val appData = System.getenv("APPDATA") ?: "$userHome\\AppData\\Roaming"

        val paths = mutableListOf<String>()

        // Standard installations
        paths.addAll(
            listOf(
                "$programFiles\\nodejs",
                "$programFilesX86\\nodejs"
            )
        )

        // nvm-windows (environment variable driven)
        System.getenv("NVM_HOME")?.let { paths.add(it) }
        System.getenv("NVM_SYMLINK")?.let { paths.add(it) }

        // Volta (environment variable or default location)
        val voltaHome = System.getenv("VOLTA_HOME") ?: "$userHome\\.volta"
        paths.add("$voltaHome\\bin")

        // fnm (correct alias path for current version)
        paths.add("$appData\\fnm\\aliases\\default")

        // Chocolatey (global bin directory)
        paths.add("C:\\ProgramData\\chocolatey\\bin")

        // Scoop (with bin subdirectory)
        paths.add("$userHome\\scoop\\apps\\nodejs\\current\\bin")

        return paths
    }

    private fun getUnixPaths(): List<String> {
        val userHome = System.getProperty("user.home")
        val paths = mutableListOf<String>()

        // Standard system paths
        paths.addAll(
            listOf(
                "/usr/bin",
                "/usr/local/bin",
                "/opt/local/bin"
            )
        )

        // Homebrew
        paths.addAll(
            listOf(
                "/opt/homebrew/bin",
                "$userHome/.homebrew/bin"
            )
        )

        // nvm - read default version from alias file
        val nvmDir = System.getenv("NVM_DIR") ?: "$userHome/.nvm"
        val defaultFile = File("$nvmDir/alias/default")
        if (defaultFile.exists()) {
            try {
                val defaultVersion = defaultFile.readText().trim()
                val normalizedVersion = if (defaultVersion.startsWith("v")) defaultVersion else "v$defaultVersion"
                paths.add("$nvmDir/versions/node/$normalizedVersion/bin")
            } catch (_: Exception) {
                // Ignore file read errors
            }
        }

        // fnm (current paths are correct)
        paths.addAll(
            listOf(
                "$userHome/.fnm/current/bin",
                "$userHome/.local/share/fnm/current/bin"
            )
        )

        // n (node version manager) - uses system installation approach
        val nPrefix = System.getenv("N_PREFIX") ?: "/usr/local"
        paths.add("$nPrefix/bin")
        paths.add("$userHome/.n/bin")

        // npm global
        paths.add("$userHome/.npm-global/bin")

        // Snap
        paths.add("/snap/bin")

        // Flatpak
        paths.add("/var/lib/flatpak/app/org.nodejs.node/current/active/files/bin")

        return paths
    }
}
