package com.example.adminweb;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminWebServer {
    private static final Pattern JSON_STRING = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern JSON_BOOLEAN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_NUMBER = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.systemDefault());
    private static final Properties DOTENV = loadDotEnv();
    private static final Store STORE = new Store();
    private static final Set<String> SESSIONS = new HashSet<>();
    private static final String ADMIN_USERNAME = env("ADMIN_USERNAME", "admin");
    private static final String ADMIN_PASSWORD = env("ADMIN_PASSWORD", "admin123");

    public static void main(String[] args) throws Exception {
        int port = intEnv("ADMIN_WEB_PORT", 8090);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", AdminWebServer::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("Study Planner admin web started on http://localhost:" + port);
        System.out.println("Data file: " + STORE.path.toAbsolutePath());
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 204, "application/json", "");
                return;
            }
            if ("/health".equals(path)) {
                sendJson(exchange, 200, "{\"ok\":true}");
                return;
            }
            if (path.startsWith("/api/mobile/")) {
                handleMobileApi(exchange, path);
                return;
            }
            if ("/api/auth/login".equals(path)) {
                handleLogin(exchange);
                return;
            }
            if ("/api/auth/logout".equals(path)) {
                handleLogout(exchange);
                return;
            }
            if ("/api/auth/session".equals(path)) {
                sendJson(exchange, 200, "{\"authenticated\":" + isAuthenticated(exchange) + ",\"username\":\"" + json(ADMIN_USERNAME) + "\"}");
                return;
            }
            if (path.startsWith("/api/")) {
                if (!isAuthenticated(exchange)) {
                    sendJson(exchange, 401, "{\"ok\":false,\"error\":\"Admin login required\"}");
                    return;
                }
                handleAdminApi(exchange, path);
                return;
            }
            handleStatic(exchange, path);
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "Unexpected server error" : exception.getMessage();
            sendJson(exchange, 500, "{\"ok\":false,\"error\":\"" + json(message) + "\"}");
        }
    }

    private static void handleStatic(HttpExchange exchange, String path) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "text/plain", "Method not allowed");
            return;
        }
        if ("/".equals(path) || "/index.html".equals(path)) {
            sendResource(exchange, "/web/index.html", "text/html; charset=utf-8");
            return;
        }
        if ("/assets/styles.css".equals(path)) {
            sendResource(exchange, "/web/styles.css", "text/css; charset=utf-8");
            return;
        }
        if ("/assets/app.js".equals(path)) {
            sendResource(exchange, "/web/app.js", "application/javascript; charset=utf-8");
            return;
        }
        if ("/assets/mascot.png".equals(path)) {
            sendResource(exchange, "/web/mascot.png", "image/png");
            return;
        }
        send(exchange, 404, "text/plain", "Not found");
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
            return;
        }
        String body = body(exchange);
        if (!ADMIN_USERNAME.equals(field(body, "username")) || !ADMIN_PASSWORD.equals(field(body, "password"))) {
            sendJson(exchange, 401, "{\"ok\":false,\"error\":\"Sai tài khoản hoặc mật khẩu quản trị\"}");
            return;
        }
        String session = UUID.randomUUID().toString();
        synchronized (SESSIONS) {
            SESSIONS.add(session);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "admin_session=" + session + "; Path=/; HttpOnly; SameSite=Strict");
        STORE.audit("Đăng nhập quản trị", ADMIN_USERNAME);
        sendJson(exchange, 200, "{\"ok\":true,\"username\":\"" + json(ADMIN_USERNAME) + "\"}");
    }

    private static void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
            return;
        }
        String session = cookie(exchange, "admin_session");
        synchronized (SESSIONS) {
            SESSIONS.remove(session);
        }
        exchange.getResponseHeaders().add("Set-Cookie", "admin_session=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private static void handleAdminApi(HttpExchange exchange, String path) throws IOException {
        if ("/api/stats".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.statsJson());
            return;
        }
        if ("/api/analytics".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.analyticsJson());
            return;
        }
        if ("/api/users".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.usersJson());
            return;
        }
        if ("/api/users/import".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.importUser(field(body, "email"), field(body, "name"), field(body, "provider"), booleanField(body, "verified"));
            sendJson(exchange, 201, "{\"ok\":true}");
            return;
        }
        if ("/api/users/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.userAction(field(body, "email"), field(body, "action"));
            sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if ("/api/announcements".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.announcementsJson());
            return;
        }
        if ("/api/announcements".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.createAnnouncement(field(body, "title"), field(body, "body"));
            sendJson(exchange, 201, "{\"ok\":true}");
            return;
        }
        if ("/api/announcements/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.announcementAction(field(body, "id"), field(body, "action"));
            sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if ("/api/issues".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.issuesJson());
            return;
        }
        if ("/api/issues/action".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.issueAction(field(body, "id"), field(body, "action"));
            sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if ("/api/audit".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.auditJson());
            return;
        }
        sendJson(exchange, 404, "{\"ok\":false,\"error\":\"Unknown admin endpoint\"}");
    }

    private static void handleMobileApi(HttpExchange exchange, String path) throws IOException {
        if ("/api/mobile/users/sync".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            String email = field(body, "email");
            if (email.isBlank()) {
                sendJson(exchange, 400, "{\"ok\":false,\"error\":\"Email is required\"}");
                return;
            }
            Store.UserAccess access = STORE.syncUser(email, field(body, "name"), field(body, "provider"), booleanField(body, "verified"));
            sendJson(exchange, 200, "{\"ok\":true,\"locked\":" + access.locked + ",\"passwordResetRequested\":" + access.passwordResetRequested + "}");
            return;
        }
        if ("/api/mobile/issues".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.createIssue(field(body, "type"), field(body, "email"), field(body, "message"));
            sendJson(exchange, 201, "{\"ok\":true}");
            return;
        }
        if ("/api/mobile/users/password-reset-complete".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.clearPasswordResetRequest(field(body, "email"));
            sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if ("/api/mobile/learning/sync".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.syncLearningSnapshot(body);
            sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }
        if ("/api/mobile/announcements".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.publicAnnouncementsJson());
            return;
        }
        sendJson(exchange, 404, "{\"ok\":false,\"error\":\"Unknown mobile endpoint\"}");
    }

    private static boolean isAuthenticated(HttpExchange exchange) {
        String session = cookie(exchange, "admin_session");
        synchronized (SESSIONS) {
            return !session.isBlank() && SESSIONS.contains(session);
        }
    }

    private static String cookie(HttpExchange exchange, String name) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return "";
        }
        for (String line : cookies) {
            for (String part : line.split(";")) {
                String trimmed = part.trim();
                int split = trimmed.indexOf('=');
                if (split > 0 && name.equals(trimmed.substring(0, split))) {
                    return trimmed.substring(split + 1);
                }
            }
        }
        return "";
    }

    private static void sendResource(HttpExchange exchange, String resource, String contentType) throws IOException {
        try (InputStream input = AdminWebServer.class.getResourceAsStream(resource)) {
            if (input == null) {
                send(exchange, 404, "text/plain", "Missing resource");
                return;
            }
            send(exchange, 200, contentType, input.readAllBytes());
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", json);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        send(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Content-Type", contentType);
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String field(String body, String name) {
        Pattern pattern = Pattern.compile(String.format(JSON_STRING.pattern(), Pattern.quote(name)));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    private static boolean booleanField(String body, String name) {
        Pattern pattern = Pattern.compile(String.format(JSON_BOOLEAN.pattern(), Pattern.quote(name)), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static int intField(String body, String name) {
        Pattern pattern = Pattern.compile(String.format(JSON_NUMBER.pattern(), Pattern.quote(name)));
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String json(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = DOTENV.getProperty(name);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String name, int fallback) {
        try {
            return Integer.parseInt(env(name, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void sendFirebasePasswordReset(String email) {
        String apiKey = firebaseApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Thiếu FIREBASE_WEB_API_KEY để gửi email reset Firebase");
        }
        try {
            URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoOutput(true);
            String payload = "{\"requestType\":\"PASSWORD_RESET\",\"email\":\"" + json(email) + "\"}";
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            String response = new String((status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream()).readAllBytes(), StandardCharsets.UTF_8);
            connection.disconnect();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Firebase reset HTTP " + status + ": " + response);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không gửi được email reset Firebase", exception);
        }
    }

    private static String firebaseApiKey() {
        String configured = env("FIREBASE_WEB_API_KEY", "");
        if (!configured.isBlank()) {
            return configured;
        }
        return firebaseApiKeyFromGoogleServices();
    }

    private static String firebaseApiKeyFromGoogleServices() {
        Path[] candidates = {
                Paths.get("..", "app", "google-services.json"),
                Paths.get("app", "google-services.json")
        };
        Pattern pattern = Pattern.compile("\"current_key\"\\s*:\\s*\"([^\"]+)\"");
        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                Matcher matcher = pattern.matcher(Files.readString(path, StandardCharsets.UTF_8));
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (IOException exception) {
                System.err.println("Cannot read Firebase API key at " + path + ": " + exception.getMessage());
            }
        }
        return "";
    }

    private static Properties loadDotEnv() {
        Properties values = new Properties();
        Path[] candidates = {Paths.get(".env"), Paths.get("..", ".env")};
        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int split = trimmed.indexOf('=');
                    if (split <= 0) {
                        continue;
                    }
                    String key = trimmed.substring(0, split).trim();
                    String value = unquote(trimmed.substring(split + 1).trim());
                    values.setProperty(key, value);
                }
                break;
            } catch (IOException exception) {
                System.err.println("Cannot read .env at " + path + ": " + exception.getMessage());
            }
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static final class Store {
        private final Path path;
        private final Properties values = new Properties();

        Store() {
            path = dataPath();
            load();
            seed();
        }

        synchronized UserAccess syncUser(String email, String name, String provider, boolean verified) {
            String normalizedEmail = limit(email.trim().toLowerCase(Locale.US), 120);
            String id = key(normalizedEmail);
            long now = System.currentTimeMillis();
            boolean exists = has("user", id, "email");
            set("user", id, "email", normalizedEmail);
            set("user", id, "name", limit(empty(name, "Chưa cập nhật"), 80));
            set("user", id, "provider", loginProvider(provider));
            set("user", id, "verified", String.valueOf(verified));
            set("user", id, "createdAt", exists ? get("user", id, "createdAt") : String.valueOf(now));
            set("user", id, "lastSeenAt", String.valueOf(now));
            set("user", id, "locked", exists ? get("user", id, "locked") : "false");
            set("user", id, "passwordResetRequested", exists ? get("user", id, "passwordResetRequested") : "false");
            audit(exists ? "Đồng bộ tài khoản" : "Ghi nhận tài khoản mới", normalizedEmail);
            save();
            return new UserAccess(
                    Boolean.parseBoolean(get("user", id, "locked")),
                    Boolean.parseBoolean(get("user", id, "passwordResetRequested"))
            );
        }

        synchronized void importUser(String email, String name, String provider, boolean verified) {
            String normalizedEmail = limit(email.trim().toLowerCase(Locale.US), 120);
            if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
                throw new IllegalArgumentException("Email tài khoản không hợp lệ");
            }
            syncUser(normalizedEmail, empty(name, normalizedEmail), empty(provider, "email"), verified);
            audit("Nhập tài khoản vào registry", normalizedEmail);
            save();
        }

        synchronized void syncLearningSnapshot(String body) {
            String email = field(body, "email").trim().toLowerCase(Locale.US);
            if (email.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            String id = key(email);
            if (!has("user", id, "email")) {
                syncUser(email, field(body, "name"), field(body, "provider"), true);
            }
            set("user", id, "learningSyncedAt", String.valueOf(System.currentTimeMillis()));
            setInt("user", id, "totalTasks", intField(body, "totalTasks"));
            setInt("user", id, "completedTasks", intField(body, "completedTasks"));
            setInt("user", id, "totalEvents", intField(body, "totalEvents"));
            setInt("user", id, "studyEvents", intField(body, "studyEvents"));
            setInt("user", id, "examEvents", intField(body, "examEvents"));
            setInt("user", id, "deadlineEvents", intField(body, "deadlineEvents"));
            setInt("user", id, "focusMinutes", intField(body, "focusMinutes"));
            setInt("user", id, "focusSessions", intField(body, "focusSessions"));
            setInt("user", id, "todayFocusMinutes", intField(body, "todayFocusMinutes"));
            setInt("user", id, "todayFocusSessions", intField(body, "todayFocusSessions"));
            setInt("user", id, "preferredHour", intField(body, "preferredHour"));
            set("user", id, "topSubject", limit(empty(field(body, "topSubject"), "Chưa có"), 80));
            set("user", id, "featureUsage", limit(empty(field(body, "featureUsage"), "schedule,tasks"), 120));
            audit("Đồng bộ thống kê học tập", email);
            save();
        }

        synchronized void userAction(String email, String action) {
            String id = key(email.trim().toLowerCase(Locale.US));
            if (!has("user", id, "email")) {
                throw new IllegalArgumentException("Không tìm thấy người dùng");
            }
            if ("delete".equals(action)) {
                removeGroup("user", id);
                audit("Xóa tài khoản khỏi registry", email);
            } else if ("lock".equals(action) || "unlock".equals(action)) {
                set("user", id, "locked", String.valueOf("lock".equals(action)));
                audit("lock".equals(action) ? "Khóa tài khoản" : "Mở khóa tài khoản", email);
            } else if ("requestReset".equals(action) || "clearReset".equals(action)) {
                set("user", id, "passwordResetRequested", String.valueOf("requestReset".equals(action)));
                if ("requestReset".equals(action)) {
                    try {
                        sendFirebasePasswordReset(get("user", id, "email"));
                        audit("Gửi email reset Firebase", email);
                    } catch (IllegalStateException exception) {
                        audit("Đánh dấu reset trong app", email + " - " + exception.getMessage());
                    }
                }
                audit("requestReset".equals(action) ? "Yêu cầu đặt lại mật khẩu" : "Gỡ yêu cầu đặt lại mật khẩu", email);
            } else {
                throw new IllegalArgumentException("Thao tác tài khoản không hợp lệ");
            }
            save();
        }

        synchronized void clearPasswordResetRequest(String email) {
            String normalizedEmail = email.trim().toLowerCase(Locale.US);
            if (normalizedEmail.isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            String id = key(normalizedEmail);
            if (!has("user", id, "email")) {
                return;
            }
            if (Boolean.parseBoolean(get("user", id, "passwordResetRequested"))) {
                set("user", id, "passwordResetRequested", "false");
                audit("Người dùng đã đặt lại mật khẩu", normalizedEmail);
                save();
            }
        }

        synchronized void createAnnouncement(String title, String body) {
            String cleanTitle = limit(empty(title, ""), 120);
            String cleanBody = limit(empty(body, ""), 600);
            if (cleanTitle.isBlank() || cleanBody.isBlank()) {
                throw new IllegalArgumentException("Thông báo cần tiêu đề và nội dung");
            }
            String id = UUID.randomUUID().toString();
            set("announcement", id, "title", cleanTitle);
            set("announcement", id, "body", cleanBody);
            set("announcement", id, "active", "true");
            set("announcement", id, "createdAt", String.valueOf(System.currentTimeMillis()));
            audit("Tạo thông báo", cleanTitle);
            save();
        }

        synchronized void announcementAction(String id, String action) {
            if (!has("announcement", id, "title")) {
                throw new IllegalArgumentException("Không tìm thấy thông báo");
            }
            if ("delete".equals(action)) {
                audit("Xóa thông báo", get("announcement", id, "title"));
                removeGroup("announcement", id);
            } else if ("toggle".equals(action)) {
                boolean active = Boolean.parseBoolean(get("announcement", id, "active"));
                set("announcement", id, "active", String.valueOf(!active));
                audit(active ? "Tắt thông báo" : "Bật thông báo", get("announcement", id, "title"));
            } else {
                throw new IllegalArgumentException("Thao tác thông báo không hợp lệ");
            }
            save();
        }

        synchronized void createIssue(String type, String email, String message) {
            String id = UUID.randomUUID().toString();
            String cleanType = issueType(type);
            String cleanEmail = limit(empty(email, "không rõ"), 120);
            String cleanMessage = limit(empty(message, "Không có mô tả lỗi"), 900);
            set("issue", id, "type", cleanType);
            set("issue", id, "email", cleanEmail);
            set("issue", id, "message", cleanMessage);
            set("issue", id, "status", "open");
            set("issue", id, "createdAt", String.valueOf(System.currentTimeMillis()));
            audit("Ghi nhận lỗi " + cleanType, cleanEmail);
            save();
        }

        synchronized void issueAction(String id, String action) {
            if (!has("issue", id, "type")) {
                throw new IllegalArgumentException("Không tìm thấy lỗi");
            }
            if ("delete".equals(action)) {
                audit("Xóa lỗi", get("issue", id, "type"));
                removeGroup("issue", id);
            } else if ("resolve".equals(action) || "reopen".equals(action)) {
                set("issue", id, "status", "resolve".equals(action) ? "resolved" : "open");
                audit("resolve".equals(action) ? "Đóng lỗi" : "Mở lại lỗi", get("issue", id, "type"));
            } else {
                throw new IllegalArgumentException("Thao tác lỗi không hợp lệ");
            }
            save();
        }

        synchronized String statsJson() {
            List<String> userIds = ids("user");
            int locked = 0;
            int verified = 0;
            int resetRequests = 0;
            for (String id : userIds) {
                if (Boolean.parseBoolean(get("user", id, "locked"))) {
                    locked++;
                }
                if (Boolean.parseBoolean(get("user", id, "verified"))) {
                    verified++;
                }
                if (Boolean.parseBoolean(get("user", id, "passwordResetRequested"))) {
                    resetRequests++;
                }
            }
            int openIssues = 0;
            for (String id : ids("issue")) {
                if ("open".equals(get("issue", id, "status"))) {
                    openIssues++;
                }
            }
            int activeAnnouncements = 0;
            for (String id : ids("announcement")) {
                if (Boolean.parseBoolean(get("announcement", id, "active"))) {
                    activeAnnouncements++;
                }
            }
            return "{\"users\":" + userIds.size()
                    + ",\"verified\":" + verified
                    + ",\"locked\":" + locked
                    + ",\"resetRequests\":" + resetRequests
                    + ",\"openIssues\":" + openIssues
                    + ",\"activeAnnouncements\":" + activeAnnouncements + "}";
        }

        synchronized String usersJson() {
            List<String> ids = ids("user");
            ids.sort(Comparator.comparingLong((String id) -> longValue(get("user", id, "lastSeenAt"))).reversed());
            StringBuilder json = new StringBuilder("{\"users\":[");
            for (String id : ids) {
                comma(json);
                json.append("{\"email\":\"").append(json(get("user", id, "email")))
                        .append("\",\"name\":\"").append(json(get("user", id, "name")))
                        .append("\",\"provider\":\"").append(json(get("user", id, "provider")))
                        .append("\",\"verified\":").append(Boolean.parseBoolean(get("user", id, "verified")))
                        .append(",\"locked\":").append(Boolean.parseBoolean(get("user", id, "locked")))
                        .append(",\"passwordResetRequested\":").append(Boolean.parseBoolean(get("user", id, "passwordResetRequested")))
                        .append(",\"totalTasks\":").append(intValue(get("user", id, "totalTasks")))
                        .append(",\"completedTasks\":").append(intValue(get("user", id, "completedTasks")))
                        .append(",\"totalEvents\":").append(intValue(get("user", id, "totalEvents")))
                        .append(",\"focusMinutes\":").append(intValue(get("user", id, "focusMinutes")))
                        .append(",\"focusSessions\":").append(intValue(get("user", id, "focusSessions")))
                        .append(",\"topSubject\":\"").append(json(empty(get("user", id, "topSubject"), "Chưa có")))
                        .append("\",\"learningSyncedAt\":\"").append(time(get("user", id, "learningSyncedAt")))
                        .append("\",\"createdAt\":\"").append(time(get("user", id, "createdAt")))
                        .append("\",\"lastSeenAt\":\"").append(time(get("user", id, "lastSeenAt"))).append("\"}");
            }
            return json.append("]}").toString();
        }

        synchronized String analyticsJson() {
            List<String> userIds = ids("user");
            long now = System.currentTimeMillis();
            long todayStart = now - (now % (24L * 60L * 60L * 1000L));
            long monthAgo = now - 30L * 24L * 60L * 60L * 1000L;
            int dau = 0;
            int mau = 0;
            int verified = 0;
            int locked = 0;
            int totalTasks = 0;
            int completedTasks = 0;
            int totalEvents = 0;
            int focusMinutes = 0;
            int focusSessions = 0;
            int[] growth = new int[7];
            int[] hourBuckets = new int[4];
            Map<String, Integer> subjects = new HashMap<>();
            Map<String, Integer> features = new HashMap<>();
            for (String id : userIds) {
                long lastSeen = longValue(get("user", id, "lastSeenAt"));
                if (lastSeen >= todayStart) {
                    dau++;
                }
                if (lastSeen >= monthAgo) {
                    mau++;
                }
                if (Boolean.parseBoolean(get("user", id, "verified"))) {
                    verified++;
                }
                if (Boolean.parseBoolean(get("user", id, "locked"))) {
                    locked++;
                }
                long createdAt = longValue(get("user", id, "createdAt"));
                int dayIndex = (int) ((createdAt - (todayStart - 6L * 24L * 60L * 60L * 1000L)) / (24L * 60L * 60L * 1000L));
                if (dayIndex >= 0 && dayIndex < growth.length) {
                    growth[dayIndex]++;
                }
                int tasks = intValue(get("user", id, "totalTasks"));
                int completed = intValue(get("user", id, "completedTasks"));
                int events = intValue(get("user", id, "totalEvents"));
                int minutes = intValue(get("user", id, "focusMinutes"));
                int sessions = intValue(get("user", id, "focusSessions"));
                totalTasks += tasks;
                completedTasks += completed;
                totalEvents += events;
                focusMinutes += minutes;
                focusSessions += sessions;
                String subject = empty(get("user", id, "topSubject"), "");
                if (!subject.isBlank()) {
                    subjects.put(subject, subjects.getOrDefault(subject, 0) + Math.max(1, tasks + events + sessions));
                }
                int hour = intValue(get("user", id, "preferredHour"));
                if (hour >= 0 && hour <= 23) {
                    hourBuckets[Math.min(3, hour / 6)]++;
                }
                for (String feature : get("user", id, "featureUsage").split(",")) {
                    String cleaned = feature.trim();
                    if (!cleaned.isEmpty()) {
                        features.put(cleaned, features.getOrDefault(cleaned, 0) + 1);
                    }
                }
            }
            int openIssues = countOpenIssues();
            int activeAnnouncements = countActiveAnnouncements();
            int completionRate = totalTasks == 0 ? 0 : Math.round((completedTasks * 100f) / totalTasks);
            return "{\"summary\":{"
                    + "\"users\":" + userIds.size()
                    + ",\"verified\":" + verified
                    + ",\"locked\":" + locked
                    + ",\"dau\":" + dau
                    + ",\"mau\":" + mau
                    + ",\"totalTasks\":" + totalTasks
                    + ",\"completedTasks\":" + completedTasks
                    + ",\"completionRate\":" + completionRate
                    + ",\"totalEvents\":" + totalEvents
                    + ",\"focusMinutes\":" + focusMinutes
                    + ",\"focusSessions\":" + focusSessions
                    + ",\"openIssues\":" + openIssues
                    + ",\"activeAnnouncements\":" + activeAnnouncements
                    + "},\"growth\":" + growthJson(growth)
                    + ",\"studyHours\":[{\"label\":\"0-5h\",\"value\":" + hourBuckets[0] + "},{\"label\":\"6-11h\",\"value\":" + hourBuckets[1] + "},{\"label\":\"12-17h\",\"value\":" + hourBuckets[2] + "},{\"label\":\"18-23h\",\"value\":" + hourBuckets[3] + "}]"
                    + ",\"subjects\":" + mapJson(subjects, 5)
                    + ",\"features\":" + mapJson(features, 5)
                    + "}";
        }

        private static final class UserAccess {
            final boolean locked;
            final boolean passwordResetRequested;

            UserAccess(boolean locked, boolean passwordResetRequested) {
                this.locked = locked;
                this.passwordResetRequested = passwordResetRequested;
            }
        }

        synchronized String announcementsJson() {
            return announcementsJson(false);
        }

        synchronized String publicAnnouncementsJson() {
            return announcementsJson(true);
        }

        synchronized String issuesJson() {
            List<String> ids = ids("issue");
            ids.sort(Comparator.comparingLong((String id) -> longValue(get("issue", id, "createdAt"))).reversed());
            StringBuilder json = new StringBuilder("{\"issues\":[");
            for (String id : ids) {
                comma(json);
                json.append("{\"id\":\"").append(json(id))
                        .append("\",\"type\":\"").append(json(get("issue", id, "type")))
                        .append("\",\"email\":\"").append(json(get("issue", id, "email")))
                        .append("\",\"message\":\"").append(json(get("issue", id, "message")))
                        .append("\",\"status\":\"").append(json(get("issue", id, "status")))
                        .append("\",\"createdAt\":\"").append(time(get("issue", id, "createdAt"))).append("\"}");
            }
            return json.append("]}").toString();
        }

        synchronized String auditJson() {
            List<String> ids = ids("audit");
            ids.sort(Comparator.comparingLong((String id) -> longValue(get("audit", id, "createdAt"))).reversed());
            StringBuilder json = new StringBuilder("{\"entries\":[");
            int limit = Math.min(ids.size(), 12);
            for (int index = 0; index < limit; index++) {
                String id = ids.get(index);
                comma(json);
                json.append("{\"action\":\"").append(json(get("audit", id, "action")))
                        .append("\",\"detail\":\"").append(json(get("audit", id, "detail")))
                        .append("\",\"createdAt\":\"").append(time(get("audit", id, "createdAt"))).append("\"}");
            }
            return json.append("]}").toString();
        }

        synchronized void audit(String action, String detail) {
            String id = UUID.randomUUID().toString();
            set("audit", id, "action", action);
            set("audit", id, "detail", detail);
            set("audit", id, "createdAt", String.valueOf(System.currentTimeMillis()));
        }

        private String announcementsJson(boolean activeOnly) {
            List<String> ids = ids("announcement");
            ids.sort(Comparator.comparingLong((String id) -> longValue(get("announcement", id, "createdAt"))).reversed());
            StringBuilder json = new StringBuilder("{\"announcements\":[");
            for (String id : ids) {
                boolean active = Boolean.parseBoolean(get("announcement", id, "active"));
                if (activeOnly && !active) {
                    continue;
                }
                comma(json);
                json.append("{\"id\":\"").append(json(id))
                        .append("\",\"title\":\"").append(json(get("announcement", id, "title")))
                        .append("\",\"body\":\"").append(json(get("announcement", id, "body")))
                        .append("\",\"active\":").append(active)
                        .append(",\"createdAt\":\"").append(time(get("announcement", id, "createdAt"))).append("\"}");
            }
            return json.append("]}").toString();
        }

        private int countOpenIssues() {
            int open = 0;
            for (String id : ids("issue")) {
                if ("open".equals(get("issue", id, "status"))) {
                    open++;
                }
            }
            return open;
        }

        private int countActiveAnnouncements() {
            int active = 0;
            for (String id : ids("announcement")) {
                if (Boolean.parseBoolean(get("announcement", id, "active"))) {
                    active++;
                }
            }
            return active;
        }

        private String growthJson(int[] growth) {
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < growth.length; index++) {
                comma(json);
                json.append("{\"label\":\"D-").append(growth.length - index - 1)
                        .append("\",\"value\":").append(growth[index]).append("}");
            }
            return json.append("]").toString();
        }

        private String mapJson(Map<String, Integer> values, int limit) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
            entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
            StringBuilder json = new StringBuilder("[");
            int count = Math.min(limit, entries.size());
            for (int index = 0; index < count; index++) {
                Map.Entry<String, Integer> entry = entries.get(index);
                comma(json);
                json.append("{\"label\":\"").append(json(entry.getKey()))
                        .append("\",\"value\":").append(entry.getValue()).append("}");
            }
            return json.append("]").toString();
        }

        private void seed() {
            if (ids("announcement").isEmpty()) {
                String id = UUID.randomUUID().toString();
                set("announcement", id, "title", "Web quản trị đã sẵn sàng");
                set("announcement", id, "body", "Tài khoản từ app Android sẽ xuất hiện sau khi app đồng bộ với ADMIN_BACKEND_URL.");
                set("announcement", id, "active", "true");
                set("announcement", id, "createdAt", String.valueOf(System.currentTimeMillis()));
                audit("Khởi tạo dữ liệu quản trị", "Thông báo mặc định");
            }
            save();
        }

        private void load() {
            try {
                if (Files.exists(path)) {
                    try (InputStream input = Files.newInputStream(path)) {
                        values.load(input);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Không đọc được dữ liệu quản trị", exception);
            }
        }

        private void save() {
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream output = Files.newOutputStream(path)) {
                    values.store(output, "Study Planner admin web data");
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Không lưu được dữ liệu quản trị", exception);
            }
        }

        private Path dataPath() {
            String override = env("ADMIN_WEB_DATA", "");
            if (!override.isBlank()) {
                return Paths.get(override);
            }
            return Paths.get("data", "admin-store.properties");
        }

        private String key(String value) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private boolean has(String group, String id, String field) {
            return values.containsKey(group + "." + id + "." + field);
        }

        private String get(String group, String id, String field) {
            return values.getProperty(group + "." + id + "." + field, "");
        }

        private void set(String group, String id, String field, String value) {
            values.setProperty(group + "." + id + "." + field, value == null ? "" : value);
        }

        private void setInt(String group, String id, String field, int value) {
            set(group, id, field, String.valueOf(Math.max(0, value)));
        }

        private List<String> ids(String group) {
            List<String> ids = new ArrayList<>();
            String prefix = group + ".";
            for (String name : values.stringPropertyNames()) {
                if (!name.startsWith(prefix)) {
                    continue;
                }
                int end = name.indexOf('.', prefix.length());
                if (end <= prefix.length()) {
                    continue;
                }
                String id = name.substring(prefix.length(), end);
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        }

        private void removeGroup(String group, String id) {
            String prefix = group + "." + id + ".";
            List<String> names = new ArrayList<>(values.stringPropertyNames());
            for (String name : names) {
                if (name.startsWith(prefix)) {
                    values.remove(name);
                }
            }
        }

        private void comma(StringBuilder json) {
            if (json.charAt(json.length() - 1) != '[') {
                json.append(',');
            }
        }

        private String time(String millis) {
            long value = longValue(millis);
            return value <= 0 ? "Chưa có" : TIME_FORMAT.format(Instant.ofEpochMilli(value));
        }

        private long longValue(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException exception) {
                return 0L;
            }
        }

        private int intValue(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }

        private String empty(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }

        private String issueType(String value) {
            String type = value == null ? "" : value.trim().toLowerCase(Locale.US);
            if ("otp".equals(type) || "ai".equals(type)) {
                return type;
            }
            return "general";
        }

        private String loginProvider(String value) {
            String provider = value == null ? "" : value.trim().toLowerCase(Locale.US);
            if ("google".equals(provider)) {
                return "google";
            }
            return "email";
        }

        private String limit(String value, int maxLength) {
            String cleaned = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
            return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength);
        }
    }
}
