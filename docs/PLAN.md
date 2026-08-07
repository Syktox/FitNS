# FitNS – Phase 1: Architektur- und MVP-Planung

Status: genehmigt. Diese Datei ist der Ausgangspunkt für den weiteren Ausbau.

## 1. Bestandsaufnahme – was bereits existiert

Der MVP-Kern (Master-Prompt Phase 2 + 3) ist weitgehend gebaut und kompilierbar:

| Bereich | Stand |
|---|---|
| Stack | Kotlin, Compose, Material 3, Clean Architecture, MVVM, Hilt, Room, DataStore, Retrofit+Moshi, WorkManager, Coroutines/Flow |
| Datenmodell | Alle 19 geforderten Room-Entities vorhanden (`data/local/entity/Entities.kt`), Sync-Metadaten, Soft Delete, UUIDs |
| Sync | Warteschlange, Idempotency-Key, exponentieller Backoff, WorkManager-Worker |
| n8n-API | `N8nApiService` (5 Endpoints), konfigurierbare Base-URL, Bearer-Token, Verbindungstest |
| Sicherheit | `EncryptedTokenStore` (Android Keystore) |
| Domain | 23 Mikronährstoffe, `NutritionCalculator`, `BodyWeightTrendCalculator`, `WorkoutProgressionCalculator`, `LabelNutritionParser`, `RecommendationEngine` |
| Features | Dashboard, Tagesernährung, manuelle Erfassung, Favoriten, Workout-Start/History/Pläne, Körpergewicht, Progress, Empfehlungen, Profil, Einstellungen |
| Scanner | CameraX + ML Kit, `BarcodeScannerScreen`, `MealAnalysisScreen` (Essensfoto via n8n), `CameraCaptureView` |
| Tests | Migration, Moving Average, Nutrition, Progression |

## 2. Lücken gegenüber dem Master-Prompt

1. OCR-Bildschirm für Nährwertetiketten (Parser existiert, Ablauf/Review-UI fehlt)
2. ~~Onboarding-Flow~~ (umgesetzt: `OnboardingScreen` + `OnboardingViewModel`, Startdestination, Google-Sign-In optional via `GoogleAuthController`, Client-ID via `local.properties` → `BuildConfig.GOOGLE_WEB_CLIENT_ID`; Setup-Anleitung in `docs/GOOGLE_SIGN_IN.md`)
3. Barcode-Ergebnis-Review vor dem Speichern (Produkt -> Portion -> Mahlzeit)
4. n8n-Endpoints fehlen: `food/product`, `food/ocr`, `recommendations`, `export`
5. Kein echtes Konflikt-Handling (Status existiert, Logik fehlt)
6. Fehlende Screens: Nährstoffdetails, fehlende Nährstoffe, Maschinenhistorie
7. Kein Datenexport via n8n (nur lokaler JSON-Preview)
8. Nur ein Datenbank-Migrationstest

## 3. Anforderungen (MoSCoW)

- **Must:** lokale Erfassung (Essen + Training), Nährwertskalierung, Double-Progression, Mikronährstoff-Aggregation mit Datenqualität, offline-fähige Sync, Bearer-Auth, Soft Deletes, Validierung.
- **Should:** Foto-basierte Erfassung (Barcode, OCR-Label, Mahlzeit), Empfehlungen mit Begründung, Export.
- **Could:** Konfliktauflösung, n8n-basierte Empfehlungen.
- **Won't (MVP):** Schlaf, Rezepte, Community, Werbung.

## 4. Annahmen

- n8n-Basis-URL konfigurierbar, https-only, Default `https://pi.pufferfish-lenok.ts.net`.
- Bearer-Token in Einstellungen, nie im Quellcode.
- n8n liefert die dokumentierten JSON-Formate.
- Trainingsgewichte in kg; `isPerSide` -> Volumen ×2.
- Nährwerte pro 100 g; OCR unterscheidet pro 100 g / 100 ml / Portion.
- UI-Kopien bleiben US-Englisch.

## 5. Architektur

```text
core      network (API-Erweiterung), sync (Konfliktlogik), settings, security
domain    model + usecase (rein)
data      local (Room + Migrations), repository, mappers
feature   Screens + ViewModels
navigation App-Graph
```

Prinzipien: Repository-Pattern für n8n, `AppResult<T>`, `DataQuality` (Verified/Estimated/Missing), Foto-Upload nur mit Consent, OCR nie ungeprüft übernehmen.

## 6. Modulstruktur

| Modul | Ist | Plan |
|---|---|---|
| `core.network` | 5 Endpoints | + `product`, `ocr`, `recommendations`, `export` |
| `core.sync` | Queue, Worker | + Konflikt-Detektion |
| `domain.usecase` | 5 Use Cases | + OCR-Anbindung, Empfehlungs-Erweiterung |
| `feature.scanner` | Barcode, Meal | + `LabelCaptureScreen` (OCR-Review) |
| `feature.nutrition` | Tag, Manual | + Nährstoffdetails, fehlende Nährstoffe |
| `feature.workout` | Start, History | + Maschinenhistorie, Satz-Erfassung RPE/RIR/Pain |
| `feature.onboarding` | Onboarding-Screen + Google-Login (neu) | + evtl. Multi-Step |

## 7. Datenmodell

Deckt Master-Prompt §8 vollständig ab. Ergänzungen später:
- `RecommendationEntity` + `basisDataQuality`, `dayCount`
- `FoodEntryEntity.micronutrientsJson` im OCR-/Menü-Flow befüllen
- Keine Schema-Brüche nötig; neue Features = neue Migrationen (Mechanismus vorhanden)

## 8. Navigationsstruktur

Ist: Dashboard, Nutrition, Add-Food, Workout, History, BodyWeight, Progress, Recommendations, Profile, Settings.

Geplant: `label-scan`, `barcode-result`, `nutrient-details`, `nutrient-missing`, `machine-history`, `onboarding`. Bottom-Bar bleibt 5 Ziele.

## 9. API-Verträge

Vorhanden: `health`, `food/barcode`, `food/analyze-image`, `nutrition/sync`, `workout/sync`, `body-weight/sync`.

Zu ergänzen: `food/product`, `food/ocr`, `recommendations`, `export`. Alle mit `Authorization` + `Idempotency-Key`.

## 10. Sicherheitskonzept

- Bearer-Token im Keystore; https-only; Foto-Upload mit Consent; temporäre Fotos; gekürzte Logs; lokale Löschung/Export.

## 11. MVP-Abgrenzung (nächste Iteration)

1. OCR-Label-Erfassung komplettieren (Foto -> Review -> Speichern) — **in Arbeit**
2. Barcode-Review-Flow (Scan -> Produkt -> Portion -> Mahlzeit)
3. ~~Onboarding-Flow~~ (inkl. optionalem Google-Sign-In)
4. Maschinenhistorie + Übungsauswahl
5. Nährstoffdetail- + Fehlnährstoff-Screen
6. n8n-Endpoints `food/product`, `recommendations`, `export`

## 12. Spätere Erweiterungen

Konflikt-Handling (serverVersion), n8n-Empfehlungen, Rezepte, Schlafdaten, Export-UI, Zielversionierung, RPE-Verlauf, mehrsprachige UI.
