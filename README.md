# Managing Your Study Schedule

Android Java/XML study planner app with a Studygram-inspired interface.

## Features

- Weekly calendar view inspired by Google Calendar.
- Study events, exams, deadlines, and task management.
- Pomodoro focus timer and progress statistics.
- Create schedules from camera/gallery images using Gemini image understanding.
- Conflict detection for overlapping study events.
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

## Admin web

The Android app keeps study data on each device. The separate `admin-web` service receives a central account registry and OTP/AI issue reports from the app when `ADMIN_BACKEND_URL` is configured.

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
