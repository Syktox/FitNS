# Google Sign-In einrichten

Die App zeigt auf dem Onboarding-Screen einen **"Continue with Google"**-Button. Damit er
funktioniert, brauchst du eine OAuth-Client-ID aus der Google Cloud Console. Die ID wird beim
Build aus `local.properties` gelesen und als `BuildConfig.GOOGLE_WEB_CLIENT_ID` kompiliert.

## Voraussetzungen

- Ein Google-Konto (z. B. dein privates).
- Der Build muss dieselbe Signatur verwenden wie die Google-Cloud-Konfiguration
  (siehe Schritt 4 – SHA-1-Fingerprint).

## Schritt-für-Schritt

1. **Projekt anlegen**

   Gehe auf <https://console.cloud.google.com/> → Projekt erstellen (z. B. `FitNS`).

2. **OAuth Consent Screen einrichten**

   - APIs & Services → **OAuth consent screen**
   - User Type: **External** (oder Internal, falls du ein Google Workspace-Konto nutzt)
   - App-Name: `FitNS`, Support-E-Mail angeben
   - Scopes: Standard (email, profile) – die App fragt nur `email` und `profile` ab.
   - Test user: deine eigene E-Mail-Adresse hinzufügen, solange der Screen im
     "Testing"-Status ist.

3. **OAuth-Client-IDs erstellen**

   APIs & Services → **Credentials** → **Create Credentials**:

   a. **Android**-Client-ID erstellen:
      - Package name: `com.raysix.fitns`
      - SHA-1: siehe Schritt 4
   b. **Web**-Client-ID erstellen (für die ID-Token-Verifikation):
      - Autorisiertes Ursprungs-/Redirect kann leer bleiben.

   Merke dir die **Web-Client-ID** (endet auf `.apps.googleusercontent.com`).
   Nur diese wird in der App benötigt.

4. **SHA-1-Fingerprint ermitteln**

   Debug-Build (Standard-Local-Key):

   ```powershell
   keytool -exportcert -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | ForEach-Object { $h = [System.BitConverter]::ToString([System.Security.Cryptography.SHA1]::Create().ComputeHash([System.IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore"))).Replace("-", "").ToLower(); $h }
   ```

   > Alternativ: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`
   > und aus der Zeile **SHA1** den Wert kopieren (ohne Doppelpunkte, Großbuchstaben).

   Für den **Release-Build** nimmst du den SHA-1 des Signing-Keystores aus `.env.android-signing`.
   Du kannst auch mehrere SHA-1-Fingerprints an einer Client-ID hinterlegen.

5. **Client-ID in der App hinterlegen**

   In `local.properties` (Wurzel des Projekts) ergänzen:

   ```properties
   GOOGLE_WEB_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.apps.googleusercontent.com
   ```

   Danach einmalig `gradlew clean` ausführen, damit `BuildConfig` neu generiert wird.

6. **Testen**

   `./gradlew installDebug`, App starten → Onboarding → "Continue with Google".
   Im Consent-Screen muss dein Testkonto erscheinen. Der Login speichert E-Mail,
   Anzeigename und Profilbild-URL lokal (DataStore).

## Troubleshooting

| Symptom | Ursache / Lösung |
|---|---|
| "not configured yet" | `GOOGLE_WEB_CLIENT_ID` fehlt in `local.properties` oder Build nicht neu generiert. |
| `12501 SIGN_IN_CANCELLED` | Nicht konfigurierte Android-Client-ID oder falscher SHA-1. Client-ID & Fingerprint prüfen. |
| `10 DEVELOPER_ERROR` | Falsche Client-ID / Keystore passt nicht zum registrierten SHA-1. |
| Consent-Screen-Fehler | App im "Testing"-Status: E-Mail muss als Test-User hinzugefügt sein. |

## Datenschutz-Hinweis

Es wird nur das Basisprofil (E-Mail, Name, Foto-URL) gespeichert – **kein** Access- oder
ID-Token. Die App ist lokal-first; das Konto dient der Personalisierung.
