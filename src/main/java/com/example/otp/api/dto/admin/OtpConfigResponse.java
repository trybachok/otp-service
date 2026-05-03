package com.example.otp.api.dto.admin;

import java.time.Instant;

public final class OtpConfigResponse {

    private final int codeLength;
    private final int ttlSeconds;
    private final Instant updatedAt;

    public OtpConfigResponse(int codeLength, int ttlSeconds, Instant updatedAt) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
        this.updatedAt = updatedAt;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}