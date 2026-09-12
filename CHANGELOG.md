# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-09-12

### Added
- Work / Self / Sleep time tracking with timestamp-based math (no background polling).
- Persistent notification with quick-switch actions for changing category without opening the app.
- Local history screen grouped by day, backed by Room.
- Midnight rollover: sessions that cross midnight are attributed to the day they end on.
- Nix flake + direnv dev environment (JDK, Android SDK, build-tools, emulator).

### Fixed
- Switching categories from the notification's quick actions no longer discards the outgoing category's elapsed time (it used to skip saving to the database, unlike switching from the in-app buttons).
- `gradlew` had lost its executable bit.

### Removed
- Unused boilerplate: `colors.xml`, unreferenced adaptive-icon drawables, unused light color scheme.
- Non-functional "Show Notification" toggle and the dead `enable-service` preference it was tangled up with — neither ever had any effect on whether the notification showed.
- `ARCHITECTURE.md` (folded into a shorter README).
