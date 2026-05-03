package com.example.otp.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String login;
    private final String passwordHash;
    private final Role role;
    private final Instant createdAt;

    public User(
            UUID id,
            String login,
            String passwordHash,
            Role role,
            Instant createdAt
    ) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public String login() {
        return login;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }
}