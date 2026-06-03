package com.example.cuoiky_qllichhoctap.data;

import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.BuildConfig;
import com.example.cuoiky_qllichhoctap.model.PomodoroSession;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.model.StudyTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminPortalClient {
    public interface AccessCallback {
        void onResult(boolean allowed, boolean synced, boolean passwordResetRequested, String message);
    }

    public interface AnnouncementCallback {
        void onResult(String id, String title, String body, boolean loaded, String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void syncUserAccess(String email, String name, String provider, AccessCallback callback) {
        executor.execute(() -> {
            if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL)) {
                callback.onResult(true, false, false, "");
                return;
            }
            try {
                JSONObject response = post("/api/mobile/users/sync", userPayload(email, name, provider, true));
                boolean locked = response.optBoolean("locked", false);
                boolean resetRequested = response.optBoolean("passwordResetRequested", false);
                callback.onResult(!locked, true, resetRequested, locked ? "Tài khoản đã bị quản trị viên khóa" : "");
            } catch (Exception exception) {
                callback.onResult(true, false, false, exception.getMessage() == null ? "Không đồng bộ được web quản trị" : exception.getMessage());
            }
        });
    }

    public void syncRegisteredUser(String email, String name, String provider, boolean verified) {
        if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL)) {
            return;
        }
        executor.execute(() -> {
            try {
                post("/api/mobile/users/sync", userPayload(email, name, provider, verified));
            } catch (Exception ignored) {
            }
        });
    }

    public void syncLearningSnapshot(String email, String name, String provider, List<StudyTask> tasks, List<StudyEvent> events,
                                     int focusMinutes, int focusSessions, int todayFocusMinutes, int todayFocusSessions,
                                     List<PomodoroSession> recentSessions) {
        if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL) || TextUtils.isEmpty(email)) {
            return;
        }
        executor.execute(() -> {
            try {
                post("/api/mobile/learning/sync", learningPayload(
                        email,
                        name,
                        provider,
                        tasks,
                        events,
                        focusMinutes,
                        focusSessions,
                        todayFocusMinutes,
                        todayFocusSessions,
                        recentSessions));
            } catch (Exception ignored) {
            }
        });
    }

    public void reportIssue(String type, String email, String message) {
        if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL)) {
            return;
        }
        executor.execute(() -> {
            try {
                post("/api/mobile/issues", new JSONObject()
                        .put("type", normalizeIssueType(type))
                        .put("email", safeText(email, 120, "không rõ"))
                        .put("message", safeText(message, 900, "Không có mô tả lỗi")));
            } catch (Exception ignored) {
            }
        });
    }

    public void notifyPasswordResetComplete(String email) {
        if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL) || TextUtils.isEmpty(email)) {
            return;
        }
        executor.execute(() -> {
            try {
                post("/api/mobile/users/password-reset-complete", new JSONObject().put("email", email));
            } catch (Exception ignored) {
            }
        });
    }

    public void fetchLatestAnnouncement(AnnouncementCallback callback) {
        executor.execute(() -> {
            if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL)) {
                callback.onResult("", "", "", false, "");
                return;
            }
            try {
                JSONObject response = get("/api/mobile/announcements");
                JSONArray announcements = response.optJSONArray("announcements");
                if (announcements == null || announcements.length() == 0) {
                    callback.onResult("", "", "", true, "");
                    return;
                }
                JSONObject latest = announcements.getJSONObject(0);
                callback.onResult(
                        latest.optString("id"),
                        latest.optString("title"),
                        latest.optString("body"),
                        true,
                        "");
            } catch (Exception exception) {
                callback.onResult("", "", "", false, exception.getMessage() == null ? "Không tải được thông báo" : exception.getMessage());
            }
        });
    }

    private JSONObject get(String path) throws Exception {
        Exception lastError = null;
        for (String baseUrl : backendUrls()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(trimSlash(baseUrl) + path).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(5000);
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readAll(stream);
                connection.disconnect();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Admin backend HTTP " + status + ": " + response);
                }
                return new JSONObject(response);
            } catch (Exception exception) {
                lastError = exception;
            }
        }
        throw lastError == null ? new IllegalStateException("Không kết nối được web quản trị") : lastError;
    }

    private JSONObject post(String path, JSONObject payload) throws Exception {
        Exception lastError = null;
        for (String baseUrl : backendUrls()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(trimSlash(baseUrl) + path).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readAll(stream);
                connection.disconnect();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("Admin backend HTTP " + status + ": " + response);
                }
                return new JSONObject(response);
            } catch (Exception exception) {
                lastError = exception;
            }
        }
        throw lastError == null ? new IllegalStateException("Không kết nối được web quản trị") : lastError;
    }

    private List<String> backendUrls() {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        String configured = BuildConfig.ADMIN_BACKEND_URL == null ? "" : BuildConfig.ADMIN_BACKEND_URL.trim();
        if (!configured.isEmpty()) {
            for (String url : configured.split(",")) {
                String cleaned = url.trim();
                if (!cleaned.isEmpty()) {
                    urls.add(cleaned);
                }
            }
        }
        urls.add("http://127.0.0.1:8090");
        urls.add("http://10.0.2.2:8090");
        return new ArrayList<>(urls);
    }

    private JSONObject userPayload(String email, String name, String provider, boolean verified) throws Exception {
        return new JSONObject()
                .put("email", email)
                .put("name", name)
                .put("provider", provider)
                .put("verified", verified);
    }

    private JSONObject learningPayload(String email, String name, String provider, List<StudyTask> tasks, List<StudyEvent> events,
                                       int focusMinutes, int focusSessions, int todayFocusMinutes, int todayFocusSessions,
                                       List<PomodoroSession> recentSessions) throws Exception {
        int completedTasks = 0;
        Map<String, Integer> subjects = new HashMap<>();
        for (StudyTask task : tasks) {
            if (task.isCompleted()) {
                completedTasks++;
            }
            addCount(subjects, task.getTag());
        }
        int studyEvents = 0;
        int examEvents = 0;
        int deadlineEvents = 0;
        int preferredHour = 19;
        int[] hours = new int[24];
        Calendar calendar = Calendar.getInstance();
        for (StudyEvent event : events) {
            if (StudyEvent.TYPE_EXAM.equals(event.getType())) {
                examEvents++;
            } else if (StudyEvent.TYPE_DEADLINE.equals(event.getType())) {
                deadlineEvents++;
            } else if (StudyEvent.TYPE_STUDY.equals(event.getType())) {
                studyEvents++;
            }
            addCount(subjects, event.getSubject());
            calendar.setTimeInMillis(event.getStartAt());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour >= 0 && hour < hours.length) {
                hours[hour]++;
            }
        }
        for (PomodoroSession session : recentSessions) {
            addCount(subjects, session.getSubjectTag());
        }
        int bestHourCount = 0;
        for (int hour = 0; hour < hours.length; hour++) {
            if (hours[hour] > bestHourCount) {
                bestHourCount = hours[hour];
                preferredHour = hour;
            }
        }
        String features = "tasks";
        if (!events.isEmpty()) {
            features += ",schedule";
        }
        if (focusSessions > 0) {
            features += ",pomodoro";
        }
        return new JSONObject()
                .put("email", email)
                .put("name", name)
                .put("provider", provider)
                .put("totalTasks", tasks.size())
                .put("completedTasks", completedTasks)
                .put("totalEvents", events.size())
                .put("studyEvents", studyEvents)
                .put("examEvents", examEvents)
                .put("deadlineEvents", deadlineEvents)
                .put("focusMinutes", focusMinutes)
                .put("focusSessions", focusSessions)
                .put("todayFocusMinutes", todayFocusMinutes)
                .put("todayFocusSessions", todayFocusSessions)
                .put("topSubject", topKey(subjects))
                .put("preferredHour", preferredHour)
                .put("featureUsage", features);
    }

    private void addCount(Map<String, Integer> values, String key) {
        String cleaned = key == null ? "" : key.trim();
        if (!cleaned.isEmpty()) {
            values.put(cleaned, values.getOrDefault(cleaned, 0) + 1);
        }
    }

    private String topKey(Map<String, Integer> values) {
        String best = "";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String trimSlash(String value) {
        String url = value == null ? "" : value.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String normalizeIssueType(String type) {
        String value = type == null ? "" : type.trim().toLowerCase();
        if ("otp".equals(value) || "ai".equals(value)) {
            return value;
        }
        return "general";
    }

    private String safeText(String value, int maxLength, String fallback) {
        String cleaned = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
    }
}
