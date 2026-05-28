package com.example.adminweb;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"))
            .withZone(ZoneId.systemDefault());
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
        if ("/api/users".equals(path) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 200, STORE.usersJson());
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
            boolean locked = STORE.syncUser(email, field(body, "name"), field(body, "provider"), booleanField(body, "verified"));
            sendJson(exchange, 200, "{\"ok\":true,\"locked\":" + locked + "}");
            return;
        }
        if ("/api/mobile/issues".equals(path) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = body(exchange);
            STORE.createIssue(field(body, "type"), field(body, "email"), field(body, "message"));
            sendJson(exchange, 201, "{\"ok\":true}");
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

    private static String json(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intEnv(String name, int fallback) {
        try {
            return Integer.parseInt(env(name, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static final class Store {
        private final Path path;
        private final Properties values = new Properties();

        Store() {
            path = dataPath();
            load();
            seed();
        }

        synchronized boolean syncUser(String email, String name, String provider, boolean verified) {
            String normalizedEmail = email.trim().toLowerCase(Locale.US);
            String id = key(normalizedEmail);
            long now = System.currentTimeMillis();
            boolean exists = has("user", id, "email");
            set("user", id, "email", normalizedEmail);
            set("user", id, "name", empty(name, "Chưa cập nhật"));
            set("user", id, "provider", empty(provider, "email"));
            set("user", id, "verified", String.valueOf(verified));
            set("user", id, "createdAt", exists ? get("user", id, "createdAt") : String.valueOf(now));
            set("user", id, "lastSeenAt", String.valueOf(now));
            set("user", id, "locked", exists ? get("user", id, "locked") : "false");
            audit(exists ? "Đồng bộ tài khoản" : "Ghi nhận tài khoản mới", normalizedEmail);
            save();
            return Boolean.parseBoolean(get("user", id, "locked"));
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
            } else {
                throw new IllegalArgumentException("Thao tác tài khoản không hợp lệ");
            }
            save();
        }

        synchronized void createAnnouncement(String title, String body) {
            if (title.isBlank() || body.isBlank()) {
                throw new IllegalArgumentException("Thông báo cần tiêu đề và nội dung");
            }
            String id = UUID.randomUUID().toString();
            set("announcement", id, "title", title);
            set("announcement", id, "body", body);
            set("announcement", id, "active", "true");
            set("announcement", id, "createdAt", String.valueOf(System.currentTimeMillis()));
            audit("Tạo thông báo", title);
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
            set("issue", id, "type", empty(type, "general"));
            set("issue", id, "email", empty(email, "không rõ"));
            set("issue", id, "message", empty(message, "Không có mô tả lỗi"));
            set("issue", id, "status", "open");
            set("issue", id, "createdAt", String.valueOf(System.currentTimeMillis()));
            audit("Ghi nhận lỗi " + empty(type, "general"), empty(email, "không rõ"));
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
            for (String id : userIds) {
                if (Boolean.parseBoolean(get("user", id, "locked"))) {
                    locked++;
                }
                if (Boolean.parseBoolean(get("user", id, "verified"))) {
                    verified++;
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
                        .append(",\"createdAt\":\"").append(time(get("user", id, "createdAt")))
                        .append("\",\"lastSeenAt\":\"").append(time(get("user", id, "lastSeenAt"))).append("\"}");
            }
            return json.append("]}").toString();
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

        private void seed() {
            if (!ids("announcement").isEmpty()) {
                return;
            }
            String id = UUID.randomUUID().toString();
            set("announcement", id, "title", "Web quản trị đã sẵn sàng");
            set("announcement", id, "body", "Tài khoản từ app Android sẽ xuất hiện sau khi app đồng bộ với ADMIN_BACKEND_URL.");
            set("announcement", id, "active", "true");
            set("announcement", id, "createdAt", String.valueOf(System.currentTimeMillis()));
            audit("Khởi tạo dữ liệu quản trị", "Thông báo mặc định");
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
            String override = System.getenv("ADMIN_WEB_DATA");
            if (override != null && !override.isBlank()) {
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

        private String empty(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }
    }
}
