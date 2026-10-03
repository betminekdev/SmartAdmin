# Configuration

SmartAdmin creates `plugins/SmartAdmin/config.yml` on first startup.

After editing normal thresholds or message settings, run `/sa reload`. Restart the server after changing storage paths.

Malformed YAML or a changed database path rejects the reload and retains the previous settings. Missing keys use defaults; your file is not rewritten.

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
| `max-score` | Upper limit for risk, clamped to 1-100. Risk levels always use the fixed 0-100 scale. |
| `decay-enabled` | Whether risk lowers over time. |
| `decay-amount` | How much risk is removed each decay interval. |
| `decay-interval-minutes` | How often decay runs. |
| `score-bypassed-players` | Whether `smartadmin.bypass` players can still gain risk. |

## Mining

`mining.valuable-ores` maps Bukkit material names to risk points.

The burst detector adds extra risk when a player mines enough configured ore in a short time window. This is a review signal, not proof of xray.

Each ore group receives at most one bonus per window, including across restarts. Diamond and deepslate diamond share a group. The historical key `mining.new-player.max-playtime-minutes` measures elapsed time since first seen, not online playtime.

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

Only SQLite is supported. Timeline cleanup uses `keep-data-days` at startup and includes staff notes. Profile scores and alert preferences are retained. Stop the server before backing up its database and WAL files.

## Notes

```yaml
notes:
  max-length: 200
```

`notes.max-length` controls the maximum length of `/sa note <player> <message>`.

The limit is clamped to 1-2000. Notes are plain text: formatting and control codes are removed. Do not enter secrets or private information that should not appear in reports.

## Evidence

```yaml
evidence:
  enabled: true
  max-timeline-events: 15
  include-recommendation: true
```

`evidence.max-timeline-events` controls how many recent events evidence reports include.

## Export

```yaml
export:
  enabled: true
  folder: "plugins/SmartAdmin/exports"
  format: "txt"
```

Evidence exports are written as text reports. Do not use exports as automatic punishment proof.

Only `txt` is supported. Exports have unique filenames and at most four outstanding writes. Keep the export folder private and redact reports before sharing.

## Top Risk

```yaml
top:
  default-limit: 10
  max-limit: 25
```

These values control `/sa top` and `/sa top <limit>`.

`top.max-limit` is capped at 100 to bound output.

## Discord

```yaml
discord:
  enabled: false
  webhook-url: ""
  high-risk-only: true
  send-evidence-summary: false
```

Discord webhook alerts are disabled by default. When enabled, SmartAdmin sends simple alert messages asynchronously after the normal staff alert threshold and cooldown checks pass.

Use an official HTTPS Discord webhook URL without query parameters. Custom proxies are not supported. Mentions are disabled. Requests have a ten-second deadline and five-second connection timeout, with at most four in flight. HTTP 429 pauses sends using `Retry-After` (1-3600 seconds, fallback 60). Delivery is best effort, not a persistent queue.

`send-evidence-summary` is reserved and has no effect. Only the triggering signal is included; notes and full evidence are not sent. Keep the webhook token secret.
