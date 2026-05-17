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
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StudyRepository {
    private static final String PREFS = "study_planner_store";
    private static final String KEY_FIRST_OPEN = "first_open";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_EVENTS = "events";
    private static final String DB_NAME = "study_planner.db";
    private static final int DB_VERSION = 1;

    private final SharedPreferences prefs;
    private final StudyDbHelper dbHelper;

    public StudyRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        dbHelper = new StudyDbHelper(context);
        seedIfNeeded();
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
    }

    public List<StudyTask> getTasks() {
        List<StudyTask> tasks = new ArrayList<>();
        try (Cursor cursor = db().query("tasks", new String[]{"id", "title", "subject", "due_at", "priority", "note", "completed"}, null, null, null, null, "due_at ASC")) {
            while (cursor.moveToNext()) {
                tasks.add(new StudyTask(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3), cursor.getString(4), cursor.getString(5), cursor.getInt(6) == 1));
            }
        }
        return tasks;
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
        db().insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteTask(String id) {
        db().delete("tasks", "id = ?", new String[]{id});
    }

    public List<StudyEvent> getEvents() {
        List<StudyEvent> events = new ArrayList<>();
        try (Cursor cursor = db().query("events", new String[]{"id", "title", "type", "subject", "start_at", "end_at", "room", "note"}, null, null, null, null, "start_at ASC")) {
            while (cursor.moveToNext()) {
                events.add(new StudyEvent(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getLong(4), cursor.getLong(5), cursor.getString(6), cursor.getString(7)));
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
        db().insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteEvent(String id) {
        db().delete("events", "id = ?", new String[]{id});
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
            if (event.getId().equals(candidate.getId()) || StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
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

    public void addFocusSession(int minutes) {
        ensureFocusRow();
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("minutes", getFocusMinutes() + minutes);
        values.put("sessions", getFocusSessions() + 1);
        db().insertWithOnConflict("focus_stats", null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
        saveEvent(newEvent("Toán rời rạc", StudyEvent.TYPE_STUDY, "Toán", DateTimeUtils.daysFromNow(0, 9, 30), DateTimeUtils.daysFromNow(0, 11, 30), "B203", "Ôn chương 4"));
        saveEvent(newEvent("Thi Cấu trúc dữ liệu", StudyEvent.TYPE_EXAM, "CTDL", DateTimeUtils.daysFromNow(2, 13, 0), DateTimeUtils.daysFromNow(2, 14, 30), "A405", "Mang thẻ sinh viên"));
        saveEvent(newEvent("Họp nhóm Mobile", StudyEvent.TYPE_STUDY, "Mobile", DateTimeUtils.daysFromNow(1, 19, 0), DateTimeUtils.daysFromNow(1, 20, 30), "Online", "Chốt demo cuối kỳ"));
        saveTask(newTask("Làm bài tập Chương 4", "CSDL", DateTimeUtils.daysFromNow(0, 21, 0), StudyTask.PRIORITY_HIGH, "Hoàn thành trước buổi học"));
        saveTask(newTask("Đọc tài liệu Android", "Mobile", DateTimeUtils.daysFromNow(1, 8, 0), StudyTask.PRIORITY_MEDIUM, "Activity, XML layout, SQLite"));
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
        StudyDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE profile (id INTEGER PRIMARY KEY CHECK(id = 1), name TEXT NOT NULL, email TEXT NOT NULL, goal TEXT NOT NULL)");
            db.execSQL("CREATE TABLE tasks (id TEXT PRIMARY KEY, title TEXT NOT NULL, subject TEXT NOT NULL, due_at INTEGER NOT NULL, priority TEXT NOT NULL, note TEXT, completed INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, title TEXT NOT NULL, type TEXT NOT NULL, subject TEXT NOT NULL, start_at INTEGER NOT NULL, end_at INTEGER NOT NULL, room TEXT, note TEXT)");
            db.execSQL("CREATE INDEX idx_tasks_due_at ON tasks(due_at)");
            db.execSQL("CREATE INDEX idx_events_start_at ON events(start_at)");
            db.execSQL("CREATE TABLE focus_stats (id INTEGER PRIMARY KEY CHECK(id = 1), minutes INTEGER NOT NULL DEFAULT 0, sessions INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS settings");
            db.execSQL("DROP TABLE IF EXISTS focus_stats");
            db.execSQL("DROP TABLE IF EXISTS events");
            db.execSQL("DROP TABLE IF EXISTS tasks");
            db.execSQL("DROP TABLE IF EXISTS profile");
            onCreate(db);
        }
    }
}
