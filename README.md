# Gym Tracker

Android workout tracker for logging gym sessions, sets, reps, weight, notes, and progress over time.

## Features

- Start blank sessions or reusable template-based workouts.
- Track sets with reps, weight, skipped sets, exercise notes, and session notes.
- Resume active sessions with a persistent workout timer.
- Review past sessions by month with duration, sets, exercises, and volume.
- See exercise-specific stats with progress charts and session history.
- View weekly muscle-volume stats and trends.
- Back up and restore workouts from files or Google Drive.
- Configure display units, templates, notifications, and keep-screen-on behavior.

## Tech Stack

- Kotlin and Jetpack Compose
- Material 3
- Room
- Hilt
- DataStore
- WorkManager
- Google Drive sign-in and backup support

## Getting Started

Requirements:

- Android Studio
- JDK 17
- Android SDK with API 35

Run from Android Studio, or use Gradle:

```bash
./gradlew :app:assembleDebug
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## Project Structure

- `app/src/main/java/dev/francescolofranco/gymtracker/data`: database, repositories, preferences, and backup code.
- `app/src/main/java/dev/francescolofranco/gymtracker/domain`: domain models and enums.
- `app/src/main/java/dev/francescolofranco/gymtracker/service`: workout timer service and tile.
- `app/src/main/java/dev/francescolofranco/gymtracker/ui`: Compose navigation, screens, components, and theme.
- `app/src/main/java/dev/francescolofranco/gymtracker/work`: background work for backups and idle sessions.
