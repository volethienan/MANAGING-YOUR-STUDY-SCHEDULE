package com.example.cuoiky_qllichhoctap.data;

import android.text.TextUtils;

import com.example.cuoiky_qllichhoctap.BuildConfig;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OtpEmailSender {
    public interface Callback {
        void onSent();

        void onError(String detail);
    }

    public void sendAsync(String email, String code, String purpose, Callback callback) {
        new Thread(() -> send(email, code, purpose, callback)).start();
    }

    private void send(String email, String code, String purpose, Callback callback) {
        StringBuilder errors = new StringBuilder();
        try {
            JSONObject payload = new JSONObject();
            payload.put("email", email);
            payload.put("code", code);
            payload.put("purpose", purpose);

            for (String baseUrl : otpBackendCandidates()) {
                try {
                    int status = postOtp(baseUrl, payload);
                    if (status >= 200 && status < 300) {
                        callback.onSent();
                        return;
                    }
                    appendOtpError(errors, baseUrl + " trả về HTTP " + status);
                } catch (Exception exception) {
                    appendOtpError(errors, baseUrl + ": " + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            appendOtpError(errors, exception.getMessage());
        }
        callback.onError(errors.toString());
    }

    private void appendOtpError(StringBuilder errors, String error) {
        if (errors.length() > 0) {
            errors.append("\n");
        }
        errors.append(error);
    }

    private List<String> otpBackendCandidates() {
        List<String> urls = new ArrayList<>();
        addOtpBackendUrls(urls, BuildConfig.OTP_BACKEND_URL);
        addOtpBackendUrl(urls, "http://127.0.0.1:8080");
        addOtpBackendUrl(urls, "http://192.168.1.238:8080");
        addOtpBackendUrl(urls, "http://10.0.2.2:8080");
        return urls;
    }

    private void addOtpBackendUrls(List<String> urls, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        for (String url : value.split(",")) {
            addOtpBackendUrl(urls, url);
        }
    }

    private void addOtpBackendUrl(List<String> urls, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        String normalized = trimTrailingSlash(url.trim());
        if (!urls.contains(normalized)) {
            urls.add(normalized);
        }
    }

    private int postOtp(String baseUrl, JSONObject payload) throws Exception {
        URL url = new URL(trimTrailingSlash(baseUrl) + "/send-otp");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            String response = readConnectionResponse(connection);
            connection.disconnect();
            throw new IOException("HTTP " + status + (TextUtils.isEmpty(response) ? "" : " - " + response));
        }
        connection.disconnect();
        return status;
    }

    private String readConnectionResponse(HttpURLConnection connection) {
        try {
            InputStream stream = connection.getErrorStream();
            if (stream == null) {
                stream = connection.getInputStream();
            }
            if (stream == null) {
                return "";
            }
            try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException ignored) {
            return "";
        }
    }

    private String trimTrailingSlash(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
