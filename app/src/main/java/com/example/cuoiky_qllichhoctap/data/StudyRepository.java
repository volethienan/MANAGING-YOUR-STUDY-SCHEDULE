package com.example.cuoiky_qllichhoctap.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.model.CountdownMilestone;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StudyRepository {
    private static final String PREFS = "study_planner_store";
    private static final String KEY_FIRST_OPEN = "first_open";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_DASHBOARD_BACKGROUND = "dashboard_background";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_MASCOT = "mascot";
    private static final String KEY_STUDY_STATUS = "study_status";
    private static final String DB_NAME = "study_planner.db";
    private static final int DB_VERSION = 4;

    private final SharedPreferences prefs;
    private final StudyDbHelper dbHelper;
    private final boolean accountScoped;
    private final FirebaseStudyStore firebaseStore;

    public StudyRepository(Context context) {
        this(context, "");
    }

    public StudyRepository(Context context, String accountEmail) {
        String normalizedAccount = accountEmail == null ? "" : accountEmail.trim().toLowerCase(Locale.US);
        accountScoped = !normalizedAccount.isEmpty();
        String suffix = accountScoped ? "_" + Integer.toHexString(normalizedAccount.hashCode()) : "";
        prefs = context.getSharedPreferences(PREFS + suffix, Context.MODE_PRIVATE);
        dbHelper = new StudyDbHelper(context, DB_NAME.replace(".db", suffix + ".db"));
        firebaseStore = accountScoped ? new FirebaseStudyStore(normalizedAccount) : null;
        ensureRuntimeTables();
        if (!accountScoped) {
            seedIfNeeded();
        }
    }

    public boolean isFirstOpen() {
        return prefs.getBoolean(KEY_FIRST_OPEN, true);
    }

    public void finishOnboarding() {
        prefs.edit().putBoolean(KEY_FIRST_OPEN, false).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public UserProfile getProfile() {
        try (Cursor cursor = db().query("profile", new String[]{"name", "email", "goal"}, "id = 1", null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new UserProfile(cursor.getString(0), cursor.getString(1), cursor.getString(2));
            }
        }
        return defaultProfile();
    }

    public void saveProfile(UserProfile profile) {
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("name", profile.getName());
        values.put("email", profile.getEmail());
        values.put("goal", profile.getGoal());
        db().insertWithOnConflict("profile", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        syncProfileToFirebase(profile);
    }

    public List<StudyTask> getTasks() {
        List<StudyTask> tasks = new ArrayList<>();
        try (Cursor cursor = db().query("tasks", new String[]{"id", "title", "subject", "due_at", "priority", "note", "completed", "important", "urgent", "tag", "reminder_time", "repeat_option", "estimated_pomodoro", "marker_type", "marker_value", "show_on_calendar"}, null, null, null, null, "due_at ASC")) {
            while (cursor.moveToNext()) {
                tasks.add(new StudyTask(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6) == 1,
                        cursor.getInt(7) == 1,
                        cursor.getInt(8) == 1,
                        cursor.getString(9),
                        cursor.getLong(10),
                        cursor.getString(11),
                        cursor.getInt(12),
                        cursor.getString(13),
                        cursor.getString(14),
                        cursor.getInt(15) == 1
                ));
            }
        }
        return tasks;
    }

    public StudyTask getTask(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (StudyTask task : getTasks()) {
            if (id.equals(task.getId())) {
                return task;
            }
        }
        return null;
    }

    public void saveTask(StudyTask task) {
        ContentValues values = new ContentValues();
        values.put("id", task.getId());
        values.put("title", task.getTitle());
        values.put("subject", task.getSubject());
        values.put("due_at", task.getDueAt());
        values.put("priority", task.getPriority());
        values.put("note", task.getNote());
        values.put("completed", task.isCompleted() ? 1 : 0);
        values.put("important", task.isImportant() ? 1 : 0);
        values.put("urgent", task.isUrgent() ? 1 : 0);
        values.put("tag", task.getTag());
        values.put("reminder_time", task.getReminderTime());
        values.put("repeat_option", task.getRepeatOption());
        values.put("estimated_pomodoro", task.getEstimatedPomodoro());
        values.put("marker_type", task.getMarkerType());
        values.put("marker_value", task.getMarkerValue());
        values.put("show_on_calendar", task.isShowOnCalendar() ? 1 : 0);
        db().insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        syncTaskToFirebase(task);
    }

    public void deleteTask(String id) {
        List<String> linkedEventIds = new ArrayList<>();
        try (Cursor cursor = db().query("events", new String[]{"id"}, "source_task_id = ?", new String[]{id}, null, null, null)) {
            while (cursor.moveToNext()) {
                linkedEventIds.add(cursor.getString(0));
            }
        }
        db().delete("events", "source_task_id = ?", new String[]{id});
        db().delete("tasks", "id = ?", new String[]{id});
        if (firebaseStore != null) {
            firebaseStore.deleteTask(id);
            for (String eventId : linkedEventIds) {
                firebaseStore.deleteEvent(eventId);
            }
        }
    }

    public List<StudyEvent> getEvents() {
        List<StudyEvent> events = new ArrayList<>();
        try (Cursor cursor = db().query("events", new String[]{"id", "title", "type", "subject", "start_at", "end_at", "room", "note", "reminder_enabled", "reminder_before_minutes", "source_task_id"}, null, null, null, null, "start_at ASC")) {
            while (cursor.moveToNext()) {
                events.add(new StudyEvent(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4),
                        cursor.getLong(5),
                        cursor.getString(6),
                        cursor.getString(7),
                        cursor.getInt(8) == 1,
                        cursor.getInt(9),
                        cursor.getString(10)
                ));
            }
        }
        return events;
    }

    public void saveEvent(StudyEvent event) {
        ContentValues values = new ContentValues();
        values.put("id", event.getId());
        values.put("title", event.getTitle());
        values.put("type", event.getType());
        values.put("subject", event.getSubject());
        values.put("start_at", event.getStartAt());
        values.put("end_at", event.getEndAt());
        values.put("room", event.getRoom());
        values.put("note", event.getNote());
        values.put("reminder_enabled", event.isReminderEnabled() ? 1 : 0);
        values.put("reminder_before_minutes", event.getReminderBeforeMinutes());
        values.put("source_task_id", event.getSourceTaskId());
        db().insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        syncEventToFirebase(event);
    }

    public void deleteEvent(String id) {
        db().delete("events", "id = ?", new String[]{id});
        if (firebaseStore != null) {
            firebaseStore.deleteEvent(id);
        }
    }

    public List<CountdownMilestone> getCountdownMilestones() {
        List<CountdownMilestone> milestones = new ArrayList<>();
        try (Cursor cursor = db().query("countdowns", new String[]{"id", "title", "type", "target_date", "note"}, null, null, null, null, "target_date ASC")) {
            while (cursor.moveToNext()) {
                milestones.add(new CountdownMilestone(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        cursor.getString(4)
                ));
            }
        }
        return milestones;
    }

    public void saveCountdownMilestone(CountdownMilestone milestone) {
        ContentValues values = new ContentValues();
        values.put("id", milestone.getId());
        values.put("title", milestone.getTitle());
        values.put("type", milestone.getType());
        values.put("target_date", milestone.getTargetDate());
        values.put("note", milestone.getNote());
        db().insertWithOnConflict("countdowns", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (firebaseStore != null) {
            firebaseStore.saveCountdown(milestone);
        }
    }

    public void deleteCountdownMilestone(String id) {
        db().delete("countdowns", "id = ?", new String[]{id});
        if (firebaseStore != null) {
            firebaseStore.deleteCountdown(id);
        }
    }

    public CountdownMilestone newCountdownMilestone(String title, String type, long targetDate, String note) {
        return new CountdownMilestone(UUID.randomUUID().toString(), title, type, targetDate, note);
    }

    public void setTaskCalendarVisibility(String taskId, boolean showOnCalendar) {
        StudyTask task = getTask(taskId);
        if (task == null) {
            return;
        }
        task.setShowOnCalendar(showOnCalendar);
        saveTask(task);
    }

    public StudyEvent getEventForTask(String taskId) {
        try (Cursor cursor = db().query("events", new String[]{"id", "title", "type", "subject", "start_at", "end_at", "room", "note", "reminder_enabled", "reminder_before_minutes", "source_task_id"}, "source_task_id = ?", new String[]{taskId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new StudyEvent(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4),
                        cursor.getLong(5),
                        cursor.getString(6),
                        cursor.getString(7),
                        cursor.getInt(8) == 1,
                        cursor.getInt(9),
                        cursor.getString(10)
                );
            }
        }
        return null;
    }

    public StudyEvent syncTaskDeadlineEvent(StudyTask task) {
        StudyEvent existing = getEventForTask(task.getId());
        if (!task.isShowOnCalendar() || task.isCompleted()) {
            if (existing != null) {
                deleteEvent(existing.getId());
            }
            return null;
        }
        long startAt = task.getDueAt();
        long endAt = startAt + 30L * 60L * 1000L;
        String note = task.getNote();
        if (!task.getPriority().isEmpty()) {
            note = (note == null || note.trim().isEmpty() ? "" : note + " • ") + "Ưu tiên: " + task.getPriority();
        }
        StudyEvent event = new StudyEvent(
                existing == null ? UUID.randomUUID().toString() : existing.getId(),
                task.getTitle(),
                StudyEvent.TYPE_DEADLINE,
                task.getSubject(),
                startAt,
                endAt,
                "",
                note,
                task.getReminderTime() > 0,
                15,
                task.getId()
        );
        saveEvent(event);
        return event;
    }

    public boolean hasConflict(StudyEvent candidate) {
        return !getConflicts(candidate).isEmpty();
    }

    public List<StudyEvent> getConflicts(StudyEvent candidate) {
        List<StudyEvent> conflicts = new ArrayList<>();
        if (StudyEvent.TYPE_DEADLINE.equals(candidate.getType())) {
            return conflicts;
        }
        for (StudyEvent event : getEvents()) {
            if (event.getId().equals(candidate.getId())) {
                continue;
            }
            if (StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
                continue;
            }
            if (DateTimeUtils.rangesOverlap(candidate.getStartAt(), candidate.getEndAt(), event.getStartAt(), event.getEndAt())) {
                conflicts.add(event);
            }
        }
        return conflicts;
    }

    public boolean hasConflict(String eventId) {
        for (StudyEvent event : getEvents()) {
            if (event.getId().equals(eventId)) {
                return hasConflict(event);
            }
        }
        return false;
    }

    public int getFocusMinutes() {
        ensureFocusRow();
        try (Cursor cursor = db().query("focus_stats", new String[]{"minutes"}, "id = 1", null, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int getFocusSessions() {
        ensureFocusRow();
        try (Cursor cursor = db().query("focus_stats", new String[]{"sessions"}, "id = 1", null, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int getTodayFocusMinutes() {
        ensureTodayFocusRow();
        try (Cursor cursor = db().query("focus_day_stats", new String[]{"minutes"}, "day_key = ?", new String[]{todayFocusKey()}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public int getTodayFocusSessions() {
        ensureTodayFocusRow();
        try (Cursor cursor = db().query("focus_day_stats", new String[]{"sessions"}, "day_key = ?", new String[]{todayFocusKey()}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void addFocusSession(int minutes) {
        ensureFocusRow();
        ensureTodayFocusRow();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("minutes", getFocusMinutes() + minutes);
        values.put("sessions", getFocusSessions() + 1);
        db().insertWithOnConflict("focus_stats", null, values, SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues dayValues = new ContentValues();
        dayValues.put("day_key", todayFocusKey());
        dayValues.put("minutes", getTodayFocusMinutes() + minutes);
        dayValues.put("sessions", getTodayFocusSessions() + 1);
        db().insertWithOnConflict("focus_day_stats", null, dayValues, SQLiteDatabase.CONFLICT_REPLACE);
        syncFocusStatsToFirebase();
    }

    public void savePomodoroSession(com.example.cuoiky_qllichhoctap.model.PomodoroSession session) {
        ContentValues values = new ContentValues();
        values.put("id", session.getId());
        values.put("task_id", session.getTaskId());
        values.put("subject_tag", session.getSubjectTag());
        values.put("mode", session.getMode());
        values.put("duration_minutes", session.getDurationMinutes());
        values.put("completed_minutes", session.getCompletedMinutes());
        values.put("started_at", session.getStartedAt());
        values.put("ended_at", session.getEndedAt());
        values.put("is_completed", session.isCompleted() ? 1 : 0);
        values.put("sound_type", session.getSoundType());
        values.put("created_at", System.currentTimeMillis());
        db().insertWithOnConflict("pomodoro_sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (firebaseStore != null) {
            firebaseStore.savePomodoroSession(session);
        }
    }

    public int getCompletedPomodoros(String taskId) {
        try (Cursor cursor = db().rawQuery("SELECT COUNT(*) FROM pomodoro_sessions WHERE task_id = ? AND mode = 'focus' AND is_completed = 1", new String[]{taskId})) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        }
        return 0;
    }

    public List<com.example.cuoiky_qllichhoctap.model.PomodoroSession> getRecentPomodoroSessions(int limit) {
        List<com.example.cuoiky_qllichhoctap.model.PomodoroSession> sessions = new ArrayList<>();
        String safeLimit = String.valueOf(Math.max(1, Math.min(50, limit)));
        try (Cursor cursor = db().query(
                "pomodoro_sessions",
                new String[]{"id", "task_id", "subject_tag", "mode", "duration_minutes", "completed_minutes", "started_at", "ended_at", "is_completed", "sound_type"},
                null,
                null,
                null,
                null,
                "created_at DESC",
                safeLimit)) {
            while (cursor.moveToNext()) {
                sessions.add(new com.example.cuoiky_qllichhoctap.model.PomodoroSession(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4),
                        cursor.getInt(5),
                        cursor.getLong(6),
                        cursor.getLong(7),
                        cursor.getInt(8) == 1,
                        cursor.getString(9)
                ));
            }
        }
        return sessions;
    }


    public boolean isNotifyEnabled() {
        return getBooleanSetting("notify", true);
    }

    public void setNotifyEnabled(boolean enabled) {
        setBooleanSetting("notify", enabled);
    }

    public boolean isSyncEnabled() {
        return getBooleanSetting("sync", true);
    }

    public void setSyncEnabled(boolean enabled) {
        setBooleanSetting("sync", enabled);
    }

    public String getAvatarChoice() {
        return prefs.getString(KEY_AVATAR, "Chữ viết tắt");
    }

    public String getDashboardBackgroundChoice() {
        return prefs.getString(KEY_DASHBOARD_BACKGROUND, "Giấy sáng");
    }

    public String getThemeColorChoice() {
        return prefs.getString(KEY_THEME_COLOR, "Hoa hồng");
    }

    public String getMascotChoice() {
        return prefs.getString(KEY_MASCOT, "Robot học tập");
    }

    public String getStudyStatus() {
        return prefs.getString(KEY_STUDY_STATUS, "Sẵn sàng học tập");
    }

    public void savePersonalization(String avatar, String dashboardBackground, String themeColor, String mascot, String studyStatus) {
        prefs.edit()
                .putString(KEY_AVATAR, avatar)
                .putString(KEY_DASHBOARD_BACKGROUND, dashboardBackground)
                .putString(KEY_THEME_COLOR, themeColor)
                .putString(KEY_MASCOT, mascot)
                .putString(KEY_STUDY_STATUS, studyStatus)
                .apply();
        if (firebaseStore != null) {
            firebaseStore.savePersonalization(avatar, dashboardBackground, themeColor, mascot, studyStatus);
        }
    }

    public StudyTask newTask(String title, String subject, long dueAt, String priority, String note) {
        return new StudyTask(UUID.randomUUID().toString(), title, subject, dueAt, priority, note, false);
    }

    public StudyEvent newEvent(String title, String type, String subject, long startAt, long endAt, String room, String note) {
        return new StudyEvent(UUID.randomUUID().toString(), title, type, subject, startAt, endAt, room, note);
    }

    public void addOcrSampleEvents() {
        if (hasEventTitle("Lập trình Mobile") || hasEventTitle("Thi Cấu trúc dữ liệu") || hasTaskTitle("Nộp báo cáo UX")) {
            return;
        }
        saveEvent(newEvent("Lập trình Mobile", StudyEvent.TYPE_STUDY, "Mobile", DateTimeUtils.daysFromNow(1, 9, 30), DateTimeUtils.daysFromNow(1, 11, 30), "B203", "Tạo từ ảnh thời khóa biểu"));
        saveEvent(newEvent("Thi Cấu trúc dữ liệu", StudyEvent.TYPE_EXAM, "CTDL", DateTimeUtils.daysFromNow(3, 13, 0), DateTimeUtils.daysFromNow(3, 14, 30), "A405", "Mang thẻ sinh viên"));
        saveTask(newTask("Nộp báo cáo UX", "UX/UI", DateTimeUtils.daysFromNow(2, 22, 0), StudyTask.PRIORITY_HIGH, "Tạo từ ảnh lịch"));
    }

    public void seedDemoDataIfEmpty() {
        if (!getEvents().isEmpty() || !getTasks().isEmpty()) {
            return;
        }
        seedDemoScheduleData();
    }

    private boolean hasEventTitle(String title) {
        for (StudyEvent event : getEvents()) {
            if (title.equalsIgnoreCase(event.getTitle())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTaskTitle(String title) {
        for (StudyTask task : getTasks()) {
            if (title.equalsIgnoreCase(task.getTitle())) {
                return true;
            }
        }
        return false;
    }

    private SQLiteDatabase db() {
        return dbHelper.getWritableDatabase();
    }

    private void ensureRuntimeTables() {
        db().execSQL("CREATE TABLE IF NOT EXISTS focus_day_stats (day_key TEXT PRIMARY KEY, minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
        db().execSQL("CREATE TABLE IF NOT EXISTS pomodoro_sessions (id TEXT PRIMARY KEY, task_id TEXT, subject_tag TEXT, mode TEXT, duration_minutes INTEGER, completed_minutes INTEGER, started_at INTEGER, ended_at INTEGER, is_completed INTEGER, sound_type TEXT, created_at INTEGER)");
        db().execSQL("CREATE TABLE IF NOT EXISTS countdowns (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, target_date INTEGER NOT NULL, note TEXT)");
        db().execSQL("CREATE INDEX IF NOT EXISTS idx_countdowns_target_date ON countdowns(target_date)");
        ensureTaskColumn("important", "INTEGER NOT NULL DEFAULT 0");
        ensureTaskColumn("urgent", "INTEGER NOT NULL DEFAULT 0");
        ensureTaskColumn("tag", "TEXT");
        ensureTaskColumn("reminder_time", "INTEGER NOT NULL DEFAULT 0");
        ensureTaskColumn("repeat_option", "TEXT NOT NULL DEFAULT 'Không lặp'");
        ensureTaskColumn("estimated_pomodoro", "INTEGER NOT NULL DEFAULT 0");
        ensureTaskColumn("completed_pomodoros", "INTEGER NOT NULL DEFAULT 0");
        ensureTaskColumn("marker_type", "TEXT NOT NULL DEFAULT 'flag'");
        ensureTaskColumn("marker_value", "TEXT NOT NULL DEFAULT ''");
        ensureTaskColumn("show_on_calendar", "INTEGER NOT NULL DEFAULT 0");
        ensureEventColumn("reminder_enabled", "INTEGER NOT NULL DEFAULT 0");
        ensureEventColumn("reminder_before_minutes", "INTEGER NOT NULL DEFAULT 15");
        ensureEventColumn("source_task_id", "TEXT NOT NULL DEFAULT ''");
        normalizeLegacyEventTypes();
    }

    private void ensureTaskColumn(String column, String definition) {
        try (Cursor cursor = db().rawQuery("PRAGMA table_info(tasks)", null)) {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return;
                }
            }
        }
        db().execSQL("ALTER TABLE tasks ADD COLUMN " + column + " " + definition);
        if ("tag".equals(column)) {
            db().execSQL("UPDATE tasks SET tag = subject WHERE tag IS NULL OR tag = ''");
        }
        if ("important".equals(column)) {
            db().execSQL("UPDATE tasks SET important = 1 WHERE priority = ?", new Object[]{StudyTask.PRIORITY_HIGH});
        }
    }

    private void ensureEventColumn(String column, String definition) {
        try (Cursor cursor = db().rawQuery("PRAGMA table_info(events)", null)) {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                    return;
                }
            }
        }
        db().execSQL("ALTER TABLE events ADD COLUMN " + column + " " + definition);
    }

    private void normalizeLegacyEventTypes() {
        db().execSQL("UPDATE events SET type = ? WHERE LOWER(type) = 'study'", new Object[]{StudyEvent.TYPE_STUDY});
        db().execSQL("UPDATE events SET type = ? WHERE LOWER(type) = 'exam'", new Object[]{StudyEvent.TYPE_EXAM});
        db().execSQL("UPDATE events SET type = ? WHERE LOWER(type) = 'deadline'", new Object[]{StudyEvent.TYPE_DEADLINE});
        db().execSQL("UPDATE events SET type = ? WHERE LOWER(type) = 'personal' OR type = 'Cá nhân'", new Object[]{StudyEvent.TYPE_PERSONAL});
    }

    private boolean getBooleanSetting(String key, boolean defaultValue) {
        try (Cursor cursor = db().query("settings", new String[]{"value"}, "key = ?", new String[]{key}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return "1".equals(cursor.getString(0));
            }
        }
        return defaultValue;
    }

    private void setBooleanSetting(String key, boolean enabled) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", enabled ? "1" : "0");
        db().insertWithOnConflict("settings", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (firebaseStore != null) {
            firebaseStore.saveSetting(key, enabled);
        }
    }

    public void syncSnapshotToFirebase() {
        if (firebaseStore == null) {
            return;
        }
        firebaseStore.syncSnapshot(
                getProfile(),
                getTasks(),
                getEvents(),
                getCountdownMilestones(),
                getRecentPomodoroSessions(50),
                getFocusMinutes(),
                getFocusSessions(),
                getTodayFocusMinutes(),
                getTodayFocusSessions(),
                todayFocusKey(),
                isNotifyEnabled(),
                isSyncEnabled(),
                getAvatarChoice(),
                getDashboardBackgroundChoice(),
                getThemeColorChoice(),
                getMascotChoice(),
                getStudyStatus()
        );
    }

    private void syncProfileToFirebase(UserProfile profile) {
        if (firebaseStore != null) {
            firebaseStore.saveProfile(profile);
        }
    }

    private void syncTaskToFirebase(StudyTask task) {
        if (firebaseStore != null) {
            firebaseStore.saveTask(task);
        }
    }

    private void syncEventToFirebase(StudyEvent event) {
        if (firebaseStore != null) {
            firebaseStore.saveEvent(event);
        }
    }

    private void syncFocusStatsToFirebase() {
        if (firebaseStore != null) {
            firebaseStore.saveFocusStats(
                    getFocusMinutes(),
                    getFocusSessions(),
                    getTodayFocusMinutes(),
                    getTodayFocusSessions(),
                    todayFocusKey()
            );
        }
    }

    private void ensureFocusRow() {
        try (Cursor cursor = db().query("focus_stats", new String[]{"id"}, "id = 1", null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return;
            }
        }
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("minutes", 0);
        values.put("sessions", 0);
        db().insertWithOnConflict("focus_stats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void ensureTodayFocusRow() {
        String dayKey = todayFocusKey();
        try (Cursor cursor = db().query("focus_day_stats", new String[]{"day_key"}, "day_key = ?", new String[]{dayKey}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return;
            }
        }
        ContentValues values = new ContentValues();
        values.put("day_key", dayKey);
        values.put("minutes", 0);
        values.put("sessions", 0);
        db().insertWithOnConflict("focus_day_stats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private String todayFocusKey() {
        return DateTimeUtils.formatDate(System.currentTimeMillis());
    }

    private void seedIfNeeded() {
        if (hasProfile()) {
            return;
        }
        if (prefs.contains(KEY_PROFILE)) {
            migrateLegacyPrefs();
            if (hasProfile()) {
                return;
            }
        }
        saveProfile(defaultProfile());
        seedDemoScheduleData();
    }

    private void seedDemoScheduleData() {
        saveEvent(newEvent("Toán rời rạc", StudyEvent.TYPE_STUDY, "Toán", DateTimeUtils.daysFromNow(0, 9, 30), DateTimeUtils.daysFromNow(0, 11, 30), "B203", "Ôn chương 4"));
        saveEvent(newEvent("Thi Cấu trúc dữ liệu", StudyEvent.TYPE_EXAM, "CTDL", DateTimeUtils.daysFromNow(2, 13, 0), DateTimeUtils.daysFromNow(2, 14, 30), "A405", "Mang thẻ sinh viên"));
        saveEvent(newEvent("Họp nhóm Mobile", StudyEvent.TYPE_STUDY, "Mobile", DateTimeUtils.daysFromNow(1, 19, 0), DateTimeUtils.daysFromNow(1, 20, 30), "https://meet.google.com/demo-study", "Chốt demo cuối kỳ"));
        saveEvent(newEvent("Nộp slide thuyết trình", StudyEvent.TYPE_DEADLINE, "Đồ án Study Planner", DateTimeUtils.daysFromNow(3, 22, 0), DateTimeUtils.daysFromNow(3, 22, 30), "https://classroom.google.com/", "Nộp PDF và slide bản cuối"));
        saveEvent(newEvent("Mua bút highlight", StudyEvent.TYPE_PERSONAL, "Chuẩn bị học tập", DateTimeUtils.daysFromNow(1, 17, 30), DateTimeUtils.daysFromNow(1, 18, 0), "Nhà sách gần trường", "Chọn màu pastel để ghi chú"));
        saveCountdownMilestone(newCountdownMilestone("Thi cuối kỳ Mobile", CountdownMilestone.TYPE_EXAM, DateTimeUtils.startOfDay(DateTimeUtils.daysFromNow(10, 0, 0)), "Ôn lại SQLite, Activity và XML layout"));
        saveCountdownMilestone(newCountdownMilestone("Sinh nhật bạn thân", CountdownMilestone.TYPE_BIRTHDAY, DateTimeUtils.startOfDay(DateTimeUtils.daysFromNow(18, 0, 0)), "Chuẩn bị thiệp nhỏ"));
        saveCountdownMilestone(newCountdownMilestone("Ngày bảo vệ đồ án", CountdownMilestone.TYPE_EVENT, DateTimeUtils.startOfDay(DateTimeUtils.daysFromNow(25, 0, 0)), "Chốt demo và slide"));
        saveTask(newTask("Làm bài tập Chương 4", "CSDL", DateTimeUtils.daysFromNow(0, 21, 0), StudyTask.PRIORITY_HIGH, "Hoàn thành trước buổi học"));
        StudyTask androidTask = newTask("Đọc tài liệu Android", "Mobile", DateTimeUtils.daysFromNow(1, 8, 0), StudyTask.PRIORITY_MEDIUM, "Activity, XML layout, SQLite");
        androidTask.setShowOnCalendar(true);
        saveTask(androidTask);
        syncTaskDeadlineEvent(androidTask);
        saveTask(newTask("Ôn tập kiểm tra", "Giải tích", DateTimeUtils.daysFromNow(2, 20, 0), StudyTask.PRIORITY_HIGH, "Làm lại đề mẫu"));
        StudyTask done = newTask("Tóm tắt bài giảng", "UX/UI", DateTimeUtils.daysFromNow(-1, 18, 0), StudyTask.PRIORITY_LOW, "");
        done.setCompleted(true);
        saveTask(done);
    }

    private boolean hasProfile() {
        try (Cursor cursor = db().rawQuery("SELECT COUNT(*) FROM profile", null)) {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
    }

    private void migrateLegacyPrefs() {
        try {
            saveProfile(UserProfile.fromJson(new JSONObject(prefs.getString(KEY_PROFILE, "{}"))));
            JSONArray taskArray = new JSONArray(prefs.getString(KEY_TASKS, "[]"));
            for (int i = 0; i < taskArray.length(); i++) {
                saveTask(StudyTask.fromJson(taskArray.getJSONObject(i)));
            }
            JSONArray eventArray = new JSONArray(prefs.getString(KEY_EVENTS, "[]"));
            for (int i = 0; i < eventArray.length(); i++) {
                saveEvent(StudyEvent.fromJson(eventArray.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
    }

    private UserProfile defaultProfile() {
        return new UserProfile("Minh Anh", "student@email.com", "Quản lý lịch học và deadline");
    }

    private static class StudyDbHelper extends SQLiteOpenHelper {
        StudyDbHelper(Context context, String dbName) {
            super(context, dbName, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id = 1), name TEXT NOT NULL, email TEXT NOT NULL, goal TEXT NOT NULL)");
            db.execSQL("CREATE TABLE tasks (id TEXT PRIMARY KEY, title TEXT NOT NULL, subject TEXT NOT NULL, due_at INTEGER NOT NULL, priority TEXT NOT NULL, note TEXT, completed INTEGER NOT NULL DEFAULT 0, important INTEGER NOT NULL DEFAULT 0, urgent INTEGER NOT NULL DEFAULT 0, tag TEXT, reminder_time INTEGER NOT NULL DEFAULT 0, repeat_option TEXT NOT NULL DEFAULT 'Không lặp', estimated_pomodoro INTEGER NOT NULL DEFAULT 0, marker_type TEXT NOT NULL DEFAULT 'flag', marker_value TEXT NOT NULL DEFAULT '', show_on_calendar INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, subject TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, room TEXT, note TEXT, reminder_enabled INTEGER NOT NULL DEFAULT 0, reminder_before_minutes INTEGER NOT NULL DEFAULT 15, source_task_id TEXT NOT NULL DEFAULT '')");
            db.execSQL("CREATE INDEX idx_tasks_due_at ON tasks(due_at)");
            db.execSQL("CREATE INDEX idx_events_start_at ON events(start_at)");
            db.execSQL("CREATE TABLE focus_stats (id INTEGER PRIMARY KEY CHECK(id = 1), minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE focus_day_stats (day_key TEXT PRIMARY KEY, minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE pomodoro_sessions (id TEXT PRIMARY KEY, task_id TEXT, subject_tag TEXT, mode TEXT, duration_minutes INTEGER, completed_minutes INTEGER, started_at INTEGER, ended_at INTEGER, is_completed INTEGER, sound_type TEXT, created_at INTEGER)");
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            db.execSQL("CREATE TABLE countdowns (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, target_date INTEGER NOT NULL, note TEXT)");
            db.execSQL("CREATE INDEX idx_countdowns_target_date ON countdowns(target_date)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS profile (id INTEGER PRIMARY KEY CHECK(id = 1), name TEXT NOT NULL, email TEXT NOT NULL, goal TEXT NOT NULL)");
                db.execSQL("CREATE TABLE IF NOT EXISTS tasks (id TEXT PRIMARY KEY, title TEXT NOT NULL, subject TEXT NOT NULL, due_at INTEGER NOT NULL, priority TEXT NOT NULL, note TEXT, completed INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE IF NOT EXISTS events (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, subject TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, room TEXT, note TEXT)");
                db.execSQL("CREATE TABLE IF NOT EXISTS focus_stats (id INTEGER PRIMARY KEY CHECK(id = 1), minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE IF NOT EXISTS focus_day_stats (day_key TEXT PRIMARY KEY, minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE IF NOT EXISTS pomodoro_sessions (id TEXT PRIMARY KEY, task_id TEXT, subject_tag TEXT, mode TEXT, duration_minutes INTEGER, completed_minutes INTEGER, started_at INTEGER, ended_at INTEGER, is_completed INTEGER, sound_type TEXT, created_at INTEGER)");
                db.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
                db.execSQL("CREATE TABLE IF NOT EXISTS countdowns (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, target_date INTEGER NOT NULL, note TEXT)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_countdowns_target_date ON countdowns(target_date)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_due_at ON tasks(due_at)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_start_at ON events(start_at)");
                boolean addedImportant = ensureColumn(db, "tasks", "important", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(db, "tasks", "urgent", "INTEGER NOT NULL DEFAULT 0");
                boolean addedTag = ensureColumn(db, "tasks", "tag", "TEXT");
                ensureColumn(db, "tasks", "reminder_time", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(db, "tasks", "repeat_option", "TEXT NOT NULL DEFAULT 'Không lặp'");
                ensureColumn(db, "tasks", "estimated_pomodoro", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(db, "tasks", "marker_type", "TEXT NOT NULL DEFAULT 'flag'");
                ensureColumn(db, "tasks", "marker_value", "TEXT NOT NULL DEFAULT ''");
                ensureColumn(db, "tasks", "show_on_calendar", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(db, "events", "reminder_enabled", "INTEGER NOT NULL DEFAULT 0");
                ensureColumn(db, "events", "reminder_before_minutes", "INTEGER NOT NULL DEFAULT 15");
                ensureColumn(db, "events", "source_task_id", "TEXT NOT NULL DEFAULT ''");
                if (addedTag) {
                    db.execSQL("UPDATE tasks SET tag = subject WHERE tag IS NULL OR tag = ''");
                }
                if (addedImportant) {
                    db.execSQL("UPDATE tasks SET important = 1 WHERE priority = ?", new Object[]{StudyTask.PRIORITY_HIGH});
                }
            }
            if (oldVersion < 4) {
                db.execSQL("CREATE TABLE IF NOT EXISTS countdowns (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, target_date INTEGER NOT NULL, note TEXT)");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_countdowns_target_date ON countdowns(target_date)");
            }
        }

        private boolean ensureColumn(SQLiteDatabase db, String table, String column, String definition) {
            try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
                while (cursor.moveToNext()) {
                    if (column.equals(cursor.getString(cursor.getColumnIndexOrThrow("name")))) {
                        return false;
                    }
                }
            }
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            return true;
        }
    }
}
