# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Real unit test coverage (`TimeFormatter`, `TrackerRepository` via Robolectric) replacing the Android Studio boilerplate tests.
- CI runs lint, unit tests, and a build check on every push/PR, not just on tagged releases.
- Bottom navigation between Timer and History, backed by a `TrackerRepository.snapshotFlow()` that both the dashboard and the notification now read from instead of separately re-deriving the same state.
- The foreground notification resumes on its own after the app process is killed and restarted, and after a device reboot if a category was left running.
- Unit test coverage for `TimeTrackerService`, `TrackerViewModel`, `HistoryViewModel`, and Compose smoke tests for the Dashboard/History screens.

### Changed
- Releases now ship a minified, R8-optimized release build (~1.6MB vs ~10.8MB debug) instead of an unminified debug build.
- `versionCode`/`versionName` are now derived from the pushed git tag instead of being hardcoded to `1`/`"1.0"`.

### Fixed
- Release APKs are now signed with a stable, committed keystore instead of a debug keystore that GitHub Actions regenerated randomly on every run — past releases could fail to install as updates over each other because of this.
- The displayed/notified elapsed time could go negative if the device's clock was set backward while a category was running.

### Known issues
- `TrackerViewModelTest`, `HistoryViewModelTest`, and `TimeTrackerServiceTest` have a real but unresolved intermittent flake (`TimeoutCancellationException` waiting on a `Flow` that mixes `UnconfinedTestDispatcher` with real Room/DataStore I/O). Re-running the failed test passes; root cause is narrowed down but not fixed - see the "test: give Robolectric flow-wait timeouts more headroom" commit for what's been tried.

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
