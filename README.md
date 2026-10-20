# Timer

A personal Android tracker for daily time quotas rather than fixed time slots.

Three focus tracks, each counted in 60-minute blocks, plus Joy:

- **Project** — work on this week's projects (QorFinder and KEFOS by default, 80/20).
  It has a daily floor as well as a target.
- **NeetCode** and **Reading** — a daily target each.
- **Joy** — a fixed daily amount of leisure (4h by default).

Each day is a **Light**, **Class** or **Deep** day, and the day type sets that day's targets:

| Day type | Project floor / target | NeetCode | Reading |
|----------|------------------------|----------|---------|
| Light    | 1 / 3                  | 0        | 0       |
| Class    | 2 / 3                  | 1        | 1       |
| Deep     | 3 / 5                  | 1        | 2       |

The block length, each day type's targets and the Joy target are editable in Settings.

## Features

- **Today**: a three-stop slider (Light · Class · Deep) for today's day type, what's running
  with a live stopwatch, Project blocks with below-floor / floor-met / target-met status and
  a button per project, NeetCode and Reading blocks, Joy progress, and one-tap switching.
- **Day-type prompt**: once a day (from 07:00 by default, set in Settings) a notification
  asks what kind of day it is, with Light / Class / Deep buttons. Until you pick, the day uses
  the last day type you chose.
- **Notification**: always there. Collapsed it shows what's running with a stopwatch, the day
  type, and `Project 2/5 · Neet 1/1 · Read 0/2`; expanded, one line per track and Joy. Its
  three buttons are Stop, the track furthest from its target, and Joy.
- **Week** (Monday start): each track's blocks vs its weekly target (the sum of the days'
  targets), this week's projects with their share of Project time vs their target share,
  Joy total and daily average, and a day-by-day history with each day's type. Tap a day to
  see its sessions and delete a mistaken one.
- **Projects per week**: add, rename, re-share or remove projects for the current week on
  the Week screen. A new week starts with last week's list, and earlier weeks keep theirs.
- **Nudge** (optional, off by default): a gentle notice if Joy starts before the day's
  first focus block. It never blocks anything.
- **Updates**: checks GitHub Releases on launch (at most once a day) and from Settings,
  shows the release notes, and installs the new APK through the system installer.

### How time is counted

- A session counts toward the day it **ends** on, so a session from 23:00 to 01:00 is two
  hours on the second day. The running session counts toward today.
- Each track's blocks are its total time that day divided by the block length, rounded down.
  Minutes don't pool across tracks or carry over between days.
- A day without its own day type keeps the latest earlier one, so the rest of the current
  week is planned with today's day type until you change it.
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
| `day`       | Day types (Light/Class/Deep) and the daily prompt                    |
| `tracking`  | Sessions, the tracker, the foreground notification, boot receiver    |
| `projects`  | Projects and their per-week plans                                    |
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
