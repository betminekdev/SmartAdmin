# Commands

SmartAdmin uses `/smartadmin` as the main command. Staff-facing examples use the shorter `/sa` alias.

Aliases: `/sa`, `/si`

| Command | Permission | Purpose |
| --- | --- | --- |
| `/sa help` | `smartadmin.staff` or `smartadmin.admin` | Show available commands. |
| `/sa profile <player>` | `smartadmin.staff` or `smartadmin.admin` | Review risk score, risk level, last seen time, and recent signals. |
| `/sa timeline <player> [limit]` | `smartadmin.staff` or `smartadmin.admin` | Review recent important events for a player. |
| `/sa evidence <player>` | `smartadmin.evidence` or `smartadmin.admin` | Show an investigation summary. |
| `/sa export <player>` | `smartadmin.export` or `smartadmin.admin` | Export an evidence report to a text file. |
| `/sa top [limit]` | `smartadmin.top` or `smartadmin.admin` | Show highest risk players. |
| `/sa watch <player>` | `smartadmin.staff` or `smartadmin.admin` | Toggle live watch messages for one player. |
| `/sa alerts` | `smartadmin.alerts`, `smartadmin.staff`, or `smartadmin.admin` | Toggle personal alert delivery. |
| `/sa reset <player>` | `smartadmin.reset` or `smartadmin.admin` | Reset a player's risk score to `0` and add a staff action timeline event. |
| `/sa note <player> <message>` | `smartadmin.note` or `smartadmin.admin` | Add a staff note to the player's timeline without changing risk. |
| `/sa reload` | `smartadmin.reload` or `smartadmin.admin` | Reload configuration. |
| `/sa version` | `smartadmin.staff` or `smartadmin.admin` | Show the installed plugin version. |

## Usage Notes

Use `/sa profile <player>` for a quick summary before taking action.

Use `/sa timeline <player> [limit]` when staff need context around the score. Timeline entries can include joins, ore mining, ore burst signals, block placement signals, chat signals, staff notes, and risk level changes. The optional limit is clamped to a safe range.

Use `/sa evidence <player>` to show a compact investigation summary with risk, suspicious signals, recent timeline events, and a manual review recommendation.

Use `/sa export <player>` to write the evidence report to `plugins/SmartAdmin/exports`.

Use `/sa top [limit]` to list the highest risk known players. Players with `0` risk are ignored.

Use `/sa watch <player>` during manual investigation. Watch mode is in-memory and resets when the server restarts.

Use `/sa reset <player>` when a reviewed score should be cleared. The command works for known player profiles, including known offline players, and records the reset in the timeline.

Use `/sa note <player> <message>` to leave staff context in the timeline. Notes do not change risk score. The default maximum length is `200` characters.

Use `/sa reload` after changing normal thresholds or message settings. Restart the server after changing storage paths.

## Investigation Reminder

Evidence reports and exports are staff review tools. They are not proof of cheating and should not be used as automatic punishment triggers.
