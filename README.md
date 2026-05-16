# ServerIntel

Smart staff assistant for Minecraft servers.

**Stop guessing. Start investigating.**

ServerIntel helps staff teams understand suspicious player behavior using risk scores, player timelines, smart alerts, and investigation-focused data.

ServerIntel is not a classic anti-cheat and does not replace human moderation. It provides server-side signals and evidence to help staff make better decisions. It does not automatically ban players and it does not claim guaranteed cheat detection.

## Features

- Player risk score from 0 to 100.
- Clear risk levels: SAFE, WATCH, SUSPICIOUS, HIGH_RISK.
- Persistent player timeline with important actions.
- Suspicious mining signals for valuable ores and ore bursts.
- Staff alerts with cooldowns.
- In-memory watch mode for live staff investigation.
- SQLite storage.
- Configurable risk values, thresholds, and decay.
- Permission-based command access.
- Prepared architecture for future evidence reports.

## Commands

Main command: `/smartadmin`  
Alias: `/sa`

| Command | Description |
| --- | --- |
| `/sa help` | Shows ServerIntel commands. |
| `/sa profile <player>` | Shows player risk score, status, and recent signals. |
| `/sa timeline <player>` | Shows recent important actions. |
| `/sa watch <player>` | Toggles live watch mode for the sender. |
| `/sa alerts` | Toggles personal staff alerts. |
| `/sa reload` | Reloads configuration. |
| `/sa version` | Shows plugin version. |
| `/sa evidence <player>` | Prepared placeholder for future investigation summaries. |

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `serverintel.admin` | Full access. | op |
| `serverintel.staff` | Can use profile, timeline, watch, and alerts. | op |
| `serverintel.reload` | Can reload config. | op |
| `serverintel.alerts` | Can receive staff alerts. | op |
| `serverintel.bypass` | Excludes a player from risk scoring unless configured otherwise. | false |

## Installation

1. Build the plugin with `.\gradlew.bat clean test build`.
2. Copy `build/libs/ServerIntel-0.1.0-beta.jar` into your server `plugins` folder.
3. Start the server.
4. Edit `plugins/ServerIntel/config.yml` if needed.
5. Run `/sa reload` after configuration changes.

## Configuration Example

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
    EMERALD_ORE: 2
    DEEPSLATE_EMERALD_ORE: 2
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

storage:
  type: sqlite
  database-file: "plugins/ServerIntel/serverintel.db"
  keep-data-days: 14
```

## Manual Test Plan

- Start a Paper/Spigot server with the plugin installed.
- Confirm `plugins/ServerIntel/config.yml` is created.
- Confirm `plugins/ServerIntel/serverintel.db` is created after startup.
- Join with a player.
- Mine valuable ores such as diamond ore or ancient debris.
- Run `/sa profile <player>`.
- Run `/sa timeline <player>`.
- Toggle `/sa alerts`.
- Toggle `/sa watch <player>`.
- Test `/sa reload`.
- Mine enough configured ores to cross the alert threshold and confirm staff alerts respect cooldowns.

## Known Limitations

- ServerIntel does not detect cheat clients.
- ServerIntel does not provide 100% xray detection.
- ServerIntel does not inspect screenshots or client-side state.
- ServerIntel does not auto-punish players.
- Risk scores are investigation signals, not proof.
- Watch mode is in-memory and resets on restart.
- SQLite writes are simple and synchronous in v0.1; the storage layer is designed so this can be improved later.

## Roadmap

- Discord webhook alerts.
- Full evidence report generation.
- Inventory GUI.
- Web dashboard.
- More detectors.
- Chat risk detector improvements.
- Grief detector.
- Claim plugin integration.
- LuckPerms integration.
- PlaceholderAPI support.
- Export reports to JSON or text.
- Admin notes per player.

## Links

- GitHub: https://github.com/betminekdev/ServerIntel
- Modrinth: _coming soon_
- Hangar: _coming soon_
- SpigotMC: _coming soon_

## License

MIT License. See [LICENSE](LICENSE).
