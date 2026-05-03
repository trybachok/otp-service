package com.example.otp.domain.model;

import java.time.Instant;

public final class OtpConfig {

    private final int id;
    private final int codeLength;
    private final int ttlSeconds;
    private final Instant updatedAt;

    public OtpConfig(
            int id,
            int codeLength,
            int ttlSeconds,
            Instant updatedAt
    ) {
        this.id = id;
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
        this.updatedAt = updatedAt;
    }

    public int id() {
        return id;
    }

    public int codeLength() {
        return codeLength;
    }

    public int ttlSeconds() {
        return ttlSeconds;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}