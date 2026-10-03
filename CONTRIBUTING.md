# Contributing

Thanks for helping improve SmartAdmin.

## Build

Windows:

```powershell
.\gradlew.bat clean build --console plain
```

Linux/macOS:

```bash
./gradlew clean build --console plain
```

## Automated Verification

The build runs `smartAdminSelfTest` using a Java 21 toolchain. It includes the risk-level boundary checks and regression tests for real SQLite transactions/restarts, the mining listener with test player/block proxies, configuration bounds, command permissions, unique exports, chat windows, and Discord payload validation.

Run `./gradlew smartAdminSelfTest --console plain` for the focused suite. Temporary test databases are isolated and removed after the run. The harness avoids a separate JUnit worker because of the project's accented Windows path. `test NO-SOURCE` in Gradle output is expected: look for `SmartAdmin regression tests passed` from the JavaExec task.

Do not interpret these tests as a live Paper/Spigot integration test or Discord delivery test. Complete `docs/manual-testing.md` for runtime behavior changes.

## Code Style

- Keep classes focused.
- Prefer existing project patterns.
- Keep changes small and reviewable.
- Avoid unrelated formatting churn.
- Use clear names for moderation and investigation concepts.

## Project Positioning

SmartAdmin is a staff assistant, not a classic anti-cheat.

Do not add or document claims such as:

- guaranteed cheat detection
- perfect xray detection
- client-side detection
- automatic punishment accuracy

Use wording such as:

- suspicious behavior
- risk signals
- timeline evidence
- staff review
- manual investigation

## Before Opening a PR

- Run the build.
- Test relevant commands on a Paper/Spigot server when behavior changes.
- Keep plugin logic, docs, and publishing text consistent.
- Explain any config changes clearly.
