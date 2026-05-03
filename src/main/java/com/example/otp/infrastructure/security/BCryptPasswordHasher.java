package com.example.otp.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.otp.application.port.PasswordHasher;

public final class BCryptPasswordHasher implements PasswordHasher {

    @Override
    public String hash(String plainPassword) {
        return BCrypt.withDefaults()
                .hashToString(12, plainPassword.toCharArray());
    }

    @Override
    public boolean verify(String plainPassword, String passwordHash) {
        return BCrypt.verifyer()
                .verify(plainPassword.toCharArray(), passwordHash)
                .verified;
    }
}