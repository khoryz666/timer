# Timer

A personal Android tracker for daily time quotas rather than fixed time slots.

- **Engine** — focused work on a short list of projects (KEFOS, QorFinder by default),
  counted in blocks: 90 min each, a daily floor of 2 and target of 4 (6h), 24 a week,
  split roughly 60/40 between the projects.
- **Joy** — a fixed daily amount of leisure (4h by default).

Everything above is editable in Settings.

## Features

- **Today**: what's running with a live stopwatch, Engine blocks with below-floor /
  floor-met / target-met status, Joy progress, and one-tap switching between projects and Joy.
- **Notification**: always there, with the running session's stopwatch, `Engine 2/4 blocks · Joy 1h 10m/4h`,
  and up to three quick actions (Stop, Joy, projects).
- **Week** (Monday start): Engine blocks vs the weekly target, each project's share of
  Engine time vs its target share, Joy total and daily average, and a day-by-day history.
  Tap a day to see its sessions and delete a mistaken one.
- **Nudge** (optional, off by default): a gentle notice if Joy starts before the day's
  first Engine block. It never blocks anything.
- **Updates**: checks GitHub Releases on launch (at most once a day) and from Settings,
  shows the release notes, and installs the new APK through the system installer.

### How time is counted

- A session counts toward the day it **ends** on, so a session from 23:00 to 01:00 is two
  hours on the second day. The running session counts toward today.
- Engine blocks are the day's total Engine time divided by the block length, rounded down.
  Weekly blocks are the sum of each day's blocks, so leftover minutes don't carry over.
- The running session is stored as a database row without an end time, so killing the
  app or rebooting the phone loses nothing; the notification comes back by itself.

## Install

1. Download the latest `Timer-X.Y.Z.apk` from [Releases](https://github.com/khoryz666/timer/releases).
2. Open it and allow "Install unknown apps" for your browser or file manager if asked.
3. Later updates install from inside the app (Settings → Updates).

Version 2.0.0 has a new application id, so it installs as a new app alongside 1.x;
uninstall 1.x yourself.

## Develop

The toolchain comes from `flake.nix`:

```sh
direnv allow                     # or: nix develop       (JDK 21, Android SDK 35)
nix develop .#emulator           # adds the emulator and an API 35 system image
```

```sh
./gradlew installDebug           # debug build: "Timer Debug", id ...timer.debug
./gradlew testDebugUnitTest      # unit tests (JVM + Robolectric, no device needed)
./gradlew lintDebug
./gradlew assembleRelease        # minified, signed with the committed keystore
```

The debug build has its own application id, so it installs next to the release build.
Update checks are off in debug builds.

Code is organised by feature under `app/src/main/java/io/github/khoryz666/timer/`:

| Package     | What's in it                                                         |
|-------------|----------------------------------------------------------------------|
| `core`      | Clock, duration formatting, the Room database, theme and shared UI   |
| `quota`     | Pure quota math: blocks, statuses, day totals, week summaries        |
| `tracking`  | Sessions, the tracker, the foreground notification, boot receiver    |
| `projects`  | The Engine project list                                              |
| `today`, `week`, `settings` | The three screens                                    |
| `update`    | GitHub release check, download and install                           |

## Release

1. Bump `timer.version` in `gradle.properties` and add a matching `## [X.Y.Z]` section to `CHANGELOG.md`.
2. Merge to `main`, then tag and push:

   ```sh
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

The `Release` workflow checks that the tag matches `timer.version`, runs the tests, builds
the signed release APK, and publishes `Timer-X.Y.Z.apk` with the CHANGELOG section as its
notes — which is what the in-app updater shows.

Every release must be signed with `keystore/release.keystore` (see `keystore/README.md`),
or it won't install over the previous one.
