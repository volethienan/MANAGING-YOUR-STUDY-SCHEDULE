# Managing Your Study Schedule

Android Java/XML study planner app with a Studygram-inspired interface.

## Features

- Weekly calendar view inspired by Google Calendar.
- Study events, exams, deadlines, and task management.
- Pomodoro focus timer and progress statistics.
- Create schedules from camera/gallery images using Gemini image understanding.
- Conflict detection for overlapping study events.

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
