# FitNS

FitNS is an Android app foundation for nutrition tracking, macro and micronutrient analysis, strength training logs, and optional synchronization with a private n8n instance.

Default configurable n8n base URL:

```text
https://pi.pufferfish-lenok.ts.net/
```

## Current Scope

This repository contains the first MVP foundation:

- Kotlin Android project
- Jetpack Compose and Material 3 UI
- Navigation for dashboard, nutrition, workout, body weight, recommendations, profile, and settings
- Domain models for nutrition, workouts, goals, data quality, and progression
- Room-backed local repositories for food, workouts, body weight, profile, and nutrition goals
- Retrofit contract and repository for n8n health checks and barcode product lookup
- Hilt application setup
- WorkManager-based sync queue for nutrition, workout, and body-weight changes
- Local JSON export preview in settings
- Custom exercise creation for workout logging
- Workout rest timer after saved sets
- Progress screen for calorie, body-weight, and training-volume trends
- Dashboard training summary for today's workouts, sets, volume, and latest exercise
- Dashboard Daily Coach with goal score, focus prompt, target status, and meal balance
- Dashboard hydration quick logging
- Nutrition daily target overview, meal split summary, and meal filters
- Workout weekly volume summary, top exercise, estimated 1RM, and next-target hints
- Body-weight goal progress, 30-day change, and target-distance summary
- Soft delete for food entries, body-weight entries, and workouts
- Unit tests for nutrition scaling, body-weight moving average, and double progression

Camera scanner, OCR, meal-photo analysis, encrypted token storage, workout plan templates, and conflict resolution are prepared architecturally and should be implemented in the next phases.

All app UI copy is kept in US English.

## Build

Open the project in Android Studio, let Gradle sync, then run:

```bash
./gradlew assembleDebug
```

Run the unit test suite after larger behavior changes:

```bash
./gradlew test
```
