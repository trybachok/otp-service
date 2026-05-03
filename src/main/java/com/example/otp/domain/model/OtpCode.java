package com.example.otp.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class OtpCode {

    private final UUID id;
    private final UUID userId;
    private final UUID operationId;
    private final String codeHash;
    private final OtpStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant usedAt;

    public OtpCode(
            UUID id,
            UUID userId,
            UUID operationId,
            String codeHash,
            OtpStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant usedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.codeHash = codeHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.usedAt = usedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID operationId() {
        return operationId;
    }

    public String codeHash() {
        return codeHash;
    }

    public OtpStatus status() {
        return status;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant usedAt() {
        return usedAt;
    }
}