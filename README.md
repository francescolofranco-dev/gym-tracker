# Gym Tracker

Android workout tracker for logging gym sessions, sets, reps, weight, notes, and progress over time.

## Features

- Start blank sessions or reusable template-based workouts.
- Track sets with reps, weight, skipped sets, session notes, and exercise notes that carry into
  the next workout, alongside separate pinned notes that stay until removed.
- Support unilateral exercises with separate left/right sets and per-side planning.
- Resume active sessions with an accurate workout timer, plus editable session timing.
- Review past sessions by month with duration, sets, exercises, and volume.
- See exercise-specific stats, progress charts, session history, and personal records.
- Review compact 7, 28, and 90-day training insights, weekly muscle coverage, and exercise progress.
- Back up and preview restores from files or Google Drive, with one-step restore recovery.
- Configure display units, templates, notifications, and keep-screen-on behavior.
- Enable **Settings → Floating timer** to allow a small, draggable time counter at the top right
  above other apps and full-screen videos while the timer runs; it hides automatically while Gym
  Tracker is active and focused. Reset/Stop stay in the notification.
  Android system surfaces and apps that explicitly block overlays can hide it.
  Switch it off to hide the overlay without stopping the timer; the choice is saved.
  A translucent dark background keeps the counter readable on light content.
  Stop removes the floating timer and notification.

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

For device acceptance, build the optimized, locally signed, non-debuggable and shell-profileable
APK with `./gradlew :app:assembleBenchmark`. Install
`app/build/outputs/apk/benchmark/app-benchmark.apk`. This is a performance-ready build;
building it does not produce measured benchmark results.

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Run the DAO SQL regression checks (Python 3 standard library, using the exported Room schema):

```bash
python3 -m unittest discover -s scripts -p 'test_*.py'
```

### Unilateral volume

Only confirmed, non-skipped sets count toward statistics and history. For unilateral exercises,
enter the weight and repetitions for each side separately. Volume sums the actual `reps × kg`
from each completed side: left 10 × 10 kg plus right 8 × 12 kg is 196 kg. An unfinished side adds
nothing. Session summaries count each completed side as a logged set; muscle coverage credits
it as 0.5 bilateral-equivalent sets (a left/right pair is 1). Secondary muscles receive half
credit. Historical set sides determine this credit even if the exercise's unilateral setting changes.

### Google Drive authorization

Drive backup requires the Google Drive API and an Android OAuth client in Google Cloud Console.
Register package `dev.francescolofranco.gymtracker` with the SHA-1 of each signing certificate used
to install the app. Get the local debug fingerprint with:

```bash
./gradlew :app:signingReport
```

For Play Store installs, also register the **App signing key** SHA-1 shown in Play Console →
App integrity, because Google re-signs the bundle.

### Play Store release

Release builds are signed with the upload key when a root `keystore.properties` (untracked) exists:

```properties
storeFile=/absolute/path/to/upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

Without it, release artifacts are built unsigned. `versionCode` is the git commit count, so every
release commit gets a higher code automatically; `versionName` is set by the release script.

```bash
./gradlew :app:bundleRelease
# app/build/outputs/bundle/release/app-release.aab
```

To ship an update:

1. Edit `app/src/main/play/release-notes/en-US/default.txt`.
2. Run `scripts/release.sh X.Y.Z`. It bumps `versionName`, runs the checks, commits, tags `vX.Y.Z`
   and pushes.
3. The tag triggers `.github/workflows/release.yml`, which tests, builds the signed bundle and
   uploads it to the Play **internal** track. Watch it with `gh run watch`.
4. Promote it in Play Console, or with
   `./gradlew :app:promoteReleaseArtifact --from-track internal --promote-track production`.

The workflow can also be started manually (Actions → Play release) with a chosen track and
release status. Use `draft` until the app has been published once.

The workflow needs these repository secrets: `KEYSTORE_BASE64` (base64 upload keystore),
`KEYSTORE_PASSWORD`, and `PLAY_SERVICE_ACCOUNT_JSON` (Play Developer API service account key).
To upload from your machine instead, add `playServiceAccount=/absolute/path/to/key.json` to
`keystore.properties` and run `./gradlew :app:publishReleaseBundle`.

## Project Structure

- `app/src/main/java/dev/francescolofranco/gymtracker/data`: database, repositories, preferences, and backup code.
- `app/src/main/java/dev/francescolofranco/gymtracker/domain`: domain models and enums.
- `app/src/main/java/dev/francescolofranco/gymtracker/service`: workout timer service and tile.
- `app/src/main/java/dev/francescolofranco/gymtracker/ui`: Compose navigation, screens, components, and theme.
- `app/src/main/java/dev/francescolofranco/gymtracker/work`: background work for backups and idle sessions.
