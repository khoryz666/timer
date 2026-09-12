# Time Tracker

A minimal Android app for tracking daily time across three categories: **Work**, **Self**, and **Sleep**.

## Features

- Track time across Work, Self, and Sleep
- Timestamp-based tracking — no background loop, no battery drain while idle
- Persistent notification with quick-switch actions (change category without opening the app)
- Local history grouped by day
- Sessions that cross midnight are attributed to the day they end on

## Setup

Dependencies (JDK, Android SDK, build tools, emulator) are declared in `flake.nix`.

```sh
direnv allow   # or: nix develop
```

Then build/run like any Android project:

```sh
./gradlew assembleDebug
```

### Emulator on WSL

The emulator needs `/dev/kvm` access for hardware acceleration. If your user isn't in the `kvm` group yet:

```sh
sudo usermod -aG kvm $USER
```

Then close and reopen your WSL session (group membership only applies to new sessions).

## Install

1. Grab the latest `TimeTracker.apk` from [Releases](https://github.com/khoryz666/timetracker/releases).
2. Install it (allow "unknown apps" if prompted).
3. Later updates install over the old app without losing your history.
