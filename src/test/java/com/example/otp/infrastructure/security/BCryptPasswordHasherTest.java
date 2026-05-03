package com.example.otp.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();

    @Test
    void hashReturnsNotPlainPassword() {
        String hash = passwordHasher.hash("user123");

        assertNotNull(hash);
        assertNotEquals("user123", hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void verifyReturnsTrueForCorrectPassword() {
        String hash = passwordHasher.hash("user123");

        assertTrue(passwordHasher.verify("user123", hash));
    }

    @Test
    void verifyReturnsFalseForWrongPassword() {
        String hash = passwordHasher.hash("user123");

        assertFalse(passwordHasher.verify("wrong-password", hash));
    }
}