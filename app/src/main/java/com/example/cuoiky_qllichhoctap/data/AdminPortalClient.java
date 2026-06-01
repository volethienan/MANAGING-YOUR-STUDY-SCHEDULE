package com.example.cuoiky_qllichhoctap.data;

import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
                JSONObject body = new JSONObject()
                        .put("email", email)
                        .put("name", name)
                        .put("provider", provider)
                        .put("verified", true);
                JSONObject response = post("/api/mobile/users/sync", body);
                boolean locked = response.optBoolean("locked", false);
                boolean resetRequested = response.optBoolean("passwordResetRequested", false);
                callback.onResult(!locked, true, resetRequested, locked ? "Tài khoản đã bị quản trị viên khóa" : "");
            } catch (Exception exception) {
                callback.onResult(true, false, false, exception.getMessage() == null ? "Không đồng bộ được web quản trị" : exception.getMessage());
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
        HttpURLConnection connection = (HttpURLConnection) new URL(trimSlash(BuildConfig.ADMIN_BACKEND_URL) + path).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(7000);
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("Admin backend HTTP " + status + ": " + response);
        }
        return new JSONObject(response);
    }

    private JSONObject post(String path, JSONObject payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(trimSlash(BuildConfig.ADMIN_BACKEND_URL) + path).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(7000);
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
