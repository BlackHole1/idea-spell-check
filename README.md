# idea-spell-check

![Build](https://github.com/BlackHole1/idea-spell-check/workflows/Build/badge.svg) [![Version](https://img.shields.io/jetbrains/plugin/v/20676-cspell-check.svg)](https://plugins.jetbrains.com/plugin/20676-cspell-check) [![Downloads](https://img.shields.io/jetbrains/plugin/d/20676-cspell-check.svg)](https://plugins.jetbrains.com/plugin/20676-cspell-check)

<!-- Plugin description -->

Automatically parses project-level CSpell configuration files inside JetBrains IDEs and synchronizes custom vocabularies with the IDE runtime dictionary, keeping spell checking noise-free for the whole team.

<!-- Plugin description end -->

![example](https://raw.githubusercontent.com/BlackHole1/idea-spell-check/main/assets/example.gif)

## Overview

`idea-spell-check` targets JetBrains IDEs such as IntelliJ IDEA, WebStorm, and PyCharm etc. It watches CSpell configuration files as well as external dictionaries, merges them, and refreshes the IDE dictionary automatically so shared spelling rules are always respected.

## Key Features

- **Priority-aware discovery**: Mirrors the official CSpell file naming hierarchy and always picks the highest-priority configuration available in project roots, `.config`, `.vscode`, and related directories.
- **Real-time file watching**: Uses Virtual File System listeners with debounce control, so any saved change triggers a re-parse and refresh with minimal overhead.
- **Dictionary aggregation**: Reads `dictionaryDefinitions` together with `dictionaries`, fetching extra word lists from referenced files and merging them into the IDE dictionary.
- **Node.js auto-detection**: For `.js/.cjs/.mjs` configs the plugin locates a Node.js runtime automatically or lets you provide a custom executable path in settings.
- **Multi-root coverage**: Add extra search folders via the settings page to support monorepos and multi-module repositories.

## Installation

### JetBrains Marketplace

1. Open `Settings/Preferences`.
2. Navigate to `Plugins` → `Marketplace` and search for **“CSpell Check”**.
3. Click `Install`, then restart the IDE.

### Manual Installation

1. Download the [latest release](https://github.com/BlackHole1/idea-spell-check/releases/latest).
2. In `Settings/Preferences` → `Plugins`, click the gear icon.
3. Choose `Install Plugin from Disk...`, select the downloaded archive, and restart the IDE.

## Usage Guide

### Configuration Discovery

- The plugin scans the project root and any configured custom paths, applying the priority list above to determine the effective CSpell configuration.
- When a higher-priority file appears, it replaces the previously active configuration automatically.

### Dictionary Files

- A dictionary definition is loaded when either `addWords` is `true` **or** the definition name is listed inside the `dictionaries` array.
- Only when `addWords` is `false` **and** the definition is never referenced by `dictionaries` will the file be ignored.
- Blank lines and lines that begin with `#`, `//`, or `;` are skipped while reading external dictionary files.

### Node.js-backed Configurations

- Parsing `.js`, `.cjs`, or `.mjs` configs requires Node.js.
- The plugin searches common locations (PATH, nvm, Volta, fnm, Homebrew, etc.) and exposes a manual override under `Settings/Preferences | Tools | CSpell Check`.
- If no runtime is available, the plugin shows a notification and skips only the script-based configs while keeping other sources active.

### Supported Configuration Sources

- Checked in order from top to bottom to determine the active configuration.
- `.cspell.json`
- `cspell.json`
- `.cSpell.json`
- `cSpell.json`
- `.cspell.jsonc`
- `cspell.jsonc`
- `.cspell.yaml`
- `cspell.yaml`
- `.cspell.yml`
- `cspell.yml`
- `.cspell.config.json`
- `cspell.config.json`
- `.cspell.config.jsonc`
- `cspell.config.jsonc`
- `.cspell.config.yaml`
- `cspell.config.yaml`
- `.cspell.config.yml`
- `cspell.config.yml`
- `.cspell.config.mjs`
- `cspell.config.mjs`
- `.cspell.config.cjs`
- `cspell.config.cjs`
- `.cspell.config.js`
- `cspell.config.js`
- `.cspell.config.toml`
- `cspell.config.toml`
- `.config/.cspell.json`
- `.config/cspell.json`
- `.config/.cSpell.json`
- `.config/cSpell.json`
- `.config/.cspell.jsonc`
- `.config/cspell.jsonc`
- `.config/cspell.yaml`
- `.config/cspell.yml`
- `.config/.cspell.config.json`
- `.config/cspell.config.json`
- `.config/.cspell.config.jsonc`
- `.config/cspell.config.jsonc`
- `.config/.cspell.config.yaml`
- `.config/cspell.config.yaml`
- `.config/.cspell.config.yml`
- `.config/cspell.config.yml`
- `.config/.cspell.config.mjs`
- `.config/cspell.config.mjs`
- `.config/.cspell.config.cjs`
- `.config/cspell.config.cjs`
- `.config/.cspell.config.js`
- `.config/cspell.config.js`
- `config/.cspell.config.toml`
- `config/cspell.config.toml`
- `.vscode/.cspell.json`
- `.vscode/cSpell.json`
- `.vscode/cspell.json`
- `package.json` (`cspell` block including `dictionaryDefinitions`)

## Contributing

1. Clone the repository and run `./gradlew build` to compile the plugin.
2. Execute `./gradlew test` to validate the parsing logic.
3. Use the `Run IDE for UI Tests` task to launch a sandbox IDE with the plugin for interactive testing.
4. Contributions via pull requests or issues are welcome to further improve the CSpell experience on JetBrains platforms.

---
Built on top of the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
