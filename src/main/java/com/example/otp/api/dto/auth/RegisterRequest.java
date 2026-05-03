package com.example.otp.api.dto.auth;

public final class RegisterRequest {

    private String login;
    private String password;
    private String role;

    public RegisterRequest() {
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}