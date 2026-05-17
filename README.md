![SmartAdmin banner](assets/banner.svg)

# SmartAdmin

[![Build](https://github.com/betminekdev/SmartAdmin/actions/workflows/build.yml/badge.svg)](https://github.com/betminekdev/SmartAdmin/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Paper/Spigot](https://img.shields.io/badge/Paper%2FSpigot-1.21.x-38bdf8)
![Version](https://img.shields.io/badge/version-0.1.1--beta-f59e0b)
![License](https://img.shields.io/badge/license-MIT-green)

Smart staff assistant for Minecraft servers.

**Stop guessing. Start investigating.**

SmartAdmin helps staff teams review suspicious player behavior with risk scores, player timelines, staff alerts, and watch mode.

> **Beta warning:** SmartAdmin `v0.1.1-beta` is an early public beta. Test it on a staging server first and tune thresholds for your community.

SmartAdmin is not a classic anti-cheat and does not replace human moderation. It provides server-side signals and timeline data for staff review. It does not auto-ban players and it does not claim guaranteed cheat detection.

## Features

- Player risk score from `0` to `100`
- Risk levels: `SAFE`, `WATCH`, `SUSPICIOUS`, `HIGH_RISK`
- Player timeline with important actions
- Mining signals for valuable ores and ore bursts
- Staff alerts with cooldowns
- In-memory watch mode for live investigation
- SQLite storage
- Configurable risk values, thresholds, and decay
- Permission-based command access

## Preview

| Profile | Timeline |
| --- | --- |
| ![Profile preview](assets/preview-profile.svg) | ![Timeline preview](assets/preview-timeline.svg) |

| Alert | Watch |
| --- | --- |
| ![Alert preview](assets/preview-alert.svg) | ![Watch preview](assets/preview-watch.svg) |

## Installation

1. Download `SmartAdmin-0.1.1-beta.jar` from the GitHub release.
2. Stop your server.
3. Place the JAR in the server `plugins` folder.
4. Start the server.
5. Confirm `plugins/SmartAdmin/config.yml` and `plugins/SmartAdmin/smartadmin.db` were created.
6. Edit `plugins/SmartAdmin/config.yml` if needed.
7. Run `/sa reload` after safe config changes, or restart after changing storage settings.

## Commands

Main command: `/smartadmin`  
Preferred short command: `/sa`  
Additional alias: `/si`

| Command | Description |
| --- | --- |
| `/sa help` | Shows SmartAdmin commands. |
| `/sa profile <player>` | Shows risk score, status, and recent signals. |
| `/sa timeline <player>` | Shows recent timeline events. |
| `/sa watch <player>` | Toggles live watch mode for the sender. |
| `/sa alerts` | Toggles personal staff alerts. |
| `/sa reset <player>` | Resets a player's risk score to `0` and records a staff action. |
| `/sa note <player> <message>` | Adds a staff note to the player's timeline. |
| `/sa reload` | Reloads configuration. |
| `/sa version` | Shows plugin version. |
| `/sa evidence <player>` | Placeholder for future investigation reports. |

See [docs/commands.md](docs/commands.md) for details.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `smartadmin.admin` | `op` | Full access to SmartAdmin. |
| `smartadmin.staff` | `op` | Access to profile, timeline, watch, alerts, help, version, and evidence placeholder. |
| `smartadmin.reload` | `op` | Access to `/sa reload`. |
| `smartadmin.alerts` | `op` | Allows receiving SmartAdmin alerts. |
| `smartadmin.reset` | `op` | Allows resetting player risk scores. |
| `smartadmin.note` | `op` | Allows adding staff notes to player timelines. |
| `smartadmin.bypass` | `false` | Excludes a player from risk scoring unless configured otherwise. |

See [docs/permissions.md](docs/permissions.md) for setup guidance.

## Configuration Preview

```yaml
risk:
  max-score: 100
  decay-enabled: true
  decay-amount: 2
  decay-interval-minutes: 30

mining:
  enabled: true
  valuable-ores:
    DIAMOND_ORE: 3
    DEEPSLATE_DIAMOND_ORE: 3
    ANCIENT_DEBRIS: 5
  burst-detection:
    enabled: true
    time-window-minutes: 10
    diamond-threshold: 10
    ancient-debris-threshold: 6
    extra-risk: 15

alerts:
  enabled: true
  threshold: 60
  high-risk-threshold: 80
  cooldown-seconds: 30

notes:
  max-length: 200
```

See [docs/configuration.md](docs/configuration.md) before tuning thresholds.

## Risk Levels

| Score | Level | Meaning |
| --- | --- | --- |
| `0-25` | `SAFE` | No major current concern. |
| `26-50` | `WATCH` | Worth keeping an eye on. |
| `51-75` | `SUSPICIOUS` | Review timeline and watch manually. |
| `76-100` | `HIGH_RISK` | Strong review priority, still not proof. |

## Example Alert

```text
[SmartAdmin] PlayerName reached Risk 64/100.
Reason: High-value ore burst detected.
Actions: /sa profile PlayerName | /sa timeline PlayerName | /sa watch PlayerName
```

## Example Timeline

```text
[18:02] Joined server
[18:06] Broke DEEPSLATE_DIAMOND_ORE at world - X:120 Y:-54 Z:300 (+3 risk)
[18:10] High ore burst detected: 11 diamond ores in 10 minutes (+15 risk)
[18:15] Risk changed to 68 - Status: SUSPICIOUS
```

## Limitations

- SmartAdmin does not detect cheat clients.
- SmartAdmin does not provide perfect xray detection.
- SmartAdmin does not inspect screenshots or client-side state.
- SmartAdmin does not auto-punish players.
- Risk scores are investigation signals, not proof.
- Watch mode is in-memory and resets on restart.
- SQLite storage is the only storage backend in `v0.1.1-beta`.

See [docs/detection.md](docs/detection.md) for detection philosophy.

## Build

Windows:

```powershell
.\gradlew.bat clean build --console plain
```

Linux/macOS:

```bash
./gradlew clean build --console plain
```

The JAR is created at:

```text
build/libs/SmartAdmin-0.1.1-beta.jar
```

## Roadmap

- Discord webhook alerts
- Evidence report generation
- Inventory GUI
- Web dashboard
- More detectors
- Chat risk detector improvements
- Grief detector
- Claim plugin integration
- LuckPerms integration
- PlaceholderAPI support
- Export reports to JSON or text
- Admin notes per player

## Links

- GitHub: https://github.com/betminekdev/SmartAdmin
- Modrinth: _coming soon_
- Hangar: _coming soon_
- SpigotMC: _coming soon_

## License

MIT License. See [LICENSE](LICENSE).
