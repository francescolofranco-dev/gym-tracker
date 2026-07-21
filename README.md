# Gym Tracker

Android workout tracker for logging gym sessions, sets, reps, weight, notes, and progress over time.

## Features

- Start blank sessions or reusable template-based workouts.
- Track sets with reps, weight, skipped sets, exercise notes, and session notes.
- Support unilateral exercises with separate left/right sets and per-side planning.
- Resume active sessions with an accurate workout timer, plus editable session timing.
- Review past sessions by month with duration, sets, exercises, and volume.
- See exercise-specific stats, progress charts, session history, and personal records.
- Compare effective muscle sets or tonnage over selectable 7, 28, and 90-day ranges.
- Back up and preview restores from files or Google Drive, with one-step restore recovery.
- Configure display units, templates, notifications, and keep-screen-on behavior.

## Tech Stack

- Kotlin and Jetpack Compose
- Material 3
- Room
- Hilt
- DataStore
- WorkManager
- Google Identity Services authorization and Google Drive backup support

## Getting Started

Requirements:

- Android Studio
- JDK 17
- Android SDK with API 37

Run from Android Studio, or use Gradle:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

### Google Drive authorization

Drive backup requires the Google Drive API and an Android OAuth client in Google Cloud Console.
Register package `dev.francescolofranco.gymtracker` with the SHA-1 of each signing certificate used
to install the app. Get the local debug fingerprint with:

```bash
./gradlew :app:signingReport
```

## Project Structure

- `app/src/main/java/dev/francescolofranco/gymtracker/data`: database, repositories, preferences, and backup code.
- `app/src/main/java/dev/francescolofranco/gymtracker/domain`: domain models and enums.
- `app/src/main/java/dev/francescolofranco/gymtracker/service`: workout timer service and tile.
- `app/src/main/java/dev/francescolofranco/gymtracker/ui`: Compose navigation, screens, components, and theme.
- `app/src/main/java/dev/francescolofranco/gymtracker/work`: background work for backups and idle sessions.
