# Manual Testing

Use this checklist on a local or staging Paper/Spigot server before publishing a release.

## Fresh Startup

- Stop the server.
- Place `SmartAdmin-0.1.1-beta.jar` in the `plugins` folder.
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

## Persistence

- Stop and restart the server.
- Run `/sa profile <player>` again.
- Run `/sa timeline <player>` again.
- Confirm stored profile and timeline data are still available.
