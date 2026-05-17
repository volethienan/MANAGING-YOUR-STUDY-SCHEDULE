package com.example.cuoiky_qllichhoctap.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class StudyRepository {
    private static final String PREFS = "study_planner_store";
    private static final String KEY_FIRST_OPEN = "first_open";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_TASKS = "tasks";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_FOCUS_MINUTES = "focus_minutes";
    private static final String KEY_FOCUS_SESSIONS = "focus_sessions";
    private static final String KEY_NOTIFY = "notify";
    private static final String KEY_SYNC = "sync";

    private final SharedPreferences prefs;

    public StudyRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        try {
            return UserProfile.fromJson(new JSONObject(prefs.getString(KEY_PROFILE, "{}")));
        } catch (JSONException exception) {
            return defaultProfile();
        }
    }

    public void saveProfile(UserProfile profile) {
        try {
            prefs.edit().putString(KEY_PROFILE, profile.toJson().toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<StudyTask> getTasks() {
        List<StudyTask> tasks = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_TASKS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                tasks.add(StudyTask.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(tasks, Comparator.comparingLong(StudyTask::getDueAt));
        return tasks;
    }

    public void saveTask(StudyTask task) {
        List<StudyTask> tasks = getTasks();
        boolean updated = false;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                updated = true;
                break;
            }
        }
        if (!updated) {
            tasks.add(task);
        }
        saveTasks(tasks);
    }

    public void deleteTask(String id) {
        List<StudyTask> tasks = getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).getId().equals(id)) {
                tasks.remove(i);
            }
        }
        saveTasks(tasks);
    }

    public List<StudyEvent> getEvents() {
        List<StudyEvent> events = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_EVENTS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                events.add(StudyEvent.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(events, Comparator.comparingLong(StudyEvent::getStartAt));
        return events;
    }

    public void saveEvent(StudyEvent event) {
        List<StudyEvent> events = getEvents();
        boolean updated = false;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.set(i, event);
                updated = true;
                break;
            }
        }
        if (!updated) {
            events.add(event);
        }
        saveEvents(events);
    }

    public void deleteEvent(String id) {
        List<StudyEvent> events = getEvents();
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).getId().equals(id)) {
                events.remove(i);
            }
        }
        saveEvents(events);
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
        return prefs.getInt(KEY_FOCUS_MINUTES, 0);
    }

    public int getFocusSessions() {
        return prefs.getInt(KEY_FOCUS_SESSIONS, 0);
    }

    public void addFocusSession(int minutes) {
        prefs.edit()
                .putInt(KEY_FOCUS_MINUTES, getFocusMinutes() + minutes)
                .putInt(KEY_FOCUS_SESSIONS, getFocusSessions() + 1)
                .apply();
    }

    public boolean isNotifyEnabled() {
        return prefs.getBoolean(KEY_NOTIFY, true);
    }

    public void setNotifyEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFY, enabled).apply();
    }

    public boolean isSyncEnabled() {
        return prefs.getBoolean(KEY_SYNC, true);
    }

    public void setSyncEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SYNC, enabled).apply();
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

    private void saveTasks(List<StudyTask> tasks) {
        JSONArray array = new JSONArray();
        for (StudyTask task : tasks) {
            try {
                array.put(task.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply();
    }

    private void saveEvents(List<StudyEvent> events) {
        JSONArray array = new JSONArray();
        for (StudyEvent event : events) {
            try {
                array.put(event.toJson());
            } catch (JSONException ignored) {
            }
        }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply();
    }

    private void seedIfNeeded() {
        if (prefs.contains(KEY_PROFILE)) {
            return;
        }

        saveProfile(defaultProfile());

        List<StudyEvent> events = new ArrayList<>();
        events.add(newEvent("Toán rời rạc", StudyEvent.TYPE_STUDY, "Toán", DateTimeUtils.daysFromNow(0, 9, 30), DateTimeUtils.daysFromNow(0, 11, 30), "B203", "Ôn chương 4"));
        events.add(newEvent("Thi Cấu trúc dữ liệu", StudyEvent.TYPE_EXAM, "CTDL", DateTimeUtils.daysFromNow(2, 13, 0), DateTimeUtils.daysFromNow(2, 14, 30), "A405", "Mang thẻ sinh viên"));
        events.add(newEvent("Họp nhóm Mobile", StudyEvent.TYPE_STUDY, "Mobile", DateTimeUtils.daysFromNow(1, 19, 0), DateTimeUtils.daysFromNow(1, 20, 30), "Online", "Chốt demo cuối kỳ"));
        saveEvents(events);

        List<StudyTask> tasks = new ArrayList<>();
        tasks.add(newTask("Làm bài tập Chương 4", "CSDL", DateTimeUtils.daysFromNow(0, 21, 0), StudyTask.PRIORITY_HIGH, "Hoàn thành trước buổi học"));
        tasks.add(newTask("Đọc tài liệu Android", "Mobile", DateTimeUtils.daysFromNow(1, 8, 0), StudyTask.PRIORITY_MEDIUM, "Activity, XML layout, SharedPreferences"));
        tasks.add(newTask("Ôn tập kiểm tra", "Giải tích", DateTimeUtils.daysFromNow(2, 20, 0), StudyTask.PRIORITY_HIGH, "Làm lại đề mẫu"));
        StudyTask done = newTask("Tóm tắt bài giảng", "UX/UI", DateTimeUtils.daysFromNow(-1, 18, 0), StudyTask.PRIORITY_LOW, "");
        done.setCompleted(true);
        tasks.add(done);
        saveTasks(tasks);
    }

    private UserProfile defaultProfile() {
        return new UserProfile("Minh Anh", "student@email.com", "Quản lý lịch học và deadline");
    }
}
