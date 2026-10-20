# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.1.1] - 2026-10-21

### Changed
- Today: when nothing is running, "Not tracking" sits beside the date instead of
  taking a card of its own; the stopwatch card only appears while something runs.

### Removed
- The Projects note in Settings; projects are edited on the Week screen.

## [2.1.0] - 2026-10-21

Day types and three focus tracks. Updating from 2.0 keeps all data: Engine time
becomes Project time, and 2.0's projects and shares become the starting weekly plan.

### Added
- Day types: Light, Class and Deep, picked on Today with a three-stop slider. Each sets
  the day's targets; a day without one keeps the latest earlier choice.
- A once-a-day notification (from 07:00 by default) that asks for the day type, with
  Light / Class / Deep buttons.
- NeetCode and Reading tracks next to Project, each with a target per day type.
- Per-week project lists, edited on the Week screen: changes apply from the current
  week on, and earlier weeks keep their own list and shares.

### Changed
- Engine is now the Project track; its floor and target are set per day type.
- Blocks are 60 minutes by default (they were 90). Each track counts its own blocks.
- Weekly targets are the sum of each day's targets instead of a separate setting.
- The notification puts the day type in its header, shows `Project 2/5 · Neet 1/1 ·
  Read 0/2` collapsed and one line per track expanded. Its buttons are Stop, the track
  furthest from its target, and Joy.
- The project list and shares moved from Settings to the Week screen. A fresh install
  starts with QorFinder 80% / KEFOS 20%.

## [2.0.0] - 2026-10-15

A rewrite around daily quotas. It has a new application id, so it installs as a
new app: uninstall 1.x yourself. There is no data migration.

### Added
- Engine and Joy pillars replace Work / Self / Sleep. Engine time goes to a
  project and is counted in blocks (default 90 min, floor 2, target 4 a day,
  24 a week); Joy has a daily target (default 4h).
- Today screen: live stopwatch for the running session, Engine blocks with
  below-floor / floor-met / target-met status, Joy progress, one-tap switching.
- Week screen (Monday start): Engine blocks vs target, project split vs target
  share, Joy total, and a daily history where you can delete a mistaken session.
- Settings: block length, floor, daily and weekly targets, Joy target, and the
  project list with each project's target share (60/40 by default).
- Optional nudge, off by default, when Joy starts before the first Engine block.
- In-app updates: checks GitHub Releases on launch (at most once a day) and from
  Settings, shows the release notes, and installs through the system installer.
- New app icon, with a themed-icon layer.

### Changed
- The notification shows the running session's stopwatch and Engine progress
  (`Engine 2/4 blocks · Joy 1h 10m/4h`), with Stop / Joy / project actions.
- Sessions are stored with start and end times, so a session that crosses
  midnight counts toward the day it ends on, and tracking survives the app being
  killed or the phone rebooting. The notification also returns after an update.
- Light and dark themes with fixed colours for each pillar.
- The app version lives in `gradle.properties`; releases fail if the tag doesn't
  match, and use the CHANGELOG section as their notes.
- The default Nix shell only has what the build needs (453 MB of SDK instead of
  5.1 GB); the emulator moved to `nix develop .#emulator`.
- Minimum Android version is now 8.0 (API 26).

### Removed
- The Work / Self / Sleep categories, Reset and Save buttons, and the old history screen.
- navigation-compose, material-icons-extended and other unused dependencies;
  committed `.idea/` files and template boilerplate.

### Fixed
- The intermittently failing ViewModel and service tests. They waited with
  timeouts on flows fed by real Room and DataStore I/O on background threads,
  racing the test dispatcher and wall-clock time. The quota logic is now pure
  and tested with a fixed clock, and the tracker test only awaits suspend calls
  against an in-memory database, so nothing depends on timing. The CI
  retry-once workaround is gone.

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
