# SmartAdmin v0.4.0-beta - Staff Workflow Update

SmartAdmin is a smart staff assistant, not an anti-cheat. Risk scores, timelines, alerts, notes, and evidence reports support manual investigation. There are no automatic punishments or guarantees of cheat detection.

## Changes from 0.3.0-beta

- Browse older events with `/sa timeline <player> [limit] [page]`. Defaults are ten entries and page one; limits are capped at 30 entries and 1,000 pages.
- Older/newer navigation hints use the player's UUID to avoid name ambiguity. Entries within each page are chronological; page one contains the newest events.
- Timeline and exported events include full dates for investigations spanning several days.
- `/sa watch list` shows your watched players; `/sa watch clear` clears only your watches. Both require staff/admin permission and work even when live watch delivery is disabled.
- Profiles show the latest three retained positive-risk signals with dates, explicitly labelled historical rather than a breakdown of the current score.
- Added SQLite pagination and watch workflow regression tests. No detector, scoring, configuration, or database schema changes.

Main command: `/smartadmin`. Aliases: `/sa` and `/si`.

## Installation and Upgrade

1. Stop the server and back up the entire `plugins/SmartAdmin` folder.
2. Replace the old JAR with `SmartAdmin-0.4.0-beta.jar`. Keep only one SmartAdmin JAR.
3. Retain existing config and SQLite data. No table recreation or config deletion is required.
4. Start the server, check the console, then run `/sa version` and `/sa help`.
5. Test timeline pages, profile reasons, permissions, and watch isolation with two staff accounts on staging. See [the manual test checklist](https://github.com/betminekdev/SmartAdmin/blob/v0.4.0-beta/docs/manual-testing.md).

Build: `./gradlew clean build --console plain` (Windows: `.\gradlew.bat clean build --console plain`).

## Compatibility and Limitations

Java 21 bytecode and the Paper API 1.21.11 baseline are unchanged. Test your exact server build; automated checks do not certify runtime compatibility. Folia is not supported.

Timeline pages are live, not a frozen snapshot. New events and retention cleanup can shift page boundaries. Export a report to preserve the current investigation context. The words `list` and `clear` are reserved watch arguments; use a known player's UUID if their name matches either word. Historical positive-risk reasons may remain after decay or a staff reset.

Database operations are still synchronous. Benchmark on busy servers. Ordinary mining, placed ores, and legitimate links can create signals. New-player age is time since first seen, not online playtime. Staff notes follow timeline retention. Reports do not automatically redact private information.

Discord is optional, disabled by default, and best effort. Alerts may be dropped during backoff, saturation, or shutdown. Full evidence summaries are not sent. Watch sessions end on staff logout or restart.

This is a beta release. The build and 84 regression checks passed locally. Live Paper/Spigot multiplayer testing for this version remains outstanding.

## Marketplace Links

- Modrinth: _coming soon_
- Hangar: _coming soon_
- [SpigotMC](https://www.spigotmc.org/resources/smartadmin-smart-staff-assistant.135328/)
