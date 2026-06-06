# SmartAdmin v0.2.0-beta - Investigation Update

SmartAdmin is a smart staff assistant for Minecraft servers.

It is not a classic anti-cheat. It does not auto-ban players and does not claim perfect cheat detection. SmartAdmin collects server-side signals, creates player timelines, calculates risk scores, and helps staff investigate suspicious behavior faster.

## What Is Included

- Player risk score from 0 to 100.
- Risk levels: SAFE, WATCH, SUSPICIOUS, HIGH_RISK.
- Persistent player timeline.
- Suspicious mining signals for valuable ores and ore bursts.
- Staff alerts with cooldowns.
- In-memory watch mode.
- Staff notes in player timelines.
- Evidence report command.
- Evidence text export command.
- Top risk players command.
- Basic Discord webhook alerts.
- SQLite storage.
- Configurable thresholds, note length, evidence limits, export folder, top limits, and risk decay.
- Commands under `/smartadmin`, with `/sa` and `/si` aliases.

## New in v0.2.0-beta

- Added `/sa evidence <player>`.
- Added `/sa export <player>`.
- Added `/sa top [limit]`.
- Added `/sa timeline <player> [limit]`.
- Improved `/sa profile <player>`.
- Improved `/sa help`.
- Added basic async Discord webhook alerts.
- Added `smartadmin.evidence`, `smartadmin.export`, and `smartadmin.top` permissions.

## Installation

1. Download `SmartAdmin-0.2.0-beta.jar`.
2. Put it in your server `plugins` folder.
3. Start your Paper/Spigot server.
4. Confirm `plugins/SmartAdmin/config.yml` was created.
5. Tune thresholds for your server.
6. Use `/sa help` in-game.

## Beta Warning

This is a beta release. Test it on a staging server before using it on a live network.

The mining detector is a configurable heuristic. Treat alerts and reports as review signals, not proof.

## Known Limitations

- No auto-ban.
- No client-side detection.
- No perfect xray detection.
- No screenshot or device inspection.
- No GUI yet.
- No web dashboard yet.
- Watch mode does not persist after restart.
- SQLite is the only storage backend in v0.2.

## Marketplace Links

- Modrinth: _coming soon_
- Hangar: _coming soon_
- SpigotMC: _coming soon_
