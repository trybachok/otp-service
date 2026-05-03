package com.example.otp.application.port;

public interface PasswordHasher {

    String hash(String plainPassword);

    boolean verify(String plainPassword, String passwordHash);
}