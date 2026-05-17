package com.example.cuoiky_qllichhoctap.model;

public class AuthUser {
    private final String email;
    private final String name;
    private final boolean verified;
    private final long createdAt;
    private final long updatedAt;

    public AuthUser(String email, String name, boolean verified, long createdAt, long updatedAt) {
        this.email = email;
        this.name = name;
        this.verified = verified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean isVerified() {
        return verified;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
