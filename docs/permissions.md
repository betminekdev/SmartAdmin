# Permissions

SmartAdmin keeps permissions simple for the beta release.

| Permission | Default | Description |
| --- | --- | --- |
| `smartadmin.admin` | `op` | Full access to all SmartAdmin commands. |
| `smartadmin.staff` | `op` | Access to profile, timeline, watch, alerts, help, version, and evidence placeholder. |
| `smartadmin.reload` | `op` | Access to `/sa reload`. |
| `smartadmin.alerts` | `op` | Allows a player to receive staff alerts. |
| `smartadmin.reset` | `op` | Allows resetting a player's risk score. |
| `smartadmin.note` | `op` | Allows adding staff notes to player timelines. |
| `smartadmin.bypass` | `false` | Excludes a player from risk scoring unless config allows scoring bypassed players. |

## Recommended Staff Setup

Give trusted moderators:

```text
smartadmin.staff
smartadmin.alerts
smartadmin.note
```

Give administrators:

```text
smartadmin.admin
smartadmin.reload
smartadmin.reset
```

Give trusted builders, owners, or test accounts `smartadmin.bypass` only when you do not want them included in risk scoring.

## Bypass Behavior

By default, players with `smartadmin.bypass` can still create timeline entries, but positive risk points are not applied.

Set this option if bypassed players should still be scored:

```yaml
risk:
  score-bypassed-players: true
```
