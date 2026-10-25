<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# idea-spell-check Changelog

## [Unreleased]

## [2.1.0] - 2026-10-25

- chore: sync with plugin template and reduce lookup overhead by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/179
- fix(parse): read Node.js output while the process runs by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/180
- fix(node): keep symlinked Node.js paths as discovered by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/181
- fix(watch): only use config files at search roots by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/182
- fix(words): keep a separate word list per project by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/183
- refactor: serialize config state changes on the service scope by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/184
- chore: remove unused extension point declaration by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/185
- refactor(settings): build the settings page with Kotlin UI DSL by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/186
- fix(parse): report config files that fail to parse by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/188
- fix(parse): load JS configs with dynamic import() by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/189
- chore(deps): drop unused ktoml-file by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/190
- refactor(deps): parse YAML configs with the bundled SnakeYAML by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/191
- refactor(deps): use the kotlinx libraries bundled with the IDE by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/192
- feat(dict): offer project words as typo suggestions by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/193
- chore: bump version to 2.1.0 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/195

## [0.1.2] - 2025-09-05

- Changelog update - `v0.1.1` by @github-actions[bot] in https://github.com/BlackHole1/idea-spell-check/pull/51
- fix(gradle): compatibility range is `223.0 — 223.*` by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/52
- chore: bump version to 0.1.2 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/53

## [0.1.1] - 2025-09-05

- Changelog update - `v0.1.0` by @github-actions[bot] in https://github.com/BlackHole1/idea-spell-check/pull/47
- chore: support >= 223 build version by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/49
- chore: bump version to 0.1.1 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/50

## [0.1.0] - 2025-05-20

- Changelog update - `v0.0.12` by @github-actions in https://github.com/BlackHole1/idea-spell-check/pull/45
- Add configurable custom path lookup by @pfouilloux in https://github.com/BlackHole1/idea-spell-check/pull/46
- @pfouilloux made their first contribution in https://github.com/BlackHole1/idea-spell-check/pull/46

## [0.0.12] - 2025-04-02

- Changelog update - `v0.0.11` by @github-actions in https://github.com/BlackHole1/idea-spell-check/pull/41
- chore(version): support 2025.1 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/43
- chore(version): upgrade version to v0.0.12 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/44

## [0.0.11] - 2024-11-14

- fix(ci): no space left on device by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/39
- chore(version): support 2024.3 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/38
- chore(version): upgrade version to v0.0.11 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/40

## [0.0.9] - 2024-03-20

- Changelog update - `v0.0.8` by @github-actions in https://github.com/BlackHole1/idea-spell-check/pull/29
- fix(ci): cannot build by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/31
- chore(version): support 2024.1 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/30
- chore(version): upgrade version to v0.0.9 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/32

## [0.0.8] - 2023-12-07

- Changelog update - `v0.0.7` by @github-actions in https://github.com/BlackHole1/idea-spell-check/pull/25
- chore(version): support 2023.3 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/27
- chore(version): upgrade version to v0.0.8 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/28

## [0.0.7] - 2023-08-02

- chore(version): support 2023.2 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/23
- chore(version): upgrade version to v0.0.7 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/24

## [0.0.6] - 2023-07-18

- fix(yaml): not support $ prefix key by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/20
- chore(version): upgrade version to v0.0.6 by @BlackHole1 in https://github.com/BlackHole1/idea-spell-check/pull/21

## [0.0.3]

### Docs

- Improve README.md

## [0.0.2]

### Chore

- Improve Plugin icon

## [0.0.1]

### Added

- Init Project

[Unreleased]: https://github.com/BlackHole1/idea-spell-check/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/BlackHole1/idea-spell-check/compare/v0.1.2...v2.1.0
[0.1.2]: https://github.com/BlackHole1/idea-spell-check/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/BlackHole1/idea-spell-check/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.12...v0.1.0
[0.0.12]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.11...v0.0.12
[0.0.11]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.9...v0.0.11
[0.0.9]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.3...v0.0.6
[0.0.3]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/BlackHole1/idea-spell-check/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/BlackHole1/idea-spell-check/commits/v0.0.1
