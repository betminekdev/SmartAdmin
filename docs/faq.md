# FAQ

## Is SmartAdmin an anti-cheat?

No. SmartAdmin is a smart staff assistant. It provides investigation signals and timelines for staff review.

## Does SmartAdmin auto-ban players?

No. SmartAdmin does not auto-ban in `v0.1.1-beta` and does not recommend punishing without review.

## Can it detect xray perfectly?

No. The mining detector is a configurable heuristic. It can highlight suspicious mining patterns, but it cannot prove xray alone.

## Does the player need a client mod?

No. SmartAdmin uses server-side data only.

## Which server software is supported?

SmartAdmin is built for Paper/Spigot-style Java servers and compiles against Paper API `1.21.11-R0.1-SNAPSHOT`.

## What Java version should I use?

Use Java 21 for Paper 1.21.x servers.

## What database does it use?

SQLite. The default file is `plugins/SmartAdmin/smartadmin.db`.

## Which command should staff use?

Use `/sa` for daily staff work. The full command is `/smartadmin`, and `/si` is also registered as an alias.

## Is v0.1.1-beta production ready?

It is a public beta. Test it on a staging server first, tune thresholds, and review staff workflows before using it on a live server.
