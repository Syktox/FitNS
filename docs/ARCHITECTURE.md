# FitNS Architecture

## MVP Boundary

The first usable version focuses on local-first tracking:

- User profile and nutrition goals
- Dashboard for today's calories, macros, water, and training status
- Dashboard Daily Coach for local goal scoring, focus prompts, target status, and meal balance
- Workout timestamps for daily dashboard summaries and history display
- Manual food logging
- Nutrition target overview, meal split summaries, and meal filters
- Exercise and machine-oriented workout logging
- Workout weekly volume summary, top exercise, estimated 1RM, and double-progression hints
- Custom exercise creation
- In-workout rest timer
- Progress reporting for calories, body weight, and training volume
- Local Room schema with sync metadata
- Daily hydration totals through nutrition summaries
- Body-weight goal progress derived from local weigh-ins and profile target weight
- n8n connection settings and API contract
- Barcode product lookup through n8n with review-before-save
- Soft delete for user-correctable logs

Camera scanning, OCR, meal-photo analysis, encrypted token storage, richer recommendations, and robust conflict resolution are next-phase features.

## Layers

```text
core        Shared technical building blocks
domain      Pure models, repository interfaces, use cases, validation
data        Room, Retrofit, DataStore, sync queue, mappers
feature     Compose screens and ViewModels
navigation  App graph and screen-level state wiring
```

## Synchronization

Every synchronizable record carries:

```text
id
createdAt
updatedAt
deletedAt
syncStatus
serverVersion
```

Local changes should enqueue `SyncQueueItemEntity` rows with an idempotency key. WorkManager can later process the queue with exponential backoff. Sensitive payloads should not be logged.

Soft deletes use `deletedAt` plus `PendingSync` and enqueue a `"delete"` operation. Read queries filter out deleted rows.

## n8n Webhooks

```text
GET  /webhook/health
POST /webhook/food/barcode
POST /webhook/food/product
POST /webhook/food/analyze-image
POST /webhook/food/ocr
POST /webhook/nutrition/sync
POST /webhook/workout/sync
POST /webhook/body-weight/sync
POST /webhook/recommendations
POST /webhook/export
```

The base URL and paths should be editable in settings. Bearer tokens belong in encrypted storage, not source code.

The current app uses:

```text
GET  /webhook/health
POST /webhook/food/barcode
POST /webhook/nutrition/sync
POST /webhook/workout/sync
POST /webhook/body-weight/sync
```
