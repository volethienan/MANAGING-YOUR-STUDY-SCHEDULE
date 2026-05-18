package com.example.cuoiky_qllichhoctap.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;

import com.example.cuoiky_qllichhoctap.model.AuthUser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

public class AuthRepository extends SQLiteOpenHelper {
    private static final String DB_NAME = "study_auth.db";
    private static final int DB_VERSION = 1;
    private static final String PREFS = "study_auth_session";
    private static final String KEY_SESSION_EMAIL = "session_email";
    private static final long OTP_TTL_MS = 5L * 60L * 1000L;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private final SharedPreferences sessionPrefs;
    private final SecureRandom random = new SecureRandom();

    public AuthRepository(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        sessionPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (email TEXT PRIMARY KEY, name TEXT NOT NULL, password_hash TEXT NOT NULL, salt TEXT NOT NULL, verified INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE otp_codes (email TEXT NOT NULL, purpose TEXT NOT NULL, code_hash TEXT NOT NULL, expires_at INTEGER NOT NULL, created_at INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(email, purpose))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (email TEXT PRIMARY KEY, name TEXT NOT NULL, password_hash TEXT NOT NULL, salt TEXT NOT NULL, verified INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS otp_codes (email TEXT NOT NULL, purpose TEXT NOT NULL, code_hash TEXT NOT NULL, expires_at INTEGER NOT NULL, created_at INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(email, purpose))");
    }

    public String beginRegistration(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        AuthUser existing = findUser(normalizedEmail);
        if (existing != null && existing.isVerified()) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }
        long now = System.currentTimeMillis();
        String salt = randomToken(18);
        ContentValues values = new ContentValues();
        values.put("email", normalizedEmail);
        values.put("name", name.trim());
        values.put("password_hash", hashPassword(password, salt));
        values.put("salt", salt);
        values.put("verified", 0);
        values.put("created_at", existing == null ? now : existing.getCreatedAt());
        values.put("updated_at", now);
        getWritableDatabase().insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return createOtp(normalizedEmail, "register");
    }

    public AuthUser verifyRegistrationOtp(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        verifyOtp(normalizedEmail, "register", code);
        ContentValues values = new ContentValues();
        values.put("verified", 1);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("users", values, "email = ?", new String[]{normalizedEmail});
        deleteOtp(normalizedEmail, "register");
        setCurrentEmail(normalizedEmail);
        AuthUser user = findUser(normalizedEmail);
        if (user == null) {
            throw new IllegalStateException("Không tìm thấy tài khoản sau xác thực");
        }
        return user;
    }

    public String resendRegistrationOtp(String email) {
        AuthUser user = findUser(email);
        if (user == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản cần xác thực");
        }
        if (user.isVerified()) {
            throw new IllegalArgumentException("Tài khoản đã xác thực");
        }
        return createOtp(user.getEmail(), "register");
    }

    public AuthUser login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        try (Cursor cursor = getReadableDatabase().query("users", new String[]{"email", "name", "password_hash", "salt", "verified", "created_at", "updated_at"}, "email = ?", new String[]{normalizedEmail}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("Tài khoản không tồn tại");
            }
            if (cursor.getInt(4) != 1) {
                throw new IllegalArgumentException("Tài khoản chưa xác thực OTP");
            }
            if (!cursor.getString(2).equals(hashPassword(password, cursor.getString(3)))) {
                throw new IllegalArgumentException("Mật khẩu không đúng");
            }
            AuthUser user = userFromCursor(cursor);
            setCurrentEmail(user.getEmail());
            return user;
        }
    }

    public String beginPasswordReset(String email) {
        AuthUser user = findUser(email);
        if (user == null || !user.isVerified()) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản đã xác thực");
        }
        return createOtp(user.getEmail(), "reset");
    }

    public void resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        verifyOtp(normalizedEmail, "reset", code);
        String salt = randomToken(18);
        ContentValues values = new ContentValues();
        values.put("password_hash", hashPassword(newPassword, salt));
        values.put("salt", salt);
        values.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("users", values, "email = ?", new String[]{normalizedEmail});
        deleteOtp(normalizedEmail, "reset");
    }

    public AuthUser getCurrentUser() {
        String email = sessionPrefs.getString(KEY_SESSION_EMAIL, "");
        if (email.isEmpty()) {
            return null;
        }
        AuthUser user = findUser(email);
        if (user == null || !user.isVerified()) {
            logout();
            return null;
        }
        return user;
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public void logout() {
        sessionPrefs.edit().remove(KEY_SESSION_EMAIL).apply();
    }

    public AuthUser findUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        try (Cursor cursor = getReadableDatabase().query("users", new String[]{"email", "name", "verified", "created_at", "updated_at"}, "email = ?", new String[]{normalizedEmail}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new AuthUser(cursor.getString(0), cursor.getString(1), cursor.getInt(2) == 1, cursor.getLong(3), cursor.getLong(4));
        }
    }

    private String createOtp(String email, String purpose) {
        String code = String.format(Locale.US, "%06d", random.nextInt(1_000_000));
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("purpose", purpose);
        values.put("code_hash", hashOtp(email, purpose, code));
        values.put("expires_at", now + OTP_TTL_MS);
        values.put("created_at", now);
        values.put("attempts", 0);
        getWritableDatabase().insertWithOnConflict("otp_codes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return code;
    }

    private void verifyOtp(String email, String purpose, String code) {
        try (Cursor cursor = getReadableDatabase().query("otp_codes", new String[]{"code_hash", "expires_at", "attempts"}, "email = ? AND purpose = ?", new String[]{email, purpose}, null, null, null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("Mã OTP không tồn tại");
            }
            long expiresAt = cursor.getLong(1);
            int attempts = cursor.getInt(2);
            if (System.currentTimeMillis() > expiresAt) {
                deleteOtp(email, purpose);
                throw new IllegalArgumentException("Mã OTP đã hết hạn");
            }
            if (attempts >= MAX_OTP_ATTEMPTS) {
                deleteOtp(email, purpose);
                throw new IllegalArgumentException("Bạn đã nhập sai OTP quá nhiều lần");
            }
            if (!cursor.getString(0).equals(hashOtp(email, purpose, code.trim()))) {
                ContentValues values = new ContentValues();
                values.put("attempts", attempts + 1);
                getWritableDatabase().update("otp_codes", values, "email = ? AND purpose = ?", new String[]{email, purpose});
                throw new IllegalArgumentException("Mã OTP chưa đúng");
            }
        }
    }

    private void deleteOtp(String email, String purpose) {
        getWritableDatabase().delete("otp_codes", "email = ? AND purpose = ?", new String[]{email, purpose});
    }

    private AuthUser userFromCursor(Cursor cursor) {
        return new AuthUser(cursor.getString(0), cursor.getString(1), cursor.getInt(4) == 1, cursor.getLong(5), cursor.getLong(6));
    }

    private void setCurrentEmail(String email) {
        sessionPrefs.edit().putString(KEY_SESSION_EMAIL, email).apply();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private String hashPassword(String password, String salt) {
        return sha256(salt + ":" + password);
    }

    private String hashOtp(String email, String purpose, String code) {
        return sha256(email + ":" + purpose + ":" + code);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể mã hóa dữ liệu", exception);
        }
    }

    private String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return Base64.encodeToString(buffer, Base64.NO_WRAP);
    }
}
