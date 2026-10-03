# SmartAdmin v0.3.0-beta - Reliability Update

SmartAdmin is a smart staff assistant, not an anti-cheat. Risk scores, timelines, alerts, notes, and evidence reports support manual investigation. There are no automatic punishments or guarantees of cheat detection.

## Highlights

- Atomic SQLite risk updates and audit events, exact ore counts, and persistent burst cooldowns.
- Bounded chat history processed on the server thread and working lava bucket signals.
- Permission-aware help and completion, with current permission checks for watch delivery.
- Offline resets preserve last-seen time; evidence includes retained notes independently of its recent timeline.
- Unique text exports written asynchronously with a bounded queue.
- Invalid YAML reloads leave previous settings active.
- Discord endpoint validation, disabled mentions, timeouts, request limits, and HTTP 429 backoff.
- SQLite JDBC 3.53.4.0 and expanded automated regression coverage.

## Installation and Upgrade

1. Stop the server and back up the entire `plugins/SmartAdmin` folder.
2. Replace the old JAR with `SmartAdmin-0.3.0-beta.jar`. Keep only one SmartAdmin JAR.
3. Retain existing config and SQLite data. No table recreation or config deletion is required.
4. Start the server, check the console, then run `/sa version` and `/sa help`.
5. Complete [the manual test checklist](docs/manual-testing.md) on staging before production.

Build: `./gradlew clean build --console plain` (Windows: `.\gradlew.bat clean build --console plain`).

## Compatibility and Limitations

Java 21 bytecode and the Paper API 1.21.11 baseline are retained. Paper 26.1+ requires Java 25; the build alone does not prove in-game compatibility. Test your exact Paper/Spigot version. Folia is not supported.

Database operations are still synchronous. Benchmark on busy servers. Ordinary mining, placed ores, and legitimate links can create signals. New-player age is time since first seen, not online playtime. Staff notes follow timeline retention. Reports do not automatically redact private information.

Discord is optional, disabled by default, and best effort. Alerts may be dropped during backoff, saturation, or shutdown. Full evidence summaries are not sent. Watch sessions end on staff logout or restart.

This is a beta candidate. Automated verification does not replace multiplayer server testing.

## Marketplace Links

- Modrinth: _coming soon_
- Hangar: _coming soon_
- SpigotMC: _coming soon_
