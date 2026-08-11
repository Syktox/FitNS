# FitNS

FitNS is a Kotlin Android fitness app for nutrition tracking, strength training, body-weight progress, coaching signals, and optional synchronization with a private n8n instance.

All visible app UI copy is kept in US English.

## App Features

- Dashboard with daily nutrition progress, hydration actions, training summary, and coach guidance.
- Nutrition logging with macro and micronutrient fields, meal filters, favorites, barcode lookup wiring, label scan, and meal photo analysis entry points.
- Workout plans/templates backed by Room, including create, edit, delete, start, ordered exercises, default sets, rep ranges, and rest times.
- Active workout mode with per-set weight, reps, RPE, RIR, set type, completion status, previous performance, automatic rest timer, and finish/discard actions.
- Automatic personal-record detection for highest weight, reps at weight, estimated 1RM, and session volume.
- Body-weight logging with trend and goal-distance summaries.
- Progress screens for calories, body weight, and training volume.
- Optional Google sign-in state and local settings.
- WorkManager sync queue for local-first changes.

## Architecture

The app keeps the existing layered structure:

- `core`: auth, design system, settings, serialization, sync, and network primitives.
- `data`: Room entities, DAOs, migrations, mappers, local repositories, and remote n8n repository.
- `domain`: models, repository interfaces, calculators, and use cases.
- `feature`: Compose screens and ViewModels by product area.
- `navigation`: Compose Navigation graph and top-level app shell.
- `di`: Hilt bindings and app-level providers.

ViewModels primarily own UI state and events. Workout calculations such as double progression, estimated 1RM, active-session construction, and PR detection live in domain use cases.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Room
- DataStore
- Retrofit
- Moshi
- WorkManager
- CameraX
- ML Kit Barcode Scanning
- ML Kit Text Recognition
- Google Authentication
- Compose Navigation

## Build Instructions

Open the project in Android Studio and let Gradle sync, or run:

```bash
./gradlew assembleDebug
```

Release signing can be provided through environment variables or `.env.android-signing`. Do not commit secrets or private tokens.

## Tests

Run the unit and Robolectric migration tests:

```bash
./gradlew testDebugUnitTest
```

Useful broader checks:

```bash
./gradlew lint
./gradlew assembleDebug
```

Current focused coverage includes nutrition scaling, body-weight moving averages, double progression, estimated workout progression suggestions, personal-record detection, and Room migrations through schema version 4.

## n8n Integration

The default configurable n8n base URL is:

```text
https://pi.pufferfish-lenok.ts.net/
```

The app includes a Retrofit `N8nApiService`, settings for the base URL and bearer token, a connection test, barcode product lookup, meal-image analysis wiring, and a WorkManager-backed sync queue. Sync remains local-first and should tolerate offline usage.

## Barcode Scanner

CameraX and ML Kit dependencies are present. The barcode flow is wired from manual food entry to scanner to product lookup, with product-prefill support in nutrition logging. Follow-up hardening should expand empty, denied-permission, retry, unknown-barcode, and offline UX states.

## Workout System

Workout plans are persisted in Room as reusable templates. Starting a plan creates an active session in memory, prefilled from template defaults and previous performance. Finishing the session writes the workout, exercises, and sets atomically to Room and enqueues a sync payload.

Supported set types:

- Warm-up
- Normal
- Drop Set
- Failure
- AMRAP

The rest timer starts automatically when a set is completed and supports add/subtract time, pause, resume, and skip.

## Nutrition System

Nutrition supports manual food logging, meal type selection, favorites, daily target tracking, micronutrients, hydration, label scanning, barcode lookup, and meal-photo analysis entry points. Saved meals/recipes, recent-food ranking, and copy-previous-meal actions remain planned.

## Optional Health Connect

Health Connect is not yet implemented. It should remain optional when added, with clean permission handling and no degradation for users who do not connect it.

## Roadmap

Completed:

- Local-first Room repositories and schema migrations through version 4.
- Workout plan create/edit/delete/start.
- Active workout logging with editable sets, set types, RPE/RIR, previous performance, and rest timer.
- Personal-record detection from workout history.
- Domain use cases for workout progression, active-session construction, estimated 1RM, and PR detection.

In Progress:

- Barcode lookup UX hardening.
- Nutrition search sections for recent, favorites, custom foods, and remote results.
- Progress analytics expansion for strength and training volume.
- Dashboard coach refactor into smaller explainable use cases.

Planned:

- Saved meals and scalable recipes.
- Copy yesterday / previous meal actions.
- Health Connect import/export.
- Compose UI tests for workout and nutrition flows.
- Release R8/resource shrinking review and ProGuard hardening.
- Expanded CI with compile, unit tests, lint, and debug build on pull requests.
