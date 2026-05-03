package com.example.otp.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public final class AdminUserResponse {

    private final UUID id;
    private final String login;
    private final String role;
    private final Instant createdAt;

    public AdminUserResponse(UUID id, String login, String role, Instant createdAt) {
        this.id = id;
        this.login = login;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}