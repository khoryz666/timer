# Release keystore

`release.keystore` is committed on purpose. The app is sideloaded from GitHub Releases
and updates itself from there, so the only job of this key is to stay the same: Android
installs an update over the existing app only if both are signed with the same key.

Alias `timetracker`, store and key password `timetracker-release`. Never replace this
file, or existing installs will refuse the next update.
