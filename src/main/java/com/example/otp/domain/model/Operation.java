package com.example.otp.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Operation {

    private final UUID id;
    private final UUID userId;
    private final String operationId;
    private final String description;
    private final Instant createdAt;

    public Operation(
            UUID id,
            UUID userId,
            String operationId,
            String description,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String operationId() {
        return operationId;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }
}