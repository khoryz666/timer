# Architecture & Tech Stack

This document outlines the technical implementation and design patterns utilized in this Time Tracker.

## Tech Stack
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Local Database**: Android Room (SQLite Abstraction)
- **State Persistence**: Android DataStore (Preferences)
- **Background Processes**: Android Foreground Service
- **Build System**: Gradle (Kotlin DSL)

## Architecture: MVVM (Model-View-ViewModel)

### 1. The View (Jetpack Compose)
The UI is built entirely declaratively using Jetpack Compose. It watches `StateFlow` streams exposed by the ViewModel. When state changes (e.g., a timer increments or the database pushes a new daily record), the Compose UI elegantly recomposes only the changed components.

### 2. The ViewModel (`TrackerViewModel`)
This acts as the brain. It takes UI intents (button clicks) and translates them into data operations. It calculates the active ticking duration by subtracting the saved `START_TIME` from `System.currentTimeMillis()` every second, keeping logic out of the UI layer.

### 3. The Model Layer (Room & DataStore)
- **Room Database (`TimeRecord`, `TimeRecordDao`)**: Used for persistent, long-term storage of the completed time tracked each day.
- **DataStore (`TrackerPreferences`)**: Stores the ephemeral "current" session state securely and asynchronously, preventing UI thread blocking. This holds data like what timer is currently active and the exact millisecond it was pressed.

## The Zero-Resource Math
Classic timer apps run a `while(true)` loop that runs every second in the background, keeping the CPU awake and draining battery. 
This application utilizes **Timestamp Tracking**. When you start a timer, it simply logs `T-0` into DataStore. The app and service then go to sleep. When the user opens the app or pulls down the notification shade, the UI draws itself and calculates `NOW - T-0`. It gives the complete illusion of an active timer but requires no background processing power whatsoever.

## Notification Synchronization
The `TimeTrackerService` relies heavily on Kotlin Coroutine `combine` functions. It actively listens to a Flow from DataStore (containing the session start time) AND a Flow from the Room Database (containing the previously accumulated time for the day). By summing these two streams together, the notification stays in perfect parity with the inside of the app.
