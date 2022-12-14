# idea-spell-check

![Build](https://github.com/BlackHole1/idea-spell-check/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/20676-cspell-check.svg)](https://plugins.jetbrains.com/plugin/20676-cspell-check)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/20676-cspell-check.svg)](https://plugins.jetbrains.com/plugin/20676-cspell-check)

<!-- Plugin description -->

To parse the CSpell configuration file and automatically add words to the IDEA dictionary to prevent warnings.

Support configuration file:
- .cspell.json
- .cSpell.json
- cspell.json
- cSpell.json
- cspell.config.js
- cspell.config.cjs
- cspell.config.json
- cspell.config.yaml
- cspell.config.yml
- cspell.yaml
- cspell.yml
- package.json `cspell` field

<!-- Plugin description end -->

![example](https://raw.githubusercontent.com/BlackHole1/idea-spell-check/main/assets/example.gif)

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Cspell Check"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/BlackHole1/idea-spell-check/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
