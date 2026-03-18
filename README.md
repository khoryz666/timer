# Time Tracker

A highly efficient, zero-resource time tracking Android application designed to cleanly separate and record your daily activities. It is built natively for Android, enforcing a strict dark mode aesthetic and utilizing background-friendly timestamp math rather than battery-draining timer loops.

## Features

- **Three Core Categories**: Track time across `Work`, `Self`, and `Sleep`. 
- **Zero-Resource Background Tracking**: The app records the precise start timestamp and defers math calculations until the UI is actively viewed, keeping background battery drain to an absolute zero.
- **Persistent Notification**: Keep track of your active timer with a persistent foreground service notification that is fully perfectly synchronized with the app UI.
- **Quick Actions**: Switch between Work, Self, and Sleep directly from the notification shade without ever opening the app!
- **Local History**: A History screen that safely stores your daily records grouped by date (YYYY-MM-DD), built on a robust local SQLite database.
- **Midnight Rollover Handling**: Sessions that cross midnight seamlessly attribute their full duration blocks to the correct chronological end date.
- **In-App Updater**: Check for updates directly within the app, which links to the GitHub Releases page to install the latest `.apk`.

## Installation

1. Navigate to the [Releases](https://github.com/khoryz666/timetracker/releases) page.
2. Download the latest `TimeTracker.apk` file.
3. Open the downloaded file on your Android device (you may need to allow your browser to "Install unknown apps").
4. Subsequent updates will install over the old app safely without deleting your local timer history!
