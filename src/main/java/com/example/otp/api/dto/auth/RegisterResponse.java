package com.example.otp.api.dto.auth;

import java.util.UUID;

public final class RegisterResponse {

    private final UUID id;
    private final String login;
    private final String role;

    public RegisterResponse(UUID id, String login, String role) {
        this.id = id;
        this.login = login;
        this.role = role;
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
}