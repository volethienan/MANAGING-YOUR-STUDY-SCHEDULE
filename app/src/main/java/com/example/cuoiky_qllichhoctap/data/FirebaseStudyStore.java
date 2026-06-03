package com.example.cuoiky_qllichhoctap.data;

import com.example.cuoiky_qllichhoctap.model.CountdownMilestone;
import com.example.cuoiky_qllichhoctap.model.PomodoroSession;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;
import com.example.cuoiky_qllichhoctap.model.UserProfile;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirebaseStudyStore {
    private static final String ROOT_NODE = "study_users";

    private final DatabaseReference userRef;

    public FirebaseStudyStore(String accountEmail) {
        String normalizedEmail = accountEmail == null ? "" : accountEmail.trim().toLowerCase(Locale.US);
        String userKey = normalizedEmail.isEmpty() ? "anonymous" : nodeKey(normalizedEmail);
        userRef = FirebaseDatabase.getInstance().getReference(ROOT_NODE).child(userKey);
        userRef.updateChildren(baseUserMap(normalizedEmail));
    }

    public void saveProfile(UserProfile profile) {
        if (profile == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("name", profile.getName());
        values.put("email", profile.getEmail());
        values.put("goal", profile.getGoal());
        values.put("updatedAt", now());
        meta("profile").updateChildren(values);
        userRef.updateChildren(userSummary(profile.getEmail(), profile.getName()));
    }

    public void saveTask(StudyTask task) {
        if (task == null || task.getId() == null || task.getId().isEmpty()) {
            return;
        }
        collection("tasks").child(nodeKey(task.getId())).updateChildren(taskMap(task));
    }

    public void deleteTask(String taskId) {
        delete(collection("tasks").child(nodeKey(taskId)));
    }

    public void saveEvent(StudyEvent event) {
        if (event == null || event.getId() == null || event.getId().isEmpty()) {
            return;
        }
        collection("events").child(nodeKey(event.getId())).updateChildren(eventMap(event));
    }

    public void deleteEvent(String eventId) {
        delete(collection("events").child(nodeKey(eventId)));
    }

    public void saveCountdown(CountdownMilestone milestone) {
        if (milestone == null || milestone.getId() == null || milestone.getId().isEmpty()) {
            return;
        }
        collection("countdowns").child(nodeKey(milestone.getId())).updateChildren(countdownMap(milestone));
    }

    public void deleteCountdown(String id) {
        delete(collection("countdowns").child(nodeKey(id)));
    }

    public void savePomodoroSession(PomodoroSession session) {
        if (session == null || session.getId() == null || session.getId().isEmpty()) {
            return;
        }
        collection("pomodoro_sessions").child(nodeKey(session.getId())).updateChildren(pomodoroMap(session));
    }

    public void saveFocusStats(int focusMinutes, int focusSessions, int todayFocusMinutes, int todayFocusSessions, String todayKey) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("minutes", focusMinutes);
        summary.put("sessions", focusSessions);
        summary.put("todayMinutes", todayFocusMinutes);
        summary.put("todaySessions", todayFocusSessions);
        summary.put("todayKey", todayKey);
        summary.put("updatedAt", now());
        meta("focus_stats").updateChildren(summary);

        Map<String, Object> today = new HashMap<>();
        today.put("dayKey", todayKey);
        today.put("minutes", todayFocusMinutes);
        today.put("sessions", todayFocusSessions);
        today.put("updatedAt", now());
        collection("focus_day_stats").child(nodeKey(todayKey)).updateChildren(today);
    }

    public void saveSetting(String key, boolean enabled) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("key", key);
        values.put("enabled", enabled);
        values.put("value", enabled ? "1" : "0");
        values.put("updatedAt", now());
        collection("settings").child(nodeKey(key)).updateChildren(values);
    }

    public void savePersonalization(String avatar, String dashboardBackground, String themeColor, String mascot, String studyStatus) {
        Map<String, Object> values = new HashMap<>();
        values.put("avatar", avatar);
        values.put("dashboardBackground", dashboardBackground);
        values.put("themeColor", themeColor);
        values.put("mascot", mascot);
        values.put("studyStatus", studyStatus);
        values.put("updatedAt", now());
        meta("personalization").updateChildren(values);
    }

    public void syncSnapshot(UserProfile profile, Iterable<StudyTask> tasks, Iterable<StudyEvent> events,
                             Iterable<CountdownMilestone> countdowns, Iterable<PomodoroSession> sessions,
                             int focusMinutes, int focusSessions, int todayFocusMinutes, int todayFocusSessions, String todayKey,
                             boolean notifyEnabled, boolean syncEnabled,
                             String avatar, String dashboardBackground, String themeColor, String mascot, String studyStatus) {
        saveProfile(profile);
        for (StudyTask task : tasks) {
            saveTask(task);
        }
        for (StudyEvent event : events) {
            saveEvent(event);
        }
        for (CountdownMilestone milestone : countdowns) {
            saveCountdown(milestone);
        }
        for (PomodoroSession session : sessions) {
            savePomodoroSession(session);
        }
        saveFocusStats(focusMinutes, focusSessions, todayFocusMinutes, todayFocusSessions, todayKey);
        saveSetting("notify", notifyEnabled);
        saveSetting("sync", syncEnabled);
        savePersonalization(avatar, dashboardBackground, themeColor, mascot, studyStatus);
    }

    private DatabaseReference meta(String id) {
        return collection("meta").child(nodeKey(id));
    }

    private DatabaseReference collection(String name) {
        return userRef.child(name);
    }

    private void delete(DatabaseReference reference) {
        if (reference != null) {
            reference.removeValue();
        }
    }

    private Map<String, Object> baseUserMap(String email) {
        Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("lastSyncedAt", now());
        return values;
    }

    private Map<String, Object> userSummary(String email, String name) {
        Map<String, Object> values = new HashMap<>();
        values.put("email", email);
        values.put("name", name);
        values.put("lastSyncedAt", now());
        return values;
    }

    private Map<String, Object> taskMap(StudyTask task) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", task.getId());
        values.put("title", task.getTitle());
        values.put("subject", task.getSubject());
        values.put("dueAt", task.getDueAt());
        values.put("priority", task.getPriority());
        values.put("note", task.getNote());
        values.put("completed", task.isCompleted());
        values.put("important", task.isImportant());
        values.put("urgent", task.isUrgent());
        values.put("tag", task.getTag());
        values.put("reminderTime", task.getReminderTime());
        values.put("repeatOption", task.getRepeatOption());
        values.put("estimatedPomodoro", task.getEstimatedPomodoro());
        values.put("markerType", task.getMarkerType());
        values.put("markerValue", task.getMarkerValue());
        values.put("showOnCalendar", task.isShowOnCalendar());
        values.put("updatedAt", now());
        return values;
    }

    private Map<String, Object> eventMap(StudyEvent event) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", event.getId());
        values.put("title", event.getTitle());
        values.put("type", event.getType());
        values.put("subject", event.getSubject());
        values.put("startAt", event.getStartAt());
        values.put("endAt", event.getEndAt());
        values.put("room", event.getRoom());
        values.put("note", event.getNote());
        values.put("reminderEnabled", event.isReminderEnabled());
        values.put("reminderBeforeMinutes", event.getReminderBeforeMinutes());
        values.put("sourceTaskId", event.getSourceTaskId());
        values.put("updatedAt", now());
        return values;
    }

    private Map<String, Object> countdownMap(CountdownMilestone milestone) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", milestone.getId());
        values.put("title", milestone.getTitle());
        values.put("type", milestone.getType());
        values.put("targetDate", milestone.getTargetDate());
        values.put("note", milestone.getNote());
        values.put("updatedAt", now());
        return values;
    }

    private Map<String, Object> pomodoroMap(PomodoroSession session) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", session.getId());
        values.put("taskId", session.getTaskId());
        values.put("subjectTag", session.getSubjectTag());
        values.put("mode", session.getMode());
        values.put("durationMinutes", session.getDurationMinutes());
        values.put("completedMinutes", session.getCompletedMinutes());
        values.put("startedAt", session.getStartedAt());
        values.put("endedAt", session.getEndedAt());
        values.put("isCompleted", session.isCompleted());
        values.put("soundType", session.getSoundType());
        values.put("updatedAt", now());
        return values;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private String nodeKey(String value) {
        String text = value == null || value.trim().isEmpty() ? "empty" : value.trim();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
