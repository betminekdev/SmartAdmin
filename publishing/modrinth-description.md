# SmartAdmin

**Smart staff assistant for Minecraft servers.**

**Stop guessing. Start investigating.**

SmartAdmin helps staff teams review suspicious player behavior using risk scores, player timelines, staff alerts, watch mode, staff notes, and evidence reports.

> SmartAdmin is not a classic anti-cheat. It does not auto-ban players and does not claim perfect cheat detection. It provides server-side signals for manual staff review.

## Features

- Player risk score from `0` to `100`
- Risk levels: `SAFE`, `WATCH`, `SUSPICIOUS`, `HIGH_RISK`
- Timeline of important player actions
- Suspicious mining signals and ore burst detection
- Staff alerts with cooldowns
- Watch mode for live investigation
- Evidence reports and text exports
- Top risk players command
- Staff notes in timelines
- Basic Discord webhook alerts
- SQLite storage
- Configurable thresholds and risk decay

## Commands

Use `/sa` for daily staff work.

Main command: `/smartadmin`  
Aliases: `/sa`, `/si`

Key commands include `/sa profile`, `/sa timeline`, `/sa evidence`, `/sa export`, `/sa top`, `/sa note`, and `/sa reset`.

## Beta Notice

SmartAdmin `v0.2.0-beta` is an early beta. Test it on a staging server before production use and tune thresholds for your server.

## Links

- GitHub: https://github.com/betminekdev/SmartAdmin
- Hangar: _coming soon_
- SpigotMC: _coming soon_
