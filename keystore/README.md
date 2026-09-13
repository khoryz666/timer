# Release keystore

`release.keystore` is committed on purpose. This app isn't distributed through
the Play Store — it's sideloaded from GitHub Releases — so the only thing this
key needs to do is stay the same across releases, so a new APK can install as
an update over an old one instead of failing with a signature mismatch.

There's no secret here worth protecting: alias `timetracker`, store/key
password `timetracker-release`. If you fork this project, feel free to
generate your own keystore instead of reusing this one.
