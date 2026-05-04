package com.example.otp.api.dto.otp;

import java.time.Instant;
import java.util.UUID;

public final class GenerateOtpResponse {

    private final UUID otpId;
    private final UUID operationDbId;
    private final String operationId;
    private final String status;
    private final Instant expiresAt;

    public GenerateOtpResponse(
            UUID otpId,
            UUID operationDbId,
            String operationId,
            String status,
            Instant expiresAt
    ) {
        this.otpId = otpId;
        this.operationDbId = operationDbId;
        this.operationId = operationId;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getOtpId() {
        return otpId;
    }

    public UUID getOperationDbId() {
        return operationDbId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}