# Manual Testing

Use this checklist on a local or staging Paper/Spigot server before publishing a release.

## Fresh Startup

- Stop the server.
- Place `SmartAdmin-0.3.0-beta.jar` in the `plugins` folder.
- Start the server.
- Confirm SmartAdmin enables without startup errors.
- Confirm `plugins/SmartAdmin/config.yml` is generated.
- Confirm `plugins/SmartAdmin/smartadmin.db` is generated.

## Basic Commands

- Run `/sa help`.
- Run `/sa version`.
- Run `/sa alerts` twice and confirm it toggles.
- Run `/sa note <player> reviewed during test` and confirm the note appears in `/sa timeline <player>`.
- Run `/sa reset <player>` and confirm the risk score resets to `0`.
- Run `/sa reload`.

## Player Investigation

- Join with a test player.
- Run `/sa profile <player>`.
- Run `/sa timeline <player>`.
- Run `/sa timeline <player> 5`.
- Run `/sa evidence <player>`.
- Run `/sa export <player>` and confirm a text file is created in `plugins/SmartAdmin/exports`.
- Run `/sa top`.
- Run `/sa watch <player>` as a staff member.
- Perform important actions with the watched player and confirm watch messages appear.

## Mining Signals

- Mine configured valuable ores such as diamond ore or ancient debris.
- Confirm timeline entries are created.
- Confirm `/sa profile <player>` shows an updated risk score.
- Mine enough configured ores to cross the burst threshold.
- Confirm a burst signal appears in timeline output.

## Alerts

- Enable alerts with `/sa alerts`.
- Raise a test player's risk past the configured alert threshold.
- Confirm staff receive an alert.
- Confirm repeated alerts respect the configured cooldown.
- Leave Discord disabled and confirm no webhook errors are logged.

## Persistence

- Stop and restart the server.
- Run `/sa profile <player>` again.
- Run `/sa timeline <player>` again.
- Confirm stored profile and timeline data are still available.

## 0.3 Regression Checklist

- Upgrade a backed-up 0.2 installation without deleting its config/database; verify scores, notes, and preferences remain.
- Cross the diamond threshold, then mine iron repeatedly. Iron must not repeat the diamond bonus.
- Restart inside the burst window and mine another diamond. The cooldown must remain active.
- Cross the ancient debris threshold separately; its independent bonus must still work.
- Reach the risk cap; further actions must have zero delta and no additional risk-increase alert.
- Reset an offline player and confirm last-seen time is unchanged and the staff action is recorded.
- Add a note, then more events than the evidence limit; evidence must still include the retained note.
- Export twice quickly and confirm distinct files with preserved contents.
- Test note-only permissions: help/completion must not offer reset/reload or their target completion.
- Revoke staff/admin permission while watching; events must stop. Reconnect and confirm watch sessions cleared.
- Toggle alerts using only `smartadmin.alerts`.
- Break YAML on staging, reload, and confirm old thresholds remain. Restore the file afterward.
- Change the database path and reload; it must request a restart.
- Cancel mining/lava placement using a protection plugin; no corresponding signal should be recorded.
- Send rapid chat and links from multiple players; check cooldowns and console thread errors.
- Test optional Discord in a private channel, including invalid URL, no mentions, and network failures.
- Measure tick time under representative load; the database layer is still synchronous.
- Record exact server and Java versions. Automated tests do not replace multiplayer testing.
