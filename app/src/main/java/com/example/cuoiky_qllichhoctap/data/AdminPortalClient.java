package com.example.cuoiky_qllichhoctap.data;

import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.BuildConfig;

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
        void onResult(boolean allowed, boolean synced, String message);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void syncUserAccess(String email, String name, String provider, AccessCallback callback) {
        executor.execute(() -> {
            if (TextUtils.isEmpty(BuildConfig.ADMIN_BACKEND_URL)) {
                callback.onResult(true, false, "");
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
                callback.onResult(!locked, true, locked ? "Tài khoản đã bị quản trị viên khóa" : "");
            } catch (Exception exception) {
                callback.onResult(true, false, exception.getMessage() == null ? "Không đồng bộ được web quản trị" : exception.getMessage());
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
                        .put("type", type)
                        .put("email", email)
                        .put("message", message));
            } catch (Exception ignored) {
            }
        });
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
}
