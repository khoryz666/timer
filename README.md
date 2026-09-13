# Timer

A minimal Android app for tracking daily time across three categories: **Work**, **Self**, and **Sleep**.

## Features

- Track time across Work, Self, and Sleep
- Timestamp-based tracking — no background loop, no battery drain while idle
- Persistent notification with quick-switch actions (change category without opening the app)
- Resumes tracking after the app is killed/restarted or the device reboots
- Local history grouped by day
- Sessions that cross midnight are attributed to the day they end on

## Setup

Dependencies (JDK, Android SDK, build tools, emulator) are declared in `flake.nix`.

```sh
direnv allow   # or: nix develop
```

### Emulator on WSL

The emulator needs `/dev/kvm` access for hardware acceleration. If your user isn't in the `kvm` group yet:

```sh
sudo usermod -aG kvm $USER
```

Then close and reopen your WSL session (group membership only applies to new sessions).

## Develop

Inside the `nix develop` shell:

```sh
./gradlew assembleDebug         # build the debug APK
./gradlew installDebug          # build + install it on a connected device/emulator
```

The debug build is unsigned (well, debug-signed) and unminified — it's for local development only and is never what gets distributed. See [Release](#release) for the build users actually install.

## Test

```sh
./gradlew testDebugUnitTest     # unit tests (Robolectric; no device/emulator needed)
./gradlew lintDebug             # static analysis
```

Both run automatically on every push/PR via `.github/workflows/ci.yml`, alongside `assembleDebug`/`assembleRelease` as a build sanity check.

A handful of tests (`TrackerViewModelTest`, `HistoryViewModelTest`, `TimerServiceTest`) occasionally fail with a `TimeoutCancellationException` — a known, unresolved flake in the test setup (real Room/DataStore I/O racing a test coroutine dispatcher), not a product bug. Re-running the failed test passes. See `CHANGELOG.md`'s "Known issues".

## Debug

```sh
./gradlew installDebug
adb logcat --pid="$(adb shell pidof -s com.example.timer)"   # tag-free app logs; filter further as needed
```

Android Studio works too: open the project, select a device/emulator, and use its normal Run/Debug/Logcat tooling against the `app` module's debug build type.

## Release

Releases are built and signed by CI, **not built locally** — `assembleRelease` requires the committed keystore (see `keystore/README.md`) and is reproducible from any checkout, but the actual distributed APK always comes from the `v*`-tag workflow below so `versionName`/`versionCode` are set correctly and the build is the one users can verify from a tag.

To cut a release:

```sh
git push origin main            # make sure main is up to date first
git tag vX.Y.Z
git push origin vX.Y.Z
```

Pushing a `vX.Y.Z` tag triggers `.github/workflows/release.yml`, which:
1. Runs `./gradlew testReleaseUnitTest`
2. Builds `./gradlew assembleRelease -PversionName=X.Y.Z -PversionCode=N` (minified with R8, signed with the committed release keystore — an official release build, not a debug build)
3. Publishes `Timer.apk` to a new [GitHub Release](https://github.com/khoryz666/timer/releases) named after the tag, with auto-generated release notes

`versionCode` is derived from the tag as `major*1_000_000 + minor*1_000 + patch`, so tags must be plain `vMAJOR.MINOR.PATCH` (e.g. `v1.2.3`).

## Install

1. Grab the latest `Timer.apk` from [Releases](https://github.com/khoryz666/timer/releases).
2. Install it (allow "unknown apps" if prompted).
3. Later updates install over the old app without losing your history.
