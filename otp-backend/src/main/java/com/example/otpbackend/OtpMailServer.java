package com.example.otpbackend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class OtpMailServer {
    private static final Pattern JSON_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern OTP = Pattern.compile("^\\d{6}$");
    private static final int MAX_REQUESTS_PER_WINDOW = intEnv("OTP_RATE_LIMIT", 5);
    private static final long RATE_LIMIT_WINDOW_MS = 10L * 60L * 1000L;
    private static final Map<String, Deque<Long>> RATE_LIMITS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        int port = intEnv("OTP_BACKEND_PORT", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/send-otp", OtpMailServer::handleSendOtp);
        server.createContext("/health", OtpMailServer::handleHealth);
        server.start();
        System.out.println("OTP mail backend started on http://localhost:" + port);
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCors(exchange, 204, "");
            return;
        }
        sendCors(exchange, 200, "{\"ok\":true}");
    }

    private static void handleSendOtp(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCors(exchange, 204, "");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendCors(exchange, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String email = jsonField(body, "email");
        String code = jsonField(body, "code");
        String purpose = cleanPurpose(jsonField(body, "purpose"));
        if (email.isBlank() || code.isBlank()) {
            sendCors(exchange, 400, "{\"ok\":false,\"error\":\"Missing email or code\"}");
            return;
        }
        if (!EMAIL.matcher(email).matches()) {
            sendCors(exchange, 400, "{\"ok\":false,\"error\":\"Invalid email\"}");
            return;
        }
        if (!OTP.matcher(code).matches()) {
            sendCors(exchange, 400, "{\"ok\":false,\"error\":\"Invalid OTP code\"}");
            return;
        }
        if (isRateLimited(exchange, email)) {
            sendCors(exchange, 429, "{\"ok\":false,\"error\":\"Too many OTP requests. Please try again later.\"}");
            return;
        }

        try {
            sendOtpEmail(email, code, purpose.isBlank() ? "xác thực tài khoản" : purpose);
            sendCors(exchange, 200, "{\"ok\":true}");
        } catch (Exception exception) {
            System.err.println("Cannot send OTP email to " + email + ": " + exception.getMessage());
            String error = "Cannot send OTP email";
            sendCors(exchange, 500, "{\"ok\":false,\"error\":\"" + error + "\"}");
        }
    }

    private static boolean isRateLimited(HttpExchange exchange, String email) {
        String ip = exchange.getRemoteAddress() == null ? "unknown" : exchange.getRemoteAddress().getAddress().getHostAddress();
        String key = ip + "|" + email.toLowerCase();
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = RATE_LIMITS.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > RATE_LIMIT_WINDOW_MS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private static void sendOtpEmail(String to, String code, String purpose) throws Exception {
        String host = requireEnv("SMTP_HOST");
        String username = requireEnv("SMTP_USERNAME");
        String password = requireEnv("SMTP_PASSWORD");
        String from = env("SMTP_FROM", username);
        int port = intEnv("SMTP_PORT", 587);
        boolean startTls = Boolean.parseBoolean(env("SMTP_STARTTLS", "true"));

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("Mã OTP " + purpose);
        message.setText("Mã OTP của bạn là: " + code
                + "\n\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này cho người khác.");
        Transport.send(message);
    }

    private static void sendCors(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS, GET");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String jsonField(String json, String field) {
        Pattern pattern = Pattern.compile(String.format(JSON_FIELD.pattern(), Pattern.quote(field)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
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

    private static String cleanPurpose(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60);
    }
}
