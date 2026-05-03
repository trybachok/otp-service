package com.example.otp.application.security;

import com.example.otp.domain.model.Role;

import java.util.UUID;

public final class AuthenticatedUser {

    private final UUID userId;
    private final String login;
    private final Role role;

    public AuthenticatedUser(UUID userId, String login, Role role) {
        this.userId = userId;
        this.login = login;
        this.role = role;
    }

    public UUID userId() {
        return userId;
    }

    public String login() {
        return login;
    }

    public Role role() {
        return role;
    }
}