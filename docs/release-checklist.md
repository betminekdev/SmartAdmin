# Release Checklist

Use this checklist before publishing a SmartAdmin beta build.

## Build

- Run `.\gradlew.bat clean build --console plain` on Windows.
- Run `./gradlew clean build --console plain` on Linux/macOS or CI.
- Confirm `build/libs/SmartAdmin-0.2.0-beta.jar` exists.
- Confirm GitHub Actions passes on `main`.

## Manual Server Test

- Start a fresh Paper/Spigot test server with the JAR installed.
- Confirm `plugins/SmartAdmin/config.yml` is created.
- Confirm `plugins/SmartAdmin/smartadmin.db` is created.
- Join with a player account.
- Run `/smartadmin help`, `/sa help`, and `/si help`.
- Run `/sa version`.
- Run `/sa evidence <player>`.
- Run `/sa export <player>`.
- Run `/sa top`.
- Test `/sa note <player> <message>`.
- Test `/sa reset <player>`.
- Toggle `/sa alerts`.
- Toggle `/sa watch <player>`.
- Mine valuable ores and verify `/sa profile <player>` and `/sa timeline <player>`.
- Test `/sa timeline <player> 5`.
- Confirm Discord disabled behavior does not log webhook errors.
- Test `/sa reload`.
- Restart the server and confirm profiles/timeline data persist.

## Public Release

- Create or update GitHub release `v0.2.0-beta`.
- Upload the built JAR.
- Paste or adapt `RELEASE_NOTES.md`.
- Add Modrinth, Hangar, and SpigotMC links when available.
- Use the matching platform description from `publishing/`.

## Messaging Review

- Do not claim perfect cheat detection.
- Do not call SmartAdmin an auto-ban anti-cheat.
- Keep the beta warning visible.
- Present suspicious behavior as signals for manual review.
