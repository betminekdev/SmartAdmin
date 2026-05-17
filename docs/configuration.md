# Configuration

SmartAdmin creates `plugins/SmartAdmin/config.yml` on first startup.

After editing normal thresholds or message settings, run `/sa reload`. Restart the server after changing storage paths.

## Messages

```yaml
messages:
  prefix: "&8[&bSmartAdmin&8]&r"
```

`messages.prefix` controls the chat prefix used by plugin messages.

## Risk

```yaml
risk:
  max-score: 100
  decay-enabled: true
  decay-amount: 2
  decay-interval-minutes: 30
  score-bypassed-players: false
```

| Setting | Meaning |
| --- | --- |
| `max-score` | Upper limit for player risk. |
| `decay-enabled` | Whether risk lowers over time. |
| `decay-amount` | How much risk is removed each decay interval. |
| `decay-interval-minutes` | How often decay runs. |
| `score-bypassed-players` | Whether `smartadmin.bypass` players can still gain risk. |

## Mining

`mining.valuable-ores` maps Bukkit material names to risk points.

The burst detector adds extra risk when a player mines enough configured ore in a short time window. This is a review signal, not proof of xray.

## Signals

The beta includes lightweight TNT, lava, chat spam, and suspicious link signals.

Keep these values conservative at first. Tune them around your server rules, staff workflow, and false-positive tolerance.

## Alerts

```yaml
alerts:
  enabled: true
  threshold: 60
  high-risk-threshold: 80
  cooldown-seconds: 30
```

| Setting | Meaning |
| --- | --- |
| `threshold` | Risk score where staff alerts can begin. |
| `high-risk-threshold` | Risk score used for stronger alert urgency. |
| `cooldown-seconds` | Per-player cooldown to reduce repeated alerts. |

## Storage

```yaml
storage:
  type: sqlite
  database-file: "plugins/SmartAdmin/smartadmin.db"
  keep-data-days: 14
```

Only SQLite is supported in `v0.1.1-beta`. Timeline cleanup uses `keep-data-days`.

## Notes

```yaml
notes:
  max-length: 200
```

`notes.max-length` controls the maximum length of `/sa note <player> <message>`.

## Discord

Discord settings are present for future releases. Webhook sending is not implemented in `v0.1.1-beta`.
