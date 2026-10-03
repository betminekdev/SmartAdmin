# Changelog

## 0.4.0-beta - Staff Workflow Update

- Added bounded timeline pagination: `/sa timeline <player> [limit] [page]`, with older/newer navigation hints.
- Timeline and exported events show full dates to distinguish investigations spanning several days.
- Added `/sa watch list` and `/sa watch clear`, scoped to the current staff member and usable while delivery is disabled.
- Profiles show the latest three retained positive-risk reasons instead of only a count, explicitly labelled historical.
- Added SQLite pagination and watch workflow regression tests. No detector, scoring, or database schema changes.

## 0.3.0-beta - Reliability Update

- Risk updates and timeline events now commit in one SQLite transaction; resets preserve offline last-seen timestamps.
- Fixed repeated diamond/debris bonuses while mining other ores. Persistent cooldowns apply per ore group and new-player window.
- Mining counts use one grouped query, exact material matching, and additional database indexes.
- Fixed chat state races, unbounded burst history, repeated link signals, and lava bucket event handling.
- Help and tab completion respect individual permissions. Known offline profiles support UUID lookup without external name resolution.
- Evidence includes retained notes independently of its recent timeline. Exports use unique filenames and a bounded async write queue.
- Watch delivery checks current permissions; sessions clear on staff logout.
- Invalid YAML reloads retain previous settings; changed database paths require restart.
- Discord now validates endpoints, disables mentions, bounds requests, uses timeouts and HTTP 429 backoff, and avoids logging webhook secrets.
- SQLite JDBC updated to 3.53.4.0. Java 21 remains the bytecode baseline.
- Added SQLite integration, mining listener, configuration, permission, export, and webhook regression tests.
- Existing tables and config keys remain supported. Back up before upgrading; manual server testing is still required.

## 0.2.0-beta - Investigation Update

### Added

- Evidence report command.
- Evidence export command.
- Top risk players command.
- Timeline limit support.
- Improved player profile output.
- Basic Discord webhook alerts.
- Improved help command.
- New permissions for investigation commands.

### Changed

- Updated release version to `0.2.0-beta`.
- Improved documentation and publishing descriptions.
- Improved config structure.

### Fixed

- Release consistency issues.
- YAML formatting issues.
- Raw GitHub multi-line file formatting.
- Version mismatch between `plugin.yml`, Gradle, and docs.

### Known Limitations

- SmartAdmin is not an anti-cheat.
- No automatic punishment system.
- No GUI yet.
- No web dashboard yet.
- Detection is based on server-side signals and requires staff review.

## v0.1.0-beta

- Initial beta release.
- Added player risk score.
- Added mining detection.
- Added player timeline.
- Added staff alerts.
- Added watch mode.
- Added SQLite storage.
- Added configurable thresholds.
- Added `/smartadmin` command with `/sa` and `/si` aliases.
- Added public beta documentation, release notes, marketplace descriptions, SVG assets, and GitHub Actions build workflow.
