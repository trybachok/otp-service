package com.example.otp.application.port;

import java.util.UUID;

public final class OtpMessage {

    private final UUID userId;
    private final String operationId;
    private final String code;

    public OtpMessage(UUID userId, String operationId, String code) {
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
    }

    public UUID userId() {
        return userId;
    }

    public String operationId() {
        return operationId;
    }

    public String code() {
        return code;
    }
}