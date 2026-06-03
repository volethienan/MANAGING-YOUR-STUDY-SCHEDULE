# Managing Your Study Schedule

Android Java/XML study planner app with a Studygram-inspired interface.

## Features

- Weekly calendar view inspired by Google Calendar.
- Study events, exams, deadlines, and task management.
- Pomodoro focus timer and progress statistics.
- Create schedules from camera/gallery images using Gemini image understanding.
- Conflict detection for overlapping study events.
- Firebase Realtime Database sync for account-scoped study data.
- Admin web for account registry, account lock state, announcements, and OTP/AI issue monitoring.

## Gemini API setup

The app reads the Gemini key from `local.properties` through `BuildConfig`.
Add this line locally before building:

```properties
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
```

Do not commit `local.properties`; it is ignored on purpose.

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
.\gradlew.bat assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Firebase data storage

The app still writes to local SQLite first so it can work as an offline cache, then syncs account-scoped data to Firebase Realtime Database.
After logging in, open Firebase Console > Realtime Database > Data and check:

```text
study_users/{encodedEmail}/meta/profile
study_users/{encodedEmail}/tasks/{encodedTaskId}
study_users/{encodedEmail}/events/{encodedEventId}
study_users/{encodedEmail}/countdowns/{encodedCountdownId}
study_users/{encodedEmail}/pomodoro_sessions/{encodedSessionId}
study_users/{encodedEmail}/settings/{encodedKey}
```

For a demo, create or edit a task/event in the Android app, then refresh Realtime Database to show the updated node.

## Admin web

The Android app keeps SQLite as a local cache and syncs study data to Firebase Realtime Database. The separate `admin-web` service receives a central account registry, learning snapshots, and OTP/AI issue reports from the app when `ADMIN_BACKEND_URL` is configured.

Run the local admin web:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25'
$env:ADMIN_USERNAME='admin'
$env:ADMIN_PASSWORD='change-this-password'
.\gradlew.bat -p admin-web run
```

Open `http://localhost:8090`.

For an Android emulator, set this in `local.properties` if the default is changed:

```properties
ADMIN_BACKEND_URL=http://10.0.2.2:8090
```
