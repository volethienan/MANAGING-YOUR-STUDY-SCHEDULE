package com.example.cuoiky_qllichhoctap.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import com.example.cuoiky_qllichhoctap.BuildConfig;
import com.example.cuoiky_qllichhoctap.model.StudyEvent;
import com.example.cuoiky_qllichhoctap.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiScheduleExtractor {
    public interface Callback {
        void onSuccess(List<StudyEvent> events, String rawJson);

        void onError(String message);
    }

    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GeminiScheduleExtractor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void extract(Uri imageUri, Callback callback) {
        executor.execute(() -> {
            try {
                if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
                    callback.onError("Chưa cấu hình GEMINI_API_KEY trong local.properties");
                    return;
                }
                String imageBase64 = encodeImage(imageUri);
                String response = callGemini(imageBase64);
                String jsonText = extractResponseText(response);
                List<StudyEvent> events = parseEvents(jsonText);
                callback.onSuccess(events, jsonText);
            } catch (Exception exception) {
                callback.onError(exception.getMessage() == null ? "Không thể đọc lịch từ ảnh" : exception.getMessage());
            }
        });
    }

    private String encodeImage(Uri uri) throws Exception {
        byte[] bytes = readScaledJpeg(uri);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private byte[] readScaledJpeg(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }

        int sampleSize = 1;
        int maxSide = Math.max(bounds.outWidth, bounds.outHeight);
        while (maxSide / sampleSize > 1600) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) {
            throw new IllegalArgumentException("Ảnh không hợp lệ hoặc không đọc được");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output);
        bitmap.recycle();
        return output.toByteArray();
    }

    private String callGemini(String imageBase64) throws Exception {
        JSONObject inlineData = new JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", imageBase64);
        String prompt = "Bạn là hệ thống OCR lịch học. Ngày hiện tại là " + DateTimeUtils.formatDate(System.currentTimeMillis()) + ". "
                + "Hãy đọc ảnh thời khóa biểu/lịch học/lịch thi/deadline và chỉ trả về JSON thuần, không markdown. "
                + "Schema bắt buộc: {\"events\":[{\"title\":\"...\",\"type\":\"Lịch học|Lịch thi|Deadline\",\"subject\":\"...\",\"date\":\"dd/MM/yyyy\",\"startTime\":\"HH:mm\",\"endTime\":\"HH:mm\",\"room\":\"...\",\"note\":\"...\",\"confidence\":0.0}]}. "
                + "Nếu thiếu năm, dùng năm hiện tại. Nếu thiếu giờ kết thúc, suy luận kéo dài 60 phút. Nếu không thấy lịch, trả {\"events\":[]}.";
        JSONObject body = new JSONObject()
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("parts", new JSONArray()
                                        .put(new JSONObject().put("inline_data", inlineData))
                                        .put(new JSONObject().put("text", prompt)))));

        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(25000);
        connection.setReadTimeout(45000);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readAll(stream);
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Gemini API lỗi " + code + ": " + response);
        }
        return response;
    }

    private String readAll(InputStream input) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String extractResponseText(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONArray candidates = root.getJSONArray("candidates");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            text.append(parts.getJSONObject(i).optString("text"));
        }
        return sanitizeJsonText(text.toString());
    }

    private String sanitizeJsonText(String value) {
        String text = value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = text.lastIndexOf("```");
            if (end >= 0) {
                text = text.substring(0, end);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        return text;
    }

    private List<StudyEvent> parseEvents(String jsonText) throws Exception {
        List<StudyEvent> events = new ArrayList<>();
        JSONObject root = new JSONObject(jsonText);
        JSONArray array = root.optJSONArray("events");
        if (array == null) {
            return events;
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String title = item.optString("title", "Lịch học từ ảnh").trim();
            String type = normalizeType(item.optString("type", StudyEvent.TYPE_STUDY));
            String subject = item.optString("subject", title).trim();
            String date = item.optString("date", DateTimeUtils.formatDate(now)).trim();
            String startTime = item.optString("startTime", "08:00").trim();
            String endTime = item.optString("endTime", "").trim();
            long startAt = DateTimeUtils.combineDateAndTime(date, startTime, DateTimeUtils.daysFromNow(0, 8, 0));
            long endAt = endTime.isEmpty()
                    ? startAt + 60L * 60L * 1000L
                    : DateTimeUtils.combineDateAndTime(date, endTime, startAt + 60L * 60L * 1000L);
            if (endAt <= startAt) {
                endAt = startAt + 60L * 60L * 1000L;
            }
            events.add(new StudyEvent(
                    UUID.randomUUID().toString(),
                    title.isEmpty() ? subject : title,
                    type,
                    subject,
                    startAt,
                    endAt,
                    item.optString("room", "").trim(),
                    item.optString("note", "Tạo tự động từ ảnh").trim()
            ));
        }
        return events;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return StudyEvent.TYPE_STUDY;
        }
        String lower = type.toLowerCase();
        if (lower.contains("thi") || lower.contains("exam")) {
            return StudyEvent.TYPE_EXAM;
        }
        if (lower.contains("deadline") || lower.contains("hạn") || lower.contains("nộp")) {
            return StudyEvent.TYPE_DEADLINE;
        }
        return StudyEvent.TYPE_STUDY;
    }
}
