package com.example.otp.api.dto.auth;

public final class LoginRequest {

    private String login;
    private String password;

    public LoginRequest() {
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}