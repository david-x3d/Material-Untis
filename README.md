# Material-Untis

Personal timetable for one student account. Phone app plus Wear OS companion.

Not affiliated with Untis. Use your own account only. Do not share credentials.

## What it does

- Find a school (search or enter server + school name)
- Sign in with password or app secret / QR (TOTP)
- Today timeline and week view, with cancelled and substitution lessons
- Lesson details (subject, teacher, room, info, subst text, times)
- Local Room cache, offline banner
- Sync to the watch (today’s periods only)
- Wear list + next-lesson complication
- Settings: account, 15/30/60 min sync, logout, watch status

Phone is the source of truth. The watch never signs in to WebUntis.

## Modules

| Module | Role |
| --- | --- |
| `:core` | JSON-RPC client, domain, Room, encrypted credentials, WatchPayload |
| `:app` | Phone UI (Compose, Material 3), WorkManager, Data Layer sender |
| `:wear` | Wear UI, Data Layer receiver, complication |

`applicationId` is `dev.x3d.dayline` on **both** APKs (required for sideloaded Data Layer).

## RPC (contract)

`POST https://<host>/WebUntis/jsonrpc.do?school=<school>`

Methods used: `authenticate`, `logout`, `getTimetable` (options form), `getLatestImportTime`, `getTimegridUnits`.

School search (optional): `POST https://mobile.webuntis.com/ms/schoolquery2` method `searchSchool`.

## Privacy

- Password / secret stored in EncryptedSharedPreferences only
- Nothing sensitive on the watch Data Layer
- No analytics, ads, or crash SDKs
- Cleartext HTTP disabled

## Build

Needs JDK 17 (Android Studio JBR), Android SDK 36, Build-Tools, and a Wear OS 3+ image if you want the emulator.

```bat
gradlew.bat :core:test
gradlew.bat :app:assembleDebug
gradlew.bat :wear:assembleDebug
```

APKs:

- Phone: `app/build/outputs/apk/debug/app-debug.apk`
- Wear: `wear/build/outputs/apk/debug/wear-debug.apk`

Release builds in this repo are **debug-signed** until you add your own keystore (`keystore.properties` is gitignored).

Install: phone first, then the watch APK (same signing key, same applicationId). Pair via Wear OS app / adb.

```bat
adb -s <phone> install -r app\build\outputs\apk\debug\app-debug.apk
adb -s <watch> install -r wear\build\outputs\apk\debug\wear-debug.apk
```

## What you still do in Android Studio

1. Install Android Studio Ladybug or newer
2. SDK Manager: Android 14/15/16 (API 36), Build-Tools, Google Play Wear OS system image (API 30+)
3. Accept licenses
4. Create a Wear emulator or use a physical watch
5. For a real release keystore: create `keystore.properties` locally (never commit) and point `signingConfigs.release` at it
6. Camera permission is optional; secret can always be pasted

## Tests

`:core` unit tests cover Untis time parsing (`7`, `720`, `930`, `1345`), cancelled/irregular mapping, one-shot session retry, and WatchPayload gzip round-trip. Fixtures live in `core/src/test/resources/fixtures` (anonymized).
