# Commands

SmartAdmin uses `/smartadmin` as the main command. Staff-facing examples use the shorter `/sa` alias.

Aliases: `/sa`, `/si`

| Command | Permission | Purpose |
| --- | --- | --- |
| `/sa help` | `smartadmin.staff` or `smartadmin.admin` | Show available commands. |
| `/sa profile <player>` | `smartadmin.staff` or `smartadmin.admin` | Review risk score, risk level, last seen time, and recent signals. |
| `/sa timeline <player>` | `smartadmin.staff` or `smartadmin.admin` | Review recent important events for a player. |
| `/sa watch <player>` | `smartadmin.staff` or `smartadmin.admin` | Toggle live watch messages for one player. |
| `/sa alerts` | `smartadmin.alerts`, `smartadmin.staff`, or `smartadmin.admin` | Toggle personal alert delivery. |
| `/sa reset <player>` | `smartadmin.reset` or `smartadmin.admin` | Reset a player's risk score to `0` and add a staff action timeline event. |
| `/sa note <player> <message>` | `smartadmin.note` or `smartadmin.admin` | Add a staff note to the player's timeline without changing risk. |
| `/sa reload` | `smartadmin.reload` or `smartadmin.admin` | Reload configuration. |
| `/sa version` | `smartadmin.staff` or `smartadmin.admin` | Show the installed plugin version. |
| `/sa evidence <player>` | `smartadmin.staff` or `smartadmin.admin` | Placeholder for future evidence reports. |

## Usage Notes

Use `/sa profile <player>` for a quick summary before taking action.

Use `/sa timeline <player>` when staff need context around the score. Timeline entries can include joins, ore mining, ore burst signals, block placement signals, chat signals, and risk level changes.

Use `/sa watch <player>` during manual investigation. Watch mode is in-memory and resets when the server restarts.

Use `/sa reset <player>` when a reviewed score should be cleared. The command works for known player profiles, including known offline players, and records the reset in the timeline.

Use `/sa note <player> <message>` to leave staff context in the timeline. Notes do not change risk score. The default maximum length is `200` characters.

Use `/sa reload` after changing normal thresholds or message settings. Restart the server after changing storage paths.

## Evidence Placeholder

`/sa evidence <player>` is reserved for future investigation reports. In `v0.1.1-beta`, use profile and timeline commands for review data.
